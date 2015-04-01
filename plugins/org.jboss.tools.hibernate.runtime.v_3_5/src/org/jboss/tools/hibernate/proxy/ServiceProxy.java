package org.jboss.tools.hibernate.proxy;

import java.io.File;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.JDBCMetaDataConfiguration;
import org.hibernate.cfg.JDBCReaderFactory;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.reveng.DefaultDatabaseCollector;
import org.hibernate.cfg.reveng.DefaultReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.JDBCReader;
import org.hibernate.cfg.reveng.OverrideRepository;
import org.hibernate.cfg.reveng.ReverseEngineeringSettings;
import org.hibernate.cfg.reveng.ReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.TableFilter;
import org.hibernate.cfg.reveng.TableIdentifier;
import org.hibernate.connection.DriverManagerConnectionProvider;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.resolver.DialectFactory;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Table;
import org.hibernate.proxy.HibernateProxyHelper;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2x.Exporter;
import org.hibernate.tool.hbm2x.HibernateMappingGlobalSettings;
import org.hibernate.tool.ide.completion.HQLCodeAssist;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;
import org.jboss.tools.hibernate.runtime.common.IFacade;
import org.jboss.tools.hibernate.runtime.common.Util;
import org.jboss.tools.hibernate.runtime.spi.HibernateException;
import org.jboss.tools.hibernate.runtime.spi.IArtifactCollector;
import org.jboss.tools.hibernate.runtime.spi.ICfg2HbmTool;
import org.jboss.tools.hibernate.runtime.spi.IColumn;
import org.jboss.tools.hibernate.runtime.spi.IConfiguration;
import org.jboss.tools.hibernate.runtime.spi.IDatabaseCollector;
import org.jboss.tools.hibernate.runtime.spi.IDialect;
import org.jboss.tools.hibernate.runtime.spi.IEnvironment;
import org.jboss.tools.hibernate.runtime.spi.IExporter;
import org.jboss.tools.hibernate.runtime.spi.IFacadeFactory;
import org.jboss.tools.hibernate.runtime.spi.IHQLCodeAssist;
import org.jboss.tools.hibernate.runtime.spi.IHQLQueryPlan;
import org.jboss.tools.hibernate.runtime.spi.IHibernateMappingExporter;
import org.jboss.tools.hibernate.runtime.spi.IHibernateMappingGlobalSettings;
import org.jboss.tools.hibernate.runtime.spi.IJDBCReader;
import org.jboss.tools.hibernate.runtime.spi.IMetaDataDialect;
import org.jboss.tools.hibernate.runtime.spi.INamingStrategy;
import org.jboss.tools.hibernate.runtime.spi.IOverrideRepository;
import org.jboss.tools.hibernate.runtime.spi.IPersistentClass;
import org.jboss.tools.hibernate.runtime.spi.IPrimaryKey;
import org.jboss.tools.hibernate.runtime.spi.IProperty;
import org.jboss.tools.hibernate.runtime.spi.IReverseEngineeringSettings;
import org.jboss.tools.hibernate.runtime.spi.IReverseEngineeringStrategy;
import org.jboss.tools.hibernate.runtime.spi.ISchemaExport;
import org.jboss.tools.hibernate.runtime.spi.IService;
import org.jboss.tools.hibernate.runtime.spi.ISessionFactory;
import org.jboss.tools.hibernate.runtime.spi.ISettings;
import org.jboss.tools.hibernate.runtime.spi.ITable;
import org.jboss.tools.hibernate.runtime.spi.ITableFilter;
import org.jboss.tools.hibernate.runtime.spi.ITableIdentifier;
import org.jboss.tools.hibernate.runtime.spi.ITypeFactory;
import org.jboss.tools.hibernate.runtime.spi.IValue;
import org.jboss.tools.hibernate.runtime.v_3_5.internal.FacadeFactoryImpl;
import org.jboss.tools.hibernate.util.OpenMappingUtilsEjb3;
import org.xml.sax.EntityResolver;

public class ServiceProxy implements IService {
	
	private IFacadeFactory facadeFactory = new FacadeFactoryImpl();

