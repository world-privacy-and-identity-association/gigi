# this script generates a simple self-signed keypair

openssl genrsa -out jetty.key 4096
openssl req -new -key jetty.key -out jetty.csr -subj "/CN=localhost" -config selfsign.config
openssl x509 -req -days 365 -in jetty.csr -signkey jetty.key -out jetty.crt
openssl pkcs12 -inkey jetty.key -in jetty.crt -export -passout pass: -out ../../config/keystore.pkcs12
