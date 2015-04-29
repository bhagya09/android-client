package com.bsb.hike.platform;

import org.json.JSONObject;

public class CardComponent {
	protected String tag;
	public CardComponent(String tag) {
		this.tag = tag;
	}
	public String getTag() {
		return tag;
	}

	

	public static class TextComponent extends CardComponent {
		public String text;

		public String getText() {
			return text;
		}

		public TextComponent(String tag,String text) {
			super(tag);
			this.text = text;
		}
	}

	public static class MediaComponent extends CardComponent {
		private String key;
		private String thumbnail;
		private String url;
		private String type, size, duration;


        public MediaComponent(String tag, String key, String url,
				String type, String size, String duration) {
			super(tag);
            this.key = key;
            this.url = url;
            this.type = type;
            this.size = size;
            this.duration = duration;


		}

		public String getKey() {
			return key;
		}

        public String getThumbnail() {
            return thumbnail;
        }

		public String getUrl() {
			return url;
		}

        public void setThumbnail(String thumbnail) {
            this.thumbnail = thumbnail;
        }

        public String getDuration() {
			return duration;
		}

		public String getSize() {
			return size;
		}

		public String getType() {
			return type;
		}
	}

	public static class ImageComponent extends MediaComponent {

		public ImageComponent(String tag, String key, String url,
                              String type, String size, String duration) {
			super(tag, key, url, type, size,duration);
		}

	}

	public static class VideoComponent extends MediaComponent {

		public VideoComponent(String tag, String key, String url,
                              String videoType, String size, String duration) {
			super(tag, key, url, videoType, size, duration);
		}

	}

	public static class AudioComponent extends MediaComponent {

		public AudioComponent(String tag,  String url, String key,
				String type, String size, String duration) {
			super(tag, key, url, type, size, duration);
		}

	}

    public static class ActionComponent extends CardComponent{
        private JSONObject android_intent;

        public ActionComponent(String tag, JSONObject android) {
            super(tag);
            this.android_intent = android;
        }

        public JSONObject getAndroidIntent() {
            return android_intent;
        }
    }
}
