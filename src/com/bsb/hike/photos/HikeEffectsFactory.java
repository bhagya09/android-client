package com.bsb.hike.photos;



import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bsb.hike.photos.FilterTools.FilterType;


public final class HikeEffectsFactory{

	private static HikeEffectsFactory instance;

	public static ColorMatrixColorFilter applyFiltertoBitmapDrawable(BitmapDrawable source,FilterType type,float value)
	{

		ColorMatrix FilterColormatrix=getColorMatrixforFilter(type, value);

		return applyColorMatrixToDrawable(source, FilterColormatrix);

	}

	public static void LoadPreviewThumbnail(Bitmap scaledOriginal,FilterType type,OnPreviewReadyListener listener )
	{
		new LoadPreviewImageTask(scaledOriginal, getColorMatrixforFilter(type, 100)  , listener).execute();

	}

	private static ColorMatrix getColorMatrixforFilter(FilterType type,float value)
	{
		if(instance==null)
			instance=new HikeEffectsFactory();

		ColorMatrix FilterColormatrix;
		switch(type)
		{
		case BRIGHTNESS:
			FilterColormatrix=instance.getBrightnessColorMatrix(value);
			break;
		case SATURATION:
			FilterColormatrix=instance.getSaturationColorMatrix(value);
			break;
		case HUE:
			FilterColormatrix=instance.getHueColorMatrix(value);
			break;
		case SEPIA:
			FilterColormatrix=instance.getSepiaColorMatrix(value);
			break;
		case CONTRAST:
			FilterColormatrix=instance.getContrastColorMatrix(value);
			break;
		case GRAYSCALE:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.getBlackAndWhiteColorMatrix(),value);
			break;
		case POLAROID:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.getPolaroidColorMatrix(),value);
			break;
		case FADED:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.getFadedColorMatrix(),value);
			break;
		case X_PRO_2:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.XPRO2(),value);
			break;
		case WILLOW:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.willow(),value);
			break;
		case WALDEN:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.walden(),value);
			break;
		case TOASTER:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.toaster(),value);
			break;
		case SUTRO:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.sutro(),value);
			break;
		case SIERRA:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.sierra(),value);
			break;
		case RISE:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.rise(),value);
			break;
		case MAYFAIR:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.mayfair(),value);
			break;
		case LO_FI:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.lofi(),value);
			break;
		case KELVIN:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.kelvin(),value);
			break;
		case INKWELL:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.inkwell(),value);
			break;
		case BGR:
			FilterColormatrix=instance.partialFilterColorMatrix(instance.getBGRColorMatrix(),value);
			break;
		case INVERSION:
			FilterColormatrix=instance.getInvertColorsColorMatrix(value);
			break;
		default:
			FilterColormatrix=new ColorMatrix();

		}
		return FilterColormatrix;
	}

	/**
	 * @author akhiltripathi
	 * 
	 * @param
	 * 		filter: ColorMatrix of the filter whose partial matrix is required
	 * 		value: %of filter to be applied.0 return Identity and 100 return the input filter
	 * 
	 * @return
	 * 		partial filter of specified percentage
	 */

	private ColorMatrix partialFilterColorMatrix(ColorMatrix filter,float value)
	{
		value=value/100;
		float[] partialArray=filter.getArray();
		for(int i=0;i<partialArray.length;i++)
		{
			if(i%6==0)
				partialArray[i]=1-(1-partialArray[i])*value;
			else
				partialArray[i]*=value;
		}
		ColorMatrix ret=new ColorMatrix(partialArray);

		return ret;
	}

	public static Bitmap applyColorMatrixToBitmap(Bitmap bitmap,ColorMatrix matrix)
	{
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
		Bitmap bitmapResult = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas canvasResult = new Canvas(bitmapResult);
		Paint paint = new Paint();
		paint.setColorFilter(filter);
		canvasResult.drawBitmap(bitmap, 0, 0, paint);
		return bitmapResult;
	}

	private static ColorMatrixColorFilter applyColorMatrixToDrawable(BitmapDrawable drawable,ColorMatrix matrix)
	{
		ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
		drawable.setColorFilter(filter);
		return filter;
	}

	private ColorMatrix getCustomEffectColorMatrix(ColorMatrix[] effects)
	{
		ColorMatrix ret=new ColorMatrix();
		for(ColorMatrix effect:effects)
		{
			ret.setConcat(effect, ret);
		}
		return ret;
	}

	private ColorMatrix getSaturationColorMatrix(float value)
	{
		ColorMatrix ret=new ColorMatrix();
		ret.setSaturation(value);
		return ret;
	}

	private ColorMatrix getOriginalColorMatrix() {

		return null;
	}

	private ColorMatrix getInvertColorsColorMatrix(float value)
	{
		float []array=new float[]{
				-1,0,0,0,255-value,
				0,-1,0,0,255-value,
				0,0,-1,0,255-value,
				0,0,0,1,0
		};
		ColorMatrix matrix = new ColorMatrix(array);

		return matrix;
	}

	private ColorMatrix  getBlackAndWhiteColorMatrix() {

		final ColorMatrix matrixA = new ColorMatrix();
		matrixA.setSaturation(0);

		return matrixA;
	}

	private ColorMatrix getContrastColorMatrix(float value) {
		float scale = value ;
		float translate = (-.5f * scale + .5f) * 255.f;
		float[] array = new float[] {
				scale, 0, 0, 0, translate,
				0, scale, 0, 0, translate,
				0, 0, scale, 0, translate,
				0, 0, 0, 1, 0};
		ColorMatrix matrix = new ColorMatrix(array);

		return matrix;
	}

	private ColorMatrix getBrightnessColorMatrix(float value) {
		value=(value-1)*100;
		float[] array = new float[] {
				1, 0, 0, 0, value,
				0, 1, 0, 0, value,
				0, 0, 1, 0, value,
				0, 0, 0, 1, 0};
		ColorMatrix matrix = new ColorMatrix(array);

		return matrix;
	}


	private ColorMatrix getBGRColorMatrix() {
		float[] array = new float[] {
				0, 0, 1, 0, 0,
				0, 1, 0, 0, 0,
				1, 0, 0, 0, 0,
				0, 0, 0, 1, 0};
		ColorMatrix matrix = new ColorMatrix(array);

		return matrix;
	}

	private ColorMatrix getOpacityColorMatrix(float value) {
		float[] array = new float[] {
				1, 0, 0, 0, 0,
				0, 1, 0, 0, 0,
				0, 0, 1, 0, 0,
				0, 0, 0, value, 0};
		ColorMatrix matrix = new ColorMatrix(array);

		return matrix;
	}


	private ColorMatrix getHueColorMatrix(float value)
	{
		value = value/ 180f * (float) Math.PI;

		float cosVal = (float) Math.cos(value);
		float sinVal = (float) Math.sin(value);
		float lumR = 0.213f;
		float lumG = 0.715f;
		float lumB = 0.072f;
		float[] mat = new float[]
				{ 
				lumR + cosVal * (1 - lumR) + sinVal * (-lumR), lumG + cosVal * (-lumG) + sinVal * (-lumG), lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0, 0, 
				lumR + cosVal * (-lumR) + sinVal * (0.143f), lumG + cosVal * (1 - lumG) + sinVal * (0.140f), lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0, 0,
				lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)), lumG + cosVal * (-lumG) + sinVal * (lumG), lumB + cosVal * (1 - lumB) + sinVal * (lumB), 0, 0, 
				0f, 0f, 0f, 1f, 0f, 
				0f, 0f, 0f, 0f, 1f };
		ColorMatrix colorMatrix =new ColorMatrix(mat);
		return colorMatrix;

	}


	private ColorMatrix getBinaryColorMatrix() {
		ColorMatrix colorMatrix = new ColorMatrix();
		colorMatrix.setSaturation(0);

		float m = 255f;
		float t = -255*128f;
		ColorMatrix threshold = new ColorMatrix(new float[] {
				m, 0, 0, 1, t,
				0, m, 0, 1, t,
				0, 0, m, 1, t,
				0, 0, 0, 1, 0
		});

		colorMatrix.postConcat(threshold);
		return colorMatrix;
	}


	private ColorMatrix getFadedColorMatrix() {

		final ColorMatrix matrixA = new ColorMatrix(new float[] {
				.66f, .33f, .33f, 0, 0, //red
				.33f, .66f, .33f, 0, 0, //green
				.33f, .33f, .66f, 0, 0, //blue
				0, 0, 0, 1, 0    //alpha
		});


		return matrixA;
	}

	private ColorMatrix getPolaroidColorMatrix() {



		final ColorMatrix matrixA = new ColorMatrix(new float[] {
				1.438f, -0.062f, -0.062f, 0, 0, //red
				-0.122f, 1.378f, -0.122f, 0, 0, //green
				-0.016f,-0.016f,1.483f, 0, 0, //blue
				0, 0, 0, 1, 0,
				-0.03f, 0.05f,-0.02f, 0,1//alpha
		});

		return matrixA;
	}


	private ColorMatrix getSepiaColorMatrix(float value) {
		value=value/100;
		ColorMatrix sepiaMatrix =new ColorMatrix();
		float[] sepMat={
				1-(1-0.3930000066757202f)*value, 0.7689999938011169f*value, 0.1889999955892563f*value, 0, 0,
				0.3490000069141388f*value, 1-(1-0.6859999895095825f)*value, 0.1679999977350235f*value, 0, 0,
				0.2720000147819519f*value, 0.5339999794960022f*value, 1-(1-0.1309999972581863f)*value, 0, 0,
				0, 0, 0, 1, 0,
				0, 0, 0, 0, 1};
		sepiaMatrix.set(sepMat);
		return sepiaMatrix;
	}

	
	//contrast(1.3) brightness(0.8) sepia(0.3) saturate(1.5) hue-rotate(-20deg)
	ColorMatrix XPRO2()
	{
		ColorMatrix ret=getContrastColorMatrix( 1.3f);
		ret.setConcat(getBrightnessColorMatrix( 0.8f), ret);
		ret.setConcat(getSepiaColorMatrix( 40f), ret);
		//ret.setSaturation(1.2f);
		ret.setConcat(getHueColorMatrix( -20f), ret);
		return ret;

	}

	//filter: saturate(0.02) contrast(0.85) brightness(1.2) sepia(0.02);
	ColorMatrix willow()
	{
		ColorMatrix ret=new ColorMatrix();
		ret.setSaturation(0.02f);

		ret.setConcat(getContrastColorMatrix( 0.85f), ret);
		ret.setConcat(getBrightnessColorMatrix( 0.75f), ret);
		ret.setConcat(getSepiaColorMatrix( 2f), ret);
		
		return ret;

	}

	//filter: sepia(0.35) contrast(0.9) brightness(1.1) hue-rotate(-10deg) saturate(1.5);
	ColorMatrix walden()
	{
		ColorMatrix ret=new ColorMatrix();
		ret.setConcat(getSepiaColorMatrix( 45f), ret);
		ret.setConcat(getContrastColorMatrix( 0.9f), ret);
		ret.setConcat(getBrightnessColorMatrix( 0.9f), ret);
		ret.setConcat(getHueColorMatrix( -10f), ret);

		
		return ret;

	}

	//filter:sepia(0.4) saturate(2.5) hue-rotate(-30deg) contrast(0.67);
	ColorMatrix toaster()
	{
		ColorMatrix ret=new ColorMatrix();
		ret.setConcat(getSepiaColorMatrix( 50f), ret);
		ret.setConcat(getHueColorMatrix( -30f), ret);
		ret.setConcat(getContrastColorMatrix( 0.67f), ret);

		
		return ret;

	}

	//filter: brightness(0.75) contrast(1.3) sepia(0.5) hue-rotate(-25deg);
	ColorMatrix sutro()
	{
		ColorMatrix ret=new ColorMatrix();
		ret.setConcat(getBrightnessColorMatrix( 0.65f), ret);
		ret.setConcat(getContrastColorMatrix( 1.3f), ret);

		ret.setConcat(getSepiaColorMatrix( 60f), ret);
		ret.setConcat(getHueColorMatrix( -25f), ret);

		
		return ret;

	}

	//filter: contrast(0.8) saturate(1.2) sepia(0.15);
	ColorMatrix sierra()
	{
		ColorMatrix ret=new ColorMatrix();
		ret.setConcat(getContrastColorMatrix( 0.8f), ret);
		//ColorMatrix ret2=new ColorMatrix();
		//ret2.setSaturation(1.2f);
		//ret.setConcat(ret2,ret);
		ret.setConcat(getSepiaColorMatrix( 15f), ret);
		
		return ret;

	}

	// filter: saturate(1.4) sepia(0.25) hue-rotate(-15deg) contrast(0.8) brightness(1.1);
	ColorMatrix rise()
	{
		ColorMatrix ret=new ColorMatrix();
		ret.setConcat(getSepiaColorMatrix( 15f), ret);
		ret.setConcat(getHueColorMatrix( -15f), ret);

		ret.setConcat(getContrastColorMatrix( 0.8f), ret);
		ret.setConcat(getBrightnessColorMatrix( 1.1f), ret);

		
		return ret;

	}

	ColorMatrix mayfair()
	{
		ColorMatrix ret=new ColorMatrix();
		//ret.setSaturation(1.4f);
		ret.setConcat(getContrastColorMatrix( 1.3f), ret);

		return ret;

	}

	ColorMatrix lofi()
	{
		ColorMatrix ret=new ColorMatrix();
		//ret.setSaturation(1.4f);
		ret.setConcat(getContrastColorMatrix( 1.4f), ret);
		ret.setConcat(getBrightnessColorMatrix( 0.7f), ret);
		ret.setConcat(getSepiaColorMatrix( 5f), ret);

		
		return ret;

	}

	// filter: sepia(0.4) saturate(2.4) brightness(1.3) contrast(1);
	ColorMatrix kelvin()
	{
		ColorMatrix ret=new ColorMatrix();
		ret.setSaturation(3.4f);
		ret.setConcat(getSepiaColorMatrix( 60f), ret);
		ret.setConcat(getBrightnessColorMatrix( 1.1f), ret);
		ret.setConcat(getContrastColorMatrix( 1f), ret);

		
		return ret;

	}

	//filter: grayscale(1) brightness(1.2) contrast(1.05);
	ColorMatrix inkwell()
	{
		ColorMatrix ret=new ColorMatrix();
		ret.setConcat(getBlackAndWhiteColorMatrix(), ret);
		ret.setConcat(getBrightnessColorMatrix( 1.2f), ret);
		ret.setConcat(getContrastColorMatrix( 1.05f), ret);

		
		return ret;

	}
	
	private static class LoadPreviewImageTask extends AsyncTask<Void, Void, Bitmap> {

		private Bitmap original;
		ColorMatrix effectToBeApplied;
		private OnPreviewReadyListener readyListener;

		public LoadPreviewImageTask(Bitmap bitmap,ColorMatrix effect,OnPreviewReadyListener listener) {
			// TODO Auto-generated constructor stub
			original=bitmap;
			effectToBeApplied=effect;
			readyListener=listener;
		}

		protected void onPostExecute(Bitmap result) {
			readyListener.onPreviewReady(result) ;
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			// TODO Auto-generated method stub

			return applyColorMatrixToBitmap(original, effectToBeApplied);



		}

	}

	public interface OnPreviewReadyListener{
		void onPreviewReady(Bitmap preview);
	}

}

