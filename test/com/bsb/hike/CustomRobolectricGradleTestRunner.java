package com.bsb.hike;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.Fs;

/**
 * Created by ashishagarwal on 27/05/16.
 */
public class CustomRobolectricGradleTestRunner extends RobolectricTestRunner
{ // https://github.com/JakeWharton/gradle-android-test-plugin/blob/master/example/src/test/java/com/example/RobolectricGradleTestRunner.java
	public CustomRobolectricGradleTestRunner(Class<?> testClass) throws InitializationError
	{
		super(testClass);
	}

	@Override
	protected AndroidManifest getAppManifest(Config config)
	{
		String manifestProperty = System.getProperty("android.manifest");
		if (config.manifest().equals(Config.DEFAULT) && manifestProperty != null)
		{
			String defaultPackageName = System.getProperty("default.package");
			String resProperty = System.getProperty("android.resources");
			String assetsProperty = System.getProperty("android.assets");
			AndroidManifest androidManifest = new AndroidManifest(Fs.fileFromPath(manifestProperty), Fs.fileFromPath(resProperty), Fs.fileFromPath(assetsProperty));
			androidManifest.setPackageName(defaultPackageName); // here
			return androidManifest;
		}
		AndroidManifest appManifest = super.getAppManifest(config);
		return appManifest;
	}
}