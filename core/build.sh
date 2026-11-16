#!/bin/sh
# Build the vendored 6.1 core with the Android NDK.
#
#   ./build.sh [OUT] [PROFILE]
#
# PROFILE is a profiles.h selector suffix: pd2571 (default) or qemu_iqoo12,
# and is passed as -DIC_PROFILE_<PROFILE upper-cased>.
#
# API 29 is required: NDK r27 with API 24 emits a TLS segment aligned to 8,
# which the arm64 bionic linker refuses at exec time ("executable's TLS
# segment is underaligned ... needs to be at least 64").
set -eu
ROOT=$(cd "$(dirname "$0")" && pwd)
OUT=${1:-$ROOT/t98260}
PROFILE=$(echo "${2:-pd2571}" | tr '[:lower:]' '[:upper:]')
# NDK resolution: an explicit CC wins, then ANDROID_NDK_HOME, then the
# default SDK location ($LOCALAPPDATA/Android/Sdk on Windows, path-
# normalized for git-bash; $HOME/AppData/Local/Android/Sdk elsewhere).
# The prebuilt host tag comes from uname so Linux hosts (e.g. CI) work.
if [ -z "${CC:-}" ]; then
    NDK=${ANDROID_NDK_HOME:-}
    if [ -z "$NDK" ]; then
        NDK=${LOCALAPPDATA:-$HOME/AppData/Local}/Android/Sdk/ndk/27.1.12297006
        if [ -n "${LOCALAPPDATA:-}" ] && command -v cygpath > /dev/null 2>&1; then
            NDK=$(cygpath -u "$NDK")
        fi
    fi
    case "$(uname -s)" in
        Linux*)  TAG=linux-x86_64 ;;
        Darwin*) TAG=darwin-$(uname -m) ;;
        *)       TAG=windows-x86_64 ;;
    esac
    CLANG=$NDK/toolchains/llvm/prebuilt/$TAG/bin/aarch64-linux-android29-clang
    if [ ! -x "$CLANG" ]; then
        echo "error: NDK clang not found under $NDK - set ANDROID_NDK_HOME or CC" >&2
        exit 1
    fi
    CC=$CLANG
fi
"$CC" -O2 -static -pthread -Wall -Wextra -DIC_PROFILE_"$PROFILE" \
    -DIC_SYMS_HAVE_ZLIB "$ROOT/timer98260.c" "$ROOT/ic_syms.c" -lz -o "$OUT"
echo "built $OUT (profile $PROFILE)"
