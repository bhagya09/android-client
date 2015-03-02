package com.bsb.hike.voip.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.VoipProfilePicImageLoader;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPClient;
import com.bsb.hike.voip.VoIPConstants;
import com.bsb.hike.voip.VoIPConstants.CallQuality;
import com.bsb.hike.voip.VoIPService;
import com.bsb.hike.voip.VoIPService.LocalBinder;
import com.bsb.hike.voip.VoIPUtils;

public class VoIPActivity extends Activity implements CallActions
{
	static final int PROXIMITY_SCREEN_OFF_WAKELOCK = 32;
//	public static boolean isRunning = false;

	private VoIPService voipService;
	// private VoIPClient clientSelf = new VoIPClient(), clientPartner = new VoIPClient();
	private boolean isBound = false;
	private final Messenger mMessenger = new Messenger(new IncomingHandler());
	private WakeLock wakeLock = null;
	private WakeLock proximityWakeLock;
	private SensorManager sensorManager;
	private float proximitySensorMaximumRange;

	public static final int MSG_SHUTDOWN_ACTIVITY = 1;
	public static final int CONNECTION_ESTABLISHED_FIRST_TIME = 2;
	public static final int MSG_AUDIO_START = 3;
	public static final int MSG_ENCRYPTION_INITIALIZED = 4;
	public static final int MSG_OUTGOING_CALL_DECLINED = 5;
	public static final int MSG_CONNECTION_FAILURE = 6;
	public static final int MSG_CURRENT_BITRATE = 7;
	public static final int MSG_EXTERNAL_SOCKET_RETRIEVAL_FAILURE = 8;
	public static final int MSG_PARTNER_SOCKET_INFO_TIMEOUT = 10;
	public static final int MSG_PARTNER_ANSWER_TIMEOUT = 11;
	public static final int MSG_INCOMING_CALL_DECLINED = 14;
	public static final int MSG_RECONNECTING = 15;
	public static final int MSG_RECONNECTED = 16;
	public static final int MSG_UPDATE_QUALITY = 17;
	public static final int MSG_NETWORK_SUCKS = 18;
	public static final int MSG_UPDATE_HOLD_BUTTON = 19;
	public static final int MSG_ALREADY_IN_CALL = 20;
	public static final int MSG_PHONE_NOT_SUPPORTED = 21;

	private CallActionsView callActionsView;
	private Chronometer callDuration;

	private ImageButton holdButton, muteButton, speakerButton;

	private boolean isActivityVisible;

	private boolean isCallActive;

	private enum CallStatus
	{
		OUTGOING_CONNECTING, OUTGOING_RINGING, INCOMING_CALL, PARTNER_BUSY, ON_HOLD, ACTIVE, ENDED
	}

	private CallStatus currentCallStatus;

