BEGIN;
ALTER TABLE "certs" ADD COLUMN "actorid" int;
CREATE INDEX ON "certs" ("actorid");
UPDATE "certs" SET "actorid" = "memid" WHERE "profile" < 10;
UPDATE "certs" SET "actorid" = (SELECT "org_admin"."memid" FROM "org_admin" WHERE "org_admin"."orgid" = "certs"."memid" LIMIT 1) WHERE "profile" >= 10;
ALTER TABLE "certs" ALTER COLUMN "actorid" SET NOT NULL;
COMMIT;