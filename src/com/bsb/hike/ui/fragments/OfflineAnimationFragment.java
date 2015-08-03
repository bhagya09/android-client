package com.bsb.hike.ui.fragments;

import java.util.Map;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.DialogInterface.OnKeyListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.IOfflineCallbacks;
import com.bsb.hike.offline.OfflineConstants;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineManager;
import com.bsb.hike.offline.OfflineConstants.ERRORCODE;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.ui.fragments.OfflineDisconnectFragment.OfflineConnectionRequestListener;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class OfflineAnimationFragment extends DialogFragment implements IOfflineCallbacks ,OfflineConnectionRequestListener
{
	protected static final String TAG = "OfflineAnimationFragment";

	String msisdn;
	
	ImageView avatarImageView;
	
	ImageView logo;
	
	View fragmentView;
	
	FrameLayout imageViewLayout;
	
	RotateAnimation  rotateAnimation;
	
	FrameLayout frame;
	
	int originalPos[] =  new int[2];
	
	TextView connectionInfo;
	
	String contactFirstName;
	
	protected static final int UPDATE_ANIMATION_MESSAGE = 1;

	protected static final int START_TIMER = 2;

	private static final String MSISDN = "msisdn";

	TextView timerText;
	
	Button retryButton;
	
	OfflineConnectionRequestListener listener;
	
	CountDownTimer  timer =null;
	
	AnimatorSet bringToCenter;
	
	OfflineDisconnectFragment offlineDisconnectFragment;
	
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
		switch(msg.what)
		{
			case UPDATE_ANIMATION_MESSAGE:
				updateAnimationText((String)(msg.obj));
				break;
			case START_TIMER:
				timerText.setVisibility(View.VISIBLE);
				startTimer();
				break;
		}
		
	}
	
	private void startTimer()
	{
		
		timerText.setText("30");
		timer = new CountDownTimer(30000,1000)
		{
			
			@Override
			public void onTick(long millisUntilFinished)
			{
				timerText.setText("" +millisUntilFinished/1000);
			}
			
			@Override
			public void onFinish()
			{
				hideTimer();
				
			}
		};
		timer.start();
	}


	private void updateAnimationText(String message)
	{
		connectionInfo.setText(message);
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
	    
	    //Toast.makeText(getActivity(), "" + xd + "-" + imageViewLayout.getWidth() + " - " + yd + " - "+ imageViewLayout.getHeight(), Toast.LENGTH_SHORT).show();
	    
	    //Translate to the middle and scale
	    ObjectAnimator translateX = ObjectAnimator.ofFloat(imageViewLayout, "translationX",xd);
	    ObjectAnimator translateY = ObjectAnimator.ofFloat(imageViewLayout, "translationY",yd);
	    ObjectAnimator scaleX = ObjectAnimator.ofFloat(imageViewLayout,"scaleX",2.6f);
	    ObjectAnimator scaleY = ObjectAnimator.ofFloat(imageViewLayout,"scaleY",2.6f);
	    translateX.setDuration(600);
	    translateY.setDuration(600);
	    scaleX.setDuration(600);
	    scaleY.setDuration(600);
	    
	    //Scale down at the middle of screen
	    ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(imageViewLayout, "scaleX", 2);
	    ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(imageViewLayout, "scaleY", 2);
	    scaleXDown.setDuration(200);
	    scaleYDown.setDuration(200);
	    
	    //Scale up at the middle of screen
		ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleX", 2.6f);
		ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleY", 2.6f);
		scaleXUp.setDuration(200);
		scaleYUp.setDuration(200);
	    
		//Alpha animation on the holo circular ring
	    ObjectAnimator alphaAnimation  =  ObjectAnimator.ofFloat(frame, View.ALPHA,0.0f,1.0f);
		alphaAnimation.setDuration(200);
		
		//Scale up holo circular ring 
		ObjectAnimator scaleXUpHolo = ObjectAnimator.ofFloat(frame, "scaleX", 1.1f);
		ObjectAnimator scaleYUpHolo = ObjectAnimator.ofFloat(frame, "scaleY", 1.1f);
		scaleXUpHolo.setDuration(400);
		scaleYUpHolo.setDuration(400);
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
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd(Animator animation)
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationCancel(Animator animation)
			{
				// TODO Auto-generated method stub
				
			}
		});
		
		//Scale down holo circular ring
		ObjectAnimator scaleXDownHolo = ObjectAnimator.ofFloat(frame, "scaleX", 1f);
		ObjectAnimator scaleYDownHolo = ObjectAnimator.ofFloat(frame, "scaleY", 1f);
		scaleXDownHolo.setDuration(400);
		scaleYDownHolo.setDuration(400);
		
		
		
		bringToCenter =  new AnimatorSet();
	    bringToCenter.playTogether(translateX,translateY,scaleX,scaleY);
	    bringToCenter.play(scaleXDown).with(scaleYDown).after(translateX);
	    bringToCenter.play(scaleXUp).with(scaleYUp).after(scaleXDown);
		bringToCenter.play(scaleXUpHolo).with(scaleYUpHolo).with(alphaAnimation).after(1000);
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
				startRotateAnimation();
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
		rotateAnimation = new RotateAnimation(0, 360,Animation.RELATIVE_TO_SELF,0.35f,Animation.RELATIVE_TO_SELF,2.8f);
		rotateAnimation.setDuration(1000);
		rotateAnimation.setRepeatCount(Animation.INFINITE);
		rotateAnimation.setInterpolator(new LinearInterpolator());
		progressBead.startAnimation(rotateAnimation);
	}

	private void setupView()
	{
		imageViewLayout = (FrameLayout)fragmentView.findViewById(R.id.animation_avator_frame);
		avatarImageView = (ImageView)fragmentView.findViewById(R.id.animation_avatar);
		connectionInfo = (TextView)fragmentView.findViewById(R.id.connectionInfo);
		timerText =(TextView)fragmentView.findViewById(R.id.timer);
		logo =(ImageView)fragmentView.findViewById(R.id.offline_icon);
		retryButton = (Button)fragmentView.findViewById(R.id.retry_button);
		frame = (FrameLayout)fragmentView.findViewById(R.id.animation_circular_progress_holder);
		connectionInfo.setVisibility(View.VISIBLE);
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
			}
		});
		Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(msisdn);
		if (drawable == null)
		{
			drawable = HikeMessengerApp.getLruCache().getDefaultAvatar(msisdn, false);
		}
		avatarImageView.setScaleType(ScaleType.FIT_CENTER);
		avatarImageView.setImageDrawable(drawable);
		
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
		
		offlineDisconnectFragment = OfflineDisconnectFragment.newInstance(OfflineUtils.getConnectingMsisdn(), "");
		offlineDisconnectFragment.setConnectionListner(this);
		fragmentTransaction.replace(R.id.disconnect_layout, offlineDisconnectFragment, OfflineConstants.OFFLINE_DISCONNECT_FRAGMENT);
		fragmentTransaction.commit();
		
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setStyle(STYLE_NO_TITLE, android.R.style.Theme_Translucent);
		
	    // handle fragment arguments
	    Bundle arguments = getArguments();
	    if(arguments != null)
	    {
	       handleArguments(arguments);
	    }
	}

	
	private void handleArguments(Bundle arguments)
	{
		msisdn = arguments.getString(MSISDN);
	}

	@Override
	public void onActivityCreated(Bundle arg0)
	{
		super.onActivityCreated(arg0);
		fragmentView.post(new Runnable()
		{
			
			@Override
			public void run()
			{
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
		sendUIMessage(UPDATE_ANIMATION_MESSAGE,getResources().getString(R.string.connecting_to,contactFirstName));
		sendUIMessage(UPDATE_ANIMATION_MESSAGE,15000,getResources().getString(R.string.offline_animation_second_message));
		sendUIMessage(UPDATE_ANIMATION_MESSAGE,30000, 
				getResources().getString(R.string.offline_animation_third_message,contactFirstName));
		sendUIMessage(START_TIMER, 30000, null);
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
		 fragmentView.post(new Runnable()
			{
				@Override
				public void run()
				{
					updateUIOnDisconnect();
				}
			});
	}
	
	private void updateUIOnDisconnect()
	{
		showRetryIcon();
		cancelRotationAnimation();
		hideAndStopTimer();
		frame.setVisibility(View.INVISIBLE);
		removePostedMessages();
		scaleUpAvatar(3.5f, 3.5f);
		showRetryButton();
		updateAnimationText(getResources().getString(R.string.retry_connection));
		retryButton.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				hideRetryButton();
				AnimatorSet  anim = getScaleDownAvatarAnimator(2.6f,2.6f);
				anim.addListener(new AnimatorListener()
				{
					
					@Override
					public void onAnimationStart(Animator animation)
					{
						
					}
					
					@Override
					public void onAnimationRepeat(Animator animation)
					{
						
					}
					
					@Override
					public void onAnimationEnd(Animator animation)
					{
						logo.setImageDrawable(getResources().getDrawable(R.drawable.iconconnection));
						frame.setVisibility(View.VISIBLE);
						startRotateAnimation();
						sendUiMessages();
						listener.onConnectionRequest(false);
					}
					
					@Override
					public void onAnimationCancel(Animator animation)
					{
						
					}
				});
				anim.start();
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
				
				 connectionInfo.setText(getResources().getString(R.string.connection_established));
				 
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
					 timerText.setVisibility(View.GONE);
				 }
				 
				 frame.setVisibility(View.INVISIBLE);
				 
				 //Scale up 
				 ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleX", 3.5f);
				 ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleY", 3.5f);
				 
				 //Translate back up
				 ObjectAnimator translateX = ObjectAnimator.ofFloat(imageViewLayout, "translationX",originalPos[0] - (imageViewLayout.getWidth()*3)/4);
				 ObjectAnimator translateY = ObjectAnimator.ofFloat(imageViewLayout, "translationY",originalPos[1] - (imageViewLayout.getHeight()*3)/4);
				 translateX.setDuration(700);
				 translateY.setDuration(700);
				 
				 
				 //Scale down
				 ObjectAnimator scaleX = ObjectAnimator.ofFloat(imageViewLayout,"scaleX",1f);
				 ObjectAnimator scaleY = ObjectAnimator.ofFloat(imageViewLayout,"scaleY",1f);
				 scaleX.setDuration(700);
				 scaleY.setDuration(700);
				 
				 //Alpha animation on imageLogo
				 ObjectAnimator alphaAnimation  =  ObjectAnimator.ofFloat(logo, View.ALPHA,1,0);
				 alphaAnimation.setDuration(700);
				 
				 AnimatorSet bringBackToTop =  new AnimatorSet();
				 bringBackToTop.setDuration(500);
				 bringBackToTop.playTogether(scaleXUp,scaleYUp);
				 bringBackToTop.play(translateX).with(translateY).with(scaleX).with(scaleY).with(alphaAnimation).after(scaleXUp);
				 bringBackToTop.addListener(new AnimatorListener()
					{
						
						@Override
						public void onAnimationStart(Animator animation)
						{
							
		                }
						
						@Override
						public void onAnimationRepeat(Animator animation)
						{
							
						}
						
						@Override
						public void onAnimationEnd(Animator animation)
						{
							((ChatThreadActivity)getActivity()).updateActionBarColor(new ColorDrawable(Color.BLACK));
						    closeFragment();
							
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
	
	public void showRetryButton()
	{
		retryButton.setVisibility(View.VISIBLE);
	}
	
	public void showRetryIcon()
	{
		logo.setImageDrawable(getResources().getDrawable(R.drawable.cross_retry));
	}
	
	public void cancelRotationAnimation()
	{
		if(rotateAnimation!=null)
			rotateAnimation.cancel();
	}
	
	public void removePostedMessages()
	{
		uiHandler.removeMessages(UPDATE_ANIMATION_MESSAGE);
		uiHandler.removeMessages(START_TIMER);
	}
	
	public void scaleUpAvatar(float xDest,float yDest)
	{
		ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleX", xDest);
		ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleY", yDest);
		AnimatorSet anim =  new AnimatorSet();
		anim.setDuration(500);
		anim.playTogether(scaleXUp,scaleYUp);
		anim.start();
	}
 
	private void hideAndStopTimer()
	{
		timerText.setVisibility(View.GONE);
		if(timer!=null)
			timer.cancel();
	}
	
	protected void hideTimer()
	{
		timerText.setVisibility(View.GONE);
	}
	
	public AnimatorSet getScaleDownAvatarAnimator(float xDest,float yDest)
	{
		ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(imageViewLayout, "scaleX", xDest);
		ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(imageViewLayout, "scaleY", yDest);
		AnimatorSet anim =  new AnimatorSet();
		anim.setDuration(500);
		anim.playTogether(scaleXDown,scaleYDown);
		return anim;
	}
	
	public void hideRetryButton()
	{
		retryButton.setVisibility(View.GONE);
	}

	@Override
	public void onConnectionRequest(Boolean startAnimation)
	{
		listener.onConnectionRequest(startAnimation);
	}
	

	@Override
	public void onDisconnectionRequest()
	{
		listener.onDisconnectionRequest();
		closeFragment();
	}

	@Override
	public void removeDisconnectFragment()
	{
		Fragment fragment = getChildFragmentManager().findFragmentByTag(OfflineConstants.OFFLINE_DISCONNECT_FRAGMENT);
		if(fragment != null)
		    getChildFragmentManager().beginTransaction().remove(fragment).commit();	
	}
	
	
	
}
