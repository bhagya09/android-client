package com.bsb.hike.platform.nativecards;

import android.content.Context;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CardComponent;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;

import java.util.List;

/**
 * Created by varunarora on 11/06/16.
 */
public abstract class ActionViewHolder extends MessagesAdapter.DetailViewHolder {
    private static final String TAG = ActionViewHolder.class.getSimpleName();
    public View actionContainer;

    protected ConvMessage convMessage;
    protected Context mContext;

    public ActionViewHolder(Context context) {
        this.mContext = context;
    }

    protected void showActionContainer(final View view, final View containerView, final List<CardComponent.ActionComponent> actionComponents) {
        clearShareViewHolder(view);
        final int noOfAction = actionComponents.size();
        View cta1 = null;
        View cta2 = null;

        if (noOfAction >= 2) {
            showActionContainer(view);
            cta1 = actionContainer.findViewById(R.id.cta1);
            cta2 = actionContainer.findViewById(R.id.cta2);

        } else if (noOfAction == 1) {
            showActionContainer(view);
            cta1 = actionContainer.findViewById(R.id.cta1);
        }
        if (cta1 != null) {
            actionContainer.findViewById(R.id.divider).setVisibility(View.GONE);
            cta1.setVisibility(View.VISIBLE);
            cta1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        NativeCardUtils.performAction(mContext, containerView, actionComponents.get(0), convMessage);
                    } catch (JSONException ex) {
                        Logger.e(TAG, ex.getMessage());
                    }
                }
            });
            TextView cta1Text = (TextView) view.findViewById(R.id.cta1Text);
            cta1Text.setText(actionComponents.get(0).getActionText());
            cta1Text.setCompoundDrawablePadding((int) (6 * Utils.densityMultiplier));
            cta1Text.setCompoundDrawablesWithIntrinsicBounds(NativeCardUtils.getDrawable(actionComponents.get(0)), 0, 0, 0);

        }
        if (cta2 != null) {
            actionContainer.findViewById(R.id.divider).setVisibility(View.VISIBLE);
            cta2.setVisibility(View.VISIBLE);
            cta2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        NativeCardUtils.performAction(mContext, containerView, actionComponents.get(1), convMessage);
                    } catch (JSONException ex) {
                        Logger.e(TAG, ex.getMessage());
                    }
                }
            });
            TextView cta2Text = (TextView) view.findViewById(R.id.cta2Text);
            cta2Text.setText(actionComponents.get(1).getActionText());
            cta2Text.setCompoundDrawablePadding((int) (6 * Utils.densityMultiplier));
            cta2Text.setCompoundDrawablesWithIntrinsicBounds(NativeCardUtils.getDrawable(actionComponents.get(1)), 0, 0, 0);
        }
    }

    protected void showActionContainer(View view) {
        if (actionContainer == null) {
            ViewStub actionContainerStub = (ViewStub) view.findViewById(R.id.share_stub);
            actionContainer = actionContainerStub.inflate();
        }
        actionContainer.setVisibility(View.VISIBLE);
    }

    public void clearShareViewHolder(View view) {
        if (actionContainer != null) {
            actionContainer.findViewById(R.id.cta1).setVisibility(View.GONE);
            actionContainer.findViewById(R.id.cta2).setVisibility(View.GONE);
            actionContainer.setVisibility(View.GONE);
            actionContainer.findViewById(R.id.divider).setVisibility(View.GONE);
        }
    }
}
