package com.bsb.hike.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.R;
import com.bsb.hike.bots.CustomKeyboard;
import com.bsb.hike.bots.Sk;
import com.bsb.hike.bots.TextPickerListener;
import com.bsb.hike.bots.Tk;
import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerPageAdapterItem;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The type Custom keyboard input box adapter.
 */
public class CustomKeyboardInputBoxAdapter implements OnClickListener
{
	private static Context mContext;

	private LayoutInflater inflater;

	/**
	 * The Vertical layout.
	 */
	LinearLayout verticalLayout;

	/**
	 * The Horizontal layout.
	 */
	LinearLayout horizontalLayout;

	/**
	 * The final View that needs to be displayed.
	 */
	View viewToDisplay;

	/**
	 * The Text picker listener.
	 */
	TextPickerListener textPickerListener;

	private StickerLoader worker;

	private StickerPickerListener stickerPickerListener;

	private BotsStickerAdapter botsStickerAdapter;

	/**
	 * Instantiates a new Custom keyboard input box adapter.
	 *
	 * @param context
	 *            the context
	 * @param textPickerListener
	 *            the text picker listener
	 * @param stickerPickerListener
	 *            the sticker picker listener
	 */
	public CustomKeyboardInputBoxAdapter(Context context, TextPickerListener textPickerListener, StickerPickerListener stickerPickerListener)
	{
		this.mContext = context;
		this.inflater = LayoutInflater.from(context);
		this.textPickerListener = textPickerListener;
		this.stickerPickerListener = stickerPickerListener;
	}

	/**
	 * Init text keyboard view.
	 *
	 * @param customKeyboardTextKeys
	 *            the custom keyboard text keys
	 * @return the view
	 */
	public View initTextKeyboardView(ArrayList<ArrayList<Tk>> customKeyboardTextKeys)
	{

		viewToDisplay = inflater.inflate(R.layout.custom_keyboard_layout, null);

		verticalLayout = (LinearLayout) viewToDisplay.findViewById(R.id.ll_custom_keyboard);

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

		View view;

		for (ArrayList<Tk> tkList : customKeyboardTextKeys)
		{
			horizontalLayout = new LinearLayout(mContext);

			for (Tk tk : tkList)
			{
				view = inflater.inflate(R.layout.custom_keyboard_text, null);
				TextView textView = (TextView) view.findViewById(R.id.text);
				FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.text_container_layout);

				textView.setText(tk.getV());
				textView.setSingleLine(true);
				params.setMargins((int) (4 * Utils.densityMultiplier), (int) (8 * Utils.densityMultiplier), (int) (4 * Utils.densityMultiplier),
						(int) (8 * Utils.densityMultiplier));

				frameLayout.setLayoutParams(params);

				view.setOnClickListener(this);

				view.setTag(tk.getV());

				horizontalLayout.addView(view);
			}

			verticalLayout.addView(horizontalLayout);
		}
		return viewToDisplay;
	}

	/**
	 * Init sticker keyboard view view.
	 *
	 * @param customKeyboardStickerKeys
	 *            the custom keyboard sticker keys
	 * @return the view
	 */
	public View initStickerKeyboardView(List<Sk> customKeyboardStickerKeys)
	{
		View stickerViewToDisplay = inflater.inflate(R.layout.custom_keyboard_sticker_page, null);
		GridView stickerGridView = (GridView) stickerViewToDisplay.findViewById(R.id.emoticon_grid);

		int stickerGridNumCols = HikePlatformConstants.stickerGridNoOfCols;

		stickerGridView.setNumColumns(stickerGridNumCols);
		List<Sticker> stickersList = new CopyOnWriteArrayList<>();
		List<StickerPageAdapterItem> stickerPageList = new CopyOnWriteArrayList<>();

		Iterator<Sk> customKeyboardStickerKeysIterator = customKeyboardStickerKeys.iterator();
		while (customKeyboardStickerKeysIterator.hasNext())
		{
			Sk customKeyboardStickerKey = customKeyboardStickerKeysIterator.next();
			Sticker customKeyboardSticker = new Sticker(customKeyboardStickerKey.getCatId(), customKeyboardStickerKey.getStkrId());
			if (customKeyboardSticker.isStickerAvailable())
			{
				stickerPageList.add(new StickerPageAdapterItem(StickerPageAdapterItem.STICKER, customKeyboardSticker));
			}
			else
			{
				stickerPageList.add(new StickerPageAdapterItem(StickerPageAdapterItem.DOWNLOADING, customKeyboardSticker));
				StickerManager.getInstance().initiateSingleStickerDownloadTask(customKeyboardStickerKey.getStkrId(), customKeyboardStickerKey.getCatId(), null);
			}
			stickersList.add(customKeyboardSticker);
		}

		if (stickersList.size() == 0)
			return stickerViewToDisplay;

		worker = new StickerLoader.Builder().downloadLargeStickerIfNotFound(true).downloadMiniStickerIfNotFound(true)
				.setDefaultBitmap(HikeBitmapFactory.decodeResource(mContext.getResources(), R.drawable.art_sticker_shape)).build();

		botsStickerAdapter = new BotsStickerAdapter(mContext, stickerPageList, worker, stickerGridView, stickerPickerListener, stickerGridNumCols);
		stickerGridView.setAdapter(botsStickerAdapter);
		return stickerViewToDisplay;
	}

	@Override
	public void onClick(View v)
	{
		textPickerListener.onTextClicked((String) v.getTag());
	}

    
	/**
	 * Gets custom key board height.
	 *
	 * @param customKeyboard
	 *            the custom keyboard object
	 * @return the custom key board height
	 */
	public static int getCustomKeyBoardHeight(CustomKeyboard customKeyboard)
	{
		// Precautionary null check
        if (customKeyboard == null)
			return 0;

		if (customKeyboard != null && customKeyboard.getT() != null && customKeyboard.getT().equals(HikePlatformConstants.BOT_CUSTOM_KEYBOARD_TYPE_TEXT))
			return Utils.dpToPx(customKeyboard.getTk().size() * 48 + (customKeyboard.getTk().size() + 1) * 16);
		else if (customKeyboard != null && customKeyboard.getT() != null && customKeyboard.getT().equals(HikePlatformConstants.BOT_CUSTOM_KEYBOARD_TYPE_STICKER))
		{
			int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;

			int stickerPadding = 2 * mContext.getResources().getDimensionPixelSize(R.dimen.sticker_padding);
			int horizontalSpacing = (HikePlatformConstants.stickerGridNoOfCols - 1) * mContext.getResources().getDimensionPixelSize(R.dimen.sticker_grid_horizontal_padding);

			int actualSpace = (screenWidth - horizontalSpacing - stickerPadding);

			return (int) Math.ceil( (double) customKeyboard.getSk().size() / HikePlatformConstants.stickerGridNoOfCols) * actualSpace/HikePlatformConstants.stickerGridNoOfCols + Utils.dpToPx(((int) Math.ceil( (double) customKeyboard.getSk().size() / HikePlatformConstants.stickerGridNoOfCols) + 0) * 10);
		}
		return 0;
	}


	/**
	 * Release resources.
	 */
	public void releaseResources()
	{
		if (botsStickerAdapter != null)
			botsStickerAdapter.unregisterListeners();
	}

}
