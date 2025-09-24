#!/bin/bash

set -eo pipefail

RESTIC_VERSION=0.18.1

unpackDebDataFromUrl() {
  local url="$1"
  shift
  local tmpdir
  tmpdir="$(mktemp -d)"
  pushd "$tmpdir"
  curl -sSfLo package.deb "$url"
  ar -x package.deb
  xz -dc data.tar.xz | tar -x
  "$@"
  popd
  rm -Rf "$tmpdir"
}

downloadBinaries() {
  local resticArch="$1"
  local packageArch="$2"
  local androidArch="$3"
  
  local target="$(pwd)/app/src/main/jniLibs/$androidArch"
  mkdir -p "$target"

  local resticFile="restic_${RESTIC_VERSION}_linux_${resticArch}.bz2"
  echo "$resticFile"
  curl -sSfL "https://github.com/restic/restic/releases/download/v${RESTIC_VERSION}/$resticFile" | bzip2 -dc > "$target/libdata_restic.so"
  
  local prootFile
  prootFile="$(curl -sSf "https://packages.termux.dev/apt/termux-main/pool/main/p/proot/" | sed -En "s/.*?(proot_.*?_${packageArch}\\.deb).*/\\1/p")"
  echo "$prootFile"
  unpackProot() {
    pushd data/data/com.termux/files/usr
    mv bin/proot "$target/libdata_proot.so"
    mv libexec/proot/loader "$target/libdata_loader.so"
    if [[ -f libexec/proot/loader32 ]]; then
      mv libexec/proot/loader32 "$target/libdata_loader32.so"
    fi
    popd
  }
  unpackDebDataFromUrl "https://packages.termux.dev/apt/termux-main/pool/main/p/proot/$prootFile" unpackProot
  
  local liballocFile
  liballocFile="$(curl -sSf "https://packages.termux.dev/apt/termux-main/pool/main/libt/libtalloc/" | sed -En "s/.*?(libtalloc_.*?_${packageArch}\\.deb).*/\\1/p")"
  echo "$liballocFile"
  unpackLibtalloc() {
    pushd data/data/com.termux/files/usr
    mv "$(readlink -f lib/libtalloc.so.2)" "$target/libdata_libtalloc.so.2.so"
    popd
  }
  unpackDebDataFromUrl "https://packages.termux.dev/apt/termux-main/pool/main/libt/libtalloc/$liballocFile" unpackLibtalloc
}

downloadBinaries arm64 aarch64 arm64-v8a
downloadBinaries arm arm armeabi-v7a
downloadBinaries amd64 x86_64 x86_64
downloadBinaries 386 i686 x86
