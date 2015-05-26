#!/bin/bash
# this script imports the cacert root certs

rm -f cacerts.jks

function import(){
  keytool -importcert -keystore ../config/cacerts.jks -file "$1.crt" -alias own -storepass "changeit" -alias "$1" $2
}

function importP(){
 keytool -importkeystore -srckeystore "$1" -noprompt -destkeystore keystore.pkcs12 -srcstoretype pkcs12 -deststoretype pkcs12 -deststorepass changeit -srcstorepass changeit
}

import ca/root -noprompt
import ca/assured
import ca/unassured

for i in ca/{,un}assured_*; do
  import ${i%.crt}
done

for i in keys/*.pkcs12; do
  importP $i
done

keytool -list -keystore ../config/cacerts.jks -storepass "changeit"
