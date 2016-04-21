package com.bsb.hike.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;

import com.bsb.hike.R;
import com.bsb.hike.view.CustomFontTextView;

/**
 * This adapter inflates data on the dialog containing mute duration list.
 *
 * Created by anubansal on 21/04/16.
 */
public class MuteDurationListAdapter extends ArrayAdapter<String>
{

    private final Context context;

    private final String[] values;

    private int selectedPosition = 0;

    public MuteDurationListAdapter(Context context, String[] values)
    {
        super(context, -1, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.mute_duration_item, parent, false);

        CustomFontTextView textView = (CustomFontTextView) rowView.findViewById(R.id.mute_duration_text);
        textView.setText(values[position]);

        RadioButton btn = (RadioButton) rowView.findViewById(R.id.mute_duration_btn);
        btn.setChecked(position == selectedPosition);
        btn.setTag(position);
        btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                selectedPosition = (Integer) v.getTag();
                notifyDataSetChanged();
            }
        });

        return rowView;
    }

}
