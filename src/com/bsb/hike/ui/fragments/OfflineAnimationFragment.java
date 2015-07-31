package com.bsb.hike.ui.fragments;

import java.util.Map;


import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.IOfflineCallbacks;
import com.bsb.hike.offline.OfflineConstants.ERRORCODE;
import com.bsb.hike.ui.fragments.OfflineDisconnectFragment.OfflineConnectionRequestListener;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class OfflineAnimationFragment extends DialogFragment implements IOfflineCallbacks
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
				timerText.setVisibility(View.GONE);
				retryButton.setVisibility(View.VISIBLE);
				retryButton.setOnClickListener(new OnClickListener()
				{
					
					@Override
					public void onClick(View v)
					{
						retryButton.setVisibility(View.GONE);
						ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(imageViewLayout, "scaleX", 2.6f);
						ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(imageViewLayout, "scaleY", 2.6f);
						AnimatorSet anim =  new AnimatorSet();
						anim.setDuration(500);
						anim.playTogether(scaleXDown,scaleYDown);
						anim.start();
						anim.addListener(new AnimatorListener()
						{
							
							@Override
							public void onAnimationStart(Animator animation)
							{
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void onAnimationRepeat(Animator animation)
							{
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void onAnimationEnd(Animator animation)
							{
								listener.onConnectionRequest(false);
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
					}
				});
				connectionInfo.setText("Connection was not \n established. Try again?");
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
	    ObjectAnimator translateX = ObjectAnimator.ofFloat(imageViewLayout, "translationX",xd);
	    ObjectAnimator translateY = ObjectAnimator.ofFloat(imageViewLayout, "translationY",yd);
	    ObjectAnimator scaleX = ObjectAnimator.ofFloat(imageViewLayout,"scaleX",2.6f);
	    ObjectAnimator scaleY = ObjectAnimator.ofFloat(imageViewLayout,"scaleY",2.6f);
	    translateX.setDuration(600);
	    translateY.setDuration(600);
	    scaleX.setDuration(600);
	    scaleY.setDuration(600);
	    ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(imageViewLayout, "scaleX", 2);
	    ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(imageViewLayout, "scaleY", 2);
	    scaleXDown.setDuration(200);
	    scaleYDown.setDuration(200);
		ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleX", 2.6f);
		ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleY", 2.6f);
		scaleXUp.setDuration(200);
		scaleYUp.setDuration(200);
	    
	    ObjectAnimator alphaAnimation  =  ObjectAnimator.ofFloat(frame, View.ALPHA,0.0f,1.0f);
		alphaAnimation.setDuration(200);
		//alphaAnimation.setInterpolator(new AccelerateInterpolator());
	    ObjectAnimator scaleXUpHolo = ObjectAnimator.ofFloat(frame, "scaleX", 1.1f);
		ObjectAnimator scaleYUpHolo = ObjectAnimator.ofFloat(frame, "scaleY", 1.1f);
		ObjectAnimator scaleXDownHolo = ObjectAnimator.ofFloat(frame, "scaleX", 1f);
		ObjectAnimator scaleYDownHolo = ObjectAnimator.ofFloat(frame, "scaleY", 1f);
		scaleXDownHolo.setDuration(400);
		scaleYDownHolo.setDuration(400);
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
		AnimatorSet anim =  new AnimatorSet();
	    anim.playTogether(translateX,translateY,scaleX,scaleY);
	    anim.play(scaleXDown).with(scaleYDown).after(translateX);
	    anim.play(scaleXUp).with(scaleYUp).after(scaleXDown);
		anim.play(scaleXUpHolo).with(scaleYUpHolo).with(alphaAnimation).after(1000);
		anim.play(scaleYDownHolo).with(scaleXDownHolo).after(scaleXUpHolo);
	    anim.addListener(new AnimatorListener()
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
				//frame.setVisibility(View.VISIBLE);
				startRotateAnimation();
			}

			@Override
			public void onAnimationCancel(Animator animation)
			{
				
			}
		});
	    anim.start();
	}
		
	private void startframeAnimation()
	{
		ObjectAnimator alphaAnimation  =  ObjectAnimator.ofFloat(frame, View.ALPHA,0.0f,1.0f);
		alphaAnimation.setDuration(300);
		alphaAnimation.setInterpolator(new AccelerateInterpolator());
		alphaAnimation.addListener(new AnimatorListener()
		{
			
			@Override
			public void onAnimationStart(Animator animation)
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationRepeat(Animator animation)
			{
				// TODO Auto-generated method stub
				
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
		
		/*AnimatorSet anim =  new AnimatorSet();
		anim.playTogether(scaleXUp,scaleYUp);
		anim.play(scaleYDown).with(scaleXDown).after(scaleXUp);
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
				startRotateAnimation();
			}
			
			@Override
			public void onAnimationCancel(Animator animation)
			{
				
			}
		});
		anim.start();*/
		
	}
	
	protected void startRotateAnimation()
	{
		ImageView progressBead = (ImageView)fragmentView.findViewById(R.id.bead);
		progressBead.setVisibility(View.VISIBLE);
		rotateAnimation = new RotateAnimation(0, 360,Animation.RELATIVE_TO_SELF,0.3f,Animation.RELATIVE_TO_SELF,2.8f);
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
				closeFragment();
			}
		});
		Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(msisdn);
		if (drawable == null)
		{
			drawable = HikeMessengerApp.getLruCache().getDefaultAvatar(msisdn, true);
		}
		avatarImageView.setScaleType(ScaleType.FIT_CENTER);
		avatarImageView.setImageDrawable(drawable);
		
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
					logo.setImageDrawable(getResources().getDrawable(R.drawable.cross_retry));
					if(rotateAnimation!=null)
						rotateAnimation.cancel();
					frame.setVisibility(View.INVISIBLE);
					ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleX", 3.5f);
					ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleY", 3.5f);
					AnimatorSet anim =  new AnimatorSet();
					anim.setDuration(500);
					anim.playTogether(scaleXUp,scaleYUp);
					anim.start();
				}
			});
	}
	@Override
	public void connectedToMsisdn(String connectedDevice)
	{
		uiHandler.removeMessages(UPDATE_ANIMATION_MESSAGE);
		uiHandler.removeMessages(START_TIMER);
	    fragmentView.post(new Runnable()
		{
			
			@Override
			public void run()
			{
				
				 connectionInfo.setText(getResources().getString(R.string.connection_established));
				 
				 if(rotateAnimation!=null)
					 rotateAnimation.cancel();
				 frame.setVisibility(View.INVISIBLE);
				 if(timer!=null)
				 {
					 timer.cancel();
					 timerText.setVisibility(View.GONE);
				 }
				 ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleX", 3.5f);
				 ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(imageViewLayout, "scaleY", 3.5f);
				 ObjectAnimator translateX = ObjectAnimator.ofFloat(imageViewLayout, "translationX",originalPos[0] - (imageViewLayout.getWidth()*3)/4);
				 ObjectAnimator translateY = ObjectAnimator.ofFloat(imageViewLayout, "translationY",originalPos[1] - (imageViewLayout.getHeight()*3)/4);
				 ObjectAnimator scaleX = ObjectAnimator.ofFloat(imageViewLayout,"scaleX",1f);
				 ObjectAnimator scaleY = ObjectAnimator.ofFloat(imageViewLayout,"scaleY",1f);
				 translateX.setDuration(700);
				 translateY.setDuration(700);
				 scaleX.setDuration(700);
				 scaleY.setDuration(700);
				 ObjectAnimator alphaAnimation  =  ObjectAnimator.ofFloat(logo, View.ALPHA,1,0);
				 alphaAnimation.setDuration(700);
				 AnimatorSet anim =  new AnimatorSet();
				 anim.setDuration(500);
				 anim.playTogether(scaleXUp,scaleYUp);
				 anim.play(translateX).with(translateY).with(scaleX).with(scaleY).with(alphaAnimation).after(scaleXUp);
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
							((ChatThreadActivity)getActivity()).updateActionBarColor(new ColorDrawable(Color.BLACK));
						    closeFragment();
							
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
	
	private void closeFragment()
	{
		Utils.unblockOrientationChange((ChatThreadActivity)getActivity());
		dismissAllowingStateLoss();
	}
	
	public void setConnectionListner(OfflineConnectionRequestListener  listener)
	{
		this.listener =listener;
	}
	
}
