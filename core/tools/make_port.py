#!/usr/bin/env python3
"""make_port.py — one-stop helper for Path B ports (see docs/porting.md).

Consolidates the port flow that used to be spread over gen_anchors.py,
verify_tables.py and hand-edited header rows into a single entry point.
All paths are repo-relative; no hardcoded author paths.

Subcommands:
  offsets IMAGE              BTF offsets + kallsyms via derive_offsets.py
  row IMAGE --kernel BANNER  resolve the IC_ANCHOR_N links from the Image's
      [--models IDS] [--version VER] [--table NAME]
                             embedded kallsyms; print the anchors.h row and
                             the extra_syms.h row, fail-closed
  selftest                   deterministic offline self check

Fail-closed rules (a wrong row corrupts kernels instead of failing
safely — never weaken them):
  - the four plan symbols (__tracepoint_sys_exit, selinux_state,
    init_user_ns, init_cred) MUST resolve, or the row is refused;
  - root_user / init_ucounts / init_groups MUST resolve for the
    fake-cred row (their deltas from init_user_ns differ between kernel
    families — never guess them).
"""
import re
import subprocess
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

HERE = Path(__file__).resolve().parent          # core/tools
CORE = HERE.parent                              # core
REPO = CORE.parent                              # repository root

PLAN_SYMBOLS = ("__tracepoint_sys_exit", "selinux_state", "init_user_ns", "init_cred")
CRED_ORDER = ("root_user", "init_ucounts", "init_groups")
KNOWN_DELTAS = {
    "vivo 6.1.145 (verified builds)": (0x298, 0x1B48, 0x15B8),
    "ctwo GKI 6.1.157 (Edge 50 Ultra)": (0x290, 0x1B18, 0x15B0),
}


def abort(msg):
    print("[make_port] ABORT: %s" % msg)
    raise SystemExit(1)


def load_anchor_names():
    p = CORE / "anchors.h"
    if not p.exists():
        abort("core/anchors.h not found (run from core/tools/).")
    text = p.read_text(encoding="utf-8", errors="replace")
    m = re.search(r"#define IC_ANCHOR_N (\d+)", text)
    if not m:
        abort("IC_ANCHOR_N not found in core/anchors.h.")
    want = int(m.group(1))
    m = re.search(r"ic_anchor_names\[IC_ANCHOR_N\] = \{(.*?)\n\};", text, re.S)
    if not m:
        abort("ic_anchor_names not found in core/anchors.h.")
    names = re.findall(r'"([^"]+)"', m.group(1))
    if len(names) != want:
        abort("ic_anchor_names has %d entries, IC_ANCHOR_N says %d." % (len(names), want))
    if tuple(names[-4:]) != PLAN_SYMBOLS:
        abort("last four anchor names are not the plan symbols: %r" % (names[-4:],))
    return names


