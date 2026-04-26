package net.pdynet.acmemanager.dao;

import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import net.pdynet.acmemanager.model.AcmeRegistration;

@RegisterFieldMapper(AcmeRegistration.class)
public interface AcmeRegistrationDao {

	@SqlUpdate("INSERT INTO acme_registrations (name, date_write, date_edit, server_url, account_url, account_key, trust_all_certificates, email) " +
			"VALUES (:name, :dateWrite, :dateEdit, :serverUrl, :accountUrl, :accountKey, :trustAllCertificates, :email)")
	@GetGeneratedKeys
	int insert(@BindBean AcmeRegistration registration);
	
	@SqlQuery("SELECT * FROM acme_registrations WHERE id = :id")
	AcmeRegistration findById(@Bind("id") int id);

	@SqlQuery("SELECT * FROM acme_registrations ORDER BY name")
	List<AcmeRegistration> findAll();

	@SqlUpdate("UPDATE acme_registrations SET name = :name, date_edit = :dateEdit, trust_all_certificates = :trustAllCertificates WHERE id = :id")
	void update(@BindBean AcmeRegistration registration);

	@SqlUpdate("UPDATE acme_registrations SET name = :name, date_edit = :dateEdit, trust_all_certificates = :trustAllCertificates, "
			+ "server_url = :serverUrl, account_url = :accountUrl, email = :email, account_key = :accountKey WHERE id = :id")
	void updateAll(@BindBean AcmeRegistration registration);

	@SqlUpdate("DELETE FROM acme_registrations WHERE id = :id")
	void deleteById(@Bind("id") int id);

	@SqlQuery("SELECT * FROM acme_registrations WHERE name = :name")
	AcmeRegistration findByName(@Bind("name") String name);
}
