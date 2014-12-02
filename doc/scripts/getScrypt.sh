#!/bin/sh
git clone https://github.com/wg/scrypt.git
BASE=$(dirname $0)/../../..

cd scrypt
[ "$(git rev-parse refs/tags/1.4.0)" != "0675236370458e819ee21e4427c5f7f3f9485d33" ] && echo "SHA-Hash failed" && exit 1

git checkout 0675236370458e819ee21e4427c5f7f3f9485d33
mkdir -p $BASE/lib/scrypt/com/lambdaworks
cp -R src/main/java/com/lambdaworks/crypto $BASE/lib/scrypt/com/lambdaworks

mkdir -p $BASE/tests/com/lambdaworks/crypto/test
cp -R src/test/java/com/lambdaworks/crypto/test $BASE/tests/com/lambdaworks/crypto

cd ..
rm -Rf scrypt
