DELETE FROM "user_groups" WHERE "permission" = 'arbitrator'::"userGroup";

ALTER TYPE "userGroup" RENAME TO "oldUserGroup";
CREATE TYPE "userGroup" AS enum('supporter','blocked-agent','blocked-applicant','blocked-login','ttp-agent','ttp-applicant', 'codesigning', 'org-agent', 'blocked-cert', 'nucleus-agent', 'locate-agent', 'verify-notification');
ALTER TABLE "user_groups" ALTER COLUMN "permission" SET DATA TYPE "userGroup" USING
    CASE "permission" WHEN 'blockedassurer' THEN 'blocked-agent'::"userGroup"
                      WHEN 'blockedassuree' THEN 'blocked-applicant'::"userGroup"
                      WHEN 'ttp-assurer' THEN 'ttp-agent'::"userGroup"
                      WHEN 'orgassurer' THEN 'org-agent'::"userGroup"
                      WHEN 'nucleus-assurer' THEN 'nucleus-agent'::"userGroup"
                      WHEN 'blockedcert' THEN 'blocked-cert'::"userGroup"
                      WHEN 'blockedlogin' THEN 'blocked-login'::"userGroup"
                      ELSE "permission"::text::"userGroup"
    END;
DROP TYPE "oldUserGroup";
