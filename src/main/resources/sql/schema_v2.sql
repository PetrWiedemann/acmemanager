ALTER TABLE certificate_definitions ADD COLUMN auto_export_jks BOOLEAN DEFAULT FALSE;
ALTER TABLE certificate_definitions ADD COLUMN export_path_jks VARCHAR(512);
ALTER TABLE certificate_definitions ADD COLUMN jks_password VARCHAR(512);
