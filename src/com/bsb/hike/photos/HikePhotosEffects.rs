

#pragma version(1)
#pragma rs java_package_name(com.bsb.hike.photos)
#pragma rs_fp_relaxed

#define round(x) ((x)>=0?(int)((x)+0.5):(int)((x)-0.5))
#define ChannelBlend_Normal(A,B)     ((A))
#define ChannelBlend_Lighten(A,B)    (((B > A) ? B:A))
#define ChannelBlend_Darken(A,B)     (((B > A) ? A:B))
#define ChannelBlend_Multiply(A,B)   (((A * B) / 255))
#define ChannelBlend_Average(A,B)    (((A + B) / 2))
#define ChannelBlend_Add(A,B)        ((min(255, (A + B))))
#define ChannelBlend_Subtract(A,B)   (((A + B < 255) ? 0:(A + B - 255)))
#define ChannelBlend_Difference(A,B) ((abs(A - B)))
#define ChannelBlend_Negation(A,B)   ((255 - abs(255 - A - B)))
#define ChannelBlend_Screen(A,B)     ((255 - (((255 - A) * (255 - B)) >> 8)))
#define ChannelBlend_Exclusion(A,B)  ((A + B - 2 * A * B / 255))
#define ChannelBlend_Overlay(A,B)    (((B < 128) ? (2 * A * B / 255):(255 - 2 * (255 - A) * (255 - B) / 255)))
#define ChannelBlend_HardLight(A,B)  (ChannelBlend_Overlay(B,A))
#define ChannelBlend_ColorDodge(A,B) (((B == 255) ? B:min(255, ((A << 8 ) / (255 - B)))))
#define ChannelBlend_ColorBurn(A,B)  (((B == 0) ? B:max(0, (255 - ((255 - A) << 8 ) / B))))
#define ChannelBlend_LinearDodge(A,B)(ChannelBlend_Add(A,B))
#define ChannelBlend_LinearBurn(A,B) (ChannelBlend_Subtract(A,B))
#define ChannelBlend_LinearLight(A,B)((B < 128)?ChannelBlend_LinearBurn(A,(2 * B)):ChannelBlend_LinearDodge(A,(2 * (B - 128))))
#define ChannelBlend_VividLight(A,B) ((B < 128)?ChannelBlend_ColorBurn(A,(2 * B)):ChannelBlend_ColorDodge(A,(2 * (B - 128))))
#define ChannelBlend_PinLight(A,B)   ((B < 128)?ChannelBlend_Darken(A,(2 * B)):ChannelBlend_Lighten(A,(2 * (B - 128))))
#define ChannelBlend_HardMix(A,B)    (((ChannelBlend_VividLight(A,B) < 128) ? 0:255))
#define ChannelBlend_Reflect(A,B)    (((B == 255) ? B:min(255, (A * A / (255 - B)))))
#define ChannelBlend_Glow(A,B)       (ChannelBlend_Reflect(B,A))
#define ChannelBlend_Phoenix(A,B)    ((min(A,B) - max(A,B) + 255))
#define ChannelBlend_Alpha(A,B,O)    ((O * A + (1 - O) * B))
#define ChannelBlend_AlphaF(A,B,F,O) (ChannelBlend_Alpha(F(A,B),A,O))
#define ChannelBlend_SoftLight(A,B)  (((B < 128)?(2*((A>>1)+64))*((float)B/255):(255-(2*(255-((A>>1)+64))*(float)(255-B)/255))))

enum blendMode { Multiply , Overlay , SoftLight , Exclusion, Screen, Normal, Lighten };

typedef enum blendMode BlendMode;

const static float3 gMonoMult = {0.299f, 0.587f, 0.114f};
 
float static d(float x) 
{
	if (x <= 0.25) {
		return x * (4 + x*(16*x - 12));
	}
	else {
		return sqrt(x);
		
	}
}

