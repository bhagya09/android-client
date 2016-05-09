package com.bsb.hike.voip.view;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.ProfileImageLoader;
import com.bsb.hike.voip.VoIPClient;
import com.bsb.hike.voip.VoIPConstants;
import com.bsb.hike.voip.VoIPConstants.CallStatus;
import com.bsb.hike.voip.VoIPService;
import com.bsb.hike.voip.VoIPService.LocalBinder;
import com.bsb.hike.voip.VoIPUtils;
import com.bsb.hike.voip.adapters.ConferenceParticipantsAdapter;

public class VoipCallFragment extends Fragment implements CallActions
{
	static final int PROXIMITY_SCREEN_OFF_WAKELOCK = 32;
	private final String tag = VoIPConstants.TAG + " VoipCallFragment";

	private VoIPService voipService;
	private boolean isBound = false;
	private final Messenger mMessenger = new Messenger(new IncomingHandler());
	private WakeLock proximityWakeLock = null;
	private int easter = 0;

	private CallActionsView callActionsView;
	private Chronometer callDuration;

	private ImageButton muteButton, speakerButton, addButton, bluetoothButton;
	private LinearLayout forceMuteContainer = null;
	private boolean isCallActive;
	private ArrayList<VoIPClient> conferenceClients = null;
	private ConferenceParticipantsAdapter confClientsAdapter = null;

	private CallFragmentListener activity;

	private String partnerName;

	@SuppressWarnings("deprecation")
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
		speakerButton = (ImageButton) view.findViewById(R.id.speaker_btn);
		addButton = (ImageButton) view.findViewById(R.id.add_btn);
		bluetoothButton = (ImageButton) view.findViewById(R.id.bluetooth_btn);
		forceMuteContainer = (LinearLayout) view.findViewById(R.id.force_mute_layout);
		
		return view;
	}

	@SuppressLint("HandlerLeak") class IncomingHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
