package net.pdynet.acmemanager.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import net.pdynet.acmemanager.model.DnsProvider;
import net.pdynet.acmemanager.model.DnsService;

@RegisterFieldMapper(DnsProvider.class)
@RegisterFieldMapper(DnsService.class)
public interface DnsProviderDao {

	@SqlUpdate("INSERT INTO dns_providers (name, dns_service_id, date_write, date_edit, api_key, secret_key) "
			+ "VALUES (:name, :dnsServiceId, :dateWrite, :dateEdit, :apiKey, :secretKey)")
	@GetGeneratedKeys
	int insert(@BindBean DnsProvider provider);

	@SqlQuery("SELECT * FROM dns_providers WHERE id = :id")
	DnsProvider findById(@Bind("id") int id);

	@SqlQuery("SELECT * FROM dns_providers ORDER BY name")
	List<DnsProvider> findAll();

	@SqlQuery("SELECT id, name FROM dns_services ORDER BY name")
	List<DnsService> findAllServices();
	
	@SqlUpdate("UPDATE dns_providers SET name = :name, dns_service_id = :dnsServiceId, "
			+ "date_edit = :dateEdit, api_key = :apiKey, secret_key = :secretKey WHERE id = :id")
	void update(@BindBean DnsProvider provider);

	@SqlUpdate("DELETE FROM dns_providers WHERE id = :id")
	void deleteById(@Bind("id") int id);

	@SqlQuery("SELECT * FROM dns_providers WHERE name = :name")
	DnsProvider findByName(@Bind("name") String name);
}
