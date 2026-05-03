ALTER TABLE certificate_definitions ADD COLUMN challenge_type VARCHAR(50) DEFAULT 'DNS-01' NOT NULL;
ALTER TABLE certificate_definitions ADD COLUMN webroot_path VARCHAR(512);
ALTER TABLE certificate_definitions ALTER COLUMN dns_provider_id DROP NOT NULL;
