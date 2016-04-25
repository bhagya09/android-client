package com.bsb.hike.ui;

import java.io.UnsupportedEncodingException;

import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.AppConfig;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.filetransfer.FTUtils;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.HikeSDKConstants;
import com.bsb.hike.platform.HikeSDKResponseCode;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.authSDKRequest;

/**
 * This activity is responsible for displaying "connect-with-hike" dialog and providing data as per user action.
 * 
 * @author AtulM
 * 
 */
public class HikeAuthActivity extends Activity
{
	private String mAppName;

	private String mAppVersion;

	private Drawable mAppIconLogo;

	private String mAppId;

	private String mAppPackage;

	private Message message;

	private IconLoader profileImageLoader;

	private String userContactId;

	private RequestToken requestToken;

	/** The Constant MESSAGE_INDEX. Used for passing messages between activities */
	public static final String MESSAGE_INDEX = "MESSAGE_INDEX";

	public static final String AUTH_SHARED_PREF_NAME = "364i5j6b3oj4";

	public static final String AUTH_SHARED_PREF_PKG_KEY = "ilugasdgi2";

	private static final long MIN_LOADING_TIME = 4 * 1000;

	/** The current state. */
	private byte CURRENT_STATE = 0;

	/** The state normal. */
	private final byte STATE_NORMAL = 0;

	/** The state is connecting. */
	private final byte STATE_IS_CONNECTING = 1;

	/** The state retry connection. */
	private final byte STATE_RETRY_CONNECTION = 2;

	/** The state connected. */
	private final byte STATE_CONNECTED = 3;

	private TextView auth_title;

	private TextView auth_app_name;

	private TextView auth_app_dev;

	private ImageView auth_app_icon;

	private TextView textViewDropdown;

	private TextView auth_app_will_setup_acc;

	private Button auth_button_accept;

	private Button auth_button_deny;

	private View layout_app_info;

	private View auth_info_layout;

	private View auth_button_deny_layout;

	private View auth_buttons_layout;

	private View layout_conn_state;

	private TextView text_conn_state;

	private ImageView image_conn_state;

	private View progress_bar_conn_state;

	/**
	 * Dont make call to server. Generate token in code.
	 */
	private boolean bypassAuthHttp = false;

	private long connectingTimestamp;

