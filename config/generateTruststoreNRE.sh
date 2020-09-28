#!/bin/bash
# this script imports the root certs into a Java key store
# additionally it can generate the certs for gigi, if none are provided and the CA-keys are available in the config folder for the Simple Signer
# This script is generally only intended for development purposes.

rm -f cacerts.jks

function import(){
  name=$1
  keytool -importcert -keystore ../config/cacerts.jks -file "$1.crt" -storepass "changeit" -alias "$(basename $name)" $2
}

function importP(){
 keytool -importkeystore -srckeystore "$1" -noprompt -destkeystore keystore.pkcs12 -srcstoretype pkcs12 -deststoretype pkcs12 -deststorepass changeit -srcstorepass changeit
}

import ca/root -noprompt
import ca/assured
import ca/unassured
import ca/orga
import ca/orgaSign
import ca/codesign

for i in ca/*_*_*; do
  import ${i%.crt}
done

# Generate Gigi certificates manually
cabasename=assured
caname=${cabasename}_$(date +%Y)_1
ca=../signer/ca/$caname/ca
if [[ -f "$ca.key" ]] && ! [[ -f keystore.pkcs12 ]]; then
    if [[ -f serial_base ]]; then
        serial_base=$(< serial_base)
    else
        serial_base=100000
    fi
    serial_base=$((serial_base + 1))
    printf '%d\n' "$serial_base" >| serial_base
    # when the domain is provided externally as environment variable, use it and do not prompt for it.
    [[ -z $DOMAIN ]] && read -rp "I need to generate gigi-certificates. I need your base domain: " DOMAIN
    # Assuming we have access to the CA-keys we generate two certificates and present them to gigi
    # One to be used for all 4 https domains and one as email certificate.

    # Generate two keys and certs requests. The CN of the SSL-server cert doesn't really matter, as we use subject alt names anyways.
    openssl req -newkey rsa:2048 -keyout www.key -out www.csr -nodes -subj "/CN=gigi server certificate"
    openssl req -newkey rsa:2048 -keyout mail.key -out mail.csr -nodes -subj "/CN=gigi system"

    # Sign the two requests with the keys in the config of the simple signer. Use the serial_base with extensions 1 and 2. These serials are long enough to probably not collide with the "simple signer"
    openssl x509 -req -days 365 -in www.csr -out www.crt -CA $ca.crt -CAkey $ca.key -set_serial ${serial_base}1 -extfile <(printf '[ext]\nsubjectAltName=DNS:www.%s,DNS:secure.%s,DNS:static.%s,DNS:api.%s\nbasicConstraints=CA:FALSE\nextendedKeyUsage=serverAuth\nkeyUsage=digitalSignature,keyEncipherment\n' "$DOMAIN" "$DOMAIN" "$DOMAIN" "$DOMAIN") -extensions ext
    openssl x509 -req -days 365 -in mail.csr -out mail.crt -CA $ca.crt -CAkey $ca.key -set_serial ${serial_base}2 -extfile <(printf '[ext]\nsubjectAltName=email:support@%s\nbasicConstraints=CA:FALSE\nextendedKeyUsage=emailProtection\nkeyUsage=digitalSignature,keyEncipherment\n' "$DOMAIN") -extensions ext

    # Store the webserver cert in 4 different pkcs12-keystores to have different "key aliases" and import them all into the "keystore.pkcs12" using the "importP"-method
    for t in www api secure static; do
        # concatenate private key and certificate chain together
        # and filter out comments from .crt files with "openssl x509"
        # before feeding them into "openssl pkcs12"
        cat www.key www.crt ca/$caname.crt ca/$cabasename.crt ca/root.crt |\
            (openssl pkey; for i in {1..4}; do openssl x509; done) |\
            openssl pkcs12 -export -out $t.pkcs12 -name "$t" -passout pass:changeit
        importP "$t.pkcs12"
    done
    # and finally add the mail certificate
    cat mail.key mail.crt ca/$caname.crt ca/$cabasename.crt ca/root.crt |\
        (openssl pkey; for i in {1..4}; do openssl x509; done) |\
        openssl pkcs12 -export -out mail.pkcs12 -name "mail" -passout pass:changeit
    importP "mail.pkcs12"
fi
keytool -list -keystore ../config/cacerts.jks -storepass "changeit"
