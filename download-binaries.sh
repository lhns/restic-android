#!/bin/bash

set -eo pipefail

RESTIC_VERSION=0.16.0
PROOT_VERSION=5.1.107-60
LIBTALLOC_VERSION=2.4.1

unpackDebDataFromUrl() {
  local url="$1"
  shift
  local tmpdir
  tmpdir="$(mktemp -d)"
  pushd "$tmpdir"
  curl -L -o package.deb "$url"
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
  
  curl -L -o "$target/libdata_restic.so" "https://github.com/restic/restic/releases/download/v${RESTIC_VERSION}/restic_${RESTIC_VERSION}_linux_${resticArch}.bz2"
  
  unpackProot() {
    pushd data/data/com.termux/files/usr
    mv bin/proot "$target/libdata_proot.so"
    mv libexec/proot/loader "$target/libdata_loader.so"
    if [[ -f libexec/proot/loader32 ]]; then
      mv libexec/proot/loader32 "$target/libdata_loader32.so"
    fi
    popd
  }
  unpackDebDataFromUrl "https://packages.termux.dev/apt/termux-main/pool/main/p/proot/proot_${PROOT_VERSION}_${packageArch}.deb" unpackProot
  
  unpackLibtalloc() {
    pushd data/data/com.termux/files/usr
    mv "$(readlink -f lib/libtalloc.so.2)" "$target/libdata_libtalloc.so.2.so"
    popd
  }
  unpackDebDataFromUrl "https://packages.termux.dev/apt/termux-main/pool/main/libt/libtalloc/libtalloc_${LIBTALLOC_VERSION}_${packageArch}.deb" unpackLibtalloc
}

downloadBinaries arm64 aarch64 arm64-v8a
downloadBinaries arm arm armeabi-v7a
downloadBinaries amd64 x86_64 x86_64
downloadBinaries 386 i686 x86
