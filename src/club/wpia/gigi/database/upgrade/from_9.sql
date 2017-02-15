ALTER TABLE "emailPinglog" ADD COLUMN "challenge" varchar(255) NULL DEFAULT NULL;

INSERT INTO "emailPinglog" SELECT CURRENT_TIMESTAMP AS "when", "memid" AS "uid", "email", 'active'::"emailPingType" AS "type", CASE WHEN "hash"='' THEN 'success'::"pingState" ELSE 'open'::"pingState" END AS state, '' AS result, "hash" AS "challenge" FROM "emails";
ALTER TABLE "emails" DROP COLUMN "attempts";
ALTER TABLE "emails" DROP COLUMN "hash";
