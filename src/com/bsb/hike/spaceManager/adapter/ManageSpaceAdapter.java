package com.bsb.hike.spaceManager.adapter;

import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.spaceManager.items.HeaderItem;
import com.bsb.hike.spaceManager.models.CategoryItem;
import com.bsb.hike.spaceManager.models.SpaceManagerItem;
import com.bsb.hike.spaceManager.models.SubCategoryItem;
import com.bsb.hike.utils.Utils;

public class ManageSpaceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements SwitchCompat.OnCheckedChangeListener
{
    private Context mContext;

    private LayoutInflater mInflater;

    private List<SpaceManagerItem> spaceItemList;

    private boolean areAllItemsSelected;

    public interface IDeleteButtonToggleListener
    {
        void toggleDeleteButton(boolean enable);
    }

    private IDeleteButtonToggleListener toggleListener;

    private Handler uiHandler;

    public ManageSpaceAdapter(Context context, List<SpaceManagerItem> list, IDeleteButtonToggleListener toggleListener)
    {
        this.mContext = context;
        this.spaceItemList = list;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.toggleListener = toggleListener;
        uiHandler = new Handler(Looper.getMainLooper());
    }

    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        TextView header;

        TextView subHeader;

        CheckBox checkBox;

        View separator;

        public ItemViewHolder(View convertView, int viewType)
        {
            super(convertView);

            // Common views
            header = (TextView) convertView.findViewById(R.id.header);
			subHeader = (TextView) convertView.findViewById(R.id.subHeader);

            switch (viewType)
            {
                case SpaceManagerItem.SUBCATEGORY:
                    checkBox = (CheckBox)convertView.findViewById(R.id.subcategory_checkbox);
                    separator = convertView.findViewById(R.id.item_seperator);
                    convertView.setOnClickListener(this);
                    break;
            }
        }

        @Override
        public void onClick(View v)
        {
            CheckBox checkBox = (CheckBox) v.findViewById(R.id.subcategory_checkbox);
            checkBox.setChecked(!checkBox.isChecked());
        }
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        TextView header;

        CheckBox checkBox;

        public HeaderViewHolder(View convertView, int viewType)
        {
            super(convertView);

            switch (viewType)
            {
                case SpaceManagerItem.HEADER:
                    header = (TextView) convertView.findViewById(R.id.header);
                    checkBox = (CheckBox) convertView.findViewById(R.id.select_all_checkbox);
                    convertView.setOnClickListener(this);
                    break;

            }
        }

        @Override
        public void onClick(View v)
        {
            CheckBox checkBox = (CheckBox) v.findViewById(R.id.select_all_checkbox);
            checkBox.setChecked(!checkBox.isChecked());
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View convertView = null;
        switch (viewType)
        {
            case SpaceManagerItem.HEADER:
                convertView = mInflater.inflate(R.layout.space_manager_header_item, parent, false);
                return new HeaderViewHolder(convertView, viewType);

            case SpaceManagerItem.CATEGORY:
                convertView = mInflater.inflate(R.layout.space_manager_category_item, parent, false);
                return new ItemViewHolder(convertView, viewType);

            case SpaceManagerItem.SUBCATEGORY:
                convertView = mInflater.inflate(R.layout.space_manager_subcategory_item, parent, false);
                return new ItemViewHolder(convertView, viewType);

            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position)
    {
        if(viewHolder instanceof HeaderViewHolder)
        {
            HeaderViewHolder holder = (HeaderViewHolder)viewHolder;
            HeaderItem headerItem = (HeaderItem) spaceItemList.get(holder.getAdapterPosition());
            holder.header.setText(headerItem.getHeader());
            holder.checkBox.setTag(headerItem);
            holder.checkBox.setChecked(areAllItemsSelected);
            holder.checkBox.setOnCheckedChangeListener(this);
        }
        else if(viewHolder instanceof ItemViewHolder)
        {
            final ItemViewHolder holder = (ItemViewHolder)viewHolder;
            int viewType = holder.getItemViewType();
            SpaceManagerItem item = spaceItemList.get(viewHolder.getAdapterPosition());

            switch (viewType)
            {
                case SpaceManagerItem.CATEGORY:
                    holder.header.setText(item.getHeader());
                    holder.subHeader.setText(Utils.getSizeForDisplay(((CategoryItem)item).computeSizeToDelete()));
                    break;

                case SpaceManagerItem.SUBCATEGORY:
                    SubCategoryItem subCategoryItem = (SubCategoryItem) item;
                    holder.header.setText(item.getHeader());
                    holder.subHeader.setText(Utils.getSizeForDisplay(item.getSize()));
                    if(holder.getAdapterPosition() == spaceItemList.size() - 1)
                    {
                        holder.separator.setVisibility(View.GONE);
                    }
                    holder.checkBox.setTag(subCategoryItem);
                    holder.checkBox.setChecked(subCategoryItem.isSelected());
                    holder.checkBox.setOnCheckedChangeListener(this);
                    break;

                default:
                    break;
            }
        }
    }

    @Override
    public int getItemCount()
    {
        return spaceItemList.size();
    }

    @Override
    public int getItemViewType(int position)
    {
        return spaceItemList.get(position).getType();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
    {
        SpaceManagerItem item = (SpaceManagerItem) compoundButton.getTag();
        if(item.getType() == SpaceManagerItem.HEADER)
        {
            headerCheckedChanged(checked);
        }
        else if(item.getType() == SpaceManagerItem.SUBCATEGORY)
        {
            SubCategoryItem subCategoryItem = (SubCategoryItem) item;
            subCategoryCheckedChanged(subCategoryItem, checked);
        }
        postNotifyDataSetChanged();
    }

    private void subCategoryCheckedChanged(SubCategoryItem subCategory, boolean isChecked)
    {
        if (isChecked)
        {
            subCategory.setIsSelected(true);
            toggleListener.toggleDeleteButton(true);

            if(allItemsSelected())
            {
                areAllItemsSelected = true;
            }
        }
        else
        {
            subCategory.setIsSelected(false);
            areAllItemsSelected = false;
            if(anySelectedItemPresent())
            {
                toggleListener.toggleDeleteButton(true);
            }
        }
    }

    private void headerCheckedChanged(boolean checked)
    {
        areAllItemsSelected = checked;

        toggleListener.toggleDeleteButton(checked);
        for(int i =0; i< spaceItemList.size(); i++)
        {
            if(spaceItemList.get(i).getType() == SpaceManagerItem.SUBCATEGORY)
            {
                ((SubCategoryItem)spaceItemList.get(i)).setIsSelected(checked);
            }
        }
    }

    private boolean anySelectedItemPresent()
    {
        for(int i =0; i< spaceItemList.size(); i++)
        {
            if(spaceItemList.get(i).getType() == SpaceManagerItem.SUBCATEGORY)
            {
                if(((SubCategoryItem)spaceItemList.get(i)).isSelected())
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean allItemsSelected()
    {
        for(int i = 0; i< spaceItemList.size(); i++)
        {
            if(spaceItemList.get(i).getType() == SpaceManagerItem.SUBCATEGORY)
            {
                if(!((SubCategoryItem)spaceItemList.get(i)).isSelected())
                {
                    return false;
                }
            }
        }
        return true;
    }

    private void postNotifyDataSetChanged()
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                notifyDataSetChanged();
            }
        });
    }

}

