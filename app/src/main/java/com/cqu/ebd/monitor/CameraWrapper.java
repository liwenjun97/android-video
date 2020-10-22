package com.cqu.ebd.monitor;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceView;

import com.cqu.ebd.utils.Logger1;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CameraWrapper {

    public static final int SRC_VIDEO_HEIGHT = 720;
    public static final int SRC_VIDEO_WIDTH = 1280;
    private Activity mContext;

    private static final String TAG = "CameraWrapper";
    private static final boolean DEBUG = true;    // TODO set false on release
    private static CameraWrapper mCameraWrapper;

    private Camera mCamera;
    private Camera.Parameters mCameraParamters;
    private boolean mIsPreviewing = false;
    private CameraPreviewCallback mCameraPreviewCallback;
    private int openCameraId = -1;
    boolean startRecordingFlag = false;
    boolean mIsInitSuccess = false;

    //是否竖屏
    private boolean isPort;

    private CameraWrapper() {
    }

    public static CameraWrapper getInstance() {
        if (mCameraWrapper == null) {
            synchronized (CameraWrapper.class) {
                if (mCameraWrapper == null) {
                    mCameraWrapper = new CameraWrapper();
                }
            }
        }
        return mCameraWrapper;
    }

    public void setScreenPort(boolean isPort) {
        this.isPort = isPort;
    }

    public void setContext(Activity context){
        mContext = context;
    }
    public boolean isPort() {
        return isPort;
    }

    public CameraWrapper setCameraId(int cameraId) {
        this.openCameraId = cameraId;
        return this;
    }

    public boolean doOpenCamera(SurfaceView surfaceView) {
        Logger1.i(TAG, "Camera open.... openCameraId=%s", openCameraId);
        if (openCameraId == -1)
            throw new RuntimeException("openCameraId is -1");

        try {
            mCamera = Camera.open(openCameraId);
        } catch (Exception e) {
            e.printStackTrace();
            Logger1.i(TAG, "doOpenCamera: open fail %s %s", openCameraId, e);
            return false;
        }

        try {
            mCamera.setPreviewDisplay(surfaceView.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.setErrorCallback(new Camera.ErrorCallback() {
            @Override
            public void onError(int error, Camera camera) {
                Logger1.e(TAG, "onError: %s", error);
            }
        });
        Logger1.i(TAG, "Camera open over....");
        return initCamera();
    }

    /**
     * 开始预览
     */
    public void startPreview() {
        if (!mIsInitSuccess) {
            Logger1.e(TAG, "camera InitSuccess is false");
            return;
        }
        if (!mIsPreviewing) {
//            MediaMuxerRunnable.reStartMuxer();

          /*  String newVideoPath = Utils.getVideoFilePath();
            startRecording(newVideoPath);*/
            mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
            mCamera.startPreview();

            mIsPreviewing = true;

            Logger1.d(TAG, "camera startPreview");
        } else {
            Logger1.e(TAG, "camera mIsPreviewing is true");
        }
    }

    /**
     * 暂停预览
     */
    public void pausePreview() {
        if (!mIsInitSuccess) {
            Logger1.e(TAG, "camera InitSuccess is false");
            return;
        }
        if (mIsPreviewing) {
//            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.stopPreview();
            mIsPreviewing = false;
            CameraWrapper.getInstance().stopRecording();

            Logger1.d(TAG, "camera stopPreview");
        } else {
            Logger1.d(TAG, "camera mIsPreviewing is false");
        }
    }

    public boolean isPreview() {
        return mIsPreviewing;
    }

    /**
     * 释放camera 资源
     */
    public void releaseCamera() {
        Logger1.i(TAG, "doStopCamera");
        if (this.mCamera != null) {
            stopRecording();
            MuxerManager.getInstance().releaseManager();
            this.mCamera.setPreviewCallback(null);
            this.mCamera.stopPreview();
            this.mIsPreviewing = false;
            this.mCamera.release();
            this.mCamera = null;

            startRecordingFlag = false;
        }
    }


    private boolean initCamera() {
        if (this.mCamera != null) {
            this.mCameraParamters = this.mCamera.getParameters();
            List<Camera.Size> sizes = mCameraParamters.getSupportedPreviewSizes();
            if (DEBUG) {
                for (Camera.Size size : this.mCameraParamters.getSupportedPreviewSizes()) {
                    Logger1.d(TAG, "support preview width=" + size.width + "," + size.height);
                }
            }
            this.mCameraParamters.setPreviewFormat(ImageFormat.NV21);
            this.mCamera.setDisplayOrientation(getCameraDisplayOrientation(openCameraId,mCamera));
            this.mCameraParamters.setPreviewSize(SRC_VIDEO_WIDTH, SRC_VIDEO_HEIGHT);//1280*720
            this.mCamera.setParameters(this.mCameraParamters);

            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size sz = parameters.getPreviewSize();
            Logger1.i(TAG, "camera width : " + sz.width + "  height  : " + sz.height);
            int bufSize = sz.width * sz.height * 3 / 2;
            mCamera.addCallbackBuffer(new byte[bufSize]);

            if (DEBUG) {

                Logger1.d(TAG, "getMaxNumDetectedFaces =" + this.mCameraParamters.getSupportedVideoSizes());
                Logger1.d(TAG, "getMaxNumDetectedFaces =" + this.mCameraParamters.getMaxNumDetectedFaces());
                Logger1.d(TAG, "getMaxNumFocusAreas =" + this.mCameraParamters.getMaxNumFocusAreas());
                Logger1.d(TAG, "getMaxNumMeteringAreas =" + this.mCameraParamters.getMaxNumMeteringAreas());
                int[] range = new int[2];
                this.mCameraParamters.getPreviewFpsRange(range);
                Logger1.d(TAG, "getPreviewFpsRange =" + Arrays.toString(range));
                List<int[]> fps = this.mCameraParamters.getSupportedPreviewFpsRange();
                if (fps != null)
                    for (int[] f : fps) {
                        Logger1.i(TAG, "initCamera: fps %s", Arrays.toString(f));
                    }

                Logger1.d(TAG, "getSupportedPreviewFormats =" + this.mCameraParamters.getSupportedPreviewFormats());
                Logger1.d(TAG, "getSupportedSceneModes =" + this.mCameraParamters.getSupportedSceneModes());

            }

            mCameraPreviewCallback = new CameraPreviewCallback();

            mIsInitSuccess = true;
            return true;
        }
        return false;
    }

    //预览旋转方向
    private int getCameraDisplayOrientation(int cameraId,Camera camera) {
        android.hardware.Camera.CameraInfo cameraInfo =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, cameraInfo);
        int rotation = mContext.getWindowManager().getDefaultDisplay().getRotation();
        Logger1.i(TAG,"--->"+rotation);
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
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        Logger1.i(TAG,"--->result"+result);
        return result;
    }
    /**
     * 开始录制
     * create at 2017/3/22 17:10
     */
    public void startRecording(String filePath) {
        startRecordingFlag = true;
        if (DEBUG)
            Logger1.i(TAG, "startRecording: %s %s", startRecordingFlag, filePath);
        MuxerManager.getInstance().reStartMuxer(filePath);
        mCamera.startPreview();
    }

    /**
     * 暂停录制
     * create at 2017/3/22 17:10
     */
    public void pauseRecording() {
        startRecordingFlag = false;
        MuxerManager.getInstance().pauseMuxer();
    }

    public void resumeRecording() {
        startRecordingFlag = true;
        MuxerManager.getInstance().resumeMuxer();
    }

    /**
     * 结束录制
     * create at 2017/3/22 17:10
     */
    public void stopRecording() {
        startRecordingFlag = false;
        mCamera.stopPreview();
        Logger1.i(TAG, "stopRecording: %s", startRecordingFlag);

        MuxerManager.getInstance().stopMuxer();
    }

    class CameraPreviewCallback implements Camera.PreviewCallback {

        private CameraPreviewCallback() {
            //startRecording();
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
//            if (DEBUG)
//                Logger1.i(TAG, "onPreviewFrame: %s t=%s", data.length, Thread.currentThread());
            //当启动录制的视频把视频源数据加入编码中
            MuxerManager.getInstance().addVideoFrameData(data);
            camera.addCallbackBuffer(data);

        }
    }

}