	@SuppressLint("HandlerLeak") class IncomingHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SHUTDOWN_ACTIVITY:
				Logger.d(VoIPConstants.TAG, "Shutting down..");
				shutdown(msg.getData());
				break;
			case CONNECTION_ESTABLISHED_FIRST_TIME:
				showCallStatus(CallStatus.OUTGOING_RINGING);
				break;
			case MSG_AUDIO_START:
				isCallActive = true;
				voipService.startChrono();
				showCallStatus(CallStatus.ACTIVE);
				activateActiveCallButtons();
				break;
			case MSG_ENCRYPTION_INITIALIZED:
				break;
			case MSG_INCOMING_CALL_DECLINED:
				break;
			case MSG_OUTGOING_CALL_DECLINED:
//				showMessage("Call was declined.");
				break;
			case MSG_CONNECTION_FAILURE:
				showMessage(getString(R.string.voip_unable_to_connect_call));
				break;
			case MSG_CURRENT_BITRATE:
//				int bitrate = voipService.getBitrate();
//				showMessage("Bitrate: " + bitrate);
				break;
			case MSG_EXTERNAL_SOCKET_RETRIEVAL_FAILURE:
				showMessage(getString(R.string.voip_unable_to_connect_to_network));
				voipService.stop();
				break;
			case MSG_PARTNER_SOCKET_INFO_TIMEOUT:
				break;
			case MSG_PARTNER_ANSWER_TIMEOUT:
				break;
			case MSG_RECONNECTING:
				showMessage(getString(R.string.voip_reconnecting_call));
				break;
			case MSG_RECONNECTED:
				break;
			case MSG_UPDATE_QUALITY:
				CallQuality quality = voipService.getQuality();
				showSignalStrength(quality);
				break;
			case MSG_NETWORK_SUCKS:
				showMessage(getString(R.string.voip_poor_network_quality));
				break;
			case MSG_UPDATE_HOLD_BUTTON:
				boolean hold = voipService.getHold();
				holdButton.setSelected(hold);
				if (hold)
					showCallStatus(CallStatus.ON_HOLD);
				else
					showCallStatus(CallStatus.ACTIVE);
				break;
			case MSG_ALREADY_IN_CALL:
				showMessage(getString(R.string.voip_already_in_call));
				break;
			case MSG_PHONE_NOT_SUPPORTED:
				showMessage(getString(R.string.voip_phone_unsupported));
				isCallActive = false;
				voipService.hangUp();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private ServiceConnection myConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			isBound = false;
			voipService = null;
			Logger.d(VoIPConstants.TAG, "VoIPService disconnected.");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Logger.d(VoIPConstants.TAG, "VoIPService connected.");
			LocalBinder binder = (LocalBinder) service;
			voipService = binder.getService();
			isBound = true;
			connectMessenger();
		}
	};
	protected Toast toast;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voip_activity);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		muteButton = (ImageButton)findViewById(R.id.mute_btn);
		holdButton = (ImageButton)findViewById(R.id.hold_btn);
		speakerButton = (ImageButton)findViewById(R.id.speaker_btn);

		Logger.d(VoIPConstants.TAG, "Binding to service..");
		// Calling start service as well so an activity unbind doesn't cause the service to stop
		startService(new Intent(getApplicationContext(), VoIPService.class));
		Intent intent = new Intent(getApplicationContext(), VoIPService.class);
		bindService(intent, myConnection, Context.BIND_AUTO_CREATE);

		intent = getIntent();
		if (intent != null) {
			handleIntent(intent);
		}

		acquireWakeLock();
//		isRunning = true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		isActivityVisible = true;
		initProximitySensor();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		isActivityVisible = false;
		if (sensorManager != null && VoIPService.isConnected() != true) {
			if (proximityWakeLock != null) 
				proximityWakeLock.release();
			sensorManager.unregisterListener(proximitySensorEventListener);
		}
		
		Logger.d(VoIPConstants.TAG, "VoIPActivity onPause()");
	}

	@SuppressLint("Wakelock") @Override
	protected void onDestroy() {
		
		if (voipService != null)
			voipService.dismissNotification();
		
		try {
			if (isBound) {
				unbindService(myConnection);
			}
		} catch (IllegalArgumentException e) {
			Logger.d(VoIPConstants.TAG, "unbindService IllegalArgumentException: " + e.toString());
		}
		
//		isRunning = false;
		if(callActionsView!=null)
		{
			callActionsView.stopPing();
			callActionsView = null;
		}

		// Proximity sensor
		if (sensorManager != null) {
			if (proximityWakeLock != null) 
				proximityWakeLock.release();
			sensorManager.unregisterListener(proximitySensorEventListener);
		}
		
		Logger.w(VoIPConstants.TAG, "VoIPActivity onDestroy()");
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Logger.d(VoIPConstants.TAG, "VoIPActivity onNewIntent().");
		handleIntent(intent);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (voipService!=null && !voipService.isAudioRunning() && 
				(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) &&
				voipService.getPartnerClient().isInitiator())
		{
			voipService.stopRingtone();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void handleIntent(Intent intent) 
	{
		String action = intent.getStringExtra(VoIPConstants.Extras.ACTION);

		if (action == null || action.isEmpty())
		{
			return;
		}
		Logger.d(VoIPConstants.TAG, "Intent action: " + action);
		
		if (action.equals(VoIPConstants.PARTNER_REQUIRES_UPGRADE)) 
		{
			String message = intent.getStringExtra(VoIPConstants.Extras.MESSAGE);
			if (message == null || message.isEmpty())
				message = getString(R.string.voip_partner_upgrade);
			showMessage(message);
			if (voipService != null)
			{
				voipService.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.ConnectionFailCodes.PARTNER_UPGRADE);
				voipService.stop();
			}
		}
		
		if (action.equals(VoIPConstants.PARTNER_INCOMPATIBLE)) 
		{
			String message = intent.getStringExtra(VoIPConstants.Extras.MESSAGE);
			if (message == null || message.isEmpty())
				message = getString(R.string.voip_partner_incompat);
			showMessage(message);
			if (voipService != null)
			{
				voipService.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.ConnectionFailCodes.PARTNER_INCOMPAT);
				voipService.stop();
			}
		}
		
		if (action.equals(VoIPConstants.PARTNER_HAS_BLOCKED_YOU)) 
		{
			if (voipService != null)
			{
				voipService.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.ConnectionFailCodes.PARTNER_BLOCKED_USER);
				voipService.stop();
			}
		}
		
		if (action.equals(VoIPConstants.PARTNER_IN_CALL)) 
		{
			showMessage(getString(R.string.voip_partner_is_busy));
			showCallStatus(CallStatus.PARTNER_BUSY);
			if (voipService != null)
			{
				voipService.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.ConnectionFailCodes.PARTNER_BUSY);
				voipService.stop();
			}
		}
		
		if (action.equals(VoIPConstants.INCOMING_NATIVE_CALL_HOLD) && voipService != null) 
		{
			if (VoIPService.isConnected()) 
			{
				if(voipService.isAudioRunning())
				{
					showMessage(getString(R.string.voip_call_on_hold));
					voipService.setHold(true);
					showCallStatus(CallStatus.ON_HOLD);
					voipService.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_NATIVE_CALL_INTERRUPT);
				}
				else
				{
					voipService.hangUp();
				}
			}
			else
			{
				voipService.stop();
			}
		}
		
		// Clear the intent so the activity doesn't process intent again on resume
		getIntent().removeExtra(VoIPConstants.Extras.ACTION);
	}

	private void connectMessenger() {
		voipService.setMessenger(mMessenger);
		
		if (VoIPService.getCallId() == 0) {
			Logger.w(VoIPConstants.TAG, "There is no active call.");
			finish();
			return;
		}
		
		VoIPClient clientPartner = voipService.getPartnerClient();
		if(voipService.isAudioRunning())
		{
			// Active Call
			isCallActive = true;
			setupActiveCallLayout();
		}
		else if (clientPartner.isInitiator())
		{
			// Incoming call
			setupCalleeLayout();
		}
		else
		{
			// Outgoing call
			setupCallerLayout();
		}
	}

	private void shutdown(Bundle bundle) {
		
		try {
			if (isBound) {
				unbindService(myConnection);
			}
		} catch (IllegalArgumentException e) {
			Logger.d(VoIPConstants.TAG, "shutdown() exception: " + e.toString());
		}
		
		releaseWakeLock();

//		isRunning = false;

		if(currentCallStatus!=CallStatus.PARTNER_BUSY)
		{
			showCallStatus(CallStatus.ENDED);
		}

		if(callDuration!=null)
		{
			callDuration.stop();
		}

		if(isCallActive)
		{
			VoIPUtils.setupCallRatePopup(getApplicationContext(), bundle);
		}
		isCallActive = false;

		if(isActivityVisible)
		{
			new Handler().postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					finish();
				}
			}, 900);
		}
		else
		{
			finish();
		}
	}

	private void acquireWakeLock() {
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		if (wakeLock == null) {
			wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HikeWL");
			wakeLock.setReferenceCounted(false);
		}
		if (!wakeLock.isHeld()) {
			wakeLock.acquire();
			Logger.d(VoIPConstants.TAG, "Wakelock acquired.");
		}
	}

	private void releaseWakeLock() {
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
			Logger.d(VoIPConstants.TAG, "Wakelock released.");
		}
		if (proximityWakeLock != null && proximityWakeLock.isHeld())
			proximityWakeLock.release();
	}

	private void showMessage(final String message) {
		Logger.d(VoIPConstants.TAG, "Toast: " + message);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (toast != null)
					toast.cancel();
				toast = Toast.makeText(VoIPActivity.this, message, Toast.LENGTH_LONG);
				toast.show();
			}
		});
	}

	private void initProximitySensor() {

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

		if (proximitySensor == null) {
			Logger.d(VoIPConstants.TAG, "No proximity sensor found.");
			return;
		}
		// Set proximity sensor
		proximitySensorMaximumRange = proximitySensor.getMaximumRange();
		proximityWakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PROXIMITY_SCREEN_OFF_WAKELOCK, "ProximityLock");
		proximityWakeLock.setReferenceCounted(false);
		sensorManager.registerListener(proximitySensorEventListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);

	}
	
	SensorEventListener proximitySensorEventListener = new SensorEventListener() {

		@SuppressLint("Wakelock") @Override
		public void onSensorChanged(SensorEvent event) {

//			Logger.d(VoIPConstants.TAG, "Proximity sensor changed. Value: " + event.values[0]
//					+ ", max: " + proximitySensorMaximumRange);

			if (event.values[0] != proximitySensorMaximumRange) {
				if (!proximityWakeLock.isHeld()) {
//					Logger.d(VoIPConstants.TAG, "Acquiring proximity sensor...");
					proximityWakeLock.acquire();
				}
			} else {
				if (proximityWakeLock.isHeld()) {
//					Logger.d(VoIPConstants.TAG, "Releasing proximity sensor...");
					proximityWakeLock.release();
				}
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};
	
	private void setupCallerLayout()
	{
		showHikeCallText();
		setAvatar();
		setContactDetails();
		showActiveCallButtons();
		showCallStatus(CallStatus.OUTGOING_CONNECTING);
	}

	private void setupCalleeLayout()
	{
		showHikeCallText();
		setAvatar();
		setContactDetails();
		showCallActionsView();
		showCallStatus(CallStatus.INCOMING_CALL);
	}

	private void setupActiveCallLayout()
	{
		showHikeCallText();
		setAvatar();
		setContactDetails();
		showActiveCallButtons();

		// Get hold status from service if activity was destroyed
		if(voipService.getHold())
		{
			showCallStatus(CallStatus.ON_HOLD);
		}
		else
		{
			showCallStatus(CallStatus.ACTIVE);
		}

		activateActiveCallButtons();
	}

	@Override
	public void acceptCall()
	{
		Logger.d(VoIPConstants.TAG, "Accepted call, starting audio...");
		voipService.acceptIncomingCall();
		callActionsView.setVisibility(View.GONE);
		showActiveCallButtons();
	}

	@Override
	public void declineCall()
	{
		Logger.d(VoIPConstants.TAG, "Declined call, rejecting...");
		voipService.rejectIncomingCall();
	}

	private void showHikeCallText()
	{
		TextView textView  = (TextView) findViewById(R.id.hike_call); 
		SpannableString ss = new SpannableString("  " + getString(R.string.voip_call)); 
		Drawable logo = getResources().getDrawable(R.drawable.ic_logo_voip); 
		logo.setBounds(0, 0, logo.getIntrinsicWidth(), logo.getIntrinsicHeight());
		ImageSpan span = new ImageSpan(logo, ImageSpan.ALIGN_BASELINE); 
		ss.setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE); 
		textView.setText(ss);
	}

	private void showActiveCallButtons()
	{
		animateActiveCallButtons();

		// Get initial setting from service
		muteButton.setSelected(voipService.getMute());
		holdButton.setSelected(voipService.getHold());
		speakerButton.setSelected(voipService.getSpeaker());

		setupActiveCallButtonActions();
	}

	private void animateActiveCallButtons()
	{
		AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(500);

		View hangupButton = findViewById(R.id.hang_up_btn);
		findViewById(R.id.active_call_group).setVisibility(View.VISIBLE);
		hangupButton.setVisibility(View.VISIBLE);

		muteButton.startAnimation(anim);
		holdButton.startAnimation(anim);
		speakerButton.startAnimation(anim);
		hangupButton.startAnimation(anim);
	}

	private void activateActiveCallButtons()
	{
		muteButton.setImageResource(R.drawable.voip_mute_btn_selector);
		holdButton.setImageResource(R.drawable.voip_hold_btn_selector);
	}

	private void setupActiveCallButtonActions()
	{
		findViewById(R.id.hang_up_btn).setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) {
				Logger.d(VoIPConstants.TAG, "Trying to hang up.");
				voipService.hangUp();
			}
		});

		muteButton.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				if(isCallActive)
				{
					boolean newMute = !voipService.getMute();
					muteButton.setSelected(newMute);
					voipService.setMute(newMute);
					voipService.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_MUTE, newMute ? 1 : 0);
				}
			}
		});

		speakerButton.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{				
				boolean newSpeaker = !voipService.getSpeaker();
				speakerButton.setSelected(newSpeaker);
				voipService.setSpeaker(newSpeaker);
				voipService.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_SPEAKER, newSpeaker ? 1 : 0);
			}
		});

		holdButton.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				if(isCallActive)
				{
					boolean newHold = !voipService.getHold();
					voipService.setHold(newHold);
					voipService.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_HOLD, newHold ? 1 : 0);
				}
			}
		});
	}

	private void showCallStatus(CallStatus status)
	{
		currentCallStatus = status;

		TextView callStatusView = (TextView)findViewById(R.id.call_status);
		Chronometer callDurationView = (Chronometer) findViewById(R.id.call_duration);

		AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(1000);

		switch(status)
		{
			case OUTGOING_CONNECTING: callStatusView.startAnimation(anim);
									  callStatusView.setText(getString(R.string.voip_connecting));
									  break;

			case OUTGOING_RINGING:	  callStatusView.startAnimation(anim);
									  callStatusView.setText(getString(R.string.voip_ringing));
									  break;

			case INCOMING_CALL:		  callStatusView.startAnimation(anim);
									  callStatusView.setText(getString(R.string.voip_incoming));
									  break;

			case PARTNER_BUSY:		  callStatusView.startAnimation(anim);
									  callStatusView.setText(getString(R.string.voip_partner_busy));
									  break;

			case ACTIVE: 			  startCallDuration();
									  callStatusView.setVisibility(View.GONE);
									  callDurationView.setVisibility(View.VISIBLE);
									  break;

			case ON_HOLD:			  callDurationView.setVisibility(View.GONE);
									  callStatusView.setVisibility(View.VISIBLE);
									  callStatusView.startAnimation(anim);
									  callStatusView.setText(getString(R.string.voip_on_hold));
									  break;

			case ENDED: 			  callStatusView.setText(getString(R.string.voip_call_ended));
									  break;
		}
	}
	
	private void startCallDuration()
	{	
		AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(500);

		callDuration = (Chronometer)VoIPActivity.this.findViewById(R.id.call_duration);
		callDuration.startAnimation(anim);
		callDuration.setBase((SystemClock.elapsedRealtime() - 1000*voipService.getCallDuration()));
		callDuration.start();
	}

	public void setAvatar()
	{
		VoIPClient clientPartner = voipService.getPartnerClient();
		String mappedId = clientPartner.getPhoneNumber() + ProfileActivity.PROFILE_PIC_SUFFIX;
		int mBigImageSize = getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);

		VoipProfilePicImageLoader profileImageLoader = new VoipProfilePicImageLoader(getApplicationContext(), mBigImageSize);
	    profileImageLoader.setDefaultAvatarIfNoCustomIcon(true);
	    profileImageLoader.setDefaultAvatarScaleType(ScaleType.CENTER);
	    profileImageLoader.setDefaultAvatarBounds(LayoutParams.MATCH_PARENT, (int)(250*Utils.densityMultiplier));
		profileImageLoader.loadImage(mappedId, (ImageView)findViewById(R.id.profile_image));
	}

	public void setContactDetails()
	{
		TextView contactNameView = (TextView) findViewById(R.id.contact_name);
		TextView contactMsisdnView = (TextView) findViewById(R.id.contact_msisdn);

		VoIPClient clientPartner = voipService.getPartnerClient();
		if (clientPartner == null) 
		{
			finish();
			Logger.w(VoIPConstants.TAG, "Partner client info is null. Returning.");
			return;
		}

		ContactInfo contactInfo = ContactManager.getInstance().getContact(clientPartner.getPhoneNumber());
		String nameOrMsisdn;

		if(contactInfo == null)
		{
			// For unsaved contacts
			nameOrMsisdn = clientPartner.getPhoneNumber();
			Logger.d(VoIPConstants.TAG, "Contact info is null for msisdn - " + nameOrMsisdn);
		}
		else
		{
			nameOrMsisdn = contactInfo.getNameOrMsisdn();
			if(contactInfo.getName() != null)
			{
				contactMsisdnView.setVisibility(View.VISIBLE);
				contactMsisdnView.setText(contactInfo.getMsisdn());
			}
		}

		if(nameOrMsisdn != null && nameOrMsisdn.length() > 16)
		{
			contactNameView.setTextSize(24);
		}
		contactNameView.setText(nameOrMsisdn);
	}
	
	public void showCallActionsView()
	{
		callActionsView = (CallActionsView)findViewById(R.id.call_actions_view);

		TranslateAnimation anim = new TranslateAnimation(0, 0.0f, 0, 0.0f, Animation.RELATIVE_TO_PARENT, 1.0f, Animation.RELATIVE_TO_SELF, 0f);
		anim.setDuration(1500);
		anim.setInterpolator(new DecelerateInterpolator(4f));

		callActionsView.setVisibility(View.VISIBLE);
		callActionsView.startAnimation(anim);
		
		callActionsView.setCallActionsListener(this);
		callActionsView.startPing();
	}

	private void showSignalStrength(CallQuality quality)
	{
		LinearLayout signalContainer = (LinearLayout)findViewById(R.id.signal_container);
		TextView signalStrengthView = (TextView) findViewById(R.id.signal_strength);
		GradientDrawable gd = (GradientDrawable)signalContainer.getBackground();

		AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(800);

		switch(quality)
		{
			case WEAK: 			gd.setColor(getResources().getColor(R.color.signal_red));
					   			signalStrengthView.setText(getString(R.string.voip_signal_weak));
					   			break;
			case FAIR:			gd.setColor(getResources().getColor(R.color.signal_yellow));
						   		signalStrengthView.setText(getString(R.string.voip_signal_fair));
						   		break;
			case GOOD:			gd.setColor(getResources().getColor(R.color.signal_good));
						   		signalStrengthView.setText(getString(R.string.voip_signal_good));
						   		break;
			case EXCELLENT: 	gd.setColor(getResources().getColor(R.color.signal_green));
					   			signalStrengthView.setText(getString(R.string.voip_signal_excellent));
					   			break;
		}
		signalContainer.startAnimation(anim);
		signalContainer.setVisibility(View.VISIBLE);
	}
}
