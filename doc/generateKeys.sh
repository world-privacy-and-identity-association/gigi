# this script generates a simple self-signed keypair

openssl genrsa -des3 -passout pass:1 -out jetty.pass.key 2048
openssl rsa -passin pass:1 -in jetty.pass.key -out jetty.key
rm jetty.pass.key
openssl req -new -key jetty.key -out jetty.csr -subj "/CN=jetty" -config selfsign.config
openssl x509 -req -days 365 -in jetty.csr -signkey jetty.key -out jetty.crt
rm jetty.csr
openssl pkcs12 -inkey jetty.key -in jetty.crt -passout pass: -export -out ../config/keystore.pkcs12
rm jetty.key
rm jetty.crt
