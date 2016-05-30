/**
 * 
 */
package com.bsb.hike.ces;

import org.json.JSONObject;

/**
 * @author suyash
 *
 */
public abstract class CesDataInfoFormatBuilder< C extends CesDataInfoFormatBuilder<C> >
{
	protected String netType;
	protected String module;
	protected abstract C self();

	public C setNetType(String netType)
	{
		this.netType = netType;
		return self();
	}

	public C setModule(String module)
	{
		this.module = module;
		return self();
	}

	public abstract JSONObject buildLevelOneInfo();
	public abstract JSONObject buildLevelTwoInfo();
}
