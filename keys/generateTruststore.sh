#!/bin/bash
# this script imports the root certificates for this installation

rm -f ../config/cacerts.jks

function import(){
  keytool -importcert -keystore ../config/cacerts.jks -file "$1.crt" -alias own -storepass "changeit" -alias "$1" $2
}

import root -noprompt
import assured
import unassured

keytool -list -keystore ../config/cacerts.jks -storepass "changeit"
