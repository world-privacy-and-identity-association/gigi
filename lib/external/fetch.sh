#!/bin/bash
wget -N "http://www.dnsjava.org/download/dnsjava-2.1.8.zip" "http://www.dnsjava.org/download/dnsjava-2.1.8.jar"
if ! sha256sum -c checksums.txt; then
    rm -vf dnsjava-2.1.8.zip dnsjava-2.1.8.jar
fi
