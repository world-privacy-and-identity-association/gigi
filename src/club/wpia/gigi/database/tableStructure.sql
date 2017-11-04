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
  "preferredName" int NULL,
  "dob" date NOT NULL,
  "verified" boolean NOT NULL DEFAULT 'false',
  "language" varchar(5) NOT NULL DEFAULT '',
  "country" varchar(2) NULL,
  PRIMARY KEY ("id")
);
CREATE INDEX ON "users" ("email");
CREATE INDEX ON "users" ("verified");


DROP TABLE IF EXISTS "organisations";
CREATE TABLE IF NOT EXISTS "organisations" (
  "id" int NOT NULL,
  "name" varchar(64) NOT NULL,
  "country" varchar(2) NOT NULL,
  "province" varchar(128) NOT NULL,
  "city" varchar(128) NOT NULL,
  "contactEmail" varchar(100) NOT NULL,
  "creator" int NOT NULL,
  "optional_name" text,
  "postal_address" text,
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
CREATE INDEX ON "domainPinglog" ("configId","when");

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
DROP TYPE IF EXISTS "revocationType";
CREATE TYPE "revocationType" AS ENUM('user', 'support', 'ping_timeout', 'key_compromise');

DROP TYPE IF EXISTS "mdType";
CREATE TYPE "mdType" AS ENUM('md5','sha1','sha256','sha384','sha512');

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

  "revoked" timestamp NULL,
  "revocationType" "revocationType" NULL,
  "revocationChallenge" varchar(32) NULL DEFAULT NULL,
  "revocationSignature" text NULL DEFAULT NULL,
  "revocationMessage" text NULL DEFAULT NULL,

  "expire" timestamp NULL DEFAULT NULL,
  "renewed" boolean NOT NULL DEFAULT 'false',
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

DROP TABLE IF EXISTS "logincerts";
CREATE TABLE "logincerts" (
  "id" int NOT NULL,

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
# TTP is default ttp verification
  "method" "notaryType" NOT NULL DEFAULT 'Face to Face Meeting',
  "location" varchar(255) NOT NULL DEFAULT '',
  "date" varchar(255) NOT NULL DEFAULT '',
# date when verification was entered
  "when" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
#?
  "expire" timestamp NULL DEFAULT NULL,
#?????????????????
  "sponsor" int NOT NULL DEFAULT '0',
# date when verification was deleted (or 0)
  "deleted" timestamp NULL DEFAULT NULL,
  "country" varchar(2) NULL,
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
  "language" varchar(5) NOT NULL DEFAULT '',
  "version" varchar(10) NOT NULL DEFAULT '',
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

# Add values to table "cats_type"
INSERT INTO `cats_type` (`type_text`) VALUES ('Agent Qualifying Challenge');
INSERT INTO `cats_type` (`type_text`) VALUES ('Organisation Agent Qualifying Challenge');
INSERT INTO `cats_type` (`type_text`) VALUES ('TTP Agent Qualifying Challenge');
INSERT INTO `cats_type` (`type_text`) VALUES ('TTP TOPUP Agent Qualifying Challenge');
INSERT INTO `cats_type` (`type_text`) VALUES ('Code Signing Challenge');
INSERT INTO `cats_type` (`type_text`) VALUES ('Organisation Administrator Data Protection Challenge');
INSERT INTO `cats_type` (`type_text`) VALUES ('Support Data Protection Challenge');

DROP TABLE IF EXISTS "arbitrations";
CREATE TABLE IF NOT EXISTS "arbitrations" (
  "user" int NOT NULL,
  "arbitration" varchar(20) NOT NULL,
  PRIMARY KEY ("user","arbitration")
);

DROP TABLE IF EXISTS "user_groups";

DROP TYPE IF EXISTS "userGroup";
CREATE TYPE "userGroup" AS enum('supporter','blocked-applicant','blocked-agent','blocked-login','ttp-agent','ttp-applicant', 'codesigning', 'org-agent', 'blocked-cert', 'nucleus-agent', 'locate-agent', 'verify-notification');

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
INSERT INTO "schemeVersion" (version)  VALUES(31);

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


/* Create table countryIsoCode' */

DROP TABLE IF EXISTS `countryIsoCode`;
CREATE TABLE `countryIsoCode` (
  `id` serial NOT NULL,
  `english` text NOT NULL,
  `code2` varchar(2) NOT NULL,
  `code3` varchar(3) NOT NULL,
  `obp_id` int NOT NULL,
  PRIMARY KEY (`id`)
);

/* Fill table countryIsoCode' */
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Andorra', 'AD',  'AND',  020);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('United Arab Emirates (the)',  'AE',  'ARE',  784);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Afghanistan',  'AF',  'AFG',  004);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Antigua and Barbuda',  'AG',  'ATG',  028);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Anguilla',  'AI',  'AIA',  660);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Albania',  'AL',  'ALB',  008);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Armenia',  'AM',  'ARM',  051);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Angola',  'AO',  'AGO',  024);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Antarctica',  'AQ',  'ATA',  010);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Argentina',  'AR',  'ARG',  032);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('American Samoa',  'AS',  'ASM',  016);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Austria',  'AT',  'AUT',  040);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Australia',  'AU',  'AUS',  036);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Aruba',  'AW',  'ABW',  533);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Åland Islands',  'AX',  'ALA',  248);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Azerbaijan',  'AZ',  'AZE',  031);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Bosnia and Herzegovina',  'BA',  'BIH',  070);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Barbados',  'BB',  'BRB',  052);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Bangladesh',  'BD',  'BGD',  050);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Belgium',  'BE',  'BEL',  056);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Burkina Faso',  'BF',  'BFA',  854);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Bulgaria',  'BG',  'BGR',  100);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Bahrain',  'BH',  'BHR',  048);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Burundi',  'BI',  'BDI',  108);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Benin',  'BJ',  'BEN',  204);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Saint Barthélemy',  'BL',  'BLM',  652);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Bermuda',  'BM',  'BMU',  060);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Brunei Darussalam',  'BN',  'BRN',  096);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Bolivia (Plurinational State of)',  'BO',  'BOL',  068);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Bonaire, Sint Eustatius and Saba',  'BQ',  'BES',  535);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Brazil',  'BR',  'BRA',  076);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Bahamas (the)',  'BS',  'BHS',  044);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Bhutan',  'BT',  'BTN',  064);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Bouvet Island',  'BV',  'BVT',  074);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Botswana',  'BW',  'BWA',  072);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Belarus',  'BY',  'BLR',  112);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Belize',  'BZ',  'BLZ',  084);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Canada',  'CA',  'CAN',  124);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Cocos (Keeling) Islands (the)',  'CC',  'CCK',  166);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Congo (the Democratic Republic of the)',  'CD',  'COD',  180);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Central African Republic (the)',  'CF',  'CAF',  140);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Congo (the)',  'CG',  'COG',  178);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Switzerland',  'CH',  'CHE',  756);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Côte d`Ivoire',  'CI',  'CIV',  384);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Cook Islands (the)',  'CK',  'COK',  184);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Chile',  'CL',  'CHL',  152);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Cameroon',  'CM',  'CMR',  120);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('China',  'CN',  'CHN',  156);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Colombia',  'CO',  'COL',  170);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Costa Rica',  'CR',  'CRI',  188);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Cuba',  'CU',  'CUB',  192);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Cabo Verde',  'CV',  'CPV',  132);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Curaçao',  'CW',  'CUW',  531);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Christmas Island',  'CX',  'CXR',  162);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Cyprus',  'CY',  'CYP',  196);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Czech Republic (the)',  'CZ',  'CZE',  203);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Germany',  'DE',  'DEU',  276);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Djibouti',  'DJ',  'DJI',  262);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Denmark',  'DK',  'DNK',  208);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Dominica',  'DM',  'DMA',  212);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Dominican Republic (the)',  'DO',  'DOM',  214);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Algeria',  'DZ',  'DZA',  012);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Ecuador',  'EC',  'ECU',  218);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Estonia',  'EE',  'EST',  233);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Egypt',  'EG',  'EGY',  818);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Western Sahara*',  'EH',  'ESH',  732);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Eritrea',  'ER',  'ERI',  232);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Spain',  'ES',  'ESP',  724);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Ethiopia',  'ET',  'ETH',  231);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Finland',  'FI',  'FIN',  246);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Fiji',  'FJ',  'FJI',  242);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Falkland Islands (the) [Malvinas]',  'FK',  'FLK',  238);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Micronesia (Federated States of)',  'FM',  'FSM',  583);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Faroe Islands (the)',  'FO',  'FRO',  234);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('France',  'FR',  'FRA',  250);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Gabon',  'GA',  'GAB',  266);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('United Kingdom of Great Britain and Northern Ireland (the)',  'GB',  'GBR',  826);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Grenada',  'GD',  'GRD',  308);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Georgia',  'GE',  'GEO',  268);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('French Guiana',  'GF',  'GUF',  254);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Guernsey',  'GG',  'GGY',  831);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Ghana',  'GH',  'GHA',  288);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Gibraltar',  'GI',  'GIB',  292);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Greenland',  'GL',  'GRL',  304);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Gambia (the)',  'GM',  'GMB',  270);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Guinea',  'GN',  'GIN',  324);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Guadeloupe',  'GP',  'GLP',  312);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Equatorial Guinea',  'GQ',  'GNQ',  226);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Greece',  'GR',  'GRC',  300);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('South Georgia and the South Sandwich Islands',  'GS',  'SGS',  239);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Guatemala',  'GT',  'GTM',  320);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Guam',  'GU',  'GUM',  316);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Guinea-Bissau',  'GW',  'GNB',  624);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Guyana',  'GY',  'GUY',  328);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Hong Kong',  'HK',  'HKG',  344);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Heard Island and McDonald Islands',  'HM',  'HMD',  334);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Honduras',  'HN',  'HND',  340);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Croatia',  'HR',  'HRV',  191);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Haiti',  'HT',  'HTI',  332);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Hungary',  'HU',  'HUN',  348);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Indonesia',  'ID',  'IDN',  360);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Ireland',  'IE',  'IRL',  372);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Israel',  'IL',  'ISR',  376);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Isle of Man',  'IM',  'IMN',  833);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('India',  'IN',  'IND',  356);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('British Indian Ocean Territory (the)',  'IO',  'IOT',  086);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Iraq',  'IQ',  'IRQ',  368);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Iran (Islamic Republic of)',  'IR',  'IRN',  364);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Iceland',  'IS',  'ISL',  352);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Italy',  'IT',  'ITA',  380);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Jersey',  'JE',  'JEY',  832);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Jamaica',  'JM',  'JAM',  388);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Jordan',  'JO',  'JOR',  400);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Japan',  'JP',  'JPN',  392);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Kenya',  'KE',  'KEN',  404);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Kyrgyzstan',  'KG',  'KGZ',  417);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Cambodia',  'KH',  'KHM',  116);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Kiribati',  'KI',  'KIR',  296);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Comoros (the)',  'KM',  'COM',  174);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Saint Kitts and Nevis',  'KN',  'KNA',  659);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Korea (the Democratic People`s Republic of)',  'KP',  'PRK',  408);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Korea (the Republic of)',  'KR',  'KOR',  410);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Kuwait',  'KW',  'KWT',  414);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Cayman Islands (the)',  'KY',  'CYM',  136);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Kazakhstan',  'KZ',  'KAZ',  398);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Lao People`s Democratic Republic (the)',  'LA',  'LAO',  418);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Lebanon',  'LB',  'LBN',  422);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Saint Lucia',  'LC',  'LCA',  662);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Liechtenstein',  'LI',  'LIE',  438);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Sri Lanka',  'LK',  'LKA',  144);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Liberia',  'LR',  'LBR',  430);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Lesotho',  'LS',  'LSO',  426);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Lithuania',  'LT',  'LTU',  440);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Luxembourg',  'LU',  'LUX',  442);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Latvia',  'LV',  'LVA',  428);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Libya',  'LY',  'LBY',  434);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Morocco',  'MA',  'MAR',  504);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Monaco',  'MC',  'MCO',  492);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Moldova (the Republic of)',  'MD',  'MDA',  498);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Montenegro',  'ME',  'MNE',  499);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Saint Martin (French part)',  'MF',  'MAF',  663);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Madagascar',  'MG',  'MDG',  450);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Marshall Islands (the)',  'MH',  'MHL',  584);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Macedonia (the former Yugoslav Republic of)',  'MK',  'MKD',  807);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Mali',  'ML',  'MLI',  466);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Myanmar',  'MM',  'MMR',  104);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Mongolia',  'MN',  'MNG',  496);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Macao',  'MO',  'MAC',  446);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Northern Mariana Islands (the)',  'MP',  'MNP',  580);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Martinique',  'MQ',  'MTQ',  474);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Mauritania',  'MR',  'MRT',  478);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Montserrat',  'MS',  'MSR',  500);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Malta',  'MT',  'MLT',  470);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Mauritius',  'MU',  'MUS',  480);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Maldives',  'MV',  'MDV',  462);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Malawi',  'MW',  'MWI',  454);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Mexico',  'MX',  'MEX',  484);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Malaysia',  'MY',  'MYS',  458);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Mozambique',  'MZ',  'MOZ',  508);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Namibia',  'NA',  'NAM',  516);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('New Caledonia',  'NC',  'NCL',  540);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Niger (the)',  'NE',  'NER',  562);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Norfolk Island',  'NF',  'NFK',  574);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Nigeria',  'NG',  'NGA',  566);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Nicaragua',  'NI',  'NIC',  558);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Netherlands (the)',  'NL',  'NLD',  528);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Norway',  'NO',  'NOR',  578);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Nepal',  'NP',  'NPL',  524);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Nauru',  'NR',  'NRU',  520);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Niue',  'NU',  'NIU',  570);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('New Zealand',  'NZ',  'NZL',  554);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Oman',  'OM',  'OMN',  512);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Panama',  'PA',  'PAN',  591);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Peru',  'PE',  'PER',  604);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('French Polynesia',  'PF',  'PYF',  258);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Papua New Guinea',  'PG',  'PNG',  598);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Philippines (the)',  'PH',  'PHL',  608);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Pakistan',  'PK',  'PAK',  586);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Poland',  'PL',  'POL',  616);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Saint Pierre and Miquelon',  'PM',  'SPM',  666);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Pitcairn',  'PN',  'PCN',  612);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Puerto Rico',  'PR',  'PRI',  630);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Palestine,  State of',  'PS',  'PSE',  275);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Portugal',  'PT',  'PRT',  620);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Palau',  'PW',  'PLW',  585);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Paraguay',  'PY',  'PRY',  600);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Qatar',  'QA',  'QAT',  634);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Réunion',  'RE',  'REU',  638);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Romania',  'RO',  'ROU',  642);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Serbia',  'RS',  'SRB',  688);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Russian Federation (the)',  'RU',  'RUS',  643);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Rwanda',  'RW',  'RWA',  646);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Saudi Arabia',  'SA',  'SAU',  682);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Solomon Islands',  'SB',  'SLB',  090);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Seychelles',  'SC',  'SYC',  690);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Sudan (the)',  'SD',  'SDN',  729);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Sweden',  'SE',  'SWE',  752);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Singapore',  'SG',  'SGP',  702);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Saint Helena, Ascension and Tristan da Cunha',  'SH',  'SHN',  654);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Slovenia',  'SI',  'SVN',  705);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Svalbard and Jan Mayen',  'SJ',  'SJM',  744);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Slovakia',  'SK',  'SVK',  703);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Sierra Leone',  'SL',  'SLE',  694);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('San Marino',  'SM',  'SMR',  674);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Senegal',  'SN',  'SEN',  686);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Somalia',  'SO',  'SOM',  706);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Suriname',  'SR',  'SUR',  740);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('South Sudan',  'SS',  'SSD',  728);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Sao Tome and Principe',  'ST',  'STP',  678);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('El Salvador',  'SV',  'SLV',  222);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Sint Maarten (Dutch part)',  'SX',  'SXM',  534);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Syrian Arab Republic',  'SY',  'SYR',  760);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Swaziland',  'SZ',  'SWZ',  748);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Turks and Caicos Islands (the)',  'TC',  'TCA',  796);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Chad',  'TD',  'TCD',  148);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('French Southern Territories (the)',  'TF',  'ATF',  260);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Togo',  'TG',  'TGO',  768);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Thailand',  'TH',  'THA',  764);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Tajikistan',  'TJ',  'TJK',  762);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Tokelau',  'TK',  'TKL',  772);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Timor-Leste',  'TL',  'TLS',  626);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Turkmenistan',  'TM',  'TKM',  795);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Tunisia',  'TN',  'TUN',  788);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Tonga',  'TO',  'TON',  776);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Turkey',  'TR',  'TUR',  792);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Trinidad and Tobago',  'TT',  'TTO',  780);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Tuvalu',  'TV',  'TUV',  798);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Taiwan (Province of China)',  'TW',  'TWN',  158);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Tanzania,  United Republic of',  'TZ',  'TZA',  834);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Ukraine',  'UA',  'UKR',  804);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Uganda',  'UG',  'UGA',  800);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('United States Minor Outlying Islands (the)',  'UM',  'UMI',  581);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('United States of America (the)',  'US',  'USA',  840);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Uruguay',  'UY',  'URY',  858);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Uzbekistan',  'UZ',  'UZB',  860);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Holy See (the)',  'VA',  'VAT',  336);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Saint Vincent and the Grenadines',  'VC',  'VCT',  670);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Venezuela (Bolivarian Republic of)',  'VE',  'VEN',  862);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Virgin Islands (British)',  'VG',  'VGB',  092);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Virgin Islands (U.S.)',  'VI',  'VIR',  850);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Viet Nam',  'VN',  'VNM',  704);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Vanuatu',  'VU',  'VUT',  548);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Wallis and Futuna',  'WF',  'WLF',  876);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Samoa',  'WS',  'WSM',  882);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Yemen',  'YE',  'YEM',  887);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Mayotte',  'YT',  'MYT',  175);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('South Africa',  'ZA',  'ZAF',  710);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Zambia',  'ZM',  'ZMB',  894);
INSERT INTO `countryIsoCode`(english, code2, code3, obp_id) VALUES ('Zimbabwe',  'ZW',  'ZWE',  716);



DROP TABLE IF EXISTS "names";
DROP TYPE IF EXISTS "nameSchemaType";
CREATE TYPE "nameSchemaType" AS ENUM ('single', 'western');

CREATE TABLE "names" (
  "id" serial NOT NULL,
  "uid" int NOT NULL,
  "type" "nameSchemaType" NOT NULL,
  "created" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" timestamp NULL DEFAULT NULL,
  "deprecated" timestamp NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);


DROP TABLE IF EXISTS "nameParts";
DROP TYPE IF EXISTS "namePartType";
CREATE TYPE "namePartType" AS ENUM ('first-name', 'last-name', 'single-name', 'suffix');

CREATE TABLE "nameParts" (
  "id" int NOT NULL,
  "position" int NOT NULL,
  "type" "namePartType" NOT NULL,
  "value" varchar(255) NOT NULL
);


DROP TABLE IF EXISTS "certificateAttachment";
DROP TYPE IF EXISTS "certificateAttachmentType";
CREATE TYPE "certificateAttachmentType" AS ENUM ('CSR','CRT');

CREATE TABLE "certificateAttachment" (
  "certid" int NOT NULL,
  "type" "certificateAttachmentType" NOT NULL,
  "content" text NOT NULL,
  PRIMARY KEY ("certid", "type")
);
