

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

float static d(float x) 
{
	if (x <= 0.25) {
		return x * (4 + x*(16*x - 12));
	}
	else {

		return sqrt(x);
	}
}

int static SoftLight(float fg, float bg) 
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

int rSpline[256];
int gSpline[256];
int bSpline[256];
int compositeSpline[256];

int r[3],g[3],b[3];

float preMatrix[20],postMatrix[20];

rs_allocation input1;
rs_allocation input2;


uchar4 static applyColorMatrix(uchar4 in,float matrix[])
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

uchar4 __attribute__((kernel)) filter_colorMatrix(uchar4 in,uint32_t x,uint32_t y)
{
	in=applyColorMatrix(in,preMatrix);
	return in;
}

uchar4 __attribute__((kernel)) filter1(uchar4 in,uint32_t x,uint32_t y) {


	in.r =  ChannelBlend_Alpha(ChannelBlend_Exclusion(r[0],in.r),in.r,0.30);

	in.g =  ChannelBlend_Alpha(ChannelBlend_Exclusion(g[0],in.g),in.g,0.30);

	in.b =  ChannelBlend_Alpha(ChannelBlend_Exclusion(b[0],in.b),in.b,0.30);

	in.r =  ChannelBlend_Alpha(SoftLight(r[1],in.r),in.r,0.75);

	in.g =  ChannelBlend_Alpha(SoftLight(g[1],in.g),in.g,0.75);

	in.b =  ChannelBlend_Alpha(SoftLight(b[1],in.b),in.b,0.75);


	return in;
}

uchar4 __attribute__((kernel)) filter_1977_or_xpro(uchar4 in,uint32_t x,uint32_t y) {

	in.r=rSpline[in.r];

	in.g=gSpline[in.g];

	in.b=bSpline[in.b];
	
	in = applyColorMatrix(in,postMatrix);

	return in;
}

uchar4 __attribute__((kernel)) filter_classic(uchar4 in,uint32_t x,uint32_t y) {

	in.r=rSpline[in.r];

	in.g=gSpline[in.g];

	in.b=bSpline[in.b];


	in.r =  ChannelBlend_Alpha(ChannelBlend_Multiply(r[0],in.r),in.r,0.50);

	in.g =  ChannelBlend_Alpha(ChannelBlend_Multiply(g[0],in.g),in.g,0.50);

	in.b =  ChannelBlend_Alpha(ChannelBlend_Multiply(b[0],in.b),in.b,0.50);

	in.r =  ChannelBlend_Exclusion(r[1],in.r);

	in.g =  ChannelBlend_Exclusion(g[1],in.g);

	in.b =  ChannelBlend_Exclusion(b[1],in.b);

	return in;
}

uchar4 __attribute__((kernel)) filter_kelvin(uchar4 in,uint32_t x,uint32_t y) {

	in.r=rSpline[in.r];

	in.g=gSpline[in.g];

	in.b=bSpline[in.b];


	in.r =  ChannelBlend_Alpha(ChannelBlend_Overlay(r[0],in.r),in.r,0.30);

	in.g =  ChannelBlend_Alpha(ChannelBlend_Overlay(g[0],in.g),in.g,0.30);

	in.b =  ChannelBlend_Alpha(ChannelBlend_Overlay(b[0],in.b),in.b,0.30);


	return in;
}

uchar4 __attribute__((kernel)) filter_retro(uchar4 in,uint32_t x,uint32_t y) {

	in.r=compositeSpline[in.r];

	in.g=compositeSpline[in.g];

	in.b=compositeSpline[in.b];

	in.b=bSpline[in.b];


	in.r =  ChannelBlend_Alpha(ChannelBlend_Multiply(r[0],in.r),in.r,0.60);

	in.g =  ChannelBlend_Alpha(ChannelBlend_Multiply(g[0],in.g),in.g,0.60);

	in.b =  ChannelBlend_Alpha(ChannelBlend_Multiply(b[0],in.b),in.b,0.60);
	 
	in = applyColorMatrix(in,preMatrix);

	return in;
}

uchar4 __attribute__((kernel)) filter_brannan(uchar4 in,uint32_t x,uint32_t y) 
{
	
	in = applyColorMatrix(in,preMatrix);
	
	in.r =  ChannelBlend_Alpha(ChannelBlend_Overlay(r[0],in.r),in.r,0.70);

	in.g =  ChannelBlend_Alpha(ChannelBlend_Overlay(g[0],in.g),in.g,0.70);

	in.b =  ChannelBlend_Alpha(ChannelBlend_Overlay(b[0],in.b),in.b,0.70);

	in.b = bSpline[in.b];
	
	in = applyColorMatrix(in,postMatrix);

	return in;
}

uchar4 __attribute__((kernel)) filter_earlyBird(uchar4 in,uint32_t x,uint32_t y) 
{
	in = applyColorMatrix(in,preMatrix);
	
	in.r = ChannelBlend_Multiply(r[0],in.r);

	in.g = ChannelBlend_Multiply(g[0],in.g);

	in.b = ChannelBlend_Multiply(b[0],in.b);

	return in;
}

uchar4 __attribute__((kernel)) filter_inkwell(uchar4 in,uint32_t x,uint32_t y) {

	in = applyColorMatrix(in,preMatrix);

	in.r=compositeSpline[in.r];

	in.g=compositeSpline[in.g];

	in.b=compositeSpline[in.b];
	
	in = applyColorMatrix(in,postMatrix);

	return in;
}

uchar4 __attribute__((kernel)) filter_lomofi(uchar4 in,uint32_t x,uint32_t y) {

	in = applyColorMatrix(in,preMatrix);

	in.r=compositeSpline[in.r];

	in.g=compositeSpline[in.g];

	in.b=compositeSpline[in.b];
	
	return in;
}

uchar4 __attribute__((kernel)) filter_nashville(uchar4 in,uint32_t x,uint32_t y) 
{
	in.g = gSpline[in.g];
	
	in.b = bSpline[in.b];
	
	in.r =  ChannelBlend_Alpha(ChannelBlend_Overlay(r[0],in.r),in.r,0.50);

	in.g =  ChannelBlend_Alpha(ChannelBlend_Overlay(g[0],in.g),in.g,0.50);

	in.b =  ChannelBlend_Alpha(ChannelBlend_Overlay(b[0],in.b),in.b,0.50);
	
	in = applyColorMatrix(in,postMatrix);
	
	in.r =  ChannelBlend_Alpha(ChannelBlend_Multiply(r[1],in.r),in.r,0.70);

	in.g =  ChannelBlend_Alpha(ChannelBlend_Multiply(g[1],in.g),in.g,0.70);

	in.b =  ChannelBlend_Alpha(ChannelBlend_Multiply(b[1],in.b),in.b,0.70);
	
	return in;
}

