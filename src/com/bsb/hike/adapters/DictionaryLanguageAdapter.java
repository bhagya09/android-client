package com.bsb.hike.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.modules.kpt.KptKeyboardManager;
import com.bsb.hike.modules.kpt.KptKeyboardManager.LanguageDictionarySatus;
import com.kpt.adaptxt.beta.KPTAddonItem;

import java.util.ArrayList;

/**
 * Created by gauravmittal on 19/10/15.
 */
public class DictionaryLanguageAdapter extends ArrayAdapter<KPTAddonItem> {

    Context mContext;
    LayoutInflater inflater;

    class ViewHolder
    {
        TextView dictionaryLanguageName;
        CheckBox dictionayStatus;
        ProgressBar progressBar;
    }

    public DictionaryLanguageAdapter(Context context, int resource, ArrayList<KPTAddonItem> addonItems) {
        super(context, resource, addonItems);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
        if (convertView == null)
        {
            convertView = inflater.inflate(R.layout.kpt_dictionary_language_list_item, null);
            viewHolder = new ViewHolder();
            viewHolder.dictionaryLanguageName = (TextView) convertView.findViewById(R.id.dictionary_language_name);
            viewHolder.dictionayStatus = (CheckBox) convertView.findViewById(R.id.checkbox_status);
            viewHolder.progressBar = (ProgressBar) convertView.findViewById(R.id.progress_bar);
            convertView.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        KPTAddonItem item = getItem(position);

        viewHolder.dictionaryLanguageName.setText(item.getDisplayName());
        LanguageDictionarySatus status = KptKeyboardManager.getInstance(mContext).getDictionaryLanguageStatus(item);
        if (status == LanguageDictionarySatus.PROCESSING || status == LanguageDictionarySatus.IN_QUEUE)
        {
            viewHolder.progressBar.setVisibility(View.VISIBLE);
            viewHolder.dictionayStatus.setVisibility(View.GONE);
        }
        else
        {
            viewHolder.progressBar.setVisibility(View.GONE);
            viewHolder.dictionayStatus.setVisibility(View.VISIBLE);
            if (status == LanguageDictionarySatus.INSTALLED_LOADED)
            {
                viewHolder.dictionayStatus.setChecked(true);
            }
            else
            {
                viewHolder.dictionayStatus.setChecked(false);
            }
        }
        viewHolder.dictionayStatus.setClickable(false);
        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return -1;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }
}
