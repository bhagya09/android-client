/**
 * 
 */
package com.bsb.hike.dialog;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.R;

import java.util.List;

/**
 * Extends {@link CustomAlertDialog} <br>
 * You can add your radio buttons to the layout. <br>
 * The basic structure of this popup is : Header Text, RadioButton(s) in body and Buttons at the end.<br>
 * Applying some methods of {@link CustomAlertDialog} like setMessage will throw an exception here. Use with caution
 * 
 * @author piyush
 * 
 */
public class CustomAlertRadioButtonDialog extends CustomAlertDialog implements OnItemClickListener
{

	protected List<RadioButtonPojo> radioButtonPojoList;

	protected RadioButtonItemCheckedListener mListener;

	public RadioButtonPojo selectedRadioGroup;
	
	ArrayAdapter<RadioButtonPojo> mAdapter;

	/**
	 * @param context
	 * @param dialogId
	 */
	public CustomAlertRadioButtonDialog(Context context, int dialogId, List<RadioButtonPojo> radioButtonPojo, RadioButtonItemCheckedListener listener)
	{
		super(context, dialogId, R.layout.custom_popup_radio_btn);
		this.radioButtonPojoList = radioButtonPojo;
		this.mListener = listener;
		initRadioGroup();
	}

	protected void initRadioGroup()
	{
		ListView checkBoxList = (ListView) findViewById(R.id.checkBoxContainer);

		final LayoutInflater mInflater = getLayoutInflater();

		mAdapter = new ArrayAdapter<RadioButtonPojo>(mContext, R.layout.custom_radio_btn, R.id.header, radioButtonPojoList)
		{

			@Override
			public RadioButtonPojo getItem(int position)
			{
				return radioButtonPojoList.get(position);
			}
			
			@Override
			public boolean isEnabled(int position)
			{
				return getItem(position).enabled;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				RadioButtonPojo radioBtn = getItem(position);

				if (convertView == null)
				{
					convertView = mInflater.inflate(R.layout.custom_radio_btn, null);
					convertView.setTag(radioBtn);
				}

				CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox1);
				TextView messageHeadingTv = (TextView) convertView.findViewById(R.id.header);
				TextView messageTextTv = (TextView) convertView.findViewById(R.id.headerSubText);
				TextView subTextTv = (TextView) convertView.findViewById(R.id.subtext);

				checkBox.setChecked(radioBtn.isChecked);

				if (radioBtn.isChecked)
				{
					selectedRadioGroup = radioBtn;
				}

				messageHeadingTv.setText(radioBtn.messageHeading);

				if (TextUtils.isEmpty(radioBtn.subText))
				{
					subTextTv.setVisibility(View.GONE);
				}

				else
				{
					subTextTv.setVisibility(View.VISIBLE);
					subTextTv.setText(radioBtn.subText);
				}

				if (TextUtils.isEmpty(radioBtn.messageText))
				{
					messageTextTv.setVisibility(View.GONE);
				}

				else
				{
					messageTextTv.setVisibility(View.VISIBLE);
					messageTextTv.setText(radioBtn.messageText);
				}
				
				ViewCompat.setAlpha(convertView, radioBtn.enabled ? 1.0f : 0.24f);
				
				return convertView;
			}
		};

		checkBoxList.setAdapter(mAdapter);
		checkBoxList.setOnItemClickListener(this);

	}

	public static class RadioButtonPojo
	{
		int id;

		boolean isChecked;

		String messageText;

		String messageHeading;

		String subText;
		
		boolean enabled = true;

		public RadioButtonPojo(int id, boolean isChecked, String messageText, String messageHeading, String subText)
		{
			this.id = id;

			this.isChecked = isChecked;

			this.messageText = messageText;

			this.messageHeading = messageHeading;

			this.subText = subText;

			validateMessageText();
		}

		private void validateMessageText()
		{
			if (TextUtils.isEmpty(messageHeading))
			{
				throw new IllegalArgumentException("You need to pass in a message heading in RadioButton Pojo");
			}
		}

		public void setChecked(boolean isChecked)
		{
			this.isChecked = isChecked;
		}
	}

	public static interface RadioButtonItemCheckedListener
	{
		public void onRadioButtonItemClicked(RadioButtonPojo whichItem, CustomAlertRadioButtonDialog dialog);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		RadioButtonPojo mPojo = (RadioButtonPojo) view.getTag();

		if (!mPojo.isChecked)
		{
			for (RadioButtonPojo pojo : radioButtonPojoList)
			{
				if (mPojo.id != pojo.id)
				{
					pojo.isChecked = false;
				}
			}

			mPojo.isChecked = true;

			((CheckBox) view.findViewById(R.id.checkbox1)).setChecked(mPojo.isChecked);

			selectedRadioGroup = mPojo;

			((ArrayAdapter<RadioButtonPojo>) parent.getAdapter()).notifyDataSetChanged();

		}
		if (mListener != null && mPojo.isChecked)
		{
			mListener.onRadioButtonItemClicked(mPojo, CustomAlertRadioButtonDialog.this);
		}
	}

	@Override
	public void setMessage(int resId)
	{
		throw new IllegalArgumentException("Cannot show body and radio buttons together in this popup");
	}

	@Override
	public void setMessage(CharSequence messageText)
	{
		throw new IllegalArgumentException("Cannot show body and radio buttons together in this popup");
	}

	public int getCheckedRadioButtonId()
	{
		if (selectedRadioGroup != null)
			return selectedRadioGroup.id;
		else
			return -1;
	}

}
