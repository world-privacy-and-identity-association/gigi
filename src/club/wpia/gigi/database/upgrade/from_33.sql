CREATE TABLE "jobLog" (
  "jobid" int NOT NULL,
  "attempt" smallint NOT NULL,
  "content" text NOT NULL,
  PRIMARY KEY ("jobid", "attempt")
);
CREATE INDEX ON "jobLog" ("jobid");

ALTER TABLE "jobs" RENAME COLUMN "warning" TO "attempt";
