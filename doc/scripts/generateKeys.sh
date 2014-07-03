#!/bin/sh
# this script generates a set of sample keys

rm -Rf *.csr *.crt *.key *.pkcs12 testca

openssl genrsa -out testca.key 4096
openssl req -new -key testca.key -out testca.csr -subj "/CN=local cacert-gigi testCA" -config selfsign.config
openssl x509 -req -days 365 -in testca.csr -signkey testca.key -out testca.crt

mkdir testca
mkdir testca/newcerts
echo 01 > testca/serial
touch testca/db
echo unique_subject = no >testca/db.attr

genserver(){

openssl genrsa -out $1.key 4096
openssl req -new -key $1.key -out $1.csr -subj "/CN=$1.cacert.local" -config selfsign.config
openssl ca -cert testca.crt -keyfile testca.key -in $1.csr -out $1.crt -days 356 -batch -config selfsign.config

openssl pkcs12 -inkey $1.key -in $1.crt -name $1 -export -passout pass: -out $1.pkcs12

keytool -importkeystore -noprompt -srckeystore $1.pkcs12 -destkeystore ../../config/keystore.pkcs12 -srcstoretype pkcs12 -deststoretype pkcs12 -srcstorepass "" -deststorepass ""

}

genserver www
genserver secure
genserver static
genserver api

keytool -list -keystore ../../config/keystore.pkcs12 -storetype pkcs12 -storepass ""
