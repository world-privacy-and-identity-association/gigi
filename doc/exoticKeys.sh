#!/bin/bash
openssl ecparam -out ec.pem -name secp521r1 -genkey
openssl req -new -key ec.pem -out ec.csr -subj "/CN=bla"

openssl dsaparam -genkey 1024 -out dsa.pem
openssl req -new -key dsa.pem -out dsa.csr -subj "/CN=bla"

