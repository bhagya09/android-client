package com.bsb.hike.ui.fragments;

import java.util.Map;

import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.IOfflineCallbacks;
import com.bsb.hike.offline.OfflineAnalytics;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.offline.OfflineConstants.DisconnectFragmentType;
import com.bsb.hike.offline.OfflineParameters;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineConstants.ERRORCODE;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.smartImageLoader.ProfilePicImageLoader;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.fragments.OfflineDisconnectFragment.OfflineConnectionRequestListener;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.HoloCircularProgress;
import com.google.gson.Gson;


public class OfflineAnimationFragment extends DialogFragment implements IOfflineCallbacks ,OfflineConnectionRequestListener
{
	private static final String TAG = "OfflineAnimationFragment";

	private String msisdn;
	
	private ImageView avatarImageView;
	
	private ImageView logo;
	
	private View fragmentView;
	
	private FrameLayout imageViewLayout;
	
	private ObjectAnimator  rotateAnimation;
	
	private FrameLayout frame;
	
	private int originalPos[] =  new int[2];
	
	private TextView connectionInfo;
	
	private String contactFirstName;
	
	private static final String MSISDN = "msisdn";

	protected static final int UPDATE_ANIMATION_MESSAGE = 1;

	protected static final int START_TIMER = 2;
	
	protected static final int UPDATE_ANIMATION_SECOND_MESSAGE = 3;

	private TextView secondMessage;
	
	private Button retryButton;
	
	private Button helpButton;
	
	private OfflineConnectionRequestListener listener;
	
	private CountDownTimer  timer = null;
	
	private AnimatorSet bringToCenter = null;
	
	private AnimatorSet bringBackToTop = null;
	
	private OfflineDisconnectFragment offlineDisconnectFragment;
	
	private View divider;
	
	private View verticalDivider;
	
	private OfflineParameters offlineParameters=null;
	
	private int timerDuration;
	
	private boolean shouldResumeFragment = false;
	
	private HoloCircularProgress circularProgress;
	
	private Boolean connectionCancelled =false;
	
	private Context context;
	
