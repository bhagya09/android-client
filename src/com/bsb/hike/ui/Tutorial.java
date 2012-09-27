package com.bsb.hike.ui;

import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.DrawerBaseActivity;
import com.bsb.hike.utils.Utils;

public class Tutorial extends DrawerBaseActivity implements OnClickListener
{
	private ViewPager tutorialPager;
	private ImageView[] pageIndicators;
	private ViewGroup pageIndicatorContainer;

	private boolean isHelpPage;
	private boolean isLandscape;
	private TextView titleBtn;

	private static final int PAGE_NUM_HELP = 3;
	private static final int PAGE_NUM_INTRO = 3;

	private int pageNum = 0;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signup_tutorial_base);

		isHelpPage = getIntent().getBooleanExtra(HikeConstants.Extras.HELP_PAGE, false);
		isLandscape = getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT;

		if(!isHelpPage)
		{
			titleBtn = (Button) findViewById(R.id.title_icon);
			titleBtn.setText(R.string.done);
			titleBtn.setEnabled(false);
			titleBtn.setVisibility(View.VISIBLE);
			findViewById(R.id.button_bar_2).setVisibility(View.VISIBLE);

			pageNum = PAGE_NUM_INTRO;
		}
		else
		{
			afterSetContentView(savedInstanceState);

			pageNum = PAGE_NUM_HELP;
		}

		TextView mTitleView = (TextView) findViewById(isHelpPage ? R.id.title_centered : R.id.title);
		mTitleView.setText(isHelpPage ? R.string.help : R.string.meet_hike);

		tutorialPager = (ViewPager) findViewById(R.id.signup_tutorial_pager);
		pageIndicatorContainer = (ViewGroup) findViewById(R.id.page_indicator_container);

		int rightMargin = (int) (10 * Utils.densityMultiplier);
		pageIndicators = new ImageView[pageNum];
		for(int i = 0; i<pageNum; i++)
		{
			pageIndicators[i] = new ImageView(this);
			LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			if(i != pageNum - 1)
			{
				lp.setMargins(0, 0, rightMargin, 0);
			}
			pageIndicators[i].setImageResource(i == 0 ?
					R.drawable.page_indicator_selected : R.drawable.page_indicator_unselected);
			pageIndicators[i].setLayoutParams(lp);
			pageIndicatorContainer.addView(pageIndicators[i]);
		}
		pageIndicatorContainer.requestLayout();

		tutorialPager.setAdapter(new TutorialPagerAdapter());

		tutorialPager.setOnPageChangeListener(new OnPageChangeListener()
		{
			@Override
			public void onPageSelected(int position) 
			{
				for(ImageView pageIndicator : pageIndicators)
				{
					pageIndicator.setImageResource(R.drawable.page_indicator_unselected);
				}
				pageIndicators[position].setImageResource(R.drawable.page_indicator_selected);
				if(!isHelpPage)
				{
					titleBtn.setEnabled(position == pageNum - 1 ? true : false);
				}
			}
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}
			@Override
			public void onPageScrollStateChanged(int arg0) {}
		});
	}

	public void onBackPressed()
	{
		if(isHelpPage)
		{
			super.onBackPressed();
		}
		else
		{
			finish();
		}
	}

	public void onTitleIconClick(View v)
	{
		Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
		editor.putBoolean(HikeMessengerApp.SHOWN_TUTORIAL, true);
		editor.commit();

		Intent i = new Intent(Tutorial.this, MessagesList.class);
		i.putExtra(HikeConstants.Extras.FIRST_TIME_USER, true);
		startActivity(i);
		finish();
	}

	private class TutorialPagerAdapter extends PagerAdapter
	{
		private LayoutInflater inflater;

		public TutorialPagerAdapter() 
		{
			inflater = (LayoutInflater) LayoutInflater.from(Tutorial.this);
		}

		@Override
		public int getCount() 
		{
			return pageNum;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) 
		{
			return view == object;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) 
		{
			((ViewPager) container).removeView((View) object);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) 
		{
			ViewGroup tutorialPage = (ViewGroup) inflater.inflate(R.layout.signup_tutorial_page, null);
			ImageView mainImg = (ImageView) tutorialPage.findViewById(R.id.img);
			ImageView header = (ImageView) tutorialPage.findViewById(R.id.heading);
			TextView info = (TextView) tutorialPage.findViewById(R.id.info);
			ViewGroup rewardsInfo = (ViewGroup) tutorialPage.findViewById(R.id.rewards_info);
			ViewGroup helpButtonsContainer = (ViewGroup) tutorialPage.findViewById(R.id.help_button_container);
			TextView disclaimer = (TextView) tutorialPage.findViewById(R.id.india_only);
			ImageButton btnContact = (ImageButton) tutorialPage.findViewById(R.id.btn_contact);
			ImageButton btnFaq = (ImageButton) tutorialPage.findViewById(R.id.btn_faq);
			ViewGroup imgLayout = (ViewGroup) tutorialPage.findViewById(R.id.img_layout);

			btnContact.setOnClickListener(Tutorial.this);
			btnFaq.setOnClickListener(Tutorial.this);

			switch (position) 
			{
			case 0:
				mainImg.setImageResource(isHelpPage ? R.drawable.ic_hike_phone : R.drawable.hike_to_hike_img);
				header.setImageResource(isHelpPage ? 0 : R.drawable.hike_to_hike_txt);
				header.setVisibility(isHelpPage ? View.GONE : View.VISIBLE);
				if(isHelpPage)
				{
					tutorialPage.findViewById(R.id.help_info).setVisibility(View.VISIBLE);
					info.setVisibility(View.GONE);
					mainImg.setScaleType(ScaleType.CENTER);
				}
				else
				{
					info.setText(R.string.hike_to_hike_free_always);
				}
				rewardsInfo.setVisibility(View.GONE);
				helpButtonsContainer.setVisibility(isHelpPage ? View.VISIBLE : View.GONE);
				disclaimer.setVisibility(isHelpPage ? View.GONE : View.VISIBLE);
				break;
			case 1:
				mainImg.setImageResource(isHelpPage ? R.drawable.hike_to_hike_img : R.drawable.hike_to_sms_img);
				header.setImageResource(isHelpPage ? R.drawable.hike_to_hike_txt : R.drawable.hike_to_sms_txt);
				header.setVisibility(View.VISIBLE);
				info.setText(isHelpPage ? R.string.hike_to_hike_free_always : R.string.hike_to_sms);
				rewardsInfo.setVisibility(View.GONE);
				helpButtonsContainer.setVisibility(View.GONE);
				disclaimer.setText(isHelpPage ? "" : getString(R.string.hike_to_sms_disclaimer));
				disclaimer.setVisibility(View.VISIBLE);
				break;
			case 2:
				if(Integer.valueOf((int) (10*Utils.densityMultiplier)) < Integer.valueOf((int) (0.9f * 10)))
				{
					if(!isHelpPage)
					{
						imgLayout.setVisibility(View.GONE);
						LayoutParams rewardsInfoLP = (LayoutParams) rewardsInfo.getLayoutParams();
						rewardsInfoLP.weight = 2;
						rewardsInfoLP.gravity = Gravity.TOP|Gravity.CENTER_HORIZONTAL;
						rewardsInfo.setLayoutParams(rewardsInfoLP);

						LayoutParams headerLP = (LayoutParams) header.getLayoutParams();
						headerLP.weight = 1.5f;
						rewardsInfo.setLayoutParams(headerLP);
					}
					else
					{
						LayoutParams imgLayoutLP = (LayoutParams) imgLayout.getLayoutParams();
						imgLayoutLP.weight--;
						imgLayout.setLayoutParams(imgLayoutLP);
					}
				}
				mainImg.setImageResource(isHelpPage ? R.drawable.hike_to_sms_img : R.drawable.rewards_img);
				header.setImageResource(isHelpPage ? R.drawable.hike_to_sms_txt : R.drawable.rewards_txt);

				header.setVisibility(View.VISIBLE);
				info.setText(isHelpPage ? R.string.hike_to_sms : R.string.rewards_intro);
				rewardsInfo.setVisibility(isHelpPage ? View.GONE : View.VISIBLE);
				helpButtonsContainer.setVisibility(View.GONE);
				disclaimer.setVisibility(isHelpPage ? View.VISIBLE : View.GONE);
				disclaimer.setText(isHelpPage ?getString(R.string.hike_to_sms_disclaimer) : "");
				break;
			}
			if(isLandscape)
			{
				imgLayout.setVisibility(View.GONE);
			}
			if(Integer.valueOf((int) (10*Utils.densityMultiplier)) < Integer.valueOf((int) (0.9f * 10)))
			{
				mainImg.setScaleType(ScaleType.CENTER);
			}

			((ViewPager) container).addView(tutorialPage);
			return tutorialPage;
		}

	}

	@Override
	public void onClick(View v) 
	{
		Intent intent = null;
		if(v.getId() == R.id.btn_faq)
		{
			Utils.logEvent(this, HikeConstants.LogEvent.HELP_FAQ);
			intent = new Intent(this, WebViewActivity.class);
			intent.putExtra(HikeConstants.Extras.URL_TO_LOAD, HikeConstants.HELP_URL);
			intent.putExtra(HikeConstants.Extras.TITLE, "FAQs");
		}
		else if(v.getId() == R.id.btn_contact)
		{
			Utils.logEvent(this, HikeConstants.LogEvent.HELP_CONTACT);
			intent = new Intent(Intent.ACTION_SENDTO);
			intent.setData(Uri.parse("mailto:" + HikeConstants.MAIL));
		}
		if(intent != null)
		{
			startActivity(intent);
		}
	}
}
