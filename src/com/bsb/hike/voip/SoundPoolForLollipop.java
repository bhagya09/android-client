package com.bsb.hike.voip;

import android.media.AudioAttributes;
import android.media.SoundPool;

public class SoundPoolForLollipop {

	static public SoundPool create() {
		SoundPool soundpool;
		
		AudioAttributes audioAttributes = new AudioAttributes.Builder()
		.setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
		.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
		.build();
		
		soundpool = new SoundPool.Builder()
		.setMaxStreams(2)
		.setAudioAttributes(audioAttributes)
		.build();	
		
		return soundpool;
	}

}
