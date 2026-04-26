package net.pdynet.acmemanager.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.config.RegisterFieldMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import net.pdynet.acmemanager.model.ConfigRecord;

public interface ConfigDao {

	@SqlUpdate("MERGE INTO config (id, int_val, text_val) KEY(id) VALUES (:id, :int_val, :text_val)")
	void updateConfig(@Bind("id") String id, @Bind("int_val") int intValue, @Bind("text_val") String textValue);
	
	@SqlUpdate("MERGE INTO config (id, int_val, text_val) KEY(id) VALUES (:id, :intValue, :textValue)")
	void save(@BindBean ConfigRecord record);

	@SqlQuery("SELECT * FROM config WHERE id = :id")
	@RegisterFieldMapper(ConfigRecord.class)
	ConfigRecord findById(@Bind("id") String id);

	@SqlQuery("SELECT * FROM config ORDER BY id")
	@RegisterFieldMapper(ConfigRecord.class)
	List<ConfigRecord> findAll();

	@SqlUpdate("DELETE FROM config WHERE id = :id")
	void deleteById(@Bind("id") String id);
}