	@Override
	public IConfiguration newAnnotationConfiguration() {
		Configuration configuration = new AnnotationConfiguration();
		return new ConfigurationProxy(facadeFactory, configuration);
	}

	@Override
	public IConfiguration newJpaConfiguration(
			String entityResolver, 
			String persistenceUnit, 
			Map<Object, Object> overrides) {
		Ejb3Configuration ejb3Configuration = new Ejb3Configuration();
		if (StringHelper.isNotEmpty(entityResolver)) {
			try {
				Class<?> resolver = ReflectHelper.classForName(entityResolver, this.getClass());
				Object object = resolver.newInstance();
				ejb3Configuration.setEntityResolver((EntityResolver)object);
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				throw new HibernateException(e);
			}
		}
		ejb3Configuration.configure(persistenceUnit, overrides);
		Configuration configuration = ejb3Configuration.getHibernateConfiguration();
		return new ConfigurationProxy(facadeFactory, configuration);
	}

	@Override
	public IConfiguration newDefaultConfiguration() {
		return new ConfigurationProxy(facadeFactory, new Configuration());
	}

	@Override
	public IHibernateMappingExporter newHibernateMappingExporter(
			IConfiguration hcfg, File file) {
		return new HibernateMappingExporterProxy(hcfg, file);
	}

	@Override
	public ISchemaExport newSchemaExport(IConfiguration hcfg) {
		ISchemaExport result = null;
		if (hcfg instanceof ConfigurationProxy) {
			SchemaExport schemaExport = 
					new SchemaExport(((ConfigurationProxy)hcfg).getTarget());
			result = facadeFactory.createSchemaExport(schemaExport);
		}
		return result;
	}

	@Override
	public IHQLCodeAssist newHQLCodeAssist(IConfiguration hcfg) {
		IHQLCodeAssist result = null;
		if (hcfg instanceof ConfigurationProxy) {
			result = new HQLCodeAssistProxy(
					facadeFactory,
					new HQLCodeAssist(((ConfigurationProxy)hcfg).getTarget()));
		}
		return result;
	}

	@Override
	public IConfiguration newJDBCMetaDataConfiguration() {
		Configuration configuration = new JDBCMetaDataConfiguration();
		return new ConfigurationProxy(facadeFactory, configuration);
	}

	@Override
	public IExporter createExporter(String exporterClassName) {
		Exporter exporter = (Exporter)Util.getInstance(
				exporterClassName, 
				facadeFactory.getClassLoader());
		return facadeFactory.createExporter(exporter);
	}

	@Override
	public IArtifactCollector newArtifactCollector() {
		return facadeFactory.createArtifactCollector();
	}

	@Override
	public IHQLQueryPlan newHQLQueryPlan(
			String query, 
			boolean shallow,
			ISessionFactory sessionFactory) {
		return new HQLQueryPlanProxy(query, shallow, sessionFactory);
	}

	@Override
	public ITypeFactory newTypeFactory() {
		return new TypeFactoryProxy(facadeFactory);
	}

	@Override
	public INamingStrategy newNamingStrategy(String strategyClassName) {
		try {
			NamingStrategy ns = (NamingStrategy) ReflectHelper.classForName(
					strategyClassName).newInstance();
			return facadeFactory.createNamingStrategy(ns);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			return null;
		}
	}

	@Override
	public IOverrideRepository newOverrideRepository() {
		return facadeFactory.createOverrideRepository(new OverrideRepository());
	}

	@Override
	public ITableFilter newTableFilter() {
		return facadeFactory.createTableFilter(new TableFilter());
	}

	@Override
	public IReverseEngineeringSettings newReverseEngineeringSettings(
			IReverseEngineeringStrategy res) {
		assert res instanceof IFacade;
		return facadeFactory.createReverseEngineeringSettings(
				new ReverseEngineeringSettings(
						(ReverseEngineeringStrategy)((IFacade)res).getTarget()));
	}

	@Override
	public IReverseEngineeringStrategy newDefaultReverseEngineeringStrategy() {
		return facadeFactory.createReverseEngineeringStrategy(
				new DefaultReverseEngineeringStrategy());
	}

