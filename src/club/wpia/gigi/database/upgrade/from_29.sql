ALTER TABLE "certs" ADD COLUMN "revocationChallenge" varchar(32) NULL DEFAULT NULL;
ALTER TABLE "certs" ADD COLUMN "revocationSignature" text NULL DEFAULT NULL;
ALTER TABLE "certs" ADD COLUMN "revocationMessage" text NULL DEFAULT NULL;

ALTER TYPE "revocationType" ADD VALUE 'key_compromise' AFTER 'ping_timeout';
