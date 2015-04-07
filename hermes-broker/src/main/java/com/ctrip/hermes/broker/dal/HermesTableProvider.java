package com.ctrip.hermes.broker.dal;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.unidal.dal.jdbc.QueryDef;
import org.unidal.dal.jdbc.QueryEngine;
import org.unidal.dal.jdbc.QueryType;
import org.unidal.dal.jdbc.mapping.TableProvider;
import org.unidal.lookup.annotation.Inject;

import com.ctrip.hermes.broker.dal.hermes.MessagePriority;
import com.ctrip.hermes.broker.dal.hermes.OffsetMessage;
import com.ctrip.hermes.broker.dal.hermes.OffsetResend;
import com.ctrip.hermes.broker.dal.hermes.ResendGroupId;
import com.ctrip.hermes.core.meta.MetaService;
import com.ctrip.hermes.meta.entity.Partition;

public class HermesTableProvider implements TableProvider, Initializable {

	@Inject
	private MetaService m_metaService;

	private String m_table;

	private Map<Class<?>, InnerTableProvider<?>> m_providers = new HashMap<>();

	@Override
	public String getLogicalTableName() {
		return m_table;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public String getDataSourceName(Map<String, Object> hints) {
		QueryDef def = (QueryDef) hints.get(QueryEngine.HINT_QUERY);
		Object dataObject = hints.get(QueryEngine.HINT_DATA_OBJECT);

		InnerTableProvider provider = m_providers.get(dataObject.getClass());

		if (provider == null) {
			throw new IllegalArgumentException(String.format("TableProvider for class '%s' not found",
			      dataObject.getClass()));
		} else {
			return provider.getDataSourceName(def, dataObject);
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public String getPhysicalTableName(Map<String, Object> hints) {
		Object dataObject = hints.get(QueryEngine.HINT_DATA_OBJECT);

		InnerTableProvider provider = m_providers.get(dataObject.getClass());

		if (provider == null) {
			throw new IllegalArgumentException(String.format("TableProvider for class '%s' not found",
			      dataObject.getClass()));
		} else {
			return provider.getPhysicalTableName(dataObject);
		}
	}

	@Override
	public void initialize() throws InitializationException {
		m_providers.put(MessagePriority.class, new InnerTableProvider<MessagePriority>() {

			@Override
			public String getDataSourceName(QueryDef def, MessagePriority dataObject) {
				return findDataSourceName(def, dataObject.getTopic(), dataObject.getPartition());
			}

			@Override
			public String getPhysicalTableName(MessagePriority dataObject) {
				String fmt = "m_%s_%s_%s";
				return String.format(fmt, dataObject.getTopic(), dataObject.getPartition(), dataObject.getPriority());
			}
		});

		m_providers.put(ResendGroupId.class, new InnerTableProvider<ResendGroupId>() {

			@Override
			public String getDataSourceName(QueryDef def, ResendGroupId dataObject) {
				return findDataSourceName(def, dataObject.getTopic(), dataObject.getPartition());
			}

			@Override
			public String getPhysicalTableName(ResendGroupId dataObject) {
				String fmt = "r_%s_%s";
				return String.format(fmt, dataObject.getTopic(), dataObject.getGroupId());
			}
		});

		m_providers.put(OffsetMessage.class, new InnerTableProvider<OffsetMessage>() {

			@Override
			public String getDataSourceName(QueryDef def, OffsetMessage dataObject) {
				return findDataSourceName(def, dataObject.getTopic(), dataObject.getPartition());
			}

			@Override
			public String getPhysicalTableName(OffsetMessage dataObject) {
				String fmt = "o_%s";
				return String.format(fmt, dataObject.getTopic());
			}
		});

		m_providers.put(OffsetResend.class, new InnerTableProvider<OffsetResend>() {

			@Override
			public String getDataSourceName(QueryDef def, OffsetResend dataObject) {
				return findDataSourceName(def, dataObject.getTopic(), dataObject.getPartition());
			}

			@Override
			public String getPhysicalTableName(OffsetResend dataObject) {
				String fmt = "o_%s";
				return String.format(fmt, dataObject.getTopic());
			}
		});
	}

	private String findDataSourceName(QueryDef def, String topic, int partition) {
		QueryType queryType = def.getType();

		// TODO cache the result in meta service for better performance
		Partition p = m_metaService.findPartition(topic, partition);

		switch (queryType) {
		case INSERT:
		case DELETE:
		case UPDATE:
			return p.getWriteDatasource();

		case SELECT:
			return p.getReadDatasource();

		default:
			throw new IllegalArgumentException(String.format("Unknown query type '%s'", queryType));
		}
	}

	public interface InnerTableProvider<T> {
		String getDataSourceName(QueryDef def, T dataObject);

		String getPhysicalTableName(T dataObject);
	}

}