	@Override
	public IJDBCReader newJDBCReader(IConfiguration configuration, ISettings settings,
			IReverseEngineeringStrategy strategy) {
		assert strategy instanceof IFacade;
		assert settings instanceof SettingsProxy;
		JDBCReader target = 
				JDBCReaderFactory.newJDBCReader(
						configuration.getProperties(), 
						((SettingsProxy)settings).getTarget(), 
						(ReverseEngineeringStrategy)((IFacade)strategy).getTarget());
		return new JDBCReaderProxy(target);
	}

	@Override
	public IReverseEngineeringStrategy newReverseEngineeringStrategy(
			String strategyName, IReverseEngineeringStrategy delegate) {
		assert delegate instanceof IFacade;
		ReverseEngineeringStrategy target = 
				newReverseEngineeringStrategy(strategyName, (ReverseEngineeringStrategy)((IFacade)delegate).getTarget());
		return facadeFactory.createReverseEngineeringStrategy(target);
	}
	

	@SuppressWarnings("unchecked")
	private ReverseEngineeringStrategy newReverseEngineeringStrategy(final String className, ReverseEngineeringStrategy delegate) {
        try {
            Class<ReverseEngineeringStrategy> clazz = (Class<ReverseEngineeringStrategy>)ReflectHelper.classForName(className);
			Constructor<ReverseEngineeringStrategy> constructor = clazz.getConstructor(new Class[] { ReverseEngineeringStrategy.class });
            return constructor.newInstance(new Object[] { delegate });
        }
        catch (NoSuchMethodException e) {
			try {
				Class<?> clazz = ReflectHelper.classForName(className);
				ReverseEngineeringStrategy rev = (ReverseEngineeringStrategy) clazz.newInstance();
				return rev;
			}
			catch (Exception eq) {
				throw new HibernateException(eq);
			}
		}
        catch (Exception e) {
			throw new HibernateException(e);
		}
    }

	@Override
	public String getReverseEngineeringStrategyClassName() {
		return ReverseEngineeringStrategy.class.getName();
	}

	@Override
	public IDatabaseCollector newDatabaseCollector(IMetaDataDialect metaDataDialect) {
		assert metaDataDialect instanceof MetaDataDialectProxy;
		return new DatabaseCollectorProxy(
				facadeFactory,
				new DefaultDatabaseCollector(
						((MetaDataDialectProxy)metaDataDialect).getTarget()));
	}

	@Override
	public ICfg2HbmTool newCfg2HbmTool() {
		return facadeFactory.createCfg2HbmTool();
	}

	@Override
	public IProperty newProperty() {
		return new PropertyProxy(new Property());
	}

	@Override
	public ITable newTable(String name) {
		return new TableProxy(facadeFactory, new Table(name));
	}

	@Override
	public IColumn newColumn(String string) {
		return new ColumnProxy(facadeFactory, new Column(string));
	}

	@Override
	public IDialect newDialect(Properties properties, Connection connection) {
		Dialect dialect = null;
		if (connection == null) {
			dialect = DialectFactory.buildDialect(properties);
		} else {
			dialect = DialectFactory.buildDialect(properties, connection);
		}
		return dialect != null ? facadeFactory.createDialect(dialect) : null;
	}

	@Override
	public Class<?> getDriverManagerConnectionProviderClass() {
		return DriverManagerConnectionProvider.class;
	}

	@Override
	public IEnvironment getEnvironment() {
		return new EnvironmentProxy(facadeFactory);
	}

	@Override
	public IValue newSimpleValue() {
		return new ValueProxy(facadeFactory, new SimpleValue());
	}

	@Override
	public IValue newPrimitiveArray(IPersistentClass persistentClass) {
		assert persistentClass instanceof PersistentClassProxy;
		return new ValueProxy(facadeFactory, new PrimitiveArray(((PersistentClassProxy)persistentClass).getTarget()));
	}

	@Override
	public IValue newArray(IPersistentClass persistentClass) {
		assert persistentClass instanceof PersistentClassProxy;
		return new ValueProxy(facadeFactory, new Array(((PersistentClassProxy)persistentClass).getTarget()));
	}

