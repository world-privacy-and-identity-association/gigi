# this script generates a simple self-signed keypair

wget http://www.cacert.org/certs/root.crt
wget http://www.cacert.org/certs/class3.crt

keytool -importcert -keystore ../config/cacerts.jks -file root.crt -alias root -storepass "changeit"
keytool -importcert -keystore ../config/cacerts.jks -file class3.crt -alias class3 -storepass "changeit"
rm root.crt
rm class3.crt

keytool -list -keystore ../config/cacerts.jks -storepass "changeit"