def load_kallsyms(image):
    mod_path = REPO / "tool" / "kallsyms_py.py"
    if not mod_path.exists():
        abort("tool/kallsyms_py.py not found next to the repo root.")
    import importlib.util
    spec = importlib.util.spec_from_file_location("kallsyms_py", mod_path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    with open(image, "rb") as f:
        _entries, syms = mod.recover(f.read(), None)
    return syms


def resolve(syms, name):
    v = syms.get(name)
    return sorted(v)[0] if v else None


def build_links(syms, names):
    """Resolve every anchor name; non-plan misses become 0ULL (disabled
    anchor), plan misses abort."""
    links = []
    zeroed = []
    for n in names:
        a = resolve(syms, n)
        if a is None:
            links.append(0)
            zeroed.append(n)
        else:
            links.append(a)
    missing_plan = [n for n in PLAN_SYMBOLS if resolve(syms, n) is None]
    if missing_plan:
        abort("plan symbols missing from the Image's kallsyms: %s"
              % ", ".join(missing_plan))
    return links, zeroed


def build_cred(syms, table):
    """Resolve the fake-cred row; any miss aborts (deltas differ per
    family, guessing them corrupts kernels)."""
    ns = resolve(syms, "init_user_ns")
    if ns is None:
        abort("init_user_ns missing from the Image's kallsyms.")
    out = {}
    for n in CRED_ORDER:
        a = resolve(syms, n)
        if a is None:
            abort("%s missing from the Image's kallsyms - do not guess "
                  "its delta from init_user_ns." % n)
        out[n] = a
    deltas = tuple(out[n] - ns for n in CRED_ORDER)
    return out, deltas


def emit_anchors_row(kernel, models, version, table, links):
    ms = '"%s"' % models if models else "NULL"
    vs = '"%s"' % version if version else "NULL"
    lines = ['    { "%s", %s, %s, "%s", {' % (kernel, ms, vs, table)]
    for i in range(0, len(links), 8):
        vals = ", ".join(("0x%xULL" % a) if a else "0ULL" for a in links[i:i + 8])
        lines.append("        %s," % vals)
    lines.append("    } },")
    return "\n".join(lines)


def emit_cred_row(table, cred):
    return ('    { "%s", 0x%xULL, 0x%xULL,\n      0x%xULL },'
            % (table, cred["root_user"], cred["init_ucounts"], cred["init_groups"]))


def family_for(deltas):
    for fam, d in KNOWN_DELTAS.items():
        if deltas == d:
            return fam
    return None


def cmd_offsets(image):
    script = HERE / "derive_offsets.py"
    if not script.exists():
        abort("derive_offsets.py not found.")
    rc = subprocess.call([sys.executable, str(script), str(image)])
    raise SystemExit(rc)


def cmd_row(image, kernel, models, version, table):
    if not kernel:
        abort("--kernel is required (the PROFILE_KERNEL_MATCH banner).")
    names = load_anchor_names()
    syms = load_kallsyms(image)
    links, zeroed = build_links(syms, names)
    cred, deltas = build_cred(syms, table)
    fam = family_for(deltas)

    print("[make_port] anchors.h row (IC_ANCHOR_N=%d, %d resolved, %d zeroed):"
          % (len(names), len(names) - len(zeroed), len(zeroed)))
    if zeroed:
        print("[make_port] zeroed (anchor disabled): %s" % ", ".join(zeroed))
    print(emit_anchors_row(kernel, models, version, table, links))
    print()
    print("[make_port] extra_syms.h row (deltas from init_user_ns: %s):"
          % ", ".join("%s=+0x%x" % (n, d) for n, d in zip(CRED_ORDER, deltas)))
    print(emit_cred_row(table, cred))
    if fam:
        print("[make_port] delta family: matches %s." % fam)
    else:
        print("[make_port] delta family: NO known family matches - eyeball the "
              "deltas against measured symbols before shipping.")
    print()
    print("[make_port] next: add the #elif defined(IC_PROFILE_...) block with "
          "PROFILE_KERNEL_MATCH \"%s\" to core/profiles.h," % kernel)
    print("[make_port] then build with core/build.sh and run the write-verify "
          "chain on the device (docs/porting.md, Path B step 5).")


def cmd_selftest():
    names = load_anchor_names()
    n = len(names)
    base = 0xFFFFFFC008000000
    mock = {}
    for i, nm in enumerate(names):
        mock[nm] = {base + 0x100 * (i + 1)}
    ns = 0xFFFFFFC00A0304F0
    mock["init_user_ns"] = {ns}
    mock["root_user"] = {ns + 0x290}
    mock["init_ucounts"] = {ns + 0x1B18}
    mock["init_groups"] = {ns + 0x15B0}

    checks = 0

    # 1: full resolve + row parses back
    links, zeroed = build_links(mock, names)
    assert not zeroed and len(links) == n
    row = emit_anchors_row("6.1.157-android14-11-gb3d4cdadf339-ab16160610",
                           None, "36e2a-a4560", "test_42_links", links)
    got = re.findall(r"(?:0x[0-9a-f]+|0)ULL", row)
    assert len(got) == n, "row does not carry %d links" % n
    assert got[-1] == "0x%xULL" % links[-1]
    assert '    { "6.1.157-android14-11-gb3d4cdadf339-ab16160610", NULL, "36e2a-a4560", "test_42_links", {' in row
    checks += 1

    # 2: missing plan symbol aborts
    mock2 = {k: set(v) for k, v in mock.items() if k != "selinux_state"}
    try:
        build_links(mock2, names)
    except SystemExit as e:
        assert e.code == 1
    else:
        raise AssertionError("missing plan symbol did not abort")
    checks += 1

    # 3: missing non-plan symbol zeroes out and reports
    mock3 = {k: set(v) for k, v in mock.items() if k != names[0]}
    links3, zeroed3 = build_links(mock3, names)
    assert links3[0] == 0 and zeroed3 == [names[0]]
    checks += 1

    # 4: cred row + family check
    cred, deltas = build_cred(mock, "test_42_links")
    assert deltas == (0x290, 0x1B18, 0x15B0)
    assert family_for(deltas) == "ctwo GKI 6.1.157 (Edge 50 Ultra)"
    crow = emit_cred_row("test_42_links", cred)
    assert crow == ('    { "test_42_links", 0xffffffc00a030780ULL, '
                    '0xffffffc00a032008ULL,\n      0xffffffc00a031aa0ULL },')
    checks += 1

    # 5: missing cred symbol aborts
    mock5 = {k: set(v) for k, v in mock.items() if k != "root_user"}
    try:
        build_cred(mock5, "test_42_links")
    except SystemExit as e:
        assert e.code == 1
    else:
        raise AssertionError("missing cred symbol did not abort")
    checks += 1

    print("[make_port] selftest OK: %d checks (IC_ANCHOR_N=%d, fail-closed "
          "on plan and cred symbols)." % (checks, n))
    return 0


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    cmd, args = argv[1], argv[2:]
    if cmd == "selftest":
        return cmd_selftest()
    if cmd == "offsets":
        if len(args) != 1:
            print("usage: make_port.py offsets IMAGE")
            return 2
        cmd_offsets(args[0])
    if cmd == "row":
        kernel = models = version = table = None
        it = iter(range(len(args)))
        i = 0
        while i < len(args):
            a = args[i]
            if a == "--kernel" and i + 1 < len(args):
                kernel = args[i + 1]; i += 2
            elif a == "--models" and i + 1 < len(args):
                models = args[i + 1]; i += 2
            elif a == "--version" and i + 1 < len(args):
                version = args[i + 1]; i += 2
            elif a == "--table" and i + 1 < len(args):
                table = args[i + 1]; i += 2
            else:
                image = a; i += 1
        try:
            image
        except NameError:
            print("usage: make_port.py row IMAGE --kernel BANNER [--models IDS] "
                  "[--version VER] [--table NAME]")
            return 2
        cmd_row(image, kernel, models, version, table or "new_links")
        return 0
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
