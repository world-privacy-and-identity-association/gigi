DROP TABLE IF EXISTS "certOwners";
CREATE TABLE "certOwners" (
  "id" serial NOT NULL,
  "created" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "modified" timestamp NULL DEFAULT NULL,
  "deleted" timestamp NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);

DROP TABLE IF EXISTS "users";
CREATE TABLE "users" (
  "id" int NOT NULL,
  "email" varchar(255) NOT NULL DEFAULT '',
  "password" varchar(255) NOT NULL DEFAULT '',
  "fname" varchar(255) NOT NULL DEFAULT '',
  "mname" varchar(255) NOT NULL DEFAULT '',
  "lname" varchar(255) NOT NULL DEFAULT '',
  "suffix" varchar(50) NOT NULL DEFAULT '',
  "dob" date NOT NULL,
  "verified" boolean NOT NULL DEFAULT 'false',
  "ccid" int NOT NULL DEFAULT '0',
  "regid" int NOT NULL DEFAULT '0',
  "locid" int NOT NULL DEFAULT '0',
  "listme" boolean NOT NULL DEFAULT 'false',
  "contactinfo" varchar(255) NOT NULL DEFAULT '',
  "language" varchar(5) NOT NULL DEFAULT '',
  PRIMARY KEY ("id")
);
CREATE INDEX ON "users" ("ccid");
CREATE INDEX ON "users" ("regid");
CREATE INDEX ON "users" ("locid");
CREATE INDEX ON "users" ("email");
CREATE INDEX ON "users" ("verified");


DROP TABLE IF EXISTS "organisations";
CREATE TABLE IF NOT EXISTS "organisations" (
  "id" int NOT NULL,
  "name" varchar(100) NOT NULL,
  "state" varchar(2) NOT NULL,
  "province" varchar(100) NOT NULL,
  "city" varchar(100) NOT NULL,
  "contactEmail" varchar(100) NOT NULL,
  "creator" int NOT NULL,
  PRIMARY KEY ("id")
);