int static blendSoftLight(float fg, float bg) 
{
	fg = fg/255.0;
	bg = bg/255.0;
	float res=0;
	if (fg <= 0.5) {
		res = bg - (1 - 2*fg) * bg * (1 - bg);
	} else {
		res = bg + (2 * fg - 1) * (d(bg) - bg);
	}
	return round(res*255.0);

}

uchar4 static setSaturation(uchar4 in,float saturationValue)
{
    float4 f4 = rsUnpackColor8888(in);
    float3 result = dot(f4.rgb, gMonoMult);
    result = mix( result, f4.rgb, saturationValue );
 
    return rsPackColorTo8888(result);
}

int rSpline[256];
int gSpline[256];
int bSpline[256];
int compositeSpline[256];
int isThumbnail,imageHeight,imageWidth;
int r[3],g[3],b[3];

float preMatrix[20],postMatrix[20];

rs_allocation input1;
rs_allocation input2;

uchar4 static validateColor(uchar4 in)
{
	if(in.r <0)
	{
		in.r =0;
	}
	if(in.r>255)
	{
		in.r = 255;
	}
	if(in.g <0)
	{
		in.g =0;
	}
	if(in.g>255)
	{
		in.g = 255;
	}
	if(in.b <0)
	{
		in.b =0;
	}
	if(in.b>255)
	{
		in.b = 255;
	}
	return in;
}

uchar4 static applyCurves(uchar4 in,int applyComposite,int applyRed,int applyGreen,int applyBlue)
{
	in = validateColor(in);
	
	if(applyComposite<0)
	{
		in.r=compositeSpline[in.r];

		in.g=compositeSpline[in.g];

		in.b=compositeSpline[in.b];
	}
	
	if(applyRed)
	{
		in.r=rSpline[in.r];
	}
	
	if(applyGreen)
	{
		in.g=gSpline[in.g];
	}

	if(applyBlue)
	{	
		in.b=bSpline[in.b];
	}
	
	if(applyComposite>0)
	{
		in.r=compositeSpline[in.r];

		in.g=compositeSpline[in.g];

		in.b=compositeSpline[in.b];
	}
	
	return in;
}

uchar4 static applyColorMatrix(uchar4 in, float matrix[])
{

	float red = in.r/255.0;
	float blue = in.b/255.0;
	float green = in.g/255.0;
	float alpha = in.a/255.0;

	float red1=matrix[0]*red+matrix[1]*green+matrix[2]*blue+matrix[3]*alpha+matrix[4]/255.0;
	float green1=matrix[5]*red+matrix[6]*green+matrix[7]*blue+matrix[8]*alpha+matrix[9]/255.0;
	float blue1=matrix[10]*red+matrix[11]*green+matrix[12]*blue+matrix[13]*alpha+matrix[14]/255.0;
	float alpha1=matrix[15]*red+matrix[16]*green+matrix[17]*blue+matrix[18]*alpha+matrix[19]/255.0;

	if(red1<0) red1 = 0;
	if(red1>1) red1 = 1;
	if(green1<0) green1 = 0;
	if(green1>1) green1 = 1;
	if(blue1<0) blue1 = 0;
	if(blue1>1) blue1 = 1;
	if(alpha1<0) alpha1 = 0;
	if(alpha1>1) alpha1 = 1;

	in.r=round(red1*255);
	in.g=round(green1*255);
	in.b=round(blue1*255);
	in.a=round(alpha1*255);

	return in;

}

uchar4 static getPixelForColor(int a, int r, int g, int b)
{
	uchar4 ret ={ 0 , 0 , 0 , 0 };
	ret.a = a;
	ret.r = r;
	ret.g = g;
	ret.b = b;
	
	return ret;
}

