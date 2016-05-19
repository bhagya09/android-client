package com.bsb.hike.platform;

import com.bsb.hike.models.HikeFile;

import org.json.JSONObject;

public class CardComponent {
	protected String tag;
	public CardComponent(String tag) {
		this.tag = tag;
	}
	public String getTag() {
		return tag;
	}

	

	public static class TextComponent extends CardComponent
	{
		public String text;

		public String color;

		public int size;

		public String getText()
		{
			return text;
		}

		public TextComponent(Builder builder)
		{
			super(builder.tag);
			this.text = builder.text;
			this.color = builder.color;
			this.size = builder.size;
		}

		public static class Builder
		{
			private String text;

			private String color;

			private String tag;

			private int size;

			public Builder(String tag)
			{
				this.tag = tag;
			}

			public Builder setText(String text)
			{
				this.text = text;
				return this;
			}

			public Builder setTextColor(String color)
			{
				this.color = color;
				return this;
			}

			public Builder setTextSize(int size)
			{
				this.size = size;
				return this;
			}

			public TextComponent build()
			{
				return new TextComponent(this);
			}
		}
	}

	public static class MediaComponent extends CardComponent
	{
		private String tag;

		private HikeFile hikeFile;

		private String url;

		public static class Builder
		{
			private String tag;

			private HikeFile hikeFile;

			private String url;

			public Builder(String tag)
			{
				this.tag = tag;
			}

			public Builder setUrl(String url)
			{
				this.url = url;
				return this;
			}

			public Builder setHikeFile(HikeFile hikeFile)
			{
				this.hikeFile = hikeFile;
				return this;
			}

			public MediaComponent build()
			{
				return new MediaComponent(this);
			}
		}

		public MediaComponent(String tag, HikeFile hikeFile)
		{
			super(tag);
			this.hikeFile = hikeFile;
		}
		public MediaComponent(String tag, String url)
		{
			super(tag);
			this.url = url;
		}
		public String getKey()
		{
			return hikeFile.getFileKey();
		}

		public String getUrl()
		{
			return url;
		}

		public HikeFile getHikeFile()
		{
			return hikeFile;
		}

		public MediaComponent(Builder builder)
		{
			super(builder.tag);
			this.tag = builder.tag;
			this.hikeFile = builder.hikeFile;
			this.url = builder.url;
		}
	}

	public static class ImageComponent extends CardComponent {
        private String url;
		private String key;
		private String type;
		private String size;
		private String duration;
		public ImageComponent(String tag, String key, String url,
                              String type, String size, String duration) {
			super(tag);
			this.url = url;
			this.key = key;
			this.type = type;
			this.size = size;
			this.duration = duration;
		}
        public String getUrl(){
			return this.url;
		}
		public String getKey(){
			return this.key;
		}
	}
//
//	public static class VideoComponent extends MediaComponent {
//
//		public VideoComponent(String tag, String key, String url,
//                              String videoType, String size, String duration) {
//			super(tag, key, url, videoType, size, duration);
//		}
//
//	}
//
//	public static class AudioComponent extends MediaComponent {
//
//		public AudioComponent(String tag,  String url, String key,
//				String type, String size, String duration) {
//			super(tag, key, url, type, size, duration);
//		}
//
//	}

    public static class ActionComponent extends CardComponent{
        private String action;
        private String actionText;
		private JSONObject extra;
        public ActionComponent(String action, String actionText, JSONObject extra) {
			super(null);
            this.action = action;
			this.actionText = actionText;
			this.extra=extra;
        }

        public String getAction() {
            return action;
        }
		public String getActionText() {
			return actionText;
		}
		public JSONObject getActionUrl() {
			return extra;
		}

	}
}
