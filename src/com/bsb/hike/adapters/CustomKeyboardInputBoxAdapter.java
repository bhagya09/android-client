package com.bsb.hike.adapters;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.R;
import com.bsb.hike.bots.CustomKeyboardStickerPickerListener;
import com.bsb.hike.bots.CustomKeyboardTextPickerListener;
import com.bsb.hike.bots.Sk;
import com.bsb.hike.bots.Tk;
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
	private Context mContext;

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
	CustomKeyboardTextPickerListener customKeyboardTextPickerListener;

	private CustomKeyboardStickerPickerListener customKeyboardStickerPickerListener;

	private BotsStickerAdapter botsStickerAdapter;

	/**
	 * Instantiates a new Custom keyboard input box adapter.
	 *
	 * @param context
	 *            the context
	 * @param customKeyboardTextPickerListener
	 *            the text picker listener
	 * @param customKeyboardStickerPickerListener
	 *            the sticker picker listener
	 */
	public CustomKeyboardInputBoxAdapter(Context context, CustomKeyboardTextPickerListener customKeyboardTextPickerListener,  CustomKeyboardStickerPickerListener customKeyboardStickerPickerListener)
	{
		this.mContext = context;
		this.inflater = LayoutInflater.from(context);
		this.customKeyboardTextPickerListener = customKeyboardTextPickerListener;
		this.customKeyboardStickerPickerListener = customKeyboardStickerPickerListener;
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
	public View initStickerKeyboardView(List<Sk> customKeyboardStickerKeys,int screenOrientation)
	{
		View stickerViewToDisplay = inflater.inflate(R.layout.custom_keyboard_sticker_page, null);
		GridView stickerGridView = (GridView) stickerViewToDisplay.findViewById(R.id.emoticon_grid);

		int stickerGridNumCols = HikePlatformConstants.stickerGridNoOfColsPortrait;

        if(screenOrientation ==  Configuration.ORIENTATION_LANDSCAPE)
            stickerGridNumCols = HikePlatformConstants.stickerGridNoOfColsLandscape;

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

		StickerLoader worker = new StickerLoader.Builder().downloadLargeStickerIfNotFound(true).downloadMiniStickerIfNotFound(true)
				.setDefaultBitmap(HikeBitmapFactory.decodeResource(mContext.getResources(), R.drawable.art_sticker_shape)).build();

		botsStickerAdapter = new BotsStickerAdapter(mContext, stickerPageList, worker, stickerGridView, customKeyboardStickerPickerListener, stickerGridNumCols);
		stickerGridView.setAdapter(botsStickerAdapter);
		return stickerViewToDisplay;
	}

	@Override
	public void onClick(View v)
	{
		customKeyboardTextPickerListener.onTextClicked((String) v.getTag());
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
