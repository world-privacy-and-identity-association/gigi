#!/bin/bash
externals=(
    'http://www.dnsjava.org/download/dnsjava-2.1.8.zip'
    'http://www.dnsjava.org/download/dnsjava-2.1.8.jar'
)
wget -N "${externals[@]}"
if ! sha256sum -c checksums.txt; then
    rm -vf $(basename -a "${externals[@]}")
fi
