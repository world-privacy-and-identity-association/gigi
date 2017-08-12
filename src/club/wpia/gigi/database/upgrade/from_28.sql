CREATE TYPE "revocationType" AS ENUM('user', 'support', 'ping_timeout');
ALTER TABLE "certs" ADD COLUMN  "revocationType" "revocationType" NULL;
