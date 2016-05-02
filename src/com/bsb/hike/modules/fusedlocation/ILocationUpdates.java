package com.bsb.hike.modules.fusedlocation;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by piyush on 01/05/16.
 */
public interface ILocationUpdates
{
	void onLocationChanged(Location var1);

	void onConnected(@Nullable Bundle var1);

}
