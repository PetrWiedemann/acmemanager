-- Table with a list of supported DNS providers.
CREATE TABLE IF NOT EXISTS dns_services (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- Table with keys for DNS providers.
CREATE TABLE IF NOT EXISTS dns_providers (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    dns_service_id INTEGER NOT NULL,
    date_write TIMESTAMP WITH TIME ZONE NOT NULL,
    date_edit TIMESTAMP WITH TIME ZONE NOT NULL,
    api_key VARCHAR(512) NOT NULL,
    secret_key VARCHAR(512),
    
    FOREIGN KEY (dns_service_id) REFERENCES dns_services(id)
);

CREATE UNIQUE INDEX idx_dns_prov_name ON dns_providers(name);

-- User accounts at certification authorities.
CREATE TABLE IF NOT EXISTS acme_registrations (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    date_write TIMESTAMP WITH TIME ZONE NOT NULL,
    date_edit TIMESTAMP WITH TIME ZONE NOT NULL,
    server_url VARCHAR(512) NOT NULL,
    account_url VARCHAR(512) NOT NULL,
    account_key TEXT,
    trust_all_certificates BOOLEAN DEFAULT FALSE,
    email VARCHAR(255) NOT NULL DEFAULT ''
);

CREATE UNIQUE INDEX idx_acme_reg_name ON acme_registrations(name);

-- certificate_definitions:
-- Defines the desired state of a certificate. It acts as a template or configuration
-- for automated issuance and renewal. It links specific domain names to a preferred 
-- ACME account and a DNS provider for challenge fulfillment.
CREATE TABLE IF NOT EXISTS certificate_definitions (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    date_write TIMESTAMP WITH TIME ZONE NOT NULL,
    date_edit TIMESTAMP WITH TIME ZONE NOT NULL,
    domain_name VARCHAR(255) NOT NULL,
    subject_alt_names TEXT,
    acme_registration_id INTEGER NOT NULL,
    dns_provider_id INTEGER NOT NULL,
    key_algorithm VARCHAR(20) DEFAULT 'RSA' NOT NULL,      -- 'RSA' or 'ECDSA'
    key_size_or_curve VARCHAR(50) DEFAULT '2048' NOT NULL, -- '2048', '4096' or 'secp384r1'    
    auto_renew BOOLEAN DEFAULT TRUE,
    days_before_expiry_to_renew INTEGER DEFAULT 10,
    auto_export BOOLEAN DEFAULT FALSE,
    export_path_cert VARCHAR(1024) DEFAULT '',
    export_path_chain VARCHAR(1024) DEFAULT '',
    export_path_key VARCHAR(1024) DEFAULT '',
    run_script BOOLEAN DEFAULT FALSE,
    script_path VARCHAR(1024) DEFAULT '',
    send_to_webhook BOOLEAN DEFAULT FALSE,
    webhook_url VARCHAR(1024) DEFAULT '',
    webhook_headers VARCHAR(2048) DEFAULT '',
    webhook_trust_all BOOLEAN DEFAULT FALSE,
    webhook_password VARCHAR(512),
    webhook_payload_id VARCHAR(512) DEFAULT '',
    
    FOREIGN KEY (acme_registration_id) REFERENCES acme_registrations(id),
    FOREIGN KEY (dns_provider_id) REFERENCES dns_providers(id)
);

CREATE INDEX idx_cert_def_acme ON certificate_definitions(acme_registration_id);
CREATE INDEX idx_cert_def_dns ON certificate_definitions(dns_provider_id);
CREATE UNIQUE INDEX idx_cert_def_name ON certificate_definitions(name);

-- certificate_orders:
-- Tracks the lifecycle of a specific ACME issuance process (equivalent to an acme4j Order).
-- It stores the progress, the ACME server's order URL for state recovery, and captures 
-- any error messages if the validation or issuance fails.
CREATE TABLE IF NOT EXISTS certificate_orders (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    definition_id INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    order_url VARCHAR(512),
    date_write TIMESTAMP WITH TIME ZONE NOT NULL,
    date_edit TIMESTAMP WITH TIME ZONE NOT NULL,
    error_message TEXT,
    
    FOREIGN KEY (definition_id) REFERENCES certificate_definitions(id)
);

CREATE INDEX idx_cert_orders_def ON certificate_orders(definition_id);
CREATE INDEX idx_cert_orders_status ON certificate_orders(status);

-- issued_certificates:
-- Stores the final artifacts of a successful issuance. This table contains the actual
-- public certificates, the full chain, and the encrypted private keys. It also holds 
-- metadata like expiration dates to facilitate easy monitoring and filtering.
CREATE TABLE IF NOT EXISTS issued_certificates (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    definition_id INTEGER NOT NULL,
    order_id INTEGER,
    serial_number VARCHAR(128),
    not_before TIMESTAMP WITH TIME ZONE NOT NULL,
    not_after TIMESTAMP WITH TIME ZONE NOT NULL,
    
    cert_pem TEXT NOT NULL,
    chain_pem TEXT,
    private_key_pem TEXT NOT NULL,
    
    date_write TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (definition_id) REFERENCES certificate_definitions(id),
    FOREIGN KEY (order_id) REFERENCES certificate_orders(id)
);

CREATE INDEX idx_issued_certs_serial ON issued_certificates(serial_number);
CREATE INDEX idx_issued_certs_not_after ON issued_certificates(not_after);
CREATE INDEX idx_issued_certs_not_before ON issued_certificates(not_before);
CREATE INDEX idx_issued_certs_def_expiry ON issued_certificates(definition_id, not_after DESC);

-- DATA --
INSERT INTO dns_services(id, name) VALUES (1, 'Active24');
INSERT INTO dns_services(id, name) VALUES (2, 'RegZone');
INSERT INTO dns_services(id, name) VALUES (3, 'TSIG (RFC 2136 - Dynamic DNS Updates)');

INSERT INTO config(id, int_val, text_val) VALUES ('dns_propagation_delay', 5, '');
