package com.bsb.hike.voip;

import android.media.AudioAttributes;
import android.media.Ringtone;

public class RingtoneForLollipop {

	static public void create(Ringtone ringtone) {
		AudioAttributes.Builder attrs = new AudioAttributes.Builder();
		attrs.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
		attrs.setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE);
		ringtone.setAudioAttributes(attrs.build());
	}
}
