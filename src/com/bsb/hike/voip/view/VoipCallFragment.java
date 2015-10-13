package com.bsb.hike.voip.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.ProfileImageLoader;
import com.bsb.hike.voip.VoIPClient;
import com.bsb.hike.voip.VoIPConstants;
import com.bsb.hike.voip.VoIPConstants.CallQuality;
import com.bsb.hike.voip.VoIPService;
import com.bsb.hike.voip.VoIPService.LocalBinder;
import com.bsb.hike.voip.VoIPUtils;

public class VoipCallFragment extends SherlockFragment implements CallActions
{
	static final int PROXIMITY_SCREEN_OFF_WAKELOCK = 32;

	private VoIPService voipService;
	private boolean isBound = false;
	private final Messenger mMessenger = new Messenger(new IncomingHandler());
	private WakeLock proximityWakeLock;
	private SensorManager sensorManager;
	private float proximitySensorMaximumRange;
	private int easter = 0;

	private CallActionsView callActionsView;
	private Chronometer callDuration;

	private ImageButton holdButton, muteButton, speakerButton;

	private boolean isCallActive;

	private CallFragmentListener activity;

	private String partnerName;

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		try
		{
			this.activity = (CallFragmentListener) activity;
		}
		catch (ClassCastException e)
		{
			throw new ClassCastException(activity.toString() + " must implement CallFragmentListener");
		}
    }

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle)
	{
		View view = inflater.inflate(R.layout.voip_call_fragment, null);

		muteButton = (ImageButton) view.findViewById(R.id.mute_btn);
		holdButton = (ImageButton) view.findViewById(R.id.hold_btn);
		speakerButton = (ImageButton) view.findViewById(R.id.speaker_btn);

		return view;
	}

	@SuppressLint("HandlerLeak") class IncomingHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			Logger.d(VoIPConstants.TAG, "Incoming handler received message: " + msg.what);
			if(!isVisible())
			{
				Logger.d(VoIPConstants.TAG, "Fragment not visible, returning");
				return;
			}
			switch (msg.what) {
			case VoIPConstants.MSG_SHUTDOWN_ACTIVITY:
				Logger.d(VoIPConstants.TAG, "Shutting down..");
				shutdown(msg.getData());
				break;
			case VoIPConstants.CONNECTION_ESTABLISHED_FIRST_TIME:
				updateCallStatus();
				break;
			case VoIPConstants.MSG_AUDIO_START:
				isCallActive = true;
				updateCallStatus();
				activateActiveCallButtons();
				break;
			case VoIPConstants.MSG_ENCRYPTION_INITIALIZED:
//				showMessage("Encryption initialized.");
				break;
			case VoIPConstants.MSG_INCOMING_CALL_DECLINED:
				// VoIPUtils.addMessageToChatThread(VoIPActivity.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0);
				break;
			case VoIPConstants.MSG_OUTGOING_CALL_DECLINED:
//				showMessage("Call was declined.");
				break;
			case VoIPConstants.MSG_CONNECTION_FAILURE:
				showCallFailedFragment(VoIPConstants.CallFailedCodes.UDP_CONNECTION_FAIL);
				break;
			case VoIPConstants.MSG_CURRENT_BITRATE:
//				int bitrate = voipService.getBitrate();
//				showMessage("Bitrate: " + bitrate);
				break;
			case VoIPConstants.MSG_EXTERNAL_SOCKET_RETRIEVAL_FAILURE:
				showCallFailedFragment(VoIPConstants.CallFailedCodes.EXTERNAL_SOCKET_RETRIEVAL_FAILURE);
				break;
			case VoIPConstants.MSG_PARTNER_SOCKET_INFO_TIMEOUT:
				showCallFailedFragment(VoIPConstants.CallFailedCodes.PARTNER_SOCKET_INFO_TIMEOUT);
				break;
			case VoIPConstants.MSG_PARTNER_ANSWER_TIMEOUT:
				showCallFailedFragment(VoIPConstants.CallFailedCodes.PARTNER_ANSWER_TIMEOUT);
				break;
			case VoIPConstants.MSG_RECONNECTING:
				updateCallStatus();
				break;
			case VoIPConstants.MSG_RECONNECTED:
				updateCallStatus();
				break;
			case VoIPConstants.MSG_UPDATE_QUALITY:
				CallQuality quality = voipService.getQuality();
				showSignalStrength(quality);
				Logger.d(VoIPConstants.TAG, "Updating call quality to: " + quality);
				break;
			case VoIPConstants.MSG_NETWORK_SUCKS:
				showCallFailedFragment(VoIPConstants.CallFailedCodes.CALLER_BAD_NETWORK);
				break;
			case VoIPConstants.MSG_UPDATE_HOLD_BUTTON:
				boolean hold = voipService.getHold();
				holdButton.setSelected(hold);
			case VoIPConstants.MSG_UPDATE_REMOTE_HOLD:
				updateCallStatus();
				break;
			case VoIPConstants.MSG_ALREADY_IN_CALL:
				showCallFailedFragment(VoIPConstants.CallFailedCodes.CALLER_IN_NATIVE_CALL);
				break;
			case VoIPConstants.MSG_AUDIORECORD_FAILURE:
				showMessage(getString(R.string.voip_mic_error));
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private ServiceConnection myConnection = new ServiceConnection() 
	{
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
	public void onResume() 
	{
		Logger.d(VoIPConstants.TAG, "VoipCallFragment onResume, Binding to service..");
		// Calling start service as well so an activity unbind doesn't cause the service to stop
		getSherlockActivity().startService(new Intent(getSherlockActivity(), VoIPService.class));
		Intent intent = new Intent(getSherlockActivity(), VoIPService.class);
		getSherlockActivity().bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
		updateCallStatus();
		initProximitySensor();
		super.onResume();
	}

	@Override
	public void onPause() 
	{
		releaseProximitySensor();
		Logger.d(VoIPConstants.TAG, "VoIPCallFragment onPause()");
		super.onPause();
	}

	@SuppressLint("Wakelock") @Override
	public void onDestroy() 
	{	
		if (voipService != null)
		{
			voipService.dismissNotification();
		}
		
		try 
		{
			if (isBound) 
			{
				getSherlockActivity().unbindService(myConnection);
			}
		}
		catch (IllegalArgumentException e) 
		{
			Logger.d(VoIPConstants.TAG, "unbindService IllegalArgumentException: " + e.toString());
		}
		
		if(callActionsView!=null)
		{
			callActionsView.stopPing();
			callActionsView = null;
		}

		partnerName = null;

		releaseWakeLock();

		// Proximity sensor
		if (sensorManager != null) 
		{
			if (proximityWakeLock != null) 
			{
				proximityWakeLock.release();
			}
			sensorManager.unregisterListener(proximitySensorEventListener);
		}
		
		Logger.d(VoIPConstants.TAG, "VoipCallFragment onDestroy()");
		super.onDestroy();
	}

	public interface CallFragmentListener
	{
		void showCallFailedFragment(Bundle bundle);

		boolean isShowingCallFailedFragment();

		void showDeclineWithMessageFragment(Bundle bundle);
	}

	private void connectMessenger() 
	{
		voipService.setMessenger(mMessenger);
		
		VoIPClient clientPartner = voipService.getPartnerClient();
		if (VoIPService.getCallId() == 0 || clientPartner.getPhoneNumber() == null) 
		{
			Logger.w(VoIPConstants.TAG, "There is no active call.");
			getSherlockActivity().finish();
			return;
		}

		if(VoIPService.isAudioRunning())
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

	void handleIntent(Intent intent) 
	{
		String action = intent.getStringExtra(VoIPConstants.Extras.ACTION);

		if (action == null || action.isEmpty())
		{
			return;
		}
		Logger.d(VoIPConstants.TAG, "Intent action: " + action);
		
		if (action.equals(VoIPConstants.PARTNER_REQUIRES_UPGRADE)) 
		{
			showCallFailedFragment(VoIPConstants.CallFailedCodes.PARTNER_UPGRADE);
			if (voipService != null)
			{
				voipService.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.PARTNER_UPGRADE);
				voipService.stop();
			}
		}
		
		if (action.equals(VoIPConstants.PARTNER_INCOMPATIBLE)) 
		{
			showCallFailedFragment(VoIPConstants.CallFailedCodes.PARTNER_INCOMPAT);
			if (voipService != null)
			{
				voipService.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.PARTNER_INCOMPAT);
				voipService.stop();
			}
		}
		
		if (action.equals(VoIPConstants.PARTNER_HAS_BLOCKED_YOU)) 
		{
			if (voipService != null)
			{
				voipService.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.PARTNER_BLOCKED_USER);
				voipService.stop();
			}
		}
		
		if (action.equals(VoIPConstants.PARTNER_IN_CALL)) 
		{
			showCallFailedFragment(VoIPConstants.CallFailedCodes.PARTNER_BUSY);
			if (voipService != null)
			{
				voipService.setCallStatus(VoIPConstants.CallStatus.PARTNER_BUSY);
				updateCallStatus();
				voipService.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CONNECTION_FAILED, VoIPConstants.CallFailedCodes.PARTNER_BUSY);
				voipService.stop();
			}
		}
		
		if (action.equals(VoIPConstants.INCOMING_NATIVE_CALL_HOLD) && voipService != null) 
		{
			if (VoIPService.getCallId() > 0) 
			{
				if(VoIPService.isAudioRunning())
				{
					showMessage(getString(R.string.voip_call_on_hold));
					voipService.setHold(true);
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
	}

	private void shutdown(final Bundle bundle) 
	{
		updateCallStatus();

		if(voipService != null)
		{
			voipService.setCallStatus(null);
		}

		try
		{
			if (isBound) 
			{
				getSherlockActivity().unbindService(myConnection);
			}
		}
		catch (IllegalArgumentException e) {
			Logger.d(VoIPConstants.TAG, "shutdown() exception: " + e.toString());
		}

		if(callDuration!=null)
		{
			callDuration.stop();
		}

		if(activity.isShowingCallFailedFragment())
		{
			return;
		}

		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				if(isAdded())
				{
					startCallRateActivity(bundle);
				}
			}
		}, 900);
	}

	private void releaseWakeLock() 
	{
		if (proximityWakeLock != null && proximityWakeLock.isHeld())
			proximityWakeLock.release();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (voipService!=null && !VoIPService.isAudioRunning() && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
			&& voipService.getPartnerClient().isInitiator())
		{
			voipService.stopRingtone();
			return true;
		}
		return false;
	}

	private void showMessage(final String message) 
	{
		Logger.d(VoIPConstants.TAG, "Toast: " + message);
		getSherlockActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (toast != null)
					toast.cancel();
				toast = Toast.makeText(getSherlockActivity(), message, Toast.LENGTH_LONG);
				toast.show();
			}
		});
	}

	private void initProximitySensor() 
	{
		sensorManager = (SensorManager) getSherlockActivity().getSystemService(Context.SENSOR_SERVICE);
		Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

		if (proximitySensor == null) 
		{
			Logger.d(VoIPConstants.TAG, "No proximity sensor found.");
			return;
		}
		// Set proximity sensor
		proximitySensorMaximumRange = proximitySensor.getMaximumRange();
		proximityWakeLock = ((PowerManager)getSherlockActivity().getSystemService(Context.POWER_SERVICE)).newWakeLock(PROXIMITY_SCREEN_OFF_WAKELOCK, "ProximityLock");
		proximityWakeLock.setReferenceCounted(false);
		sensorManager.registerListener(proximitySensorEventListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);

	}

	private void releaseProximitySensor()
	{
		if (sensorManager != null) 
		{
			if (proximityWakeLock != null)
			{
				proximityWakeLock.release();
			}
			sensorManager.unregisterListener(proximitySensorEventListener);
		}
	}
	
	SensorEventListener proximitySensorEventListener = new SensorEventListener() 
	{

		@SuppressLint("Wakelock") @Override
		public void onSensorChanged(SensorEvent event) 
		{
			if (event.values[0] != proximitySensorMaximumRange) 
			{
				if (!proximityWakeLock.isHeld()) 
				{
					proximityWakeLock.acquire();
				}
			}
			else
			{
				if (proximityWakeLock.isHeld()) 
				{
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
		updateCallStatus();
	}

	private void setupCalleeLayout()
	{
		showHikeCallText();
		setAvatar();
		setContactDetails();
		hideActiveCallButtons();
		showCallActionsView();
		updateCallStatus();
	}

	private void setupActiveCallLayout()
	{
		showHikeCallText();
		setAvatar();
		setContactDetails();
		showActiveCallButtons();

		// Get hold status from service if activity was destroyed
		updateCallStatus();

		activateActiveCallButtons();
	}

	@Override
	public void onAcceptCall()
	{
		Logger.d(VoIPConstants.TAG, "Accepted call, starting audio...");
		voipService.acceptIncomingCall();
		callActionsView.setVisibility(View.GONE);
		showActiveCallButtons();
	}

	@Override
	public void onDeclineCall()
	{
		Logger.d(VoIPConstants.TAG, "Declined call, rejecting...");
		voipService.rejectIncomingCall();
	}

	@Override
	public void onMessage()
	{
		Logger.d(VoIPConstants.TAG, "Declined call, messaging...");
		Bundle bundle = new Bundle();
		bundle.putString(VoIPConstants.PARTNER_MSISDN, voipService.getPartnerClient().getPhoneNumber());
		activity.showDeclineWithMessageFragment(bundle);
	}

	private void showHikeCallText()
	{
		TextView textView  = (TextView) getView().findViewById(R.id.hike_call); 
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

		View hangupButton = getView().findViewById(R.id.hang_up_btn);
		getView().findViewById(R.id.active_call_group).setVisibility(View.VISIBLE);
		hangupButton.setVisibility(View.VISIBLE);

		muteButton.startAnimation(anim);
		holdButton.startAnimation(anim);
		speakerButton.startAnimation(anim);
		hangupButton.startAnimation(anim);
	}

	private void hideActiveCallButtons()
	{
		View hangupButton = getView().findViewById(R.id.hang_up_btn);
		hangupButton.setVisibility(View.GONE);

		getView().findViewById(R.id.active_call_group).setVisibility(View.GONE);
	}

	private void activateActiveCallButtons()
	{
		muteButton.setImageResource(R.drawable.voip_mute_btn_selector);
		holdButton.setImageResource(R.drawable.voip_hold_btn_selector);
	}

	private void setupActiveCallButtonActions()
	{
		getView().findViewById(R.id.hang_up_btn).setOnClickListener(new OnClickListener() 
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
				easterEgg(0, 1);
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
				easterEgg(2, 3);
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
				easterEgg(4);
				if(isCallActive)
				{
					boolean newHold = !voipService.getHold();
					voipService.setHold(newHold);
					voipService.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_HOLD, newHold ? 1 : 0);
				}
			}
		});
	}

	private void updateCallStatus()
	{
		if(!isAdded() || voipService == null)
		{
			return;
		}

		VoIPConstants.CallStatus status = voipService.getCallStatus();
		if(status == null)
		{
			return;
		}

		if(callDuration == null)
		{
			callDuration = (Chronometer) getView().findViewById(R.id.call_duration);
			callDuration.setVisibility(View.VISIBLE);
		}

		AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(1000);

		switch(status)
		{
			case OUTGOING_CONNECTING:
				callDuration.startAnimation(anim);
				callDuration.setText(getString(R.string.voip_connecting));
				break;

			case OUTGOING_RINGING:
				callDuration.startAnimation(anim);
				callDuration.setText(getString(R.string.voip_ringing));
				break;

			case INCOMING_CALL:
				callDuration.startAnimation(anim);
				callDuration.setText(getString(R.string.voip_incoming));
				break;

			case PARTNER_BUSY:
				callDuration.startAnimation(anim);
				callDuration.setText(getString(R.string.voip_partner_busy));
				break;

			case ACTIVE:
				startCallDuration();
				break;

			case ON_HOLD:
				callDuration.stop();
				callDuration.startAnimation(anim);
				callDuration.setText(getString(R.string.voip_on_hold));
				break;

			case RECONNECTING:
				callDuration.stop();
				callDuration.startAnimation(anim);
				callDuration.setText(getString(R.string.voip_reconnecting));
				break;

			case ENDED:
				if(!isCallActive)
				{
					callDuration.setText(getString(R.string.voip_call_ended));
				}
				break;
		}
	}
	
	private void startCallDuration()
	{	
		AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(500);

		callDuration.startAnimation(anim);
		callDuration.setBase((SystemClock.elapsedRealtime() - 1000*voipService.getCallDuration()));
		callDuration.start();
	}

	public void setContactDetails()
	{
		TextView contactNameView = (TextView) getView().findViewById(R.id.contact_name);
		TextView contactMsisdnView = (TextView) getView().findViewById(R.id.contact_msisdn);

		VoIPClient clientPartner = voipService.getPartnerClient();
		if (clientPartner == null) 
		{
			getSherlockActivity().finish();
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
			partnerName = contactInfo.getName();
			if(partnerName != null)
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
		callActionsView = (CallActionsView) getView().findViewById(R.id.call_actions_view);

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
		LinearLayout signalContainer = (LinearLayout) getView().findViewById(R.id.signal_container);
		TextView signalStrengthView = (TextView) getView().findViewById(R.id.signal_strength);
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

	private void startCallRateActivity(Bundle bundle)
	{
		if(isCallActive)
		{
			if(bundle!=null && VoIPUtils.shouldShowCallRatePopupNow())
			{
				Intent intent = IntentFactory.getVoipCallRateActivityIntent(getSherlockActivity());
				intent.putExtra(VoIPConstants.CALL_RATE_BUNDLE, bundle);
				startActivity(intent);
			}
			VoIPUtils.setupCallRatePopupNextTime();
		}
		isCallActive = false;
		getSherlockActivity().finish();
	}

	public void showCallFailedFragment(int callFailCode)
	{
		if(activity == null || voipService == null)
		{
			return;
		}

		releaseProximitySensor();

		Bundle bundle = new Bundle();
		bundle.putString(VoIPConstants.PARTNER_MSISDN, voipService.getPartnerClient().getPhoneNumber());
		bundle.putInt(VoIPConstants.CALL_FAILED_REASON, callFailCode);
		bundle.putString(VoIPConstants.PARTNER_NAME, partnerName);

		activity.showCallFailedFragment(bundle);
	}

	public void setAvatar()
	{
		VoIPClient clientPartner = voipService.getPartnerClient();
		String msisdn = clientPartner.getPhoneNumber();

		ImageView imageView = (ImageView) getView().findViewById(R.id.profile_image);
		int size = getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);

		ProfileImageLoader profileImageLoader = new ProfileImageLoader(getActivity(), msisdn, imageView, size, false);
		profileImageLoader.setDefaultDrawable(getResources().getDrawable(R.drawable.ic_avatar_voip_hires));
		boolean hasCustomImage = profileImageLoader.loadProfileImage(getLoaderManager());
		if(!hasCustomImage)
		{
			imageView.setScaleType(ScaleType.FIT_START);
		}
	}
	
	private void easterEgg(int... allowedValues) {
		for (int i = 0; i < allowedValues.length; i++)
			if (easter == allowedValues[i]) {
				easter++;
				if (easter == 5) {
					// Easter success
					showMessage("Encryption key: " + voipService.getSessionKeyHash());
				}
				return;
			}
		easter = 0;
	}
}
