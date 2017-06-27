package com.topplusvision.topfaceplus;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import com.topplusvision.topfaceplus.exif.ExifInterface;
import com.topplusvision.topfaceplus.exif.ExifTag;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CameraHelper {
    private final static String FOCALLENGTH = "focalLength";
    private final static String CAMERA_ID_KEY = "cameraId";
    private final static int DEFAULT_FOCAL_LENGTH = 24;
    private static CameraHelper gCameraHelper;
    private static int mPictureWidth = 1280;
    private static int mPictureHeigth = 720;
    private final Map<Integer, Float> mFocalLengths = new HashMap<Integer, Float>();
    private int mCameraCount = 0;
    private int mFrontCameraId = -1;
    private int mBackCameraId = -1;
    private SharedPreferences mShared;
    private Context mContext;
    private int mPictureCameraId;
    private Camera.PictureCallback mJpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            int cameraId = mPictureCameraId;
            ExifInterface exifInterface = new ExifInterface();
            try {
                exifInterface.readExif(data, ExifInterface.Options.OPTION_IFD_EXIF);
                ExifTag focalTag = exifInterface.getTag(ExifInterface.TAG_FOCAL_LENGTH_IN_35_MM_FILE);
                focalTag.getIfd();
                int focalLength = focalTag.getValueAsInt(0);
                if (focalLength < 15 || focalLength > 40) {
                    focalLength = DEFAULT_FOCAL_LENGTH;
                }
                putFocalLength(cameraId, focalLength);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                putFocalLength(cameraId, DEFAULT_FOCAL_LENGTH);
            }
            camera.startPreview();
        }
    };

    private CameraHelper(Context context) {
        mContext = context;
        mCameraCount = Camera.getNumberOfCameras();
        mShared = context.getSharedPreferences("camera_info", 0);
        for (int i = 0; i < mCameraCount; i++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mFrontCameraId = i;
            }
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mBackCameraId = i;
            }
            float focalLengh = mShared.getFloat(FOCALLENGTH + i, -1.f);
            mFocalLengths.put(i, focalLengh);
        }
        if (mFrontCameraId == -1) {
            mFrontCameraId = 0;
        }
    }

    /**
     * 获取CameraHelper的实例
     *
     * @param context
     * @return
     */
    public static CameraHelper getInstance(Context context) {
        if (gCameraHelper == null) {
            synchronized (CameraHelper.class) {
                if (gCameraHelper == null) {
                    gCameraHelper = new CameraHelper(context);
                }
            }
        }
        return gCameraHelper;
    }

    /**
     * 获取当前设备的camera数量
     *
     * @return camera数量
     */
    public int getCameraCount() {
        return mCameraCount;
    }

    private void putFocalLength(int id, float focalLength) {
        synchronized (mFocalLengths) {
            mFocalLengths.put(id, focalLength);
        }
        SharedPreferences.Editor editor = mShared.edit();
        editor.putFloat(FOCALLENGTH + id, focalLength);
        editor.apply();
    }

    public float getFocalLength(int id) {
        return mShared.getFloat(FOCALLENGTH + id, DEFAULT_FOCAL_LENGTH);
    }

    //不好阻塞的方式
    public float getFocalLengthAsync(int cameraId) {
        float focus = -1;
        synchronized (mFocalLengths) {
            if (mFocalLengths.containsKey(cameraId)) {
                focus = mFocalLengths.get(cameraId);
            }
        }
        return focus;
    }

    /**
     * 获取前置摄像头的cameraId
     *
     * @return 前置cameraId
     */
    public int getFrontCameraId() {
        return mFrontCameraId;
    }

    /**
     * 获取后置摄像头的cameraId
     *
     * @return 后置cameraId
     */
    public int getBackCameraId() {
        return mBackCameraId;
    }

    /**
     * 打开对应cameraId的摄像头
     *
     * @param cameraId 摄像头的cameraId
     * @return 打开的camera
     */
    public Camera openCamera(int cameraId) {
        Camera camera = Camera.open(cameraId);
        Camera.Parameters parameters = camera.getParameters();
        List<String> list = parameters.getSupportedFocusModes();
        if (list.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        int min = parameters.getMinExposureCompensation();
        int max = parameters.getMaxExposureCompensation();
        if (min < 1 && max >= 1) {
            parameters.setExposureCompensation(1);
        }
        // parameters.setPreviewFpsRange(30000, 60000);
        int displayRotation = getCameraDisplayOrientation(cameraId);
        camera.setDisplayOrientation(displayRotation);
        camera.setParameters(parameters);
        return camera;
    }

    /**
     * 设置一些摄像机相关的参数
     *
     * @param camera
     */
    public void setOptmlParameters(Camera camera, int rotation) {
        if (camera != null) {
            Camera.Parameters parameters;
            parameters = camera.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            Camera.Size optimalSize = getYUVPreviewSize(sizes, rotation);
//            Camera.Size optimalSize = getOptimalPreviewSize(sizes, previewWidth, previewHeight);
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            sizes = parameters.getSupportedPictureSizes();
            Camera.Size pictureSize = getOptimalPreviewSize(sizes, mPictureWidth, mPictureHeigth);
            mPictureWidth = pictureSize.width;
            mPictureHeigth = pictureSize.height;
            parameters.setPictureSize(mPictureWidth, mPictureHeigth);
            parameters.setPreviewFormat(ImageFormat.NV21);
            camera.setParameters(parameters);
        }
    }


    /**
     * 设置一些摄像机相关的参数
     *
     * @param camera
     */
    public void setMinParameters(Camera camera) {
        if (camera != null) {
            Camera.Parameters parameters;
            parameters = camera.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            Camera.Size optimalSize = getMinPreviewSize(sizes);
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            sizes = parameters.getSupportedPictureSizes();
            Camera.Size pictureSize = getOptimalPreviewSize(sizes, mPictureWidth, mPictureHeigth);
            mPictureWidth = pictureSize.width;
            mPictureHeigth = pictureSize.height;
            parameters.setPictureSize(mPictureWidth, mPictureHeigth);
            parameters.setPreviewFormat(ImageFormat.NV21);
            camera.setParameters(parameters);
        }
    }

    /**
     * 设置一些摄像机相关的参数
     *
     * @param camera
     */
    public void setMediumParameters(Camera camera, Context context) {
        if (camera != null) {
            Camera.Parameters parameters;
            parameters = camera.getParameters();
            Camera.Size size = parameters.getPreferredPreviewSizeForVideo();
            parameters.setPreviewSize(size.width, size.height);
            List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
            Camera.Size pictureSize = getOptimalPreviewSize(sizes, mPictureWidth, mPictureHeigth);
            mPictureWidth = pictureSize.width;
            mPictureHeigth = pictureSize.height;
            parameters.setPictureSize(mPictureWidth, mPictureHeigth);
            parameters.setPreviewFormat(ImageFormat.NV21);
            camera.setParameters(parameters);
        }
    }

    /**
     * 开始摄像头预览,使用这个函数来开始预览，
     * 才可以得到相机的等效焦距
     *
     * @param camera   相机
     * @param cameraId 相机id
     */
    public void startPreview(Camera camera, int cameraId) {
        if (camera != null) {
            camera.startPreview();
            mPictureCameraId = cameraId;
            if (mFocalLengths.get(cameraId) < 0) {
                camera.takePicture(null, null, mJpegCallback);
            }
        }
    }

    /**
     * 关闭对应的camera
     *
     * @param camera
     */
    public void closeCamera(Camera camera) {
        camera.setPreviewCallbackWithBuffer(null);
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    /**
     * 获取相机预览时需要设置的displayOrientation，以便
     * 正面显示图片
     *
     * @param cameraId 对应的cameraId
     * @return 需要旋转的度数
     */
    private int getCameraDisplayOrientation(final int cameraId) {
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /**
     * 获取相机对应的图片，需要旋转多少度才能回到正常
     *
     * @param cameraId 相机的cameraId
     * @return 图片需要顺时针旋转的度数
     */
    public int getImageRotation(final int cameraId) {
        int rotation = getCameraDisplayOrientation(cameraId);
        if (cameraId == mFrontCameraId) {
            rotation = (360 - rotation) % 360;
        }
        return rotation;
    }

    //像素最小并且大于3W
    private Camera.Size getMinPreviewSize(List<Camera.Size> sizes) {
        int min = Integer.MAX_VALUE;
        Camera.Size minus = null;
        for (Camera.Size size : sizes) {
            int result = size.width * size.height;
            if (result < min && result > 50000) {
                min = result;
                minus = size;
            }
        }
        return minus;
    }


    //width整除 3 height整除 2
    private Camera.Size getYUVPreviewSize(List<Camera.Size> sizes, int rotation) {
        int min = Integer.MAX_VALUE;
        Camera.Size minus = null;
        boolean flip = rotation % 90 == 0;
        for (Camera.Size size : sizes) {
            int result = size.width * size.height;
            if (!flip) {
                if (size.width % 2 == 0 && size.height % 3 == 0) {
                    if (result < min && result > 50000) {
                        min = result;
                        minus = size;
                    }
                }
            } else {
                if (size.width % 3 == 0 && size.height % 2 == 0) {
                    if (result < min && result > 50000) {
                        min = result;
                        minus = size;
                    }
                }
            }
        }
        return minus;
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;

        if (sizes == null) {
            return null;
        }

        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;

            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }


        return optimalSize;
    }
}