uchar4 static applyBlendToRGB(uchar4 source ,uchar4 target, BlendMode type, float opacity)
{
	switch(type)
	{
		case Multiply :
			source.r =  ChannelBlend_Alpha(ChannelBlend_Multiply(target.r,source.r),source.r,opacity);
			source.g =  ChannelBlend_Alpha(ChannelBlend_Multiply(target.g,source.g),source.g,opacity);
			source.b =  ChannelBlend_Alpha(ChannelBlend_Multiply(target.b,source.b),source.b,opacity);
			break;
		case Overlay :
			source.r =  ChannelBlend_Alpha(ChannelBlend_Overlay(target.r,source.r),source.r,opacity);
			source.g =  ChannelBlend_Alpha(ChannelBlend_Overlay(target.g,source.g),source.g,opacity);
			source.b =  ChannelBlend_Alpha(ChannelBlend_Overlay(target.b,source.b),source.b,opacity);
			break;
		case SoftLight :
			source.r =  ChannelBlend_Alpha(blendSoftLight(target.r,source.r),source.r,opacity);
			source.g =  ChannelBlend_Alpha(blendSoftLight(target.g,source.g),source.g,opacity);
			source.b =  ChannelBlend_Alpha(blendSoftLight(target.b,source.b),source.b,opacity);
			break;
		case Exclusion :
			source.r =  ChannelBlend_Alpha(ChannelBlend_Exclusion(target.r,source.r),source.r,opacity);
			source.g =  ChannelBlend_Alpha(ChannelBlend_Exclusion(target.g,source.g),source.g,opacity);
			source.b =  ChannelBlend_Alpha(ChannelBlend_Exclusion(target.b,source.b),source.b,opacity);
			break;
		case Screen : 
			source.r =  ChannelBlend_Alpha(ChannelBlend_Screen(target.r,source.r),source.r,opacity);
			source.g =  ChannelBlend_Alpha(ChannelBlend_Screen(target.g,source.g),source.g,opacity);
			source.b =  ChannelBlend_Alpha(ChannelBlend_Screen(target.b,source.b),source.b,opacity);
			break;
		case Normal : 
			source.r =  ChannelBlend_Alpha(ChannelBlend_Normal(target.r,source.r),source.r,opacity);
			source.g =  ChannelBlend_Alpha(ChannelBlend_Normal(target.g,source.g),source.g,opacity);
			source.b =  ChannelBlend_Alpha(ChannelBlend_Normal(target.b,source.b),source.b,opacity);
			break;
		case Lighten : 
			source.r =  ChannelBlend_Alpha(ChannelBlend_Lighten(target.r,source.r),source.r,opacity);
			source.g =  ChannelBlend_Alpha(ChannelBlend_Lighten(target.g,source.g),source.g,opacity);
			source.b =  ChannelBlend_Alpha(ChannelBlend_Lighten(target.b,source.b),source.b,opacity);
			break;
		default :
			break;
	}
	return source;
}

uchar4 __attribute__((kernel)) filter_colorMatrix(uchar4 in,uint32_t x,uint32_t y)
{
	in=applyColorMatrix(in,preMatrix);
	return in;
}

uchar4 __attribute__((kernel)) filter_solomon(uchar4 in,uint32_t x,uint32_t y) {

	in = applyBlendToRGB(in , getPixelForColor(255,r[0],g[0],b[0]),Exclusion,0.30);		
		
	in = applyBlendToRGB(in , getPixelForColor(255,r[1],g[1],b[1]),SoftLight,0.75);		
 		 
 	return in;
}

uchar4 __attribute__((kernel)) filter_xpro(uchar4 in,uint32_t x,uint32_t y) {

	in = applyCurves(in,0,1,1,1);
	
	in = applyColorMatrix(in,postMatrix);
	
	if(!isThumbnail)
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
	
		in = applyBlendToRGB(in , v ,Multiply,0.72);
		
	}
	return in;
}

uchar4 __attribute__((kernel)) filter_1977(uchar4 in,uint32_t x,uint32_t y) {

	in = applyCurves(in,0,1,1,1);
	
	if(!isThumbnail)
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
	
		in = applyBlendToRGB(in , v ,Multiply,0.72);
		
	}
	return in;
}

uchar4 __attribute__((kernel)) filter_apollo(uchar4 in,uint32_t x,uint32_t y) {

	in = applyCurves(in,0,1,1,1);
		
	in = applyColorMatrix(in,postMatrix);
	
	if(!isThumbnail)
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
	
		in = applyBlendToRGB(in , v ,Multiply,0.88);
	}
	return in;
}

