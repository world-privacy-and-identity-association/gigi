CREATE TYPE "certificateAttachmentType" AS ENUM ('CSR','CRT');

CREATE TABLE "certificateAttachment" (
  "certid" int NOT NULL,
  "type" "certificateAttachmentType" NOT NULL,
  "content" text NOT NULL,
  PRIMARY KEY ("certid", "type")
);
