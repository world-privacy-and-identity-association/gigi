cd `dirname $0`

for i in {4..100}; do
openssl req -newkey rsa:1024 -nodes -keyout /dev/null \
	-out $i.csr -subj "/CN=tmp.cacert.local" \
	-config ../../keys/selfsign.config;
done