	private  Handler uiHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg)
		{
			if (msg == null)
			{
				Logger.e(TAG, "Getting a null message in Offline Animation Fragment");
				return;
			}
			handleUIMessage(msg);
		}

	};
	
	public static OfflineAnimationFragment newInstance(String msisdn)
	{
		OfflineAnimationFragment offlineAnimationFragment = new OfflineAnimationFragment();
		Bundle data = new Bundle(1);
		data.putString(MSISDN, msisdn);
		offlineAnimationFragment.setArguments(data);
		return offlineAnimationFragment; 
	}
	
	
	/**
	 * Performs tasks on the UI thread.
	 */
	protected void handleUIMessage(Message msg)
	{
		switch (msg.what)
		{
		case UPDATE_ANIMATION_MESSAGE:
			if (!isOfflineConnected())
			{
				updateAnimationText(connectionInfo, (String) (msg.obj), false);
			}
			break;
		case UPDATE_ANIMATION_SECOND_MESSAGE:
			if (timerDuration > 0)
			{
				if (!isOfflineConnected())
					updateAnimationText(secondMessage, (String) msg.obj, false);
			}
			break;
		case START_TIMER:
			if (!isOfflineConnected())
			{
				if (timerDuration > 0)
				{
					updateAnimationText(connectionInfo, "" + timerDuration / 1000, true);
				}
				else
				{
					updateAnimationText(connectionInfo, getString(R.string.disconnecting_offline), false);
				}

			}
			break;
		}
		
	}
	
	public boolean isOfflineConnected()
	{
		return OfflineUtils.isConnectedToSameMsisdn(msisdn);
	}
	
	private void startTimer()
	{
		
		timer = new CountDownTimer(timerDuration,1000)
		{
			
			@Override
			public void onTick(long millisUntilFinished)
			{
				connectionInfo.setText("" +millisUntilFinished/1000);
			}
			
			@Override
			public void onFinish()
			{
					if(isAdded())
					{
						connectionInfo.setText(getString(R.string.disconnecting_offline));
						secondMessage.setVisibility(View.GONE);
					}
			}
		};
		timer.start();
				
	}


	private void updateAnimationText(final TextView source,final String message,final boolean startTimer)
	{
		
		AlphaAnimation  disappearAnimation = new AlphaAnimation(1.0f, 0.0f);
		disappearAnimation.setDuration(400);
		final AlphaAnimation  appearAnimation = new AlphaAnimation(0.0f, 1.0f);
		appearAnimation.setDuration(400);
		
		disappearAnimation.setAnimationListener(new AnimationListener()
		{
			
			@Override
			public void onAnimationStart(Animation animation)
			{
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				if(isAdded())
				{
					source.setText("");
					source.setVisibility(View.VISIBLE);
					source.startAnimation(appearAnimation);
				}
				
			}
		});
		
		appearAnimation.setAnimationListener(new AnimationListener()
		{
			
			@Override
			public void onAnimationStart(Animation animation)
			{
				if(isAdded())
				{
					source.setText(message);
				}
			}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				if(isAdded())
				{
					if(startTimer)
					{
						startTimer();
					}
				}
			}
		});
		
		source.startAnimation(disappearAnimation);
		
	}

	protected void sendUIMessage(int what, Object data)
	{
		Message message = Message.obtain();
		message.what = what;
		message.obj = data;
		uiHandler.sendMessage(message);
	}

	protected void sendUIMessage(int what, long delayTime, Object data)
	{
		Message message = Message.obtain();
		message.what = what;
		message.obj = data;
		uiHandler.sendMessageDelayed(message, delayTime);
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		fragmentView = inflater.inflate(R.layout.offline_animation, null);
		setupView();
		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    return fragmentView;
	}
	
	private void startAnimation()
	{
		final DisplayMetrics dm = new DisplayMetrics();
	    getActivity().getWindowManager().getDefaultDisplay().getMetrics( dm );
	    imageViewLayout.getLocationOnScreen(originalPos);
	    final int xDest = dm.widthPixels/2 - (imageViewLayout.getMeasuredWidth()/2);;
	    final int yDest = dm.heightPixels/2 - (imageViewLayout.getMeasuredHeight()/2) ;
	    final int xd = (xDest -(imageViewLayout.getMeasuredHeight()*3)/4 );
	    final int yd  = (yDest -(imageViewLayout.getMeasuredHeight())/2);
	    
	    //Translate to the middle and scale
	    ObjectAnimator translateX = ObjectAnimator.ofFloat(imageViewLayout, "translationX",xd);
	    ObjectAnimator translateY = ObjectAnimator.ofFloat(imageViewLayout, "translationY",yd);
	    ObjectAnimator scaleX = ObjectAnimator.ofFloat(imageViewLayout,"scaleX",2.2f);
	    ObjectAnimator scaleY = ObjectAnimator.ofFloat(imageViewLayout,"scaleY",2.2f);
	    translateX.setDuration(800);
	    translateY.setDuration(800);
	    scaleX.setDuration(800);
	    scaleY.setDuration(800);
	    translateX.setInterpolator(new OvershootInterpolator(0.2f));
	    translateY.setInterpolator(new OvershootInterpolator(0.2f));   
	  
	    //Scale up at the middle of screen
		ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleX", 2.9f);
		ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleY", 2.9f);
		scaleXUp.setDuration(400);
		scaleYUp.setDuration(400);
		
	    //Scale down at the middle of screen
		ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(imageViewLayout, "scaleX", 2.7f);
		ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(imageViewLayout, "scaleY", 2.7f);
		scaleXDown.setDuration(250);
		scaleYDown.setDuration(250);
	    
		//Scale up holo circular ring 
		ObjectAnimator scaleXUpHolo = ObjectAnimator.ofFloat(frame, "scaleX", 1.1f);
		ObjectAnimator scaleYUpHolo = ObjectAnimator.ofFloat(frame, "scaleY", 1.1f);
		scaleXUpHolo.setDuration(250);
		scaleYUpHolo.setDuration(250);
		scaleXUpHolo.setInterpolator(new OvershootInterpolator(0.9f));
		scaleYUpHolo.setInterpolator(new OvershootInterpolator(0.9f));
		scaleXUpHolo.addListener(new AnimatorListener()
		{
			
			@Override
			public void onAnimationStart(Animator animation)
			{
				frame.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAnimationRepeat(Animator animation)
			{
				
			}
			
			@Override
			public void onAnimationEnd(Animator animation)
			{
				
			}
			
			@Override
			public void onAnimationCancel(Animator animation)
			{
				
			}
		});
		
		//Scale down holo circular ring
		ObjectAnimator scaleXDownHolo = ObjectAnimator.ofFloat(frame, "scaleX", 1f);
		ObjectAnimator scaleYDownHolo = ObjectAnimator.ofFloat(frame, "scaleY", 1f);
		scaleXDownHolo.setDuration(350);
		scaleYDownHolo.setDuration(350);
		scaleXDownHolo.setInterpolator(new OvershootInterpolator(0.9f));
		scaleYDownHolo.setInterpolator(new OvershootInterpolator(0.9f));
		
		
		bringToCenter =  new AnimatorSet();
	
	    bringToCenter.playTogether(translateX,translateY,scaleX,scaleY);
	    bringToCenter.play(scaleXUp).with(scaleYUp).after(translateX);
	    bringToCenter.play(scaleXDown).with(scaleYDown).after(scaleXUp);
		bringToCenter.play(scaleXUpHolo).with(scaleYUpHolo).after(1100);
		bringToCenter.play(scaleYDownHolo).with(scaleXDownHolo).after(scaleXUpHolo);
	    
		bringToCenter.addListener(new AnimatorListener()
		{
			
			@Override
			public void onAnimationStart(Animator animation)
			{
				ImageView  connectionIcon = (ImageView)fragmentView.findViewById(R.id.offline_icon);
				connectionIcon.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAnimationRepeat(Animator animation)
			{
				
			}
			
			@Override
			public void onAnimationEnd(Animator animation)
			{
				if(shouldResumeFragment && OfflineController.getInstance().getOfflineState() != OFFLINE_STATE.CONNECTING)
				{
					updateUIOnDisconnect();
				}
				else
				{
					startRotateAnimation();
				}
				
			}

			@Override
			public void onAnimationCancel(Animator animation)
			{
				
			}
		});
	    bringToCenter.start();
	}
		
	
	protected void startRotateAnimation()
	{
		
			ImageView progressBead = (ImageView)fragmentView.findViewById(R.id.bead);
			progressBead.setVisibility(View.VISIBLE);
			rotateAnimation = ObjectAnimator.ofFloat(progressBead,View.ROTATION,0, 359.9f);   
			//,Animation.RELATIVE_TO_SELF,0.38f,Animation.RELATIVE_TO_SELF,2.9f);
			progressBead.setPivotX((float)frame.getWidth()/21);
			progressBead.setPivotY(frame.getHeight()/2);
			rotateAnimation.setDuration(1000);
			rotateAnimation.setRepeatCount(Animation.INFINITE);
			rotateAnimation.setInterpolator(new LinearInterpolator());
			rotateAnimation.start();
		
	}

	private void setupView()
	{
		imageViewLayout = (FrameLayout)fragmentView.findViewById(R.id.animation_avator_frame);
		avatarImageView = (ImageView)fragmentView.findViewById(R.id.animation_avatar);
		connectionInfo = (TextView)fragmentView.findViewById(R.id.connectionInfo);
		divider = (View)fragmentView.findViewById(R.id.divider);
		secondMessage =(TextView)fragmentView.findViewById(R.id.second_message);
		logo =(ImageView)fragmentView.findViewById(R.id.offline_icon);
		retryButton = (Button)fragmentView.findViewById(R.id.retry_button);
		frame = (FrameLayout)fragmentView.findViewById(R.id.animation_circular_progress_holder);
		circularProgress = (HoloCircularProgress)fragmentView.findViewById(R.id.animation_circular_progress);
		helpButton = (Button)fragmentView.findViewById(R.id.help_button);
		helpButton.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				context = HikeMessengerApp.getInstance().getApplicationContext();
				Intent intent = IntentFactory.getHikeDirectHelpPageActivityIntent(context);
				startActivity(intent);
			}
		});
	    verticalDivider = (View)fragmentView.findViewById(R.id.v_divider);
		ContactInfo contactInfo  = ContactManager.getInstance().getContact(msisdn);
		contactFirstName = msisdn;
		if(contactInfo!=null && !TextUtils.isEmpty(contactInfo.getFirstName()))
		{
			contactFirstName = contactInfo.getFirstName();
		}
		ImageView closeAnimation = (ImageView)fragmentView.findViewById(R.id.cross_animation);
		closeAnimation.setOnClickListener(new OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				if(OfflineController.getInstance().getOfflineState()== OFFLINE_STATE.CONNECTING)
				{
					showPreviouslyConnectingFragment();
				}
				else
				{
					closeFragment();
				}
				OfflineAnalytics.closeAnimationCrossClicked();
			}
		});
		int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 95, getActivity().getResources().getDisplayMetrics());

		ProfilePicImageLoader profileImageLoader=new ProfilePicImageLoader(getActivity(), size);
		profileImageLoader.setDefaultAvatarIfNoCustomIcon(true);
		profileImageLoader.setHiResDefaultAvatar(true);
		String mapedId = msisdn + ProfileActivity.PROFILE_PIC_SUFFIX;
		profileImageLoader.loadImage(mapedId, avatarImageView, true,true);
		
	}
	
	protected void showPreviouslyConnectingFragment()
	{
		FragmentManager fragmentManager = getChildFragmentManager();
		Fragment fragment = fragmentManager.findFragmentByTag(OfflineConstants.OFFLINE_DISCONNECT_FRAGMENT);
		if(fragment!=null && fragment.isAdded())
		{
			return;
		}
		FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
		
		Logger.d(TAG,msisdn);
		offlineDisconnectFragment = OfflineDisconnectFragment.newInstance(msisdn,null,DisconnectFragmentType.CONNECTING);
		offlineDisconnectFragment.setConnectionListner(this);
		fragmentTransaction.replace(R.id.disconnect_layout, offlineDisconnectFragment, OfflineConstants.OFFLINE_DISCONNECT_FRAGMENT);
		fragmentTransaction.commit();
		
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setStyle(STYLE_NO_TITLE, android.R.style.Theme_Translucent);
		offlineParameters = new Gson().fromJson(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.OFFLINE, "{}"), OfflineParameters.class);
	    timerDuration = offlineParameters.getConnectionTimeout() - OfflineConstants.TIMER_START_TIME;
	    Bundle arguments = getArguments();
	    if(arguments != null)
	    {
	       handleArguments(arguments);
	    }
	}


	@Override
	public void onStart()
	{
		super.onStart();
	}
	
	@Override
	public void onStop()
	{
		super.onStop();
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
	}
	private void handleArguments(Bundle arguments)
	{
		msisdn = arguments.getString(MSISDN);
	}

	@Override
	public void onActivityCreated(Bundle arg0)
	{
		super.onActivityCreated(arg0);
		shouldResumeFragment=(arg0!=null);
		
		fragmentView.post(new Runnable()
		{
			
			@Override
			public void run()
			{
				if(!isAdded())
				{
					return;
				}
				circularProgress.setMarkerEnabled(false);
				sendUiMessages();
				startAnimation();
			}
		});
		
		getDialog().setOnKeyListener(new OnKeyListener() 
	    { 
	        @Override 
	        public boolean onKey(android.content.DialogInterface dialog, int keyCode,android.view.KeyEvent event) {
	 
	            if ((keyCode ==  android.view.KeyEvent.KEYCODE_BACK))
	                { 

			        	if(OfflineController.getInstance().getOfflineState()== OFFLINE_STATE.CONNECTING)
			        	{
			                showPreviouslyConnectingFragment();
			        	}
			        	else
			        	{
			        		closeFragment();
			        	}
	                    return true; // pretend we've processed it 
	                } 
	            else  
	                return false; // pass on to be processed as normal 
	        } 
	    }); 
	}
	
	protected void sendUiMessages()
	{

		connectionInfo.setText(getResources().getString(R.string.connecting_to, contactFirstName));
		connectionInfo.setVisibility(View.VISIBLE);
		secondMessage.setText("");
		if (!shouldResumeFragment)
		{
			sendUIMessage(UPDATE_ANIMATION_MESSAGE, OfflineConstants.FIRST_MESSAGE_TIME, getResources().getString(R.string.offline_animation_second_message));
			sendUIMessage(UPDATE_ANIMATION_SECOND_MESSAGE,OfflineConstants.SECOND_MESSAGE_TIME, getResources().getString(R.string.offline_animation_third_message, contactFirstName));
			sendUIMessage(START_TIMER, OfflineConstants.TIMER_START_TIME, null);
		}
		else
		{
			removeDisconnectFragment(false);
		}
	}

	@Override
	public void wifiP2PScanResults(WifiP2pDeviceList peerList)
	{
		
		
	}
	@Override
	public void wifiScanResults(Map<String, ScanResult> results)
	{
		
	}
	@Override
	public void onDisconnect(ERRORCODE errorCode)
	{

		switch (errorCode)
		{
		case DISCONNECTING:
			break;

		case OUT_OF_RANGE:
		case TIMEOUT:
		case COULD_NOT_CONNECT:
		case REQUEST_CANCEL:
		case SHUTDOWN:
			fragmentView.post(new Runnable()
			{
				@Override
				public void run()
				{
					if(!connectionCancelled)
					{
						updateUIOnDisconnect();
					}
				}
			});
			break;
		}

	}
	
	private void updateUIOnDisconnect()
	{
		if (!isAdded())
		{
			return;
		}
		removePostedMessages();
		hideAndStopTimer();
		showRetryIcon(R.drawable.cross_retry);
		connectionInfo.setText(getResources().getString(R.string.retry_connection));
		connectionInfo.setVisibility(View.VISIBLE);
		frame.setVisibility(View.INVISIBLE);
		secondMessage.setVisibility(View.INVISIBLE);	
		cancelRotationAnimation();		
		showConnectionFailurePanel();
		retryButton.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				sendUiMessages();
				hideConnectionFailurePanel();
				showRetryIcon(R.drawable.iconconnection);
				frame.setVisibility(View.VISIBLE);
				startRotateAnimation();		
				listener.onConnectionRequest(false);
				OfflineAnalytics.retryButtonClicked();
			}

		});
	}

	

	@Override
	public void connectedToMsisdn(String connectedDevice)
	{
		removePostedMessages();
		
	    fragmentView.post(new Runnable()
		{
			
			@Override
			public void run()
			{
				
				if (!isAdded())
				{
					return;
				}
				
				frame.setVisibility(View.INVISIBLE);
				secondMessage.setVisibility(View.INVISIBLE);
				if(rotateAnimation!=null)
				 {
					 rotateAnimation.cancel();
				 }
					 
				 if(bringToCenter!=null)
				 {
					 bringToCenter.cancel();
				 }
				 
				 if(timer!=null)
				 {
					 timer.cancel();
				 }
				 
				 updateAnimationText(connectionInfo,getResources().getString(R.string.connection_established),false);
				
				 //Scale up 
				 ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleX", 3.5f);
				 ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleY", 3.5f);
				 scaleXUp.setDuration(1500);
				 scaleYUp.setDuration(1500);
				
				 //Translate back up
				 ObjectAnimator translateX = ObjectAnimator.ofFloat(imageViewLayout, "translationX",originalPos[0] - (imageViewLayout.getWidth()*3)/4);
				 ObjectAnimator translateY = ObjectAnimator.ofFloat(imageViewLayout, "translationY",originalPos[1] - (imageViewLayout.getHeight()*3)/4);
				 translateX.setDuration(500);
				 translateY.setDuration(500);
				 
				 
				 //Scale down
				 ObjectAnimator scaleX = ObjectAnimator.ofFloat(imageViewLayout,"scaleX",1f);
				 ObjectAnimator scaleY = ObjectAnimator.ofFloat(imageViewLayout,"scaleY",1f);
				 scaleX.setDuration(500);
				 scaleY.setDuration(500);
				 
				 //Alpha animation on imageLogo
				 ObjectAnimator alphaAnimation  =  ObjectAnimator.ofFloat(logo, View.ALPHA,1,0);
				 alphaAnimation.setDuration(500);
				 
				 bringBackToTop =  new AnimatorSet();
				 bringBackToTop.playTogether(scaleXUp,scaleYUp);
				 bringBackToTop.play(translateX).with(translateY).with(scaleX).with(scaleY).with(alphaAnimation).after(scaleXUp);
				 bringBackToTop.addListener(new AnimatorListener()
					{
						
						@Override
						public void onAnimationStart(Animator animation)
						{
							flipRetryIcon();
		                }
						
						@Override
						public void onAnimationRepeat(Animator animation)
						{
							
						}
						
						@Override
						public void onAnimationEnd(Animator animation)
						{
							if (isAdded())
							{
								closeFragment();
							}
						}
						
						@Override
						public void onAnimationCancel(Animator animation)
						{
						
						}
					});
				
				 bringBackToTop.start();
	       }
		
		});
		
		
    }
	
	private void closeFragment()
	{
		Utils.unblockOrientationChange((ChatThreadActivity)getActivity());
		dismissAllowingStateLoss();
	}
	
	public void setConnectionListner(OfflineConnectionRequestListener  listener)
	{
		this.listener =listener;
	}
	
	public void showConnectionFailurePanel()
	{
		retryButton.setVisibility(View.VISIBLE);
		divider.setVisibility(View.VISIBLE);
		helpButton.setVisibility(View.VISIBLE);
		verticalDivider.setVisibility(View.VISIBLE);
	}
	
	public void showRetryIcon(final int drawrable)
	{
		if (!isAdded())
		{
			return;
		}
		Animation flipAnimationHalf = AnimationUtils.loadAnimation(getActivity(), R.anim.to_middle);
		final Animation flipAnimationFull = AnimationUtils.loadAnimation(getActivity(), R.anim.from_middle);
		
		AnimationListener listener=new AnimationListener()
		{
			
			@Override
			public void onAnimationStart(Animation animation)
			{
					
			}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				logo.setImageDrawable(getActivity().getResources().getDrawable(drawrable));
				logo.clearAnimation();
				logo.setAnimation(flipAnimationFull);
				logo.startAnimation(flipAnimationFull);
			}
		};
		logo.clearAnimation();
		flipAnimationHalf.setAnimationListener(listener);
		logo.setAnimation(flipAnimationHalf);
		logo.startAnimation(flipAnimationHalf);
		
	}
	
	public void flipRetryIcon()
	{
		Animation animation1 = AnimationUtils.loadAnimation(getActivity(), R.anim.to_middle);
		AlphaAnimation hideAnimationHalf = new AlphaAnimation(1.0f,0.0f);
		hideAnimationHalf.setDuration(250);
		AnimationSet flipAniamtionSetHalf = new AnimationSet(false);
		
		AnimationListener listener=new AnimationListener()
		{
			
			@Override
			public void onAnimationStart(Animation animation)
			{
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{
					
			}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				logo.setVisibility(View.GONE);
			}
		};
		logo.clearAnimation();
		animation1.setAnimationListener(listener);
		flipAniamtionSetHalf.addAnimation(animation1);
		flipAniamtionSetHalf.addAnimation(hideAnimationHalf);
		logo.setAnimation(flipAniamtionSetHalf);
		logo.startAnimation(flipAniamtionSetHalf);
	}
	
	public void cancelRotationAnimation()
	{
		if(rotateAnimation!=null)
			rotateAnimation.cancel();
	}
	
	public void removePostedMessages()
	{
		uiHandler.removeMessages(UPDATE_ANIMATION_MESSAGE);
		uiHandler.removeMessages(UPDATE_ANIMATION_SECOND_MESSAGE);
		uiHandler.removeMessages(START_TIMER);
	}
	
	private void hideAndStopTimer()
	{
		connectionInfo.setVisibility(View.INVISIBLE);
		if(timer!=null)
			timer.cancel();
	}
	
	protected void hideTimer()
	{
		if(isAdded())
		{
			connectionInfo.setVisibility(View.INVISIBLE);
		}
	}
	
	public void hideConnectionFailurePanel()
	{
		retryButton.setVisibility(View.GONE);
		divider.setVisibility(View.GONE);
		helpButton.setVisibility(View.GONE);
		verticalDivider.setVisibility(View.GONE);
	}

	@Override
	public void onConnectionRequest(Boolean startAnimation)
	{
		listener.onConnectionRequest(startAnimation);
	}
	

	@Override
	public void onDisconnectionRequest()
	{
		connectionCancelled = true;
		listener.onDisconnectionRequest();
	}

	@Override
	public void removeDisconnectFragment(boolean removeParent)
	{
		Fragment fragment = getChildFragmentManager().findFragmentByTag(OfflineConstants.OFFLINE_DISCONNECT_FRAGMENT);
		if(fragment != null)
		    getChildFragmentManager().beginTransaction().remove(fragment).commit();	
		if(removeParent)
		{
			closeFragment();
		}
	}
}
