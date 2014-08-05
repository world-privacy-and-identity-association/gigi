#!/bin/bash
# this script imports the cacert root certs

rm -f ../config/cacerts.jks

#wget -N http://www.cacert.org/certs/root.crt
#wget -N http://www.cacert.org/certs/class3.crt

#keytool -importcert -keystore ../config/cacerts.jks -file root.crt -alias root -storepass "changeit" $1
#keytool -importcert -keystore ../config/cacerts.jks -file class3.crt -alias class3 -storepass "changeit" $1

function import(){
  keytool -importcert -keystore ../config/cacerts.jks -file "$1.crt" -alias own -storepass "changeit" -alias "$1" $2
}

import root -noprompt
import assured
import unassured

keytool -list -keystore ../config/cacerts.jks -storepass "changeit"
