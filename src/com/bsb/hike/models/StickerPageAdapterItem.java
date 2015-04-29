package com.bsb.hike.models;

public class StickerPageAdapterItem
{
	private int type;

	private Object item;

	public static final int STICKER = 1;

	public static final int UPDATE = 2;

	public static final int DOWNLOADING = 3;

	public static final int RETRY = 4;

	public static final int DONE = 5;

	public static final int PLACE_HOLDER = 6;

	public StickerPageAdapterItem(int type, Object item)
	{
		this.type = type;
		this.item = item;
	}

	public StickerPageAdapterItem(int type)
	{
		this.type = type;
	}

	public Sticker getSticker()
	{
		if(getItem() == null || type != STICKER)
		{
			return null;
		}
		return (Sticker) item;
	}
	
	public int getCategoryMoreStickerCount()
	{
		if(getItem() == null || type != UPDATE)
		{
			return 0;
		}
		return (Integer) item;
	}
	
	public Object getItem()
	{
		return item;
	}

	public int getType()
	{
		return type;
	}

	/* Need to override equals and hashcode inorder to use them in recentStickers linkedhashset */
	@Override
	public boolean equals(Object object)
	{
		if (object == null)
			return false;
		if (object == this)
			return true;
		if (!(object instanceof StickerPageAdapterItem))
			return false;
		if (((StickerPageAdapterItem) object).getType() != this.getType())
		{
			return false;
		}
		if (((StickerPageAdapterItem) object).getType() == this.getType())
		{
			if (this.getType() == STICKER)
			{
				return (this.getSticker().equals(((StickerPageAdapterItem) object).getSticker()));
			}
			else
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 37;
		if (this.item != null)
		{
			hash += 7 * hash + this.item.hashCode();
		}
		hash += 7 * hash + this.type;

		return hash;
	}

}
