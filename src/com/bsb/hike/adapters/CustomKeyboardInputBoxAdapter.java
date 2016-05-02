package com.bsb.hike.adapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources.NotFoundException;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

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
		verticalLayoutParams.setMargins(0, (int) (15 * Utils.densityMultiplier), 0, 0);

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

		Button button;

		for (ArrayList<TextKey> textKeyArrayList : customKeyboardTextKeys)
		{
			horizontalLayout = new LinearLayout(mContext);

            int textKeyArrayListSize = textKeyArrayList.size(),maxLengthOfTextButton;

            switch (textKeyArrayListSize)
            {
                case HikePlatformConstants.BotsTextKeyboardNumCols.ONE_COL:
                    maxLengthOfTextButton = 30;
                    break;
                case HikePlatformConstants.BotsTextKeyboardNumCols.TWO_COLS:
                    maxLengthOfTextButton = 15;
                    break;
                case HikePlatformConstants.BotsTextKeyboardNumCols.THREE_COLS:
                    maxLengthOfTextButton = 10;
                    break;
                default:
                    maxLengthOfTextButton = 10;
            }

			for (TextKey textKey : textKeyArrayList)
			{
                button = new Button(mContext);

				button.setText(textKey.getText());

				params.setMargins((int) (4 * Utils.densityMultiplier), (int) (4 * Utils.densityMultiplier), (int) (4 * Utils.densityMultiplier),
						(int) (4 * Utils.densityMultiplier));

				button.setLayoutParams(params);

				// Set button text size to 12 sp
				button.setTextSize(12);
                button.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLengthOfTextButton)});
				button.setSingleLine(true);
				button.setEllipsize(TextUtils.TruncateAt.END);

				button.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.bordered_button));

				try
				{
					button.setTextColor(ColorStateList.createFromXml(mContext.getResources(), mContext.getResources().getXml(R.color.custom_keyboard_button_color)));
				}
				catch (NotFoundException e)
				{
					e.printStackTrace();
				}
				catch (XmlPullParserException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

				button.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);

				button.setAllCaps(false);

				button.setOnClickListener(this);

				button.setTag(textKey.getText());

				horizontalLayout.addView(button);
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
		View stickerViewToDisplay = inflater.inflate(R.layout.sticker_page, null);
		GridView stickerGridView = (GridView) stickerViewToDisplay.findViewById(R.id.emoticon_grid);

		int stickerGridNumCols;

		switch (stickerSize)
		{
		case HikePlatformConstants.BotsStickerSize.LARGE:
			stickerGridNumCols = HikePlatformConstants.BotsStickerGridType.largeStkrGridCols;
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) stickerGridView.getLayoutParams();
			lp.setMargins(10, 100, 10, 0);
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

		Iterator<StkrKey> customKeyboardStickerKeysIterator = customKeyboardStickerKeys.iterator();
		while (customKeyboardStickerKeysIterator.hasNext())
		{
            StkrKey customKeyboardStickerKey = customKeyboardStickerKeysIterator.next();
            Sticker customKeyboardSticker = new Sticker(customKeyboardStickerKey.getCatId(),customKeyboardStickerKey.getStkrId());
            stickersList.add(customKeyboardSticker);
		}

        if(stickersList.size() == 0)
            return stickerViewToDisplay;

		List<StickerPageAdapterItem> stickerPageList = StickerManager.getInstance().generateStickerPageAdapterItemList(stickersList);

		worker = new StickerLoader.Builder().downloadLargeStickerIfNotFound(true).downloadMiniStickerIfNotFound(true)
				.setDefaultBitmap(HikeBitmapFactory.decodeResource(mContext.getResources(), R.drawable.shop_placeholder)).build();

		final BotsStickerAdapter botsStickerAdapter = new BotsStickerAdapter(mContext, stickerPageList, worker, stickerGridView, stickerPickerListener, stickerSize, stickerGridNumCols);
		stickerGridView.setAdapter(botsStickerAdapter);
		return stickerViewToDisplay;
	}

	@Override
	public void onClick(View v)
	{
		textPickerListener.onTextClicked((String) v.getTag());
	}
}
