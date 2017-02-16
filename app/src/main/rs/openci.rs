#pragma version(1)
#pragma rs java_package_name(com.topplusvision.topface)
#include "rs_debug.rsh"
rs_allocation gIn;
rs_allocation gOut;
rs_script gScript;

static int mImageWidth;
static int mImageHeight;
const uchar4 *gPixels;
//旋转角度
int rotation;
//flip的类型1表示镜像，0表示非镜像
int flip;

void init() {
}
void root(const uchar4 *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {
    //270
    if(rotation==270){
        if(flip==1){
            *v_out = gPixels[(mImageHeight-1-y)+(mImageWidth-x-1)*mImageHeight];
        }else{
            *v_out = gPixels[(mImageHeight-1-y)+x*mImageHeight];
        }
    }else{
        //90度
        if(flip==1){
            *v_out = gPixels[y + x*mImageHeight];
        }else{
            *v_out = gPixels[y + (mImageWidth-1-x)*mImageHeight];
        }
    }
	return;
}

void filter() {
    mImageWidth = rsAllocationGetDimX(gOut);
    mImageHeight = rsAllocationGetDimY(gOut);
    rsForEach(gScript, gIn, gOut);
}