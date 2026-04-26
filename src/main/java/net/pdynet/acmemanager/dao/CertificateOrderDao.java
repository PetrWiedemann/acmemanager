package net.pdynet.acmemanager.dao;

import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import net.pdynet.acmemanager.model.CertificateOrder;

@RegisterFieldMapper(CertificateOrder.class)
public interface CertificateOrderDao {

	@SqlUpdate("INSERT INTO certificate_orders (definition_id, status, order_url, date_write, date_edit, error_message) "
			+ "VALUES (:definitionId, :status, :orderUrl, :dateWrite, :dateEdit, :errorMessage)")
	@GetGeneratedKeys
	int insert(@BindBean CertificateOrder order);

	@SqlQuery("SELECT * FROM certificate_orders WHERE id = :id")
	CertificateOrder findById(@Bind("id") int id);

	@SqlQuery("SELECT * FROM certificate_orders WHERE definition_id = :defId ORDER BY date_write DESC")
	List<CertificateOrder> findAllByDefinition(@Bind("defId") int definitionId);

	@SqlQuery("SELECT * FROM certificate_orders WHERE definition_id = :defId ORDER BY date_write DESC LIMIT 1")
	CertificateOrder findLatestByDefinition(@Bind("defId") int definitionId);

	@SqlUpdate("UPDATE certificate_orders SET status = :status, order_url = :orderUrl, "
			+ "date_edit = :dateEdit, error_message = :errorMessage WHERE id = :id")
	void update(@BindBean CertificateOrder order);

	@SqlQuery("SELECT * FROM certificate_orders WHERE status = :status")
	List<CertificateOrder> findByStatus(@Bind("status") String status);
	
	@SqlUpdate("DELETE FROM certificate_orders WHERE id = :id")
	void deleteById(@Bind("id") int id);
	
	@SqlQuery("SELECT o.id "
			+ "FROM certificate_orders o "
			+ "LEFT JOIN issued_certificates c ON c.order_id = o.id "
			+ "WHERE o.definition_id = :defId "
			+ "  AND (c.id IS NULL OR c.not_after < CURRENT_TIMESTAMP) "
			+ "ORDER BY o.id DESC "
			+ "OFFSET 5")
	List<Integer> findObsoleteOrderIds(@Bind("defId") int definitionId);

	@SqlUpdate("DELETE FROM issued_certificates WHERE order_id IN (<orderIds>)")
	void deleteIssuedCertsByOrderIds(@BindList("orderIds") List<Integer> orderIds);

	@SqlUpdate("DELETE FROM certificate_orders WHERE id IN (<orderIds>)")
	void deleteOrdersByIds(@BindList("orderIds") List<Integer> orderIds);	
}
