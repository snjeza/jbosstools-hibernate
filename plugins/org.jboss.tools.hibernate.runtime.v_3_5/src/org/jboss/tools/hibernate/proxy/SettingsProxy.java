package org.jboss.tools.hibernate.proxy;

import org.hibernate.cfg.Settings;
import org.hibernate.connection.ConnectionProvider;
import org.jboss.tools.hibernate.runtime.common.AbstractSettingsFacade;
import org.jboss.tools.hibernate.runtime.spi.IFacadeFactory;

public class SettingsProxy extends AbstractSettingsFacade {
	
	private Settings target;

	public SettingsProxy(
			IFacadeFactory facadeFactory, 
			Settings settings) {
		super(facadeFactory, settings);
		target = settings;
	}

	public Settings getTarget() {
		return (Settings)super.getTarget();
	}

	@Override
	public String getDefaultCatalogName() {
		return target.getDefaultCatalogName();
	}

	@Override
	public String getDefaultSchemaName() {
		return target.getDefaultSchemaName();
	}
	
	/*
	 * @deprecated This method is not supported anymore in recent Hibernate versions
	 */
	@Deprecated
	public ConnectionProvider getConnectionProvider() {
		return target.getConnectionProvider();
	}

}
