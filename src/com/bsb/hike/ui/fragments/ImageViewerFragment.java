package com.bsb.hike.ui.fragments;

import android.animation.TimeInterpolator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.ColorMatrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.SettingsActivity;
import com.bsb.hike.utils.ProfileImageLoader;
import com.bsb.hike.utils.Utils;

public class ImageViewerFragment extends SherlockFragment implements OnClickListener, Listener
{
	ImageView imageView;

	private ProgressDialog mDialog;

	private String mappedId;

	private String key;

	private boolean isStatusImage;

	private int imageSize;
	
	public static final int FROM_PROFILE_ACTIVITY = 1;

	public static final int FROM_SETTINGS_ACTIVITY = 2;
	
	private int whichActivity;

	private DisplayPictureEditListener mProfilePhotoEditListener;
	
	private String[] profilePicPubSubListeners = { HikePubSub.ICON_CHANGED};
	
	private boolean isViewEditable = false;

	private View background;

	/**
	 * Default constructor
	 */
	public ImageViewerFragment() 
	{
	}
		
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		HikeMessengerApp.getPubSub().addListeners(this, profilePicPubSubListeners);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.image_viewer, null);
		imageView = (ImageView) parent.findViewById(R.id.image);
		imageView.setOnClickListener(this);
		background = parent.findViewById(R.id.background);
		return parent;
	}

	 int mLeftDelta;
	    int mTopDelta;
	    float mWidthScale;
	    float mHeightScale;
	    
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mappedId = getArguments().getString(HikeConstants.Extras.MAPPED_ID);

		isStatusImage = getArguments().getBoolean(HikeConstants.Extras.IS_STATUS_IMAGE);

		imageSize = this.getActivity().getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);

		String PACKAGE_NAME = "com.bsb.hike";
		final int thumbnailTop = getArguments().getInt(PACKAGE_NAME + ".top");
		final int thumbnailLeft = getArguments().getInt(PACKAGE_NAME + ".left");
		final int thumbnailWidth = getArguments().getInt(PACKAGE_NAME + ".width");
		final int thumbnailHeight = getArguments().getInt(PACKAGE_NAME + ".height");
		final int mOriginalOrientation = getArguments().getInt(PACKAGE_NAME + ".orientation");
		
		ViewTreeObserver observer = imageView.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            
            @Override
            public boolean onPreDraw() {
            	imageView.getViewTreeObserver().removeOnPreDrawListener(this);

                // Figure out where the thumbnail and full size versions are, relative
                // to the screen and each other
                int[] screenLocation = new int[2];
                imageView.getLocationOnScreen(screenLocation);
                mLeftDelta = thumbnailLeft - screenLocation[0];
                mTopDelta = thumbnailTop - screenLocation[1];
                
                // Scale factors to make the large version the same size as the thumbnail
                mWidthScale = (float) thumbnailWidth / imageView.getWidth();
                mHeightScale = (float) thumbnailHeight / imageView.getHeight();

                runEnterAnimation();
                
                return true;
            }
        });
        
		showImage();
	}

	private static final TimeInterpolator sDecelerator = new DecelerateInterpolator();
    private static final TimeInterpolator sAccelerator = new AccelerateInterpolator();
    private static final int ANIM_DURATION = 500;

    private BitmapDrawable mBitmapDrawable;
    private ColorMatrix colorizerMatrix = new ColorMatrix();
    
	public void runEnterAnimation() {
        final long duration = (long) (ANIM_DURATION * 1);
        
        // Set starting values for properties we're going to animate. These
        // values scale and position the full size version down to the thumbnail
        // size/location, from which we'll animate it back up
        imageView.setPivotX(0);
        imageView.setPivotY(0);
        imageView.setScaleX(mWidthScale);
        imageView.setScaleY(mHeightScale);
        imageView.setTranslationX(mLeftDelta);
        imageView.setTranslationY(mTopDelta);
        
        
        // Animate scale and translation to go from thumbnail to full size
        imageView.animate().setDuration(duration).
                scaleX(1).scaleY(1).
                translationX(0).translationY(0).
                setInterpolator(sDecelerator).
                withEndAction(new Runnable() {
                    public void run() {
                        // Animate the description in after the image animation
                        // is done. Slide and fade the text in from underneath
                        // the picture.
                    }
                });
        
//        // Fade in the black background
        ObjectAnimator bgAnim = ObjectAnimator.ofInt(background, "alpha", 0, 255);
        bgAnim.setDuration(duration);
        bgAnim.start();
//        
//        // Animate a color filter to take the image from grayscale to full color.
//        // This happens in parallel with the image scaling and moving into place.
//        ObjectAnimator colorizer = ObjectAnimator.ofFloat(PictureDetailsActivity.this,
//                "saturation", 0, 1);
//        colorizer.setDuration(duration);
//        colorizer.start();

//        // Animate a drop-shadow of the image
//        ObjectAnimator shadowAnim = ObjectAnimator.ofFloat(mShadowLayout, "shadowDepth", 0, 1);
//        shadowAnim.setDuration(duration);
//        shadowAnim.start();
    }
	
	private void showImage() 
	{
		key = mappedId;
		
		if (!isStatusImage)
		{
			int idx = key.lastIndexOf(ProfileActivity.PROFILE_PIC_SUFFIX);
			
			if (idx > 0)
			{
				key = new String(key.substring(0, idx));
			}
		}
		ProfileImageLoader profileImageLoader = new ProfileImageLoader(getActivity(), key, imageView, imageSize, isStatusImage);
		profileImageLoader.setLoaderListener(new ProfileImageLoader.LoaderListener() {

			@Override
			public void onLoaderReset(Loader<Boolean> arg0) {
				dismissProgressDialog();
			}

			@Override
			public void onLoadFinished(Loader<Boolean> arg0, Boolean arg1) {
				dismissProgressDialog();
				if (isStatusImage)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, null);
				}
			}

			@Override
			public Loader<Boolean> onCreateLoader(int arg0, Bundle arg1) {
				mDialog = ProgressDialog.show(getActivity(), null, getResources().getString(R.string.downloading_image));
				mDialog.setCancelable(true);
				return null;
			}
		});
		profileImageLoader.loadProfileImage(getLoaderManager());
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		dismissProgressDialog();
		HikeMessengerApp.getPubSub().removeListeners(this, profilePicPubSubListeners);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		menu.clear();			

		if(isViewEditable)
		{
			inflater.inflate(R.menu.edit_dp, menu);			
		}	
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch(item.getItemId())
		{
			case R.id.edit_dp:
				if(mProfilePhotoEditListener != null)
				{
					mProfilePhotoEditListener.onDisplayPictureEditClicked(whichActivity);
				}
			break;
		}
		return true;
	}

	@Override
	public void onAttach(Activity activity) 
	{
		super.onAttach(activity);
		
		if(activity instanceof SettingsActivity)
		{
			whichActivity = FROM_SETTINGS_ACTIVITY;
		}
		else if(activity instanceof ProfileActivity)
		{
			whichActivity = FROM_PROFILE_ACTIVITY;
		}					

		isViewEditable = getArguments().getBoolean(HikeConstants.CAN_EDIT_DP);

		if(isViewEditable)
		{
			// activity should implement DisplayPictureEditListener interface
			try 
			{
	            mProfilePhotoEditListener = (DisplayPictureEditListener) activity;            
	        }
			catch (ClassCastException e) 
			{
	            throw new ClassCastException(activity.toString() + " must implement DisplayPictureEditListener");
	        }
		}
	}

	private void dismissProgressDialog()
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
	}

	@Override
	public void onClick(View v)
	{
		/*
		 * This object can become null, if the method is called when the fragment is not attached with the activity. In that case we do nothing and return.
		 */
		if (getActivity() == null)
		{
			return;
		}
		getActivity().onBackPressed();
	}
	
	public interface DisplayPictureEditListener
	{
		public void onDisplayPictureEditClicked(int fromWhichActivity);
	}

	@Override
	public void onEventReceived(String type, Object object) 
	{
		/**
		 * If fragment is not added, returning it
		 */
		if (!isAdded())
		{
			return;
		}
		
		if (HikePubSub.ICON_CHANGED.equals(type))
		{
			ContactInfo contactInfo = Utils.getUserContactInfo(getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0));

			if (contactInfo.getMsisdn().equals((String) object))
			{
				getActivity().runOnUiThread(new Runnable()
				{
					
					@Override
					public void run() 
					{
						showImage();						
					}
				});
			}
		}		
	}	
}