//			Logger.d(tag, "Incoming handler received message: " + msg.what);
			if(!isVisible())
			{
				Logger.d(tag, "Fragment not visible, returning");
				return;
			}
			switch (msg.what) {
			case VoIPConstants.MSG_SHUTDOWN_ACTIVITY:
				Logger.d(tag, "Shutting down activity..");
				shutdown(msg.getData());
				break;
			case VoIPConstants.CONNECTION_ESTABLISHED_FIRST_TIME:
				if (!voipService.inActiveCall()) {
					VoIPClient clientPartner = voipService.getPartnerClient();
					if (clientPartner == null)
						break;
					if (clientPartner.isInitiator())
						setupCalleeLayout();
					else
						setupCallerLayout();
				} else
					updateCallStatus();
				break;
			case VoIPConstants.MSG_AUDIO_START:
				isCallActive = true;
				updateCallStatus();
				activateActiveCallButtons();
				initProximityWakelock();
				break;
			case VoIPConstants.MSG_CONNECTION_FAILURE:
				showCallFailedFragment(VoIPConstants.CallFailedCodes.UDP_CONNECTION_FAIL);
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
			case VoIPConstants.MSG_NETWORK_SUCKS:
				showCallFailedFragment(VoIPConstants.CallFailedCodes.CALLER_BAD_NETWORK);
				break;
			case VoIPConstants.MSG_UPDATE_REMOTE_HOLD:
				updateCallStatus();
				break;
			case VoIPConstants.MSG_ALREADY_IN_NATIVE_CALL:
				showCallFailedFragment(VoIPConstants.CallFailedCodes.CALLER_IN_NATIVE_CALL);
				break;
			case VoIPConstants.MSG_AUDIORECORD_FAILURE:
				showMessage(getString(R.string.voip_mic_error));
				break;
			case VoIPConstants.MSG_UPDATE_CONTACT_DETAILS:
				setContactDetails();
				break;
			case VoIPConstants.MSG_UPDATE_SPEAKING:
				if (voipService.hostingConference())
					updateConferenceList();
				break;
			case VoIPConstants.MSG_BLUETOOTH_SHOW:
				toggleBluetoothButton(true);
				break;
			case VoIPConstants.MSG_DOES_NOT_SUPPORT_CONFERENCE:
				Bundle bundle = msg.getData();
				String name = bundle.getString(VoIPConstants.PARTNER_NAME);
				showMessage(getString(R.string.voip_conference_not_supported, name));
				break;
			case VoIPConstants.MSG_CUSTOM_ERROR_FROM_SERVER:
				if (voipService != null)
				{
					Bundle bundle2 = msg.getData();
					String msisdn = bundle2.getString(VoIPConstants.MSISDN);
					partnerName = bundle2.getString(VoIPConstants.PARTNER_NAME);
					String message = bundle2.getString(VoIPConstants.CUSTOM_MESSAGE);
					if (!voipService.hostingConference()) {
						showCallFailedFragment(VoIPConstants.CallFailedCodes.CUSTOM_MESSAGE, msisdn, message);
					}
				}
				break;
			case VoIPConstants.MSG_PARTNER_BUSY:
				if (voipService != null)
				{
					Bundle bundle2 = msg.getData();
					String msisdn = bundle2.getString(VoIPConstants.MSISDN);
					if (voipService.hostingConference()) {
						showMessage(getString(R.string.voip_callee_busy, msisdn));
					} else {
						showCallFailedFragment(VoIPConstants.CallFailedCodes.PARTNER_BUSY, msisdn, null);
						voipService.setCallStatus(VoIPConstants.CallStatus.PARTNER_BUSY);
						updateCallStatus();
					}
				}
				break;
			case VoIPConstants.MSG_PARTNER_INCOMPATIBLE_PLATFORM:
				if (voipService != null)
				{
					Bundle bundle2 = msg.getData();
					String msisdn = bundle2.getString(VoIPConstants.MSISDN);
					if (!voipService.hostingConference()) {
						showCallFailedFragment(VoIPConstants.CallFailedCodes.PARTNER_INCOMPAT, msisdn, null);
					}
				}
				break;
			case VoIPConstants.MSG_PARTNER_UPGRADABLE_PLATFORM:
				if (voipService != null)
				{
					Bundle bundle2 = msg.getData();
					String msisdn = bundle2.getString(VoIPConstants.MSISDN);
					if (!voipService.hostingConference()) {
						showCallFailedFragment(VoIPConstants.CallFailedCodes.PARTNER_UPGRADE, msisdn, null);
					}
				}
				break;
			case VoIPConstants.MSG_UPDATE_CALL_BUTTONS:
				if (voipService.getCallStatus() != CallStatus.INCOMING_CALL)
					showActiveCallButtons();
				break;
			case VoIPConstants.MSG_UPDATE_FORCE_MUTE_LAYOUT:
				if (voipService.getCallStatus() != CallStatus.INCOMING_CALL) {
					showActiveCallButtons();
					setupForceMuteLayout();
				}
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
			Logger.d(tag, "VoIPService disconnected.");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Logger.d(tag, "VoIPService connected.");
			LocalBinder binder = (LocalBinder) service;
			voipService = binder.getService();
			isBound = true;
			connectMessenger();
			
			// Should we show the bluetooth button?
			boolean bluetooth = voipService.isOnHeadsetSco();
			if (bluetooth)
				toggleBluetoothButton(true);
		}
	};

	protected Toast toast;
	
	@Override
	public void onResume() 
	{
		Logger.d(tag, "VoipCallFragment onResume, Binding to service..");
		// Calling start service as well so an activity unbind doesn't cause the service to stop

		if (isBound) {
			connectMessenger();
		} else {
			getActivity().startService(new Intent(getActivity(), VoIPService.class));
			Intent intent = new Intent(getActivity(), VoIPService.class);
			getActivity().bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
		}

		initProximityWakelock();
		updateCallStatus();
		super.onResume();
	}

	@Override
	public void onPause() 
	{
		releaseProximityWakelock();
		Logger.d(tag, "VoIPCallFragment onPause()");

		super.onPause();
	}

	@SuppressLint("Wakelock") @Override
	public void onDestroy() 
	{	
		if (voipService != null)
		{
			voipService.dismissNotification();
		}
		
		if(callDuration!=null)
		{
			callDuration.stop();
		}

		if(callActionsView!=null)
		{
			callActionsView.stopPing();
			callActionsView = null;
		}

		unbindVoipService();

		partnerName = null;
		Logger.d(tag, "VoipCallFragment onDestroy()");
		super.onDestroy();
	}

	private void unbindVoipService() {
		// Disconnect from service
		try
		{
			if (isBound)
			{
				isBound = false;
				getActivity().unbindService(myConnection);
			}
		}
		catch (IllegalArgumentException e)
		{
//			Logger.d(tag, "unbindService IllegalArgumentException: " + e.toString());
		}
	}

	public interface CallFragmentListener
	{
		void showCallFailedFragment(Bundle bundle);

		boolean isShowingCallFailedFragment();
		
		void clearActivityFlags();

		void showDeclineWithMessageFragment(Bundle bundle);
	}

	private void connectMessenger() 
	{
		voipService.setMessenger(mMessenger);
		
		VoIPClient clientPartner = voipService.getPartnerClient();
		if (VoIPService.getCallId() == 0 ||
				clientPartner == null ||
				clientPartner.getPhoneNumber() == null) 
		{
			Logger.w(tag, "There is no active call.");
			if (!activity.isShowingCallFailedFragment()) {
				// Bugfix AND-1315
				Logger.d(tag, "Finishing activity.");
				getActivity().finish();	// Bugfix AND-354
			}
			return;
		}
		
		if(voipService.inActiveCall())
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
		
		setupForceMuteLayout();
	}

	private void shutdown(final Bundle bundle) 
	{
		updateCallStatus();

		if(voipService != null)
		{
			voipService.setCallStatus(VoIPConstants.CallStatus.UNINITIALIZED);
		}

		unbindVoipService();

		playHangUpTone();

		if(activity.isShowingCallFailedFragment())
		{
			Logger.d(tag, "Not shutting down because call failed fragment is being displayed.");
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

	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (voipService == null)
			return false;
		
		if (!voipService.inActiveCall() && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
			&& voipService.getPartnerClient() != null && voipService.getPartnerClient().isInitiator())
		{
			voipService.stopRingtone();
			return true;
		}
		
		if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {

			if (voipService.getCallStatus() == CallStatus.INCOMING_CALL) {
				onAcceptCall();
			} else
				voipService.hangUp();
			
			return true;
		}
		
		return false;
	}

	private void showMessage(final String message) 
	{
		Logger.d(tag, "Toast: " + message);
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (toast != null)
					toast.cancel();
				toast = Toast.makeText(getActivity(), message, Toast.LENGTH_LONG);
				toast.show();
			}
		});
	}


		private void initProximityWakelock() 
		{
			if(activity.isShowingCallFailedFragment() || proximityWakeLock != null)
				return;

			// Set proximity sensor
			proximityWakeLock = ((PowerManager)getActivity().getSystemService(Context.POWER_SERVICE)).newWakeLock(PROXIMITY_SCREEN_OFF_WAKELOCK, "ProximityLock");
			proximityWakeLock.setReferenceCounted(false);
			proximityWakeLock.acquire();
		}

	private void releaseProximityWakelock()
	{
		if (proximityWakeLock != null)
		{
			proximityWakeLock.release();
			proximityWakeLock = null;
		}
	}
	
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

		if (callActionsView != null)
			callActionsView.setVisibility(View.GONE);

		// Get hold status from service if activity was destroyed
		updateCallStatus();

		activateActiveCallButtons();
	}

	@Override
	public void onAcceptCall()
	{
		Logger.d(tag, "Accepted call, starting audio...");
		if (voipService != null) {
			voipService.acceptIncomingCall();
			setupActiveCallLayout();
		}
	}

	@Override
	public void onDeclineCall()
	{
		Logger.d(tag, "Declined call, rejecting...");
		if (voipService != null)
			voipService.declineIncomingCall();
	}

	@Override
	public void onMessage()
	{
		VoIPClient client = voipService.getPartnerClient();
		if (client == null)
			return;
		
		Logger.d(tag, "Declined call, messaging " + client.getPhoneNumber());
		Bundle bundle = new Bundle();
		bundle.putString(VoIPConstants.PARTNER_MSISDN, client.getPhoneNumber());
		activity.showDeclineWithMessageFragment(bundle);
	}

	private void showHikeCallText()
	{
		TextView textView  = (TextView) getView().findViewById(R.id.hike_call); 
		SpannableString ss = new SpannableString("  " + getString(R.string.voip_call)); 
		@SuppressWarnings("deprecation")
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
		speakerButton.setSelected(voipService.getSpeaker());
		addButton.setSelected(false);

		setupActiveCallButtonActions();
	}

	private void setupForceMuteLayout() {
		
		TextView muteAllTextView = (TextView) getView().findViewById(R.id.mute_all_textview);
		ImageView muteAllIcon= (ImageView) getView().findViewById(R.id.force_mute_icon);
		boolean showForceMuteLayout = false;

		if (voipService.hostingConference()) {
			showForceMuteLayout = true;
			boolean forceMute = voipService.getHostForceMute();

			if (forceMute == true) {
				muteAllTextView.setText(getString(R.string.voip_conf_mute_all_on));
				muteAllIcon.setImageResource(R.drawable.ic_force_unmute);
			} else {
				muteAllTextView.setText(getString(R.string.voip_conf_mute_all_off));
				muteAllIcon.setImageResource(R.drawable.ic_force_mute);
			}
		} else {
			// If we are a participant in a conference in which the
			// host is using force mute, then display that info
			VoIPClient client = voipService.getPartnerClient();
			if (client != null && client.forceMute) {
				showForceMuteLayout = true;
				muteAllTextView.setText(getString(R.string.voip_error_force_mute));
				muteAllIcon.setImageResource(R.drawable.ic_force_unmute);
			} else
				showForceMuteLayout = false;
		}
		
		// Don't show mute status while phone is ringing.
		if (voipService.getCallStatus() == CallStatus.INCOMING_CALL)
			showForceMuteLayout = false;
		
		if (showForceMuteLayout) {
			// Remove the margin from the participants listview
			ListView conferenceList = (ListView) getView().findViewById(R.id.conference_list);
			ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) conferenceList.getLayoutParams();
			layoutParams.setMargins(0, 0, 0, 20);
			conferenceList.setLayoutParams(layoutParams);

			forceMuteContainer.setVisibility(View.VISIBLE);
			AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
			anim.setDuration(500);
			forceMuteContainer.startAnimation(anim);
			
		} else {
			// Set the margin from the participants listview
			ListView conferenceList = (ListView) getView().findViewById(R.id.conference_list);
			ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) conferenceList.getLayoutParams();
			layoutParams.setMargins(0, 50, 0, 20);
			conferenceList.setLayoutParams(layoutParams);
			forceMuteContainer.setVisibility(View.GONE);
		}
	}
	
	private void animateActiveCallButtons()
	{
		AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(500);

		View hangupButton = getView().findViewById(R.id.hang_up_btn);
		getView().findViewById(R.id.active_call_group).setVisibility(View.VISIBLE);
		hangupButton.setVisibility(View.VISIBLE);

		muteButton.startAnimation(anim);
		speakerButton.startAnimation(anim);
		bluetoothButton.startAnimation(anim);
		hangupButton.startAnimation(anim);

		if (addButton.getVisibility() == View.VISIBLE)
			addButton.startAnimation(anim);
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
	}

	private void setupActiveCallButtonActions()
	{
		getView().findViewById(R.id.hang_up_btn).setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) {
				Logger.d(tag, "Trying to hang up.");
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
					boolean success = voipService.setMute(newMute);
					if (success) {
						muteButton.setSelected(newMute);
						voipService.sendAnalyticsEvent(HikeConstants.LogEvent.VOIP_CALL_MUTE, newMute ? 1 : 0);
					} else {
						showMessage(getString(R.string.voip_error_force_mute));
					}
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
				
				if (newSpeaker == true)
					releaseProximityWakelock();
				else
					initProximityWakelock();
			}
		});

		addButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				releaseProximityWakelock();
				Intent intent = new Intent(HikeMessengerApp.getInstance(), ComposeChatActivity.class);
				intent.putExtra(HikeConstants.Extras.ADD_TO_CONFERENCE, true);
				startActivityForResult(intent, HikeConstants.ADD_TO_CONFERENCE_REQUEST);
			}
		});
		
		bluetoothButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				bluetoothButton.setSelected(!bluetoothButton.isSelected());
				voipService.toggleBluetoothFromActivity(bluetoothButton.isSelected());
			}
		});
		
		forceMuteContainer.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (voipService.hostingConference()) {
					boolean newMute = !voipService.getHostForceMute();
					Logger.w(tag, "Setting force mute to: " + newMute);
					voipService.setHostForceMute(newMute);
					setupForceMuteLayout();
				}
			}
		});
		
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == HikeConstants.ADD_TO_CONFERENCE_REQUEST) {
			if (resultCode == Activity.RESULT_OK) {
				if (data.hasExtra(HikeConstants.HIKE_CONTACT_PICKER_RESULT_FOR_CONFERENCE)) {
					ArrayList<String> msisdns = data.getStringArrayListExtra(HikeConstants.HIKE_CONTACT_PICKER_RESULT_FOR_CONFERENCE);
					Logger.w(tag, "Adding to conference: " + msisdns.toString());
					Intent intent = IntentFactory.getVoipCallIntent(HikeMessengerApp.getInstance(),
							msisdns, null, VoIPUtils.CallSource.ADD_TO_CONFERENCE);
					if (intent != null)
						getActivity().startService(intent);
				}
			}
		}
	};
	
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
				hideConferenceList();
				callDuration.startAnimation(anim);
				callDuration.setText(getString(R.string.voip_incoming));
				releaseProximityWakelock();
				break;

			case PARTNER_BUSY:
				callDuration.startAnimation(anim);
				callDuration.setText(getString(R.string.voip_partner_busy));
				break;

			case ACTIVE:
				startCallDuration();
				setupForceMuteLayout();
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
				
		default:
			// Logger.w(tag, "Unhandled status: " + status);
			callDuration.startAnimation(anim);
			callDuration.setText("");
			break;
		}
		
	}
	
	private void startCallDuration()
	{	
		AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(500);

		int duration = voipService.getCallDuration();
		if (duration >= 0) {
			callDuration.startAnimation(anim);
			callDuration.setBase((SystemClock.elapsedRealtime() - 1000 * duration));
			callDuration.start();
		}
	}

	private void setContactDetails()
	{
		TextView contactNameView = (TextView) getView().findViewById(R.id.contact_name);
		TextView contactMsisdnView = (TextView) getView().findViewById(R.id.contact_msisdn);

		VoIPClient clientPartner = voipService.getPartnerClient();
		if (clientPartner == null) 
		{
			// AND-1247
			Logger.w(tag, "Partner client info is null. Returning.");
			return;
		}

		ContactInfo contactInfo = ContactManager.getInstance().getContact(clientPartner.getPhoneNumber());
		String nameOrMsisdn;

		if(contactInfo == null)
		{
			// For unsaved contacts
			nameOrMsisdn = clientPartner.getPhoneNumber();
			Logger.d(tag, "Contact info is null for msisdn - " + nameOrMsisdn);
		}
		else
		{
			nameOrMsisdn = contactInfo.getNameOrMsisdn();
			partnerName = contactInfo.getName();
			if(partnerName != null && !voipService.hostingConference())
			{
				contactMsisdnView.setVisibility(View.VISIBLE);
				contactMsisdnView.setText(contactInfo.getMsisdn());
			}
		}

		if (voipService.hostingConference() || clientPartner.isHostingConference) {
			
			// Display the contact name and participant count
			contactNameView.setText(R.string.voip_conference_label);
			int clientCount = voipService.getClientCount();
			String numberOfParticipants;
			if (clientCount == 1)
				numberOfParticipants = getString(R.string.voip_conference_waiting_for_participants_info);
			else
				numberOfParticipants = clientCount + " " + getString(R.string.participants);
			
			if (voipService.getCallStatus() == CallStatus.INCOMING_CALL) {
				contactMsisdnView.setVisibility(View.VISIBLE);
				contactMsisdnView.setText(nameOrMsisdn);
			} else {
				contactMsisdnView.setVisibility(View.VISIBLE);
				contactMsisdnView.setText(numberOfParticipants);
			}
			
			// When a call is initiated or received, 
			// show the participants list only to the host
			// and to the participants after they accept the call
			if (voipService.inActiveCall() || voipService.hostingConference())
				updateConferenceList();
		} else {
			if(nameOrMsisdn != null && nameOrMsisdn.length() > 16)
			{
				contactNameView.setTextSize(24);
			}
			
			contactNameView.setText(nameOrMsisdn);
		}

		if (VoIPUtils.isConferencingEnabled(HikeMessengerApp.getInstance())) {
			if (clientPartner.isHostingConference || clientPartner.isUsingHikeDirect()) {
				addButton.setVisibility(View.GONE);
			} else {
				addButton.setVisibility(View.VISIBLE);
			}
		}

	}
	
	private void updateConferenceList() {

		ListView conferenceList = (ListView) getView().findViewById(R.id.conference_list);
		conferenceClients = voipService.getConferenceClients();

		confClientsAdapter = new ConferenceParticipantsAdapter(getActivity(), 0, 0, conferenceClients);
		conferenceList.setAdapter(confClientsAdapter);
		conferenceList.setVisibility(View.VISIBLE);
		conferenceList.setFocusable(false);
		conferenceList.setClickable(false);

		// Remove profile image
		ImageView profileView = (ImageView) getView().findViewById(R.id.profile_image);
		profileView.setVisibility(View.INVISIBLE);

	}
	
	private void hideConferenceList() {
		
		ListView conferenceList = (ListView) getView().findViewById(R.id.conference_list);
		conferenceList.setVisibility(View.GONE);
		ImageView profileView = (ImageView) getView().findViewById(R.id.profile_image);
		profileView.setVisibility(View.VISIBLE);
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

	private void startCallRateActivity(Bundle bundle)
	{
		int duration = bundle.getInt(VoIPConstants.CALL_DURATION);
		if(duration > VoIPConstants.MIN_CALL_DURATION_FOR_RATING_POPUP && isCallActive)
		{
			if(bundle!=null && VoIPUtils.shouldShowCallRatePopupNow())
			{
				Intent intent = IntentFactory.getVoipCallRateActivityIntent(getActivity());
				intent.putExtra(VoIPConstants.CALL_RATE_BUNDLE, bundle);
				startActivity(intent);
			}
		}
		isCallActive = false;
		getActivity().finish();
	}

	public void showCallFailedFragment(int callFailCode)
	{
		if(activity == null || voipService == null)
		{
			return;
		}
		
		if (voipService.getPartnerClient() == null) {
			Logger.w(tag, "Unable to retrieve client.");
			return;
		}
		
		// Disable call failed fragment when in a conference
		if (voipService.hostingConference())
			return;
		
		showCallFailedFragment(callFailCode, voipService.getPartnerClient().getPhoneNumber(), null);
	}

	public void showCallFailedFragment(int callFailCode, String msisdn, String customMessage)
	{
		if(activity == null || voipService == null)
		{
			return;
		}

		releaseProximityWakelock();

		Bundle bundle = new Bundle();
		bundle.putString(VoIPConstants.PARTNER_MSISDN, msisdn);
		bundle.putInt(VoIPConstants.CALL_FAILED_REASON, callFailCode);
		bundle.putString(VoIPConstants.PARTNER_NAME, partnerName);
		bundle.putString(VoIPConstants.CUSTOM_MESSAGE, customMessage);

		Logger.d(tag, "Showing call failed fragment.");
		activity.showCallFailedFragment(bundle);
		voipService.stop();
	}

	@SuppressWarnings("deprecation")
	public void setAvatar()
	{
		VoIPClient clientPartner = voipService.getPartnerClient();
		if (clientPartner == null) 
			return;
		
		String msisdn = clientPartner.getPhoneNumber();

		ImageView imageView = (ImageView) getView().findViewById(R.id.profile_image);
		int size = getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);

		ProfileImageLoader profileImageLoader = new ProfileImageLoader(getActivity(), msisdn, imageView, size, false, false);
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
					// showMessage("Encryption key: " + voipService.getSessionKeyHash());
					boolean conferencing = voipService.toggleConferencing();
					showMessage("Conferencing: " + conferencing);
				}
				return;
			}
		easter = 0;
	}
	
	private void toggleBluetoothButton(boolean show) {
		if (show) {
			bluetoothButton.setSelected(true);
			bluetoothButton.setVisibility(View.VISIBLE);
		} else
			bluetoothButton.setVisibility(View.GONE);
	}
	
	private void playHangUpTone() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				ToneGenerator tg = null;
				try {
					tg = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
					tg.startTone(ToneGenerator.TONE_SUP_PIP);
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (RuntimeException e) {
					Logger.e(tag, "ToneGenerator RuntimeException: " + e.toString());
				} finally {
					if (tg != null) {
						tg.stopTone();
						tg.release();
					}
				}
			}
		}).start();
	}
	
}
	