	@Override
	public IValue newBag(IPersistentClass persistentClass) {
		assert persistentClass instanceof PersistentClassProxy;
		return new ValueProxy(facadeFactory, new Bag(((PersistentClassProxy)persistentClass).getTarget()));
	}

	@Override
	public IValue newList(IPersistentClass persistentClass) {
		assert persistentClass instanceof PersistentClassProxy;
		return new ValueProxy(facadeFactory, new org.hibernate.mapping.List(((PersistentClassProxy)persistentClass).getTarget()));
	}

	@Override
	public IValue newMap(IPersistentClass persistentClass) {
		assert persistentClass instanceof PersistentClassProxy;
		return new ValueProxy(facadeFactory, new org.hibernate.mapping.Map(((PersistentClassProxy)persistentClass).getTarget()));
	}

	@Override
	public IValue newSet(IPersistentClass persistentClass) {
		assert persistentClass instanceof PersistentClassProxy;
		return new ValueProxy(facadeFactory, new Set(((PersistentClassProxy)persistentClass).getTarget()));
	}

	@Override
	public IValue newManyToOne(ITable table) {
		assert table instanceof TableProxy;
		return new ValueProxy(facadeFactory, new ManyToOne(((TableProxy)table).getTarget()));
	}

	@Override
	public IValue newOneToMany(IPersistentClass persistentClass) {
		assert persistentClass instanceof PersistentClassProxy;
		return new ValueProxy(facadeFactory, new OneToMany(((PersistentClassProxy)persistentClass).getTarget()));
	}

	@Override
	public IValue newOneToOne(IPersistentClass persistentClass) {
		assert persistentClass instanceof PersistentClassProxy;
		return new ValueProxy(facadeFactory, new OneToOne(((PersistentClassProxy)persistentClass).getTarget().getTable(), ((PersistentClassProxy)persistentClass).getTarget()));
	}

	@Override
	public IPersistentClass newSingleTableSubclass(
			IPersistentClass persistentClass) {
		assert persistentClass instanceof PersistentClassProxy;
		return new PersistentClassProxy(facadeFactory, new SingleTableSubclass(((PersistentClassProxy)persistentClass).getTarget()));
	}

	@Override
	public IPersistentClass newJoinedSubclass(IPersistentClass persistentClass) {
		assert persistentClass instanceof PersistentClassProxy;
		return new PersistentClassProxy(facadeFactory, new JoinedSubclass(((PersistentClassProxy)persistentClass).getTarget()));
	}

	@Override
	public IPersistentClass newSpecialRootClass(IProperty ormElement) {
		return new SpecialRootClassProxy(facadeFactory, ormElement);
	}

	@Override
	public IPersistentClass newRootClass() {
		return new PersistentClassProxy(facadeFactory, new RootClass());
	}

	@Override
	public IPrimaryKey newPrimaryKey() {
		return new PrimaryKeyProxy(facadeFactory, new PrimaryKey());
	}

	@Override
	public IHibernateMappingGlobalSettings newHibernateMappingGlobalSettings() {
		return facadeFactory.createHibernateMappingGlobalSettings(
				new HibernateMappingGlobalSettings());
	}

	@Override
	public ITableIdentifier createTableIdentifier(ITable table) {
		assert table instanceof TableProxy;
		return facadeFactory.createTableIdentifier(
				TableIdentifier.create(
						((TableProxy)table).getTarget()));
	}

	@Override
	public ITableIdentifier newTableIdentifier(String catalog, String schema,
			String name) {
		return facadeFactory.createTableIdentifier(
				new TableIdentifier(catalog, schema, name));
	}

	@Override
	public boolean isInitialized(Object object) {
		return Hibernate.isInitialized(object);
	}

	@Override
	public List<String> getJPAMappingFilePaths(
			String persistenceUnitName, EntityResolver entityResolver) {
		return OpenMappingUtilsEjb3.enumDocuments(persistenceUnitName, entityResolver);
	}

	@Override
	public Class<?> getClassWithoutInitializingProxy(Object reflectedObject) {
		return HibernateProxyHelper.getClassWithoutInitializingProxy(reflectedObject);
	}

	@Override
	public ClassLoader getClassLoader() {
		return ServiceProxy.class.getClassLoader();
	}
	
	

}
