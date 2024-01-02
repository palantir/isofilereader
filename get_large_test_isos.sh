#!/bin/bash

if [ ! -d "./test_isos" ]; then
    mkdir ./test_isos
fi

function downloadRocky() {
    echo "Downloading Rocky ISO"
    curl -L https://dl.rockylinux.org/vault/rocky/9.2/isos/x86_64/Rocky-9.2-x86_64-minimal.iso -o ./test_isos/rocky.iso
    if [ "$(uname -s)" == "Darwin" ]; then
        shasum -a 256 ./test_isos/rocky.iso
    else
        sha256sum ./test_isos/rocky.iso
    fi
    echo "should be: 06505828e8d5d052b477af5ce62e50b938021f5c28142a327d4d5c075f0670dc"
}

function downloadUbuntu() {
    echo "Downloading Ubuntu ISO"
    curl -L https://old-releases.ubuntu.com/releases/kinetic/ubuntu-22.10-live-server-amd64.iso -o ./test_isos/ubuntu.iso
    if [ "$(uname -s)" == "Darwin" ]; then
        shasum -a 256 ./test_isos/ubuntu.iso
    else
        sha256sum ./test_isos/ubuntu.iso
    fi
    echo "should be: 874452797430a94ca240c95d8503035aa145bd03ef7d84f9b23b78f3c5099aed"
}

function downloadWindows() {
    echo "Downloading Windows ISO"
    curl -L "https://go.microsoft.com/fwlink/p/?LinkID=2208844&clcid=0x409&culture=en-us&country=US" -o ./test_isos/windows.iso
    if [ "$(uname -s)" == "Darwin" ]; then
        shasum -a 256 ./test_isos/windows.iso
    else
        sha256sum ./test_isos/windows.iso
    fi
    echo "should be: ef7312733a9f5d7d51cfa04ac497671995674ca5e1058d5164d6028f0938d668"
    # https://download.microsoft.com/download/c/1/1/c11d2ca5-967c-45c0-bc7d-2d9ca3f1fe07/Windows10Enterprise22H2HashValues.pdf
}

# Rocky and the RHEL 8+ EL systems have been using 128+ character file names, I want to test against that.
# The ./base/repodata/* files will be that long
if [ ! -e "./test_isos/rocky.iso" ]; then
    downloadRocky
else
    if [ "$(uname -s)" == "Darwin" ]; then
        rockySha=$(shasum -a 256 ./test_isos/rocky.iso | awk '{print $1}')
    else
        rockySha=$(sha256sum ./test_isos/rocky.iso | awk '{print $1}')
    fi
    if [ "$rockySha" != "06505828e8d5d052b477af5ce62e50b938021f5c28142a327d4d5c075f0670dc" ]; then
        echo "rocky ISO sha256sum is not correct"
        rm -f ./test_isos/rocky.iso
        downloadRocky
    else
        echo "rocky ISO sha256sum is correct"
    fi
fi

# Ubuntu
if [ ! -e "./test_isos/ubuntu.iso" ]; then
    downloadUbuntu
else
    if [ "$(uname -s)" == "Darwin" ]; then
        ubuntuSha=$(shasum -a 256 ./test_isos/ubuntu.iso | awk '{print $1}')
    else
        ubuntuSha=$(sha256sum ./test_isos/ubuntu.iso | awk '{print $1}')
    fi
    if [ "$ubuntuSha" != "874452797430a94ca240c95d8503035aa145bd03ef7d84f9b23b78f3c5099aed" ]; then
        echo "ubuntu ISO sha256sum is not correct"
        rm -f ./test_isos/ubuntu.iso
        downloadUbuntu
    else
        echo "ubuntu ISO sha256sum is correct"
    fi
fi

# This downloads the latest enterprise preview of Windows 10, we really need any UDF image. Windows is the most popular.
# 19045.2006.220908-0225.22h2_release_svc_refresh_CLIENTENTERPRISEEVAL_OEMRET_x64FRE_en-us.iso
if [ ! -e "./test_isos/windows.iso" ]; then
    downloadWindows
else
    if [ "$(uname -s)" == "Darwin" ]; then
        windowsSha=$(shasum -a 256 ./test_isos/windows.iso | awk '{print $1}')
    else
        windowsSha=$(sha256sum ./test_isos/windows.iso | awk '{print $1}')
    fi
    if [ "$windowsSha" != "ef7312733a9F5d7d51cfa04ac497671995674ca5e1058d5164d6028f0938d668" ]; then
        echo "windows ISO sha256sum is not correct"
        rm -f ./test_isos/windows.iso
        downloadWindows
    else
        echo "windows ISO sha256sum is correct"
    fi
fi

# To read the linux files on Mac you need to run "dd conv=notrunc  bs=1k count=2 if=/dev/zero of=file.iso"
# This is because mac doesnt understand the boot loader section of the ISO and says its a unrecogized image
# This commands wipes the front then allows you to mount, this WILL MAKE IT NOT BOOT ANYMORE
