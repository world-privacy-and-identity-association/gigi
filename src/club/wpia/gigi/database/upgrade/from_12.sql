ALTER TABLE "organisations" ALTER COLUMN  "name" TYPE  varchar(64);

ALTER TABLE "organisations" ADD COLUMN  "optional_name"  text;
ALTER TABLE "organisations" ADD COLUMN  "postal_address"  text;
