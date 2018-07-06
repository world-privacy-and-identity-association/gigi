DROP TABLE IF EXISTS "user_contracts";
DROP TYPE IF EXISTS "contractType";
CREATE TYPE "contractType" AS ENUM ('RA Agent Contract', 'Org RA Agent Contract');

CREATE TABLE "user_contracts" (
  "id" serial NOT NULL,
  "token" varchar(32) NOT NULL,
  "memid" int NOT NULL,
  "document" "contractType" NOT NULL,
  "agentname" varchar(255) NOT NULL,
  "datesigned" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "daterevoked" timestamp DEFAULT NULL,
  PRIMARY KEY ("id")
);
CREATE INDEX ON "user_contracts" ("memid");
CREATE INDEX ON "user_contracts" ("document");
CREATE INDEX ON "user_contracts" ("datesigned");
CREATE INDEX ON "user_contracts" ("daterevoked");
