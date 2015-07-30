/**
 * 
 */
package com.bsb.hike.dialog;

import java.util.List;

import android.content.Context;
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

	private List<RadioButtonPojo> radioButtonPojoList;

	private RadioButtonItemCheckedListener mListener;

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

	private void initRadioGroup()
	{
		ListView checkBoxList = (ListView) findViewById(R.id.checkBoxContainer);

		final LayoutInflater mInflater = getLayoutInflater();

		ArrayAdapter<RadioButtonPojo> mAdapter = new ArrayAdapter<RadioButtonPojo>(mContext, R.layout.custom_popup_radio_btn, R.id.header, radioButtonPojoList)
		{

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = mInflater.inflate(R.layout.custom_popup_radio_btn, null);
				}

				CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);
				TextView headerText = (TextView) convertView.findViewById(R.id.header);
				TextView headerMessageText = (TextView) convertView.findViewById(R.id.headerSubText);
				TextView subTextView = (TextView) convertView.findViewById(R.id.subtext);

				RadioButtonPojo radioBtn = getItem(position);

				checkBox.setChecked(radioBtn.isChecked);

				headerText.setText(radioBtn.messageHeading);

				if (TextUtils.isEmpty(radioBtn.subText))
				{
					subTextView.setVisibility(View.GONE);
				}

				else
				{
					subTextView.setVisibility(View.VISIBLE);
					subTextView.setText(radioBtn.subText);
				}

				if (TextUtils.isEmpty(radioBtn.messageText))
				{
					headerMessageText.setVisibility(View.GONE);
				}

				else
				{
					headerMessageText.setVisibility(View.VISIBLE);
					headerMessageText.setText(radioBtn.messageText);
				}

				convertView.setTag(radioBtn);

				return convertView;
			}
		};

		checkBoxList.setAdapter(mAdapter);
		checkBoxList.setOnItemClickListener(this);

	}

	public static class RadioButtonPojo
	{
		private int id;

		boolean isChecked;

		String messageText;

		String messageHeading;

		String subText;

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
	}

	public static interface RadioButtonItemCheckedListener
	{
		public void onRadioButtonItemClicked(RadioButtonPojo whichItem);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);

		checkBox.setChecked(!checkBox.isChecked());

		RadioButtonPojo mPojo = (RadioButtonPojo) view.getTag();

		mPojo.isChecked = checkBox.isChecked();

		if (mListener != null)
		{
			mListener.onRadioButtonItemClicked(mPojo);
		}

	}

	@Override
	public void setMessage(int resId)
	{
		throw new IllegalArgumentException("Cannot show body and radio buttons together in this popup");
	}

	@Override
	public void setMessage(String messageText)
	{
		throw new IllegalArgumentException("Cannot show body and radio buttons together in this popup");
	}

}
