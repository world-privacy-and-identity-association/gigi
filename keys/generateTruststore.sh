#!/bin/sh
# this script imports the cacert root certs

#wget -N http://www.cacert.org/certs/root.crt
#wget -N http://www.cacert.org/certs/class3.crt

#keytool -importcert -keystore ../config/cacerts.jks -file root.crt -alias root -storepass "changeit" $1
#keytool -importcert -keystore ../config/cacerts.jks -file class3.crt -alias class3 -storepass "changeit" $1
keytool -importcert -keystore ../config/cacerts.jks -file testca.crt -alias own -storepass "changeit" $1

keytool -list -keystore ../config/cacerts.jks -storepass "changeit"
