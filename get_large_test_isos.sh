#!/bin/bash

if [ ! -d "./test_isos" ]; then
    mkdir ./test_isos
fi

# This downloads the latest enterprise preview of Windows 10, we really need any UDF image. Windows is the most popular.
if [ ! -e "./test_isos/windows.iso" ]; then
    curl https://go.microsoft.com/fwlink/p/\?LinkID\=2208844\&clcid\=0x409\&culture\=en-us\&country\=US -o ./test_isos/windows.iso
else
    echo "windows ISO downloaded"
fi

# Ubuntu
if [ ! -e "./test_isos/ubuntu.iso" ]; then
    curl https://releases.ubuntu.com/22.04.2/ubuntu-22.04.2-live-server-amd64.iso -o ./test_isos/ubuntu.iso
else
    echo "ubuntu ISO downloaded"
fi

# Rocky and the RHEL 8+ EL systems have been using 128+ character file names, I want to test against that.
# The ./base/repodata/* files will be that long
if [ ! -e "./test_isos/rocky.iso" ]; then
    curl https://download.rockylinux.org/pub/rocky/9/isos/x86_64/Rocky-9.2-x86_64-minimal.iso -o ./test_isos/rocky.iso
else
    echo "rocky ISO downloaded"
fi

# To read the linux files on Mac you need to run "dd conv=notrunc  bs=1k count=2 if=/dev/zero of=dd conv=notrunc  bs=1k count=2 if=/dev/zero of=file.iso"
# This is because mac doesnt understand the boot loader section of the ISO and says its a unrecogized image
# This commands wipes the front then allows you to mount, this WILL MAKE IT NOT BOOT ANYMORE