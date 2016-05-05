package com.bsb.hike.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.R;
import com.bsb.hike.bots.StkrKey;
import com.bsb.hike.bots.TextKey;
import com.bsb.hike.bots.TextPickerListener;
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
	public View initTextKeyboardView(ArrayList<ArrayList<TextKey>> customKeyboardTextKeys)
	{

		viewToDisplay = inflater.inflate(R.layout.custom_keyboard_layout, null);

		verticalLayout = (LinearLayout) viewToDisplay.findViewById(R.id.ll_custom_keyboard);

		LinearLayout.LayoutParams verticalLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

		verticalLayoutParams.gravity = Gravity.TOP;
		verticalLayoutParams.setMargins(0, (int) (16 * Utils.densityMultiplier), 0, 0);

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

        View view;

		for (ArrayList<TextKey> textKeyArrayList : customKeyboardTextKeys)
		{
			horizontalLayout = new LinearLayout(mContext);

			for (TextKey textKey : textKeyArrayList)
			{
                view = inflater.inflate(R.layout.custom_keyboard_text, null);
                TextView textView = (TextView) view.findViewById(R.id.text);
                RelativeLayout relativeLayout = (RelativeLayout) view.findViewById(R.id.text_container_layout);

                textView.setText(textKey.getText());
                textView.setSingleLine(true);
                params.setMargins((int) (4 * Utils.densityMultiplier), (int) (8 * Utils.densityMultiplier), (int) (4 * Utils.densityMultiplier),
                        (int) (8 * Utils.densityMultiplier));

                relativeLayout.setLayoutParams(params);

				view.setOnClickListener(this);

                view.setTag(textKey.getText());

				horizontalLayout.addView(view);
			}

			verticalLayout.addView(horizontalLayout);
			verticalLayout.setLayoutParams(verticalLayoutParams);
		}
		return viewToDisplay;
	}

	/**
	 * Init sticker keyboard view view.
	 *
	 * @param customKeyboardStickerKeys
	 *            the custom keyboard sticker keys
	 * @param stickerSize
	 *            the sticker size
	 * @return the view
	 */
	public View initStickerKeyboardView(List<StkrKey> customKeyboardStickerKeys, String stickerSize)
	{
		View stickerViewToDisplay = inflater.inflate(R.layout.custom_keyboard_sticker_page, null);
		GridView stickerGridView = (GridView) stickerViewToDisplay.findViewById(R.id.emoticon_grid);

		int stickerGridNumCols;

		switch (stickerSize)
		{
		case HikePlatformConstants.BotsStickerSize.LARGE:
			stickerGridNumCols = HikePlatformConstants.BotsStickerGridType.largeStkrGridCols;
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) stickerGridView.getLayoutParams();
			lp.setMargins((int) (2  * Utils.densityMultiplier), (int) (24  * Utils.densityMultiplier), (int) (2  * Utils.densityMultiplier), 0);
			stickerGridView.setLayoutParams(lp);
			break;
		case HikePlatformConstants.BotsStickerSize.MEDIUM:
			stickerGridNumCols = HikePlatformConstants.BotsStickerGridType.midStkrGridCols;
			break;
		case HikePlatformConstants.BotsStickerSize.SMALL:
			stickerGridNumCols = HikePlatformConstants.BotsStickerGridType.smallStkrGridCols;
			break;
		default:
			stickerGridNumCols = HikePlatformConstants.BotsStickerGridType.largeStkrGridCols;
		}

		stickerGridView.setNumColumns(stickerGridNumCols);
		List<Sticker> stickersList = new ArrayList<>();
		List<StickerPageAdapterItem> stickerPageList = new ArrayList<>();

		Iterator<StkrKey> customKeyboardStickerKeysIterator = customKeyboardStickerKeys.iterator();
		while (customKeyboardStickerKeysIterator.hasNext())
		{
			StkrKey customKeyboardStickerKey = customKeyboardStickerKeysIterator.next();
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

		botsStickerAdapter = new BotsStickerAdapter(mContext, stickerPageList, worker, stickerGridView, stickerPickerListener, stickerSize, stickerGridNumCols);
		stickerGridView.setAdapter(botsStickerAdapter);
		return stickerViewToDisplay;
	}

	@Override
	public void onClick(View v)
	{
		textPickerListener.onTextClicked((String) v.getTag());
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
