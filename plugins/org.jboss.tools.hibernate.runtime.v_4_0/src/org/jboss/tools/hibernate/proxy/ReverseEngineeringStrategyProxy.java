package org.jboss.tools.hibernate.proxy;

import org.hibernate.cfg.reveng.ReverseEngineeringSettings;
import org.hibernate.cfg.reveng.ReverseEngineeringStrategy;
import org.jboss.tools.hibernate.runtime.common.AbstractReverseEngineeringStrategyFacade;
import org.jboss.tools.hibernate.runtime.common.IFacade;
import org.jboss.tools.hibernate.runtime.spi.IFacadeFactory;
import org.jboss.tools.hibernate.runtime.spi.IReverseEngineeringSettings;

public class ReverseEngineeringStrategyProxy 
extends AbstractReverseEngineeringStrategyFacade  {
	
	private ReverseEngineeringStrategy target = null;
	
	public ReverseEngineeringStrategyProxy(
			IFacadeFactory facadeFactory, 
			ReverseEngineeringStrategy res) {
		super(facadeFactory, res);
		target = res;
	}

	public ReverseEngineeringStrategy getTarget() {
		return target;
	}

	@Override
	public void setSettings(IReverseEngineeringSettings settings) {
		assert settings instanceof IFacade;
		target.setSettings((ReverseEngineeringSettings)((IFacade)settings).getTarget());
	}

}