	private static Handler stateHandler;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.bsb.hike.utils.HikeAppStateBaseFragmentActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.auth_main);

		stateHandler = new Handler(Looper.getMainLooper());

		ContactInfo contactInfo = Utils.getUserContactInfo(getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE));

		userContactId = contactInfo.getMsisdn();

		profileImageLoader = new IconLoader(this, getResources().getDimensionPixelSize(R.dimen.auth_permission_icon));

		retrieveContent();

		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_AUTH_DIALOG_VIEWED);
			metadata.put(HikeConstants.LogEvent.SOURCE_APP, HikePlatformConstants.GAME_SDK_ID);
			metadata.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, mAppPackage);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		initVar();

		bindContentsAndActions();

		CURRENT_STATE = STATE_NORMAL;
	}

	/**
	 * Initialize variables and references.
	 */
	private void initVar()
	{
		auth_title = ((TextView) findViewById(R.id.auth_title));

		auth_app_name = ((TextView) findViewById(R.id.auth_app_name));

		auth_app_dev = ((TextView) findViewById(R.id.auth_app_dev));

		auth_app_icon = ((ImageView) findViewById(R.id.auth_app_icon));

		textViewDropdown = ((TextView) findViewById(R.id.auth_app_access_to));

		auth_app_will_setup_acc = ((TextView) findViewById(R.id.auth_app_will_setup_acc));

		auth_button_accept = ((Button) findViewById(R.id.auth_button_accept));

		auth_button_deny = ((Button) findViewById(R.id.auth_button_deny));

		layout_app_info = findViewById(R.id.layout_app_info);

		auth_info_layout = findViewById(R.id.auth_info_layout);

		auth_button_deny_layout = findViewById(R.id.auth_button_deny_layout);

		auth_buttons_layout = findViewById(R.id.auth_buttons_layout);

		layout_conn_state = findViewById(R.id.layout_conn_state);

		text_conn_state = ((TextView) findViewById(R.id.text_conn_state));

		progress_bar_conn_state = findViewById(R.id.progress_bar_conn_state);

		image_conn_state = ((ImageView) findViewById(R.id.image_conn_state));
	}

	/**
	 * Retrieve content from intent.
	 */
	private void retrieveContent()
	{
		if (message == null)
		{
			message = (Message) getIntent().getParcelableExtra(MESSAGE_INDEX);
		}

		if (message != null)
		{
			Bundle authBundle = message.getData();

			if (authBundle != null)
			{
				String data = authBundle.getString(HikeSDKConstants.HIKE_REQ_DATA_ID);

				try
				{
					JSONObject authJSON = new JSONObject(data);
					mAppId = authJSON.getString(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_APP_ID);
					mAppPackage = authJSON.getString(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_PKG_NAME);

					if (TextUtils.isEmpty(mAppId) || TextUtils.isEmpty(mAppPackage))
					{
						onFailed("App id and App secret are mandatory for auth");
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
					onFailed("Improper JSON from SDK");
				}
			}
		}
		else
		{
			this.finish();
			return;
		}
		// We have host application package name. Get its name, dev name and image from package info
		PackageManager pm = getApplicationContext().getPackageManager();
		try
		{
			PackageInfo packageInfo = pm.getPackageInfo(mAppPackage, PackageManager.GET_ACTIVITIES);
			mAppName = packageInfo.applicationInfo.loadLabel(getApplicationContext().getPackageManager()).toString();
			mAppIconLogo = packageInfo.applicationInfo.loadIcon(pm);
			mAppVersion = "ver " + packageInfo.versionName;
		}
		catch (PackageManager.NameNotFoundException e)
		{
			e.printStackTrace();
			onFailed("Invalid/unavailable package name");
		}
	}

	/**
	 * Bind data and actions.
	 */
	private void bindContentsAndActions()
	{
		try
		{
			// Set title
			auth_title.setText(String.format(getApplicationContext().getString(R.string.auth_title), mAppName));

			// Set app name
			auth_app_name.setText(mAppName);

			// Set app version
			auth_app_dev.setText(mAppVersion);

			// Set app icon
			auth_app_icon.setImageDrawable(mAppIconLogo);

			// Set "will have access to" text
			textViewDropdown.setText(String.format(getApplicationContext().getString(R.string.auth_app_access_to), mAppName));

			textViewDropdown.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					TextView dropdown = ((TextView) findViewById(R.id.auth_app_access_to));
					if (dropdown.getTag().equals("0"))
					{
						dropdown.setCompoundDrawablesWithIntrinsicBounds(getApplicationContext().getResources().getDrawable(R.drawable.arrowup), null, null, null);
						dropdown.setTag("1");
						findViewById(R.id.auth_info_layout).setVisibility(View.VISIBLE);
					}
					else
					{
						dropdown.setCompoundDrawablesWithIntrinsicBounds(getApplicationContext().getResources().getDrawable(R.drawable.arrowdown), null, null, null);
						dropdown.setTag("0");
						findViewById(R.id.auth_info_layout).setVisibility(View.GONE);
					}
				}
			});

			stateHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					if (textViewDropdown != null && textViewDropdown.getTag().equals("0"))
					{
						if (CURRENT_STATE == STATE_NORMAL)
						{
							textViewDropdown.setCompoundDrawablesWithIntrinsicBounds(getApplicationContext().getResources().getDrawable(R.drawable.arrowup), null, null, null);
							textViewDropdown.setTag("1");
							findViewById(R.id.auth_info_layout).setVisibility(View.VISIBLE);
						}
					}
				}
			}, 2000);

			// Set "will setup account on" text
			auth_app_will_setup_acc.setText(String.format(getApplicationContext().getString(R.string.auth_will_setup_acc), mAppName));

			// Set user avatar
			profileImageLoader.loadImage(userContactId, ((ImageView) findViewById(R.id.auth_user_avatar)), false, true, true);

			// Set default contact avatar
			profileImageLoader.loadImage("nullround", ((ImageView) findViewById(R.id.auth_contacts_avatar)), false, false, true);

			// Set "connect" button event
			auth_button_accept.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					try
					{
						JSONObject metadata = new JSONObject();
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_AUTH_DIALOG_CONNECT);
						metadata.put(HikeConstants.LogEvent.SOURCE_APP, HikePlatformConstants.GAME_SDK_ID);
						metadata.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, mAppPackage);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
					requestAccess();
				}
			});

			// Set "declined" button event
			auth_button_deny.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					try
					{
						JSONObject metadata = new JSONObject();
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_AUTH_DIALOG_DECLINED);
						metadata.put(HikeConstants.LogEvent.SOURCE_APP, HikePlatformConstants.GAME_SDK_ID);
						metadata.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, mAppPackage);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
					message.arg2 = HikeSDKResponseCode.STATUS_FAILED;
					HikeService.mHikeSDKRequestHandler.handleMessage(message);
					Logger.d(HikeAuthActivity.class.getCanonicalName(), "shutting auth activity successfully!");
					HikeAuthActivity.this.finish();
				}
			});

		}
		catch (NullPointerException npe)
		{
			// Invalid layout file. Not usually possible.
			npe.printStackTrace();
		}

		((ImageView) findViewById(R.id.auth_hike_logo)).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (AppConfig.ALLOW_STAGING_TOGGLE)
				{
					if (bypassAuthHttp == true)
					{
						bypassAuthHttp = false;
						Toast.makeText(HikeAuthActivity.this.getApplicationContext(), "Auth enabled", Toast.LENGTH_SHORT).show();
					}
					else
					{
						bypassAuthHttp = true;
						Toast.makeText(HikeAuthActivity.this.getApplicationContext(), "Auth bypassed", Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
	}

	/**
	 * Request access token from hike api.
	 */
	public void requestAccess()
	{

		final HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance(AUTH_SHARED_PREF_NAME);
		
		displayIsConnectingState();

		List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();

		if (AccountUtils.SDK_AUTH_BASE.equals(AccountUtils.SDK_AUTH_BASE_URL_STAGING))
		{
			params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_RESPONSE_TYPE, "token"));
			params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_CLIENT_ID, "VIVTB9Y18EOCw7d-:VIVTqNY18EOCw7d_.apps.hike.in"));
			params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_SCOPE, "publish_actions"));
			params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_PACKAGE_NAME, mAppPackage));
			params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_SHA1, "test_sha1"));
		}
		else
		{
			params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_RESPONSE_TYPE, "token"));
			params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_CLIENT_ID, mAppId));
			params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_SCOPE, "publish_actions"));
			params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_PACKAGE_NAME, mAppPackage));
			params.add(new BasicNameValuePair(AccountUtils.SDK_AUTH_PARAM_SHA1, "test_sha1"));
		}

		String paramString = URLEncodedUtils.format(params, "UTF-8");
		try
		{
			paramString = URLDecoder.decode(paramString, "UTF-8");
		}
		catch (UnsupportedEncodingException e1)
		{
			e1.printStackTrace();
			displayRetryConnectionState();
			return;
		}

		RequestToken requestToken = authSDKRequest(paramString, new IRequestListener()
		{
			public void onRequestSuccess(Response result)
			{
				// response example - {"state":null,"expires_in":5184000,"access_token":"F78SWrzfx-SKNbo2QZZBHA==","token_type":"Bearer"}}
				try
				{
					Logger.d(HikeAuthActivity.class.getCanonicalName(), "on task success");
					JSONObject responseJSON = (JSONObject) result.getBody().getContent();
					JSONObject responseData = responseJSON.getJSONObject("response");
					if (responseData != null)
					{
						if (responseData.has("error"))
						{
							onRequestFailure(null);
							return;
						}
						String expiresIn = responseData.getString("expires_in");
						String accessToken = responseData.getString("access_token");

						if (TextUtils.isEmpty(prefs.getData(mAppPackage, "")))
						{
							prefs.saveData(AUTH_SHARED_PREF_PKG_KEY,
									TextUtils.isEmpty(prefs.getData(AUTH_SHARED_PREF_PKG_KEY, "")) ? mAppPackage + ":" + expiresIn : prefs.getData(AUTH_SHARED_PREF_PKG_KEY, "")
											+ "," + mAppPackage + ":" + expiresIn);
						}

						prefs.saveData(mAppPackage, Integer.toString(accessToken.hashCode()));

						HikeMessengerApp.getPubSub().publish(HikePubSub.AUTH_TOKEN_RECEIVED, accessToken);

						displayConnectedState();
					}
					else
					{
						onRequestFailure(null);
						return;
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
					onRequestFailure(null);
					return;
				}
				
			}
			
			@Override
			public void onRequestProgressUpdate(float progress)
			{
				
			}
			
			@Override
			public void onRequestFailure(HttpException httpException)
			{
				Logger.d(HikeAuthActivity.class.getCanonicalName(), "on task failed");

				if (bypassAuthHttp)
				{
					String expiresIn = "12341";
					String accessToken = "ashjfbqiywgr13irb";

					if (TextUtils.isEmpty(prefs.getData(mAppPackage, "")))
					{
						prefs.saveData(AUTH_SHARED_PREF_PKG_KEY,
								TextUtils.isEmpty(prefs.getData(AUTH_SHARED_PREF_PKG_KEY, "")) ? mAppPackage + ":" + expiresIn : prefs.getData(AUTH_SHARED_PREF_PKG_KEY, "") + ","
										+ mAppPackage + ":" + expiresIn);
					}

					prefs.saveData(mAppPackage, Integer.toString(accessToken.hashCode()));

					HikeMessengerApp.getPubSub().publish(HikePubSub.AUTH_TOKEN_RECEIVED, accessToken);

					displayConnectedState();
				}
				else
				{
					displayRetryConnectionState();
				}
				
			}
		});
		requestToken.execute();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop()
	{
		if (requestToken != null && requestToken.isRequestRunning())
		{
			requestToken.cancel();
		}

		super.onStop();
	}

	/**
	 * Gets the app name.
	 * 
	 * @return the app name
	 */
	public String getmAppName()
	{
		return mAppName;
	}

	/**
	 * Sets the app name.
	 * 
	 * @param mAppName
	 *            the new app name
	 */
	public void setmAppName(String mAppName)
	{
		this.mAppName = mAppName;
	}

	/**
	 * Gets the app dev name.
	 * 
	 * @return the app dev name
	 */
	public String getmAppDevName()
	{
		return mAppVersion;
	}

	/**
	 * Sets the app dev name.
	 * 
	 * @param mAppDevName
	 *            the new m app dev name
	 */
	public void setmAppDevName(String mAppDevName)
	{
		this.mAppVersion = mAppDevName;
	}

	/**
	 * Gets the app id.
	 * 
	 * @return the app id
	 */
	public String getmAppId()
	{
		return mAppId;
	}

	/**
	 * Sets the app id.
	 * 
	 * @param mAppId
	 *            the new app id
	 */
	public void setmAppId(String mAppId)
	{
		this.mAppId = mAppId;
	}

	/**
	 * Gets the app package.
	 * 
	 * @return the app package
	 */
	public String getmAppPackage()
	{
		return mAppPackage;
	}

	/**
	 * Sets the app package.
	 * 
	 * @param mAppPackage
	 *            the new app package
	 */
	public void setmAppPackage(String mAppPackage)
	{
		this.mAppPackage = mAppPackage;
	}

	/**
	 * Called whenever there is an exception/failure in auth process.
	 * 
	 * @param argMessage
	 *            the arg message
	 */
	public void onFailed(String argMessage)
	{
		Logger.d(HikeAuthActivity.class.getCanonicalName(), "onfailed is called");

		if (message != null)
		{
			message.arg2 = HikeSDKResponseCode.STATUS_FAILED;

			Bundle messageBundle = new Bundle();

			messageBundle.putString(HikeSDKConstants.HIKE_REQ_DATA_ID, argMessage);

			message.setData(messageBundle);

			try
			{
				message.replyTo.send(message);
				Logger.d(HikeAuthActivity.class.getCanonicalName(), "shutting auth activity successfully!");
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
			}
		}
		HikeAuthActivity.this.finish();
	}

	/**
	 * Gets the m app image.
	 * 
	 * @return the m app image
	 */
	public Drawable getmAppImage()
	{
		return mAppIconLogo;
	}

	/**
	 * Sets the m app image.
	 * 
	 * @param mAppImage
	 *            the new m app image
	 */
	public void setmAppImage(Drawable mAppImage)
	{
		this.mAppIconLogo = mAppImage;
	}

	/**
	 * Verify request.
	 * 
	 * @param argContext
	 *            the arg context
	 * @param argMessage
	 *            the arg message
	 * @return true, if successful
	 */
	public static boolean verifyRequest(Context argContext, Message argMessage)
	{
		Bundle messageData = argMessage.getData();
		String jsonString = messageData.getString(HikeSDKConstants.HIKE_REQ_DATA_ID);
		try
		{
			JSONObject jsonObject = new JSONObject(jsonString);
			String access = jsonObject.getString(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_ACC_TOKEN);
			String pkg = jsonObject.getString(HikeSDKConstants.HIKE_REQ_SDK_CLIENT_PKG_NAME);
			return verifyRequest(argContext, pkg, access);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return false;
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Verify request.
	 * 
	 * @param argContext
	 *            the arg context
	 * @param pkgName
	 *            the pkg name
	 * @param accessT
	 *            the access t
	 * @return true, if successful
	 */
	public static boolean verifyRequest(Context argContext, String pkgName, String accessT)
	{
		try
		{
			String access = Integer.toString(accessT.hashCode());
			String pkg = pkgName;
			HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance(AUTH_SHARED_PREF_NAME);
			String savedAccess = prefs.getData(pkg, "");
			if (access.equals(savedAccess))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause()
	{
		overridePendingTransition(0, 0);
		super.onPause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed()
	{
		onFailed("Declined");
		super.onBackPressed();
	}

	/**
	 * Display is connecting state.
	 */
	public void displayIsConnectingState()
	{
		CURRENT_STATE = STATE_IS_CONNECTING;
		
		if (auth_title == null)
		{
			return;
		}
		connectingTimestamp = System.currentTimeMillis();
		auth_button_accept.setText("");
		auth_title.setVisibility(View.GONE);
		layout_app_info.setVisibility(View.GONE);
		textViewDropdown.setVisibility(View.GONE);
		auth_info_layout.setVisibility(View.GONE);
		auth_button_deny_layout.setVisibility(View.GONE);

		// Hack to improve UX
		stateHandler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				if (auth_button_accept != null)
				{
					auth_button_accept.setText(getString(R.string.auth_state_connecting));
					auth_button_accept.setOnClickListener(null);
				}
			}
		}, 500);
		layout_conn_state.setVisibility(View.VISIBLE);
		text_conn_state.setText(getString(R.string.auth_please_wait_connect));
		image_conn_state.setVisibility(View.GONE);
		progress_bar_conn_state.setVisibility(View.VISIBLE);
	}

	/**
	 * Display connected state.
	 */
	public void displayConnectedState()
	{
		CURRENT_STATE = STATE_CONNECTED;
		long timestampDiff = System.currentTimeMillis() - connectingTimestamp;
		if (timestampDiff < MIN_LOADING_TIME)
		{
			stateHandler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					displayConnectedState();
				}
			}, timestampDiff);
			return;
		}

		if (auth_title == null)
		{
			return;
		}
		auth_title.setVisibility(View.GONE);
		auth_button_accept.setVisibility(View.GONE);
		layout_app_info.setVisibility(View.GONE);
		layout_app_info.setVisibility(View.GONE);
		auth_info_layout.setVisibility(View.GONE);
		auth_button_deny_layout.setVisibility(View.GONE);
		auth_buttons_layout.setVisibility(View.GONE);
		stateHandler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				if (auth_title == null)
				{
					return;
				}
				HikeAuthActivity.this.finish();
			}
		}, 2500);
		layout_conn_state.setVisibility(View.VISIBLE);
		text_conn_state.setText(String.format(getApplicationContext().getString(R.string.auth_connected_to_hike), mAppName));
		image_conn_state.setVisibility(View.VISIBLE);
		image_conn_state.setImageResource(R.drawable.ic_tick_auth);
		progress_bar_conn_state.setVisibility(View.GONE);
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_AUTH_SUCCESS);
			metadata.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, mAppPackage);
			metadata.put(HikeConstants.LogEvent.SOURCE_APP, HikePlatformConstants.GAME_SDK_ID);
			metadata.put(HikeConstants.Extras.SDK_CONNECTION_TYPE, FTUtils.getNetworkType(getApplicationContext()));
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Display retry connection state.
	 */
	public void displayRetryConnectionState()
	{
		CURRENT_STATE = STATE_RETRY_CONNECTION;
		if (auth_title == null)
		{
			return;
		}
		long timestampDiff = System.currentTimeMillis() - connectingTimestamp;
		if (timestampDiff < MIN_LOADING_TIME)
		{
			stateHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					displayRetryConnectionState();
				}
			}, timestampDiff);
			return;
		}
		auth_title.setVisibility(View.GONE);
		layout_app_info.setVisibility(View.GONE);
		textViewDropdown.setVisibility(View.GONE);
		auth_info_layout.setVisibility(View.GONE);
		auth_button_deny_layout.setVisibility(View.VISIBLE);
		auth_button_deny.setText(getString(R.string.CANCEL));
		auth_button_accept.setText(getString(R.string.auth_retry));
		auth_button_deny.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (auth_title == null)
				{
					return;
				}
				HikeAuthActivity.this.finish();
			}
		});
		auth_button_accept.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				requestAccess();
			}
		});
		layout_conn_state.setVisibility(View.VISIBLE);
		text_conn_state.setText(getString(R.string.auth_went_wrong));
		image_conn_state.setVisibility(View.VISIBLE);
		image_conn_state.setImageResource(R.drawable.ic_error);
		progress_bar_conn_state.setVisibility(View.GONE);
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_AUTH_FAILURE);
			metadata.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, mAppPackage);
			metadata.put(HikeConstants.LogEvent.SOURCE_APP, HikePlatformConstants.GAME_SDK_ID);
			metadata.put(HikeConstants.Extras.SDK_CONNECTION_TYPE, FTUtils.getNetworkType(getApplicationContext()));
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
}
