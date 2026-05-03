package net.pdynet.acmemanager.dao;

import java.time.OffsetDateTime;
import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import net.pdynet.acmemanager.model.CertificateDefinition;
import net.pdynet.acmemanager.model.CertificateDefinitionView;

public interface CertificateDefinitionDao {

	@SqlUpdate("INSERT INTO certificate_definitions ("
			+ " name, domain_name, subject_alt_names, acme_registration_id, dns_provider_id,"
			+ " key_algorithm, key_size_or_curve, auto_renew, days_before_expiry_to_renew, date_write, date_edit,"
			+ " auto_export, export_path_cert, export_path_chain, export_path_key,"
			+ " run_script, script_path,"
			+ " auto_export_jks, export_path_jks, jks_password,"
			+ " send_to_webhook, webhook_url, webhook_headers, webhook_trust_all, webhook_password, webhook_payload_id,"
			+ " challenge_type, webroot_path) "
			+ "VALUES ("
			+ " :name, :domainName, :subjectAltNames, :acmeRegistrationId, :dnsProviderId,"
			+ " :keyAlgorithm, :keySizeOrCurve, :autoRenew, :daysBeforeExpiryToRenew, :dateWrite, :dateEdit,"
			+ " :autoExport, :exportPathCert, :exportPathChain, :exportPathKey,"
			+ " :runScript, :scriptPath,"
			+ " :autoExportJks, :exportPathJks, :jksPassword,"
			+ " :sendToWebhook, :webhookUrl, :webhookHeaders, :webhookTrustAll, :webhookPassword, :webhookPayloadId,"
			+ " :challengeType, :webrootPath)")
	@GetGeneratedKeys
	int insert(@BindBean CertificateDefinition definition);
	
	@SqlQuery("SELECT * FROM certificate_definitions WHERE id = :id")
	@RegisterFieldMapper(CertificateDefinition.class)
	CertificateDefinition findById(@Bind("id") int id);

	@SqlQuery("SELECT * FROM certificate_definitions ORDER BY name")
	@RegisterFieldMapper(CertificateDefinition.class)
	List<CertificateDefinition> findAll();

	@SqlQuery("SELECT d.*, " +
			"(SELECT MAX(not_after) FROM issued_certificates i WHERE i.definition_id = d.id) as expiry_date " +
			"FROM certificate_definitions d ORDER BY d.name")
	@RegisterFieldMapper(CertificateDefinitionView.class)
	List<CertificateDefinitionView> findAllWithExpiry();
	
	@SqlUpdate("UPDATE certificate_definitions SET"
			+ " name = :name, domain_name = :domainName, subject_alt_names = :subjectAltNames,"
			+ " acme_registration_id = :acmeRegistrationId, dns_provider_id = :dnsProviderId,"
			+ " key_algorithm = :keyAlgorithm, key_size_or_curve = :keySizeOrCurve,"
			+ " auto_renew = :autoRenew, days_before_expiry_to_renew = :daysBeforeExpiryToRenew,"
			+ " date_edit = :dateEdit, auto_export = :autoExport, export_path_cert = :exportPathCert,"
			+ " export_path_chain = :exportPathChain, export_path_key = :exportPathKey,"
			+ " run_script = :runScript, script_path = :scriptPath,"
			+ " auto_export_jks = :autoExportJks, export_path_jks = :exportPathJks, jks_password = :jksPassword,"
			+ " send_to_webhook = :sendToWebhook, webhook_url = :webhookUrl, webhook_headers = :webhookHeaders,"
			+ " webhook_trust_all = :webhookTrustAll, webhook_password = :webhookPassword, webhook_payload_id = :webhookPayloadId,"
			+ " challenge_type = :challengeType, webroot_path = :webrootPath"
			+ " WHERE id = :id")
	void update(@BindBean CertificateDefinition definition);
	
	@SqlUpdate("DELETE FROM certificate_definitions WHERE id = :id")
	void deleteById(@Bind("id") int id);
	
	@SqlQuery("SELECT * FROM ("
			+ "  SELECT d.*, "
			+ "  (SELECT MAX(not_after) FROM issued_certificates i WHERE i.definition_id = d.id) as expiry_date "
			+ "  FROM certificate_definitions d "
			+ "  WHERE d.auto_renew = TRUE"
			+ ") "
			+ "WHERE expiry_date IS NOT NULL "
			+ "  AND :now >= DATEADD(DAY, -days_before_expiry_to_renew, expiry_date) "
			+ "ORDER BY name")
	@RegisterFieldMapper(CertificateDefinitionView.class)
	List<CertificateDefinitionView> findDefinitionsForRenewal(@Bind("now") OffsetDateTime now);	
}
