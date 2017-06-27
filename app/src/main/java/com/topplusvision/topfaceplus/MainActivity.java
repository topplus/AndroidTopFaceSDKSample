package com.topplusvision.topfaceplus;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicYuvToRGB;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.topplusvision.topface.ScriptC_openci;
import com.topplusvision.topface.TopFace;

import java.io.IOException;


public class MainActivity extends Activity implements SurfaceHolder.Callback,
        Camera.PreviewCallback, View.OnClickListener {
    ScriptIntrinsicYuvToRGB y2r;
    Paint paint;
    private Camera mCamera;
    private SurfaceTexture mSurfaceTexture = null;
    private byte[] mBuffer = null;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private int mTextureID = 0;
    private SurfaceView mSurfaceView = null;
    private RenderScript mRs;
    private Bitmap mFrameBitmap;
    private Bitmap mFrameBitmapRotate;
    private Allocation mInYuvAlloc;
    private Allocation mOutBmpAlloc;
    private Allocation mOutB2Alloc;
    private ScriptC_openci mScriptOpenCI;
    private CameraHelper mCameraHelper;
    private int mImageRotation;
    private boolean mFlipHorital;
    private int mCameraId = 1;
    private int mScreenWidth;
    private int mScreenHeight;
    private Rect mTargetRect;
    private Paint mTextPaint;
    //debug
    private TextView mDebug;
    private boolean isDebug;

    protected void onCreate(Bundle savedInstanceState) {
        paint = new Paint();
        super.onCreate(savedInstanceState);
        //1.屏幕宽高
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;
        //2.设置预览控件
        setContentView(R.layout.act_layout);
        mSurfaceView = (SurfaceView) findViewById(R.id.sv);
        mSurfaceView.getHolder().addCallback(this);
        mDebug = (TextView) findViewById(R.id.tv);
        mDebug.setOnClickListener(this);
        isDebug = false;
        //3.初始化RenderScript
        mRs = RenderScript.create(this);
        y2r = ScriptIntrinsicYuvToRGB.create(mRs, Element.U8_4(mRs));
        mTextureID = 0;
        this.mSurfaceTexture = new SurfaceTexture(this.mTextureID);
        //4.初始化相机
        mCameraHelper = CameraHelper.getInstance(this);
        mCameraId = mCameraHelper.getFrontCameraId();
        mCamera = mCameraHelper.openCamera(mCameraId);
//        mCameraHelper.setMinParameters(mCamera);
        mCameraHelper.setMediumParameters(mCamera, this);
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Camera.Parameters parameters = mCamera.getParameters();
        mPreviewWidth = parameters.getPreviewSize().width;
        mPreviewHeight = parameters.getPreviewSize().height;
        mImageRotation = mCameraHelper.getImageRotation(mCameraId);
        mFlipHorital = (mCameraId == mCameraHelper.getFrontCameraId());
        mFrameBitmap = Bitmap.createBitmap(mPreviewWidth, mPreviewHeight, Bitmap.Config.ARGB_8888);

        mFrameBitmapRotate = Bitmap.createBitmap(
                Math.min(mPreviewWidth, mPreviewHeight)
                , Math.max(mPreviewWidth, mPreviewHeight)
                , Bitmap.Config.ARGB_8888);

        int bufferSize = mPreviewWidth * mPreviewHeight;
        bufferSize = bufferSize * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
        mBuffer = new byte[bufferSize];

        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.addCallbackBuffer(mBuffer);
            mCamera.setPreviewCallbackWithBuffer(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //2.初始化TOPFaceSDK
        float focusLength = mCameraHelper.getFocalLength(mCameraId);
        int result = TopFace.initWithFocus(this, focusLength);
        if (result < 0) {
            Toast.makeText(this, "初始化失败，请重试！", Toast.LENGTH_SHORT).show();
        }
        //开始预览
        mCameraHelper.startPreview(mCamera, mCameraId);
        //计算rect
        float bmpRatio = mFrameBitmapRotate.getWidth() / (float) mFrameBitmapRotate.getHeight();
        float sRatio = mScreenWidth / (float) mScreenHeight;
        if (bmpRatio > sRatio) {
            //宽图
            int margin = (int) ((bmpRatio * mScreenHeight - mScreenWidth) / 2);
            mTargetRect = new Rect(-margin, 0, margin + mScreenWidth, mScreenHeight);
        } else if (bmpRatio < sRatio) {
            int margin = (int) (((mScreenWidth / bmpRatio) - mScreenHeight) / 2.f);
            mTargetRect = new Rect(0, -margin, mScreenWidth, mScreenHeight + margin);
        } else {
            mTargetRect = new Rect(0, 0, mScreenWidth, mScreenHeight);
        }
        //初始化文字的画笔
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.RED);
        mTextPaint.setTextSize(60.f);
        mTextPaint.setStrokeWidth(2.f);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv) {
            isDebug = !isDebug;
            if (isDebug) {
                mDebug.setBackgroundResource(R.drawable.shape_bg_normal);
            } else {
                mDebug.setBackgroundResource(R.drawable.shape_bg_pressed);
            }
        }

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        decodeYUV420SPAndRotate(data);
        mCamera.addCallbackBuffer(mBuffer);
        TopFace.detectBitmap(mFrameBitmapRotate);
        Canvas canvas = this.mSurfaceView.getHolder().lockCanvas();
        //绘制大小
        if (canvas != null) {
            canvas.drawBitmap(this.mFrameBitmapRotate, null, mTargetRect, null);
            this.mSurfaceView.getHolder().unlockCanvasAndPost(canvas);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (0 != TopFace.getVerifyCode()) {
            Toast.makeText(this, "授权失败，请检查授权码！", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //1.绘制背景
        Canvas canvas = holder.lockCanvas();
        canvas.drawARGB(255, 255, 255, 255);
        holder.unlockCanvasAndPost(canvas);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //1.关闭相机
        mCameraHelper.closeCamera(mCamera);
        mSurfaceTexture.release();
        mCamera = null;
        //2.销毁rs
        mRs.destroy();
    }


    private void decodeYUV420SPAndRotate(byte[] yuv420sp) {
        //1.将yuv转为rgb
        if (mInYuvAlloc == null) {
            mInYuvAlloc = Allocation.createSized(mRs, Element.U8(mRs), yuv420sp.length);
        }
        mInYuvAlloc.copyFrom(yuv420sp);
        if (this.mOutBmpAlloc == null) {
            this.mOutBmpAlloc = Allocation.createFromBitmap(this.mRs, this.mFrameBitmap);
        }
        y2r.setInput(mInYuvAlloc);
        y2r.forEach(mOutBmpAlloc);

        //2.旋转图像
        if (this.mOutB2Alloc == null) {
            this.mOutB2Alloc = Allocation.createFromBitmap(this.mRs, this.mFrameBitmapRotate,
                    Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        }
        if (this.mScriptOpenCI == null) {
            this.mScriptOpenCI = new ScriptC_openci(this.mRs, getResources(), R.raw.openci);
            this.mScriptOpenCI.set_rotation(mImageRotation);
            this.mScriptOpenCI.set_flip(mFlipHorital ? 1 : 0);
        }
        this.mScriptOpenCI.bind_gPixels(this.mOutBmpAlloc);
        this.mScriptOpenCI.set_gIn(this.mOutB2Alloc);
        this.mScriptOpenCI.set_gOut(this.mOutB2Alloc);
        this.mScriptOpenCI.set_gScript(this.mScriptOpenCI);
        this.mScriptOpenCI.invoke_filter();
        this.mOutB2Alloc.copyTo(this.mFrameBitmapRotate);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }
}
