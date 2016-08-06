ALTER TABLE "certs" DROP "disablelogin";


ALTER TABLE "clientcerts" RENAME TO "logincerts";
DELETE FROM "logincerts" WHERE "disablelogin" = 'true';
ALTER TABLE "logincerts" DROP "disablelogin";
