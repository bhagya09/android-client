package com.bsb.hike.ui;

import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.bsb.hike.AppConfig;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class ServerHostChangeActivity extends HikeAppStateBaseFragmentActivity implements OnClickListener
{

	private EditText mqttHost;

	private EditText mqttPort;

	private EditText httpHost;

	private EditText httpPort;

	@Override
	public void onCreate(Bundle savedState)
	{
		super.onCreate(savedState);

		if (!AppConfig.ALLOW_STAGING_TOGGLE)
		{
			// This activity should not open in production builds.
			finish();
		}

		setContentView(R.layout.serverhostchange_activity);

		mqttHost = (EditText) findViewById(R.id.mqtthost);
		mqttPort = (EditText) findViewById(R.id.mqttport);
		httpHost = (EditText) findViewById(R.id.httphost);
		httpPort = (EditText) findViewById(R.id.httpport);

		findViewById(R.id.change_server_urls_button).setOnClickListener(this);
	}

	@Override
	public void onClick(View v)
	{
		if (v.getId() == R.id.change_server_urls_button)
		{
			if (isValidInput())
			{
				Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();

				editor.putString(HikeMessengerApp.CUSTOM_MQTT_HOST, mqttHost.getText().toString());
				editor.putInt(HikeMessengerApp.CUSTOM_MQTT_PORT, Integer.parseInt(mqttPort.getText().toString()));
				editor.putString(HikeMessengerApp.CUSTOM_HTTP_HOST, httpHost.getText().toString());
				editor.putInt(HikeMessengerApp.CUSTOM_HTTP_PORT, Integer.parseInt(httpPort.getText().toString()));

				editor.putInt(HikeMessengerApp.PRODUCTION_HOST_TOGGLE, AccountUtils._CUSTOM_HOST);
				editor.putBoolean(HikeMessengerApp.PRODUCTION, false);

				editor.commit();
				
				HttpRequestConstants.toggleStaging();
				
				Utils.setupUri();

				finish();
				Toast.makeText(this, AccountUtils.base, Toast.LENGTH_SHORT).show();
			}
			else
			{
				Toast.makeText(this, "Do not leave any field empty.", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private boolean isValidInput()
	{
		if (mqttHost.getText().toString().isEmpty() || mqttPort.getText().toString().isEmpty() || httpHost.getText().toString().isEmpty()
				|| httpPort.getText().toString().isEmpty())
			return false;

		return true;
	}

}
