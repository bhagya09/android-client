package com.bsb.hike.voip.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;

public class CallActionsView extends RelativeLayout
{
	private CallActions listener;

	private GlowPadViewWrapper glowPadView;

	private int COMPAT_VIEW_EXTRA_PADDING = 30;

	public CallActionsView(Context context) 
	{
		super(context);
		init();
	}

	public CallActionsView(Context context, AttributeSet attrs) 
	{
        super(context, attrs);
        init();
    }

    public CallActionsView(Context context, AttributeSet attrs, int defStyle) 
    {
        super(context, attrs, defStyle);
        init();
    }

    private void init()
    {
		inflate(getContext(), R.layout.voip_call_actions_view, this);
		glowPadView = (GlowPadViewWrapper) findViewById(R.id.glow_pad_view);
    }

    public void setCallActionsListener(CallActions listener)
    {   	
    	if(glowPadView == null)
    	{
    		this.listener = listener;
    	}
    	else
    	{
    		glowPadView.setCallActionsListener(listener);
    	}
    }

    public void startPing()
    {
    	if(glowPadView!=null)
    	{
    		glowPadView.startPing();
    	}
    }

    public void stopPing()
    {
    	if(glowPadView!=null)
    	{
    		glowPadView.stopPing();
    		glowPadView = null;
    	}
    }

    private void setupCompatActions()
    {
    	findViewById(R.id.call_hang_compat).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) 
			{
				listener.onDeclineCall();
			}
		});
		
		findViewById(R.id.call_pick_compat).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) 
			{
				listener.onAcceptCall();
			}
		});
    }
}