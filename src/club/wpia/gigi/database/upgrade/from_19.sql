DROP TYPE IF EXISTS "nameSchemaType";
CREATE TYPE "nameSchemaType" AS ENUM ('single', 'western');

DROP TABLE IF EXISTS "names";
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

INSERT INTO "names" ("uid", "type") (SELECT "id" as "uid", 'western'::"nameSchemaType" AS "type" from users ORDER BY id);
INSERT INTO "nameParts" SELECT names.id, 1, 'first-name'::"namePartType", "fname" FROM "users" INNER JOIN "names" ON "names"."uid" = "users"."id";

INSERT INTO "nameParts" SELECT names.id, 2, 'first-name'::"namePartType", "mname" FROM "users" INNER JOIN "names" ON "names"."uid" = "users"."id" WHERE "mname" != '';
INSERT INTO "nameParts" SELECT names.id, 3, 'last-name'::"namePartType", "lname" FROM "users" INNER JOIN "names" ON "names"."uid" = "users"."id";
INSERT INTO "nameParts" SELECT names.id, 4, 'suffix'::"namePartType", "suffix" FROM "users" INNER JOIN "names" ON "names"."uid" = "users"."id" WHERE "suffix" != '';

UPDATE "notary" SET "to"=(SELECT "id" FROM "names" WHERE "uid"="notary"."to");

ALTER TABLE "users" ADD "preferredName" int;
UPDATE "users" SET "preferredName"=(SELECT "id" FROM "names" WHERE "uid"="users"."id");
ALTER TABLE "users" DROP "fname";
ALTER TABLE "users" DROP "mname";
ALTER TABLE "users" DROP "lname";
ALTER TABLE "users" DROP "suffix";
