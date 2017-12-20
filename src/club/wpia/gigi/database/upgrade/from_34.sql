ALTER TABLE "domainPinglog" ADD COLUMN "needsAction" boolean DEFAULT false;
CREATE INDEX ON "domainPinglog" ("when", "needsAction");