uchar4 __attribute__((kernel)) filter_classic(uchar4 in,uint32_t x,uint32_t y) {

	in = applyCurves(in,0,1,1,1);

	return in;
}

uchar4 __attribute__((kernel)) filter_kelvin(uchar4 in,uint32_t x,uint32_t y) {

	in = applyCurves(in,0,1,1,1);

	if(!isThumbnail)
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
	
		in = applyBlendToRGB(in , v ,Multiply,0.66);
	}

	return in;
}

uchar4 __attribute__((kernel)) filter_retro(uchar4 in,uint32_t x,uint32_t y) {

	in = applyCurves(in,-1,0,0,1);

	in = applyBlendToRGB(in , getPixelForColor(255,r[0],g[0],b[0]),Multiply,0.60);
	 
	in = applyColorMatrix(in,preMatrix);
	
	if(!isThumbnail)
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
	
		in = applyBlendToRGB(in , v ,Multiply,0.62);
	}
	
	return in;
}

uchar4 __attribute__((kernel)) filter_brannan(uchar4 in,uint32_t x,uint32_t y) 
{
	
	in = applyCurves(in,0,1,1,1);
	
	if(!isThumbnail)
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
	
		in = applyBlendToRGB(in , v ,Overlay,1);
	}
	
	in = applyColorMatrix(in,preMatrix);
	
	return in;
}

uchar4 __attribute__((kernel)) filter_earlyBird(uchar4 in,uint32_t x,uint32_t y) 
{
	in = applyCurves(in,0,1,1,1);
	
	if(!isThumbnail)
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
	
		in = applyBlendToRGB(in , v ,Overlay,1);
	}
	
	return in;
}

uchar4 __attribute__((kernel)) filter_inkwell(uchar4 in,uint32_t x,uint32_t y) {

	in = applyColorMatrix(in,preMatrix);

	in = applyCurves(in,-1,1,1,1);
	
	in = applyColorMatrix(in,postMatrix);

	return in;
}

uchar4 __attribute__((kernel)) filter_lomofi(uchar4 in,uint32_t x,uint32_t y) {

	in = applyColorMatrix(in,preMatrix);

	in = applyCurves(in,-1,0,0,0);
	
	return in;
}

uchar4 __attribute__((kernel)) filter_nashville(uchar4 in,uint32_t x,uint32_t y) 
{
	in = applyCurves(in,0,1,1,1);
	
	return in;
}

uchar4 __attribute__((kernel)) filter_junglee(uchar4 in,uint32_t x,uint32_t y) 
{
	in = applyCurves(in,0,1,1,1);
	
	return in;
	
}

uchar4 __attribute__((kernel)) filter_gulaal(uchar4 in,uint32_t x,uint32_t y)
{
	in = applyColorMatrix(in,preMatrix);
	
	in = applyBlendToRGB(in , getPixelForColor(255,r[0],g[0],b[0]),Exclusion,1);
	
	if(!isThumbnail)
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
	
		in = applyBlendToRGB(in , v ,Screen,1);
	}
	
	in = applyCurves(in,-1,0,0,0);
	
	return in;
	
}

uchar4 __attribute__((kernel)) filter_chillum(uchar4 in,uint32_t x,uint32_t y)
{
	in = applyCurves(in,0,1,1,1);
	
	if(!isThumbnail)
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
	
		in = applyBlendToRGB(in , v ,Multiply,0.72);
	}
	
	return in;
}

uchar4 __attribute__((kernel)) filter_ghostly(uchar4 in,uint32_t x,uint32_t y)
{
	in = applyColorMatrix(in,preMatrix);
	
	in = applyCurves(in,0,0,0,1);
	
	in = applyColorMatrix(in,postMatrix);
	
	if(!isThumbnail)
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
	
		in = applyBlendToRGB(in , v ,Multiply,0.72);
	}
	
	return in;
	
}

