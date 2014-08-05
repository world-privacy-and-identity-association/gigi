#!/bin/sh
# this script generates a set of sample keys
DOMAIN="cacert.local"
KEYSIZE=4096
PRIVATEPW="changeit"

[ -f config ] && . ./config


rm -Rf *.csr *.crt *.key *.pkcs12 *.ca *.crl


####### create various extensions files for the various certificate types ######
cat <<TESTCA > test_ca.cnf
subjectKeyIdentifier = hash
#extendedKeyUsage = critical
basicConstraints = CA:true
keyUsage = digitalSignature, nonRepudiation, keyCertSign, cRLSign
TESTCA

cat <<TESTCA > test_subca.cnf
subjectKeyIdentifier = hash
#extendedKeyUsage = critical,
basicConstraints = CA:true
keyUsage = digitalSignature, nonRepudiation, keyCertSign, cRLSign
TESTCA

cat <<TESTCA > test_req.cnf
basicConstraints = critical,CA:false
keyUsage = keyEncipherment, digitalSignature
extendedKeyUsage=serverAuth
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer:always
#crlDistributionPoints=URI:http://www.my.host/ca.crl
#authorityInfoAccess = OCSP;URI:http://ocsp.my.host/
TESTCA

cat <<TESTCA > test_reqMail.cnf
basicConstraints = critical,CA:false
keyUsage = keyEncipherment, digitalSignature
extendedKeyUsage=emailProtection
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer:always
#crlDistributionPoints=URI:http://www.my.host/ca.crl
#authorityInfoAccess = OCSP;URI:http://ocsp.my.host/
TESTCA


genca(){ #subj, internalName

    openssl genrsa -out $2.key ${KEYSIZE}
    openssl req -new -key $2.key -out $2.csr -subj "$1/O=Test Environment CA Ltd./OU=Test Environment CAs"
    
    mkdir $2.ca
    mkdir $2.ca/newcerts
    echo 01 > $2.ca/serial
    touch $2.ca/db
    echo unique_subject = no >$2.ca/db.attr

}

caSign(){ # key,ca,config
    cd $2.ca
    openssl ca -cert ../$2.crt -keyfile ../$2.key -in ../$1.csr -out ../$1.crt -days 365 -batch -config ../selfsign.config -extfile ../$3
    cd ..
}

rootSign(){ # key
    caSign $1 root test_subca.cnf
}

genserver(){ #key, subject, config
    openssl genrsa -out $1.key ${KEYSIZE}
    openssl req -new -key $1.key -out $1.csr -subj "$2" -config selfsign.config
    caSign $1 env "$3"
    
    openssl pkcs12 -inkey $1.key -in $1.crt -CAfile env.chain.crt -chain -name $1 -export -passout pass:changeit -out $1.pkcs12
    
    keytool -importkeystore -noprompt -srckeystore $1.pkcs12 -destkeystore ../config/keystore.pkcs12 -srcstoretype pkcs12 -deststoretype pkcs12 -srcstorepass "changeit" -deststorepass "$PRIVATEPW"
}


# Generate the super Root CA
genca "/CN=Cacert-gigi testCA" root
openssl x509 -req -days 365 -in root.csr -signkey root.key -out root.crt -extfile test_ca.cnf

# generate the various sub-CAs
genca "/CN=Environment" env
rootSign env
genca "/CN=Unassured" unassured
rootSign unassured
genca "/CN=Assured" assured
rootSign assured
genca "/CN=Codesigning" codesign
rootSign codesign
genca "/CN=Timestamping" timestamp
rootSign timestamp
genca "/CN=Orga" orga
rootSign orga
genca "/CN=Orga sign" orgaSign
rootSign orgaSign


cat env.crt root.crt > env.chain.crt

# generate orga-keys specific to gigi.
# first the server keys
genserver www "/CN=www.${DOMAIN}" test_req.cnf
genserver secure "/CN=secure.${DOMAIN}" test_req.cnf
genserver static "/CN=static.${DOMAIN}" test_req.cnf
genserver api "/CN=api.${DOMAIN}" test_req.cnf

# then the email signing key
genserver mail "/emailAddress=support@${DOMAIN}" test_reqMail.cnf

keytool -list -keystore ../config/keystore.pkcs12 -storetype pkcs12 -storepass "$PRIVATEPW"

rm test_ca.cnf test_subca.cnf test_req.cnf test_reqMail.cnf
rm env.chain.crt