DROP TABLE IF EXISTS "domains";
CREATE TABLE "domains" (
  "id" serial NOT NULL,
  "memid" int NOT NULL,
  "domain" varchar(255) NOT NULL,
  "created" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "modified" timestamp NULL DEFAULT NULL,
  "deleted" timestamp NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
CREATE INDEX ON "domains" ("memid");
CREATE INDEX ON "domains" ("domain");
CREATE INDEX ON "domains" ("deleted");

DROP TABLE IF EXISTS "emails";
CREATE TABLE "emails" (
  "id" serial NOT NULL,
  "memid" int NOT NULL DEFAULT '0',
  "email" varchar(255) NOT NULL DEFAULT '',
  "created" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "modified" timestamp NULL DEFAULT NULL,
  "deleted" timestamp NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
CREATE INDEX ON "emails" ("memid");
CREATE INDEX ON "emails" ("deleted");
CREATE INDEX ON "emails" ("email");

DROP TABLE IF EXISTS "emailPinglog";
DROP TABLE IF EXISTS "domainPinglog";

DROP TYPE IF EXISTS "emailPingType";
CREATE TYPE "emailPingType" AS ENUM ('fast', 'active');
DROP TYPE IF EXISTS "pingState";
CREATE TYPE "pingState" AS ENUM ('open', 'success', 'failed');

CREATE TABLE "emailPinglog" (
  "when" timestamp NOT NULL,
  "uid" int NOT NULL,
  "email" varchar(255) NOT NULL,
  "type" "emailPingType" NOT NULL,
  "status" "pingState" NOT NULL,
  "result" varchar(255) NOT NULL,
  "challenge" varchar(255) NULL DEFAULT NULL
);

DROP TABLE IF EXISTS "pingconfig";

DROP TYPE IF EXISTS "pingType";
CREATE TYPE "pingType" AS ENUM ('email', 'ssl', 'http', 'dns');

CREATE TABLE "pingconfig" (
  "id" serial NOT NULL,
  "domainid" int NOT NULL,
  "type" "pingType" NOT NULL,
  "info" varchar(255) NOT NULL,
  "deleted" timestamp NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);


CREATE TABLE "domainPinglog" (
  "when" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "configId" int NOT NULL,
  "state" "pingState" NOT NULL,
  "challenge" varchar(16),
  "result" varchar(255)
);

DROP TABLE IF EXISTS "baddomains";
CREATE TABLE "baddomains" (
  "domain" varchar(255) NOT NULL DEFAULT ''
);


DROP TABLE IF EXISTS "alerts";
CREATE TABLE "alerts" (
  "memid" int NOT NULL DEFAULT '0',
  "general" boolean NOT NULL DEFAULT 'false',
  "country" boolean NOT NULL DEFAULT 'false',
  "regional" boolean NOT NULL DEFAULT 'false',
  "radius" boolean NOT NULL DEFAULT 'false',
  PRIMARY KEY ("memid")
);

DROP TABLE IF EXISTS "user_agreements";
CREATE TABLE "user_agreements" (
  "id" serial NOT NULL,
  "memid" int NOT NULL,
  "secmemid" int DEFAULT NULL,
  "document" varchar(50) DEFAULT NULL,
  "date" timestamp DEFAULT NULL,
  "active" boolean NOT NULL,
  "method" varchar(100) NOT NULL,
  "comment" varchar(100) DEFAULT NULL,
  PRIMARY KEY ("id")
);

DROP TABLE IF EXISTS "certs";

DROP TYPE IF EXISTS "mdType";
CREATE TYPE "mdType" AS ENUM('md5','sha1','sha256','sha512');

DROP TYPE IF EXISTS "csrType";
CREATE TYPE "csrType" AS ENUM ('CSR', 'SPKAC');

CREATE TABLE "certs" (
  "id" serial NOT NULL,
  "memid" int NOT NULL DEFAULT '0',
  "serial" varchar(50) NOT NULL DEFAULT '',
  "keytype" char(2) NOT NULL DEFAULT 'NS',
  "codesign" boolean NOT NULL DEFAULT 'false',
  "md" "mdType" NOT NULL DEFAULT 'sha512',
  "profile" int NOT NULL,
  "caid" int NULL DEFAULT NULL,

  "csr_name" varchar(255) NOT NULL DEFAULT '',
  "csr_type" "csrType" NOT NULL,
  "crt_name" varchar(255) NOT NULL DEFAULT '',
  "created" timestamp NULL DEFAULT NULL,
  "modified" timestamp NULL DEFAULT NULL,
  "revoked" timestamp NULL DEFAULT NULL,
  "expire" timestamp NULL DEFAULT NULL,
  "renewed" boolean NOT NULL DEFAULT 'false',
  "disablelogin" boolean NOT NULL DEFAULT 'false',
  "pkhash" char(40) DEFAULT NULL,
  "certhash" char(40) DEFAULT NULL,
  "description" varchar(100) NOT NULL DEFAULT '',
  PRIMARY KEY ("id")
);
CREATE INDEX ON "certs" ("pkhash");
CREATE INDEX ON "certs" ("revoked");
CREATE INDEX ON "certs" ("created");
CREATE INDEX ON "certs" ("memid");
CREATE INDEX ON "certs" ("serial");
CREATE INDEX ON "certs" ("expire");
CREATE INDEX ON "certs" ("crt_name");



DROP TABLE IF EXISTS "certAvas";
CREATE TABLE "certAvas" (
  "certId" int NOT NULL,
  "name" varchar(20) NOT NULL,
  "value" varchar(255) NOT NULL,

  PRIMARY KEY ("certId", "name")
);

DROP TABLE IF EXISTS "clientcerts";
CREATE TABLE "clientcerts" (
  "id" int NOT NULL,
  "disablelogin" boolean NOT NULL DEFAULT 'false',

  PRIMARY KEY ("id")
);

DROP TABLE IF EXISTS "profiles";
CREATE TABLE "profiles" (
  "id" serial NOT NULL,
  "keyname" varchar(60) NOT NULL,
  "include" varchar(200) NOT NULL,
  "requires" varchar(200) NOT NULL,
  "name" varchar(100) NOT NULL,
  PRIMARY KEY ("id"),
  UNIQUE ("keyname")
);

DROP TABLE IF EXISTS "subjectAlternativeNames";

DROP TYPE IF EXISTS "SANType";
CREATE TYPE "SANType" AS ENUM ('email', 'DNS');

CREATE TABLE "subjectAlternativeNames" (
  "certId" int NOT NULL,
  "contents" varchar(50) NOT NULL,
  "type" "SANType" NOT NULL
);

DROP TABLE IF EXISTS "cacerts";
CREATE TABLE "cacerts" (
  "id" serial NOT NULL,
  "keyname" varchar(60) NOT NULL,
  "link" varchar(160) NOT NULL,
  "parentRoot" int NOT NULL,
  "validFrom" timestamp NULL DEFAULT NULL,
  "validTo" timestamp NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
  UNIQUE ("keyname")
);

DROP TABLE IF EXISTS "jobs";

DROP TYPE IF EXISTS "jobType";
CREATE TYPE "jobType" AS ENUM ('sign', 'revoke');
DROP TYPE IF EXISTS "jobState";
CREATE TYPE "jobState" AS ENUM ('open', 'done', 'error');


CREATE TABLE "jobs" (
  "id" serial NOT NULL,
  "targetId" int NOT NULL,
  "task" "jobType" NOT NULL,
  "state" "jobState" NOT NULL DEFAULT 'open',
  "warning" smallint NOT NULL DEFAULT '0',
  "executeFrom" DATE,
  "executeTo" VARCHAR(11),
  PRIMARY KEY ("id")
);

CREATE INDEX ON "jobs" ("state");

DROP TABLE IF EXISTS "notary";

DROP TYPE IF EXISTS "notaryType";
CREATE TYPE "notaryType" AS enum('Face to Face Meeting', 'TOPUP', 'TTP-Assisted', 'Nucleus Bonus');

CREATE TABLE "notary" (
  "id" serial NOT NULL,
  "from" int NOT NULL DEFAULT '0',
  "to" int NOT NULL DEFAULT '0',
# total points that have been entered
  "points" int NOT NULL DEFAULT '0',
# awarded and the "experience points" are calculated virtually
# Face to Face is default, TOPUP is for the remaining 30Points after two TTP
# TTP is default ttp assurance
  "method" "notaryType" NOT NULL DEFAULT 'Face to Face Meeting',
  "location" varchar(255) NOT NULL DEFAULT '',
  "date" varchar(255) NOT NULL DEFAULT '',
# date when assurance was entered
  "when" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
#?
  "expire" timestamp NULL DEFAULT NULL,
#?????????????????
  "sponsor" int NOT NULL DEFAULT '0',
# date when assurance was deleted (or 0)
  "deleted" timestamp NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);

CREATE INDEX ON "notary"("from");
CREATE INDEX ON "notary"("to");
CREATE INDEX ON "notary"("when");
CREATE INDEX ON "notary"("method");


DROP TABLE IF EXISTS "cats_passed";
CREATE TABLE "cats_passed" (
  "id" serial NOT NULL,
  "user_id" int NOT NULL,
  "variant_id" int NOT NULL,
  "pass_date" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  UNIQUE ("user_id","variant_id","pass_date")
);

# --------------------------------------------------------

#
# Table structure for table "cats_type"
#

DROP TABLE IF EXISTS "cats_type";
CREATE TABLE "cats_type" (
  "id" serial NOT NULL,
  "type_text" varchar(255) NOT NULL,
  PRIMARY KEY ("id"),
  UNIQUE ("type_text")
);

DROP TABLE IF EXISTS "arbitrations";
CREATE TABLE IF NOT EXISTS "arbitrations" (
  "user" int NOT NULL,
  "arbitration" varchar(20) NOT NULL,
  PRIMARY KEY ("user","arbitration")
);

DROP TABLE IF EXISTS "user_groups";

DROP TYPE IF EXISTS "userGroup";
CREATE TYPE "userGroup" AS enum('supporter','arbitrator','blockedassuree','blockedassurer','blockedlogin','ttp-assurer','ttp-applicant', 'codesigning', 'orgassurer', 'blockedcert', 'nucleus-assurer');

CREATE TABLE IF NOT EXISTS "user_groups" (
  "id" serial NOT NULL,
  "user" int NOT NULL,
  "permission" "userGroup" NOT NULL,
  "granted" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" timestamp NULL DEFAULT NULL,
  "grantedby" int NOT NULL,
  "revokedby" int DEFAULT NULL,
  PRIMARY KEY ("id")
);

DROP TABLE IF EXISTS "org_admin";

DROP TYPE IF EXISTS "yesno";
CREATE TYPE "yesno" AS enum('y', 'n');


CREATE TABLE IF NOT EXISTS "org_admin" (
  "orgid" int NOT NULL,
  "memid" int NOT NULL,
  "master" "yesno" NOT NULL,
  "creator" int NOT NULL,
  "created" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleter" int NULL DEFAULT NULL,
  "deleted" timestamp NULL DEFAULT NULL
);
CREATE INDEX ON "org_admin"("orgid", "memid");


DROP TABLE IF EXISTS "adminLog";
CREATE TABLE "adminLog" (
  "when" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "uid" int NOT NULL,
  "admin" int NOT NULL,
  "type" varchar(100) NOT NULL DEFAULT '',
  "information" varchar(50) NOT NULL DEFAULT ''
);
CREATE INDEX ON "adminLog"("when");


DROP TABLE IF EXISTS "schemeVersion";
CREATE TABLE "schemeVersion" (
  "version" smallint NOT NULL,
  PRIMARY KEY ("version")
);
INSERT INTO "schemeVersion" (version)  VALUES(10);

DROP TABLE IF EXISTS `passwordResetTickets`;
CREATE TABLE `passwordResetTickets` (
  `id` serial NOT NULL,
  `memid` int NOT NULL,
  `creator` int NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `used` timestamp NULL DEFAULT NULL,
  `token` varchar(32) NOT NULL,
  `private_token` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
);
