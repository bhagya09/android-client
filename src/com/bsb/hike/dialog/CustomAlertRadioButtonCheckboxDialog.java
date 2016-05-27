package com.bsb.hike.dialog;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.R;

import java.util.List;

/**
 *  Extends {@link CustomAlertRadioButtonDialog} <br>
 * You can add your radio buttons to the layout. <br>
 * The basic structure of this popup is : Header Text, RadioButton(s) in body, one checkbox and Buttons at the end.<br>
 *
 * Created by anubansal on 16/05/16.
 */
public class CustomAlertRadioButtonCheckboxDialog extends CustomAlertRadioButtonDialog
{
    protected CheckBoxPojo checkBoxPojo;

    protected CheckBoxListener checkBoxListener;

    public CustomAlertRadioButtonCheckboxDialog(Context context, int dialogId, List<RadioButtonPojo> radioButtonPojo, RadioButtonItemCheckedListener radioButtonItemCheckedListener, CheckBoxPojo checkBoxPojo, CheckBoxListener checkBoxListener)
    {
        super(context, dialogId, radioButtonPojo, radioButtonItemCheckedListener);
        this.checkBoxPojo = checkBoxPojo;
        this.checkBoxListener = checkBoxListener;
        adjustTitlebarPadding();
        initCheckBox();
    }

    private void adjustTitlebarPadding()
    {
        LinearLayout titlebar = (LinearLayout) findViewById(R.id.title_template);
        titlebar.setPadding(titlebar.getPaddingLeft(), titlebar.getPaddingTop(), titlebar.getPaddingRight(), 15);
    }

    private void initCheckBox()
    {
        LinearLayout checkboxContainer = (LinearLayout) findViewById(R.id.checkbox_container);
        checkboxContainer.setVisibility(View.VISIBLE);
        View divider = checkboxContainer.findViewById(R.id.divider);
        divider.setVisibility(View.VISIBLE);
        LinearLayout checkboxView = (LinearLayout) checkboxContainer.findViewById(R.id.checkbox_panel);
        checkboxView.setVisibility(View.VISIBLE);
        final CheckBox checkBox = (CheckBox) checkboxView.findViewById(R.id.checkbox);
        checkBox.setVisibility(View.VISIBLE);
        TextView messageTextView = (TextView) checkboxView.findViewById(R.id.checkbox_text);
        messageTextView.setVisibility(View.VISIBLE);
        messageTextView.setText(checkBoxPojo.messageText);
        checkBox.setChecked(checkBoxPojo.isChecked);

        ViewCompat.setAlpha(messageTextView, checkBox.isEnabled() ? 1.0f : 0.24f);

        checkboxView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                checkBoxPojo.isChecked = !(checkBoxPojo.isChecked);
                checkBox.setChecked(checkBoxPojo.isChecked);

                if (checkBoxListener != null)
                {
                    checkBoxListener.onCheckboxClicked(checkBoxPojo, CustomAlertRadioButtonCheckboxDialog.this);
                }
            }
        });
    }

    public static class CheckBoxPojo
    {
        int id;

        boolean isChecked;

        String messageText;

        boolean enabled = true;

        public CheckBoxPojo(int id, boolean isChecked, String messageText)
        {
            this.id = id;

            this.isChecked = isChecked;

            this.messageText = messageText;

            validateMessageText();
        }

        private void validateMessageText()
        {
            if (TextUtils.isEmpty(messageText))
            {
                throw new IllegalArgumentException("You need to pass in a message heading in CheckBox Pojo");
            }
        }

        public void setChecked(boolean isChecked)
        {
            this.isChecked = isChecked;
        }
    }

    public interface CheckBoxListener
    {
        void onCheckboxClicked(CheckBoxPojo whichItem, CustomAlertRadioButtonDialog dialog);
    }

    protected void initRadioGroup()
    {
        ListView checkBoxList = (ListView) findViewById(R.id.checkBoxContainer);

        final LayoutInflater mInflater = getLayoutInflater();

        mAdapter = new ArrayAdapter<RadioButtonPojo>(mContext, R.layout.custom_radio_btn_2, R.id.header, radioButtonPojoList)
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
                    convertView = mInflater.inflate(R.layout.custom_radio_btn_2, null);
                    convertView.setTag(radioBtn);
                }

                CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox1);
                TextView messageHeadingTv = (TextView) convertView.findViewById(R.id.header);

                checkBox.setChecked(radioBtn.isChecked);

                if (radioBtn.isChecked)
                {
                    selectedRadioGroup = radioBtn;
                }

                messageHeadingTv.setText(radioBtn.messageHeading);

                ViewCompat.setAlpha(convertView, radioBtn.enabled ? 1.0f : 0.24f);

                return convertView;
            }
        };

        checkBoxList.setAdapter(mAdapter);
        checkBoxList.setOnItemClickListener(this);

    }
}
