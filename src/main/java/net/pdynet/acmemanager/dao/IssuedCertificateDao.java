package net.pdynet.acmemanager.dao;

import java.time.OffsetDateTime;
import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import net.pdynet.acmemanager.model.IssuedCertificate;
import net.pdynet.acmemanager.model.IssuedCertificateView;

@RegisterFieldMapper(IssuedCertificate.class)
public interface IssuedCertificateDao {

	@SqlUpdate("INSERT INTO issued_certificates (definition_id, order_id, serial_number, not_before, not_after, cert_pem, chain_pem, private_key_pem, date_write) "
			+ "VALUES (:definitionId, :orderId, :serialNumber, :notBefore, :notAfter, :certPem, :chainPem, :privateKeyPem, :dateWrite)")
	@GetGeneratedKeys
	int insert(@BindBean IssuedCertificate certificate);

	@SqlQuery("SELECT * FROM issued_certificates WHERE definition_id = :defId ORDER BY not_after DESC")
	List<IssuedCertificate> findByDefinition(@Bind("defId") int definitionId);

	// Klíčová metoda pro tvůj scheduler obnovy
	@SqlQuery("SELECT * FROM issued_certificates WHERE not_after < :threshold")
	List<IssuedCertificate> findExpiringBefore(@Bind("threshold") OffsetDateTime threshold);

	@SqlQuery("SELECT * FROM issued_certificates WHERE serial_number = :serial")
	IssuedCertificate findBySerialNumber(@Bind("serial") String serialNumber);
	
	@SqlQuery("SELECT i.*, o.status as orderStatus, o.error_message as orderErrorMessage " +
			"FROM issued_certificates i " +
			"LEFT JOIN certificate_orders o ON i.order_id = o.id " +
			"WHERE i.definition_id = :defId ORDER BY i.date_write DESC")
	@RegisterFieldMapper(IssuedCertificateView.class)
	List<IssuedCertificateView> findHistoryByDefinition(@Bind("defId") int definitionId);

	@SqlQuery("SELECT " +
			"  o.id AS order_id, " + 
			"  o.status AS orderStatus, " + 
			"  o.error_message AS orderErrorMessage, " +
			"  o.date_write AS date_write, " +
			"  c.id AS id, " +
			"  c.serial_number, " +
			"  c.not_before, " +
			"  c.not_after, " +
			"  c.cert_pem, " +
			"  c.chain_pem, " +
			"  c.private_key_pem " +
			"FROM certificate_orders o " +
			"LEFT JOIN issued_certificates c ON c.order_id = o.id " +
			"WHERE o.definition_id = :definitionId " +
			"ORDER BY o.date_write DESC")
	@RegisterFieldMapper(IssuedCertificateView.class)
	List<IssuedCertificateView> findHistoryByDefinitionId(@Bind("definitionId") int definitionId);

	@SqlUpdate("DELETE FROM issued_certificates WHERE id = :id")
	void deleteById(@Bind("id") int id);
	
	@SqlUpdate("DELETE FROM issued_certificates WHERE order_id = :orderId")
	void deleteByOrderId(@Bind("orderId") int orderId);
}
