#!/bin/bash
[[ -d JSON-java ]] || git clone https://github.com/stleary/JSON-java JSON-java
base=$(dirname $0)/../../..

cd JSON-java
if [[ "$(git rev-parse refs/tags/20160810)" != "37582a44ada8e5bbe6d987f41d3f834aaf28934c" ]]; then
    echo "tag 20160810 has incorrect checksum" >&2
    exit 1
fi

git checkout refs/tags/20160810
mkdir -p $base/lib/json/org/json
cp -R JSON* $base/lib/json/org/json
rm $base/lib/json/org/json/JSONML.java
# $base/lib/json/org/json/JSONPointer*