uchar4 __attribute__((kernel)) filter_bgr(uchar4 in,uint32_t x,uint32_t y)
{
	in = applyColorMatrix(in,preMatrix);
	
	in = applyCurves(in,0,0,0,1);
	
	if(!isThumbnail)
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
	
		in = applyBlendToRGB(in , v ,Multiply,0.72);
	}
	
	return in;
	
}

uchar4 __attribute__((kernel)) filter_jalebi(uchar4 in,uint32_t x,uint32_t y)
{

	in = applyCurves(in,0,1,1,1);


	if(!isThumbnail)
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
	
		in = applyBlendToRGB(in , v ,Overlay,0.45);
	}
	
	else
	{
		
		in = applyBlendToRGB(in , getPixelForColor(255,r[0],g[0],b[0]),Multiply,0.70);
	
	}

	return in;
}

uchar4 __attribute__((kernel)) filter_polaroid(uchar4 in,uint32_t x,uint32_t y)
{

	in = applyColorMatrix(in,preMatrix);
	
	in = applyCurves(in,0,1,1,1);
	
	if(!isThumbnail)
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
	
		in = applyBlendToRGB(in , v ,Overlay,1);
	}

	return in;
}

uchar4 __attribute__((kernel)) filter_auto(uchar4 in,uint32_t x,uint32_t y)
{

	in = applyBlendToRGB(in , in ,Overlay,1);
	
	return in;
	
}

uchar4 __attribute__((kernel)) filter_HDR_init(uchar4 in,uint32_t x,uint32_t y)
{
	uchar4 out = rsGetElementAt_uchar4(input2, x, y);
	out = applyColorMatrix(out,preMatrix);
	return out;
}

uchar4 __attribute__((kernel)) filter_HDR_post(uchar4 in,uint32_t x,uint32_t y)
{
	uchar4 out = rsGetElementAt_uchar4(input1, x, y);
	out = applyColorMatrix(out,postMatrix);
	out = applyBlendToRGB(in , out ,Normal,0.45);
	uchar4 out2 = rsGetElementAt_uchar4(input2, x, y);
	in = applyBlendToRGB(in , out2 ,Overlay,0.75);
	in = applyBlendToRGB(in , out ,Overlay,0.75);
	return in;
} 



uchar4 __attribute__((kernel)) filter_sunlitt(uchar4 in,uint32_t x,uint32_t y)
{
 
 	in = applyCurves(in,-1,1,1,1);
	
	if(!isThumbnail)
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
	
		in = applyBlendToRGB(in , v ,Screen,0.7);
	}
 	
	return in;

}

uchar4 __attribute__((kernel)) filter_tirangaa(uchar4 in,uint32_t x,uint32_t y) {

	int minDimen = (imageWidth<imageHeight)?imageWidth:imageHeight;
	int maxDimen = (imageWidth>imageHeight)?imageWidth:imageHeight;
	int offset = (imageWidth==imageHeight)?(minDimen/4):10;
	
	if(in.r == in.b && in.b == in.g && in.r>245)
	{
		in.r-= 10;
		in.g-= 10;
		in.b-= 10;
	}
	
	if(isThumbnail)
	{
		if(x+y<minDimen-offset)
		{
			in = applyBlendToRGB(in , getPixelForColor(255,r[0],g[0],b[0]),Normal,0.6);
		}
		else if(x+y<maxDimen+offset)
		{
			in = applyBlendToRGB(in , getPixelForColor(255,r[1],g[1],b[1]),Normal,0.6);
		}
		else
		{
			in = applyBlendToRGB(in , getPixelForColor(255,r[2],g[2],b[2]),Normal,0.6);
		}
	}
	else
	{
		uchar4 v = rsGetElementAt_uchar4(input1, x, y);
		in = applyBlendToRGB(in , v ,Overlay,1);
	}
	return in;
}

uchar4 __attribute__((kernel)) filter_original(uchar4 in,uint32_t x,uint32_t y) {

	return in;
}
