
package com.example.testdemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback,
        OnTouchListener, AutoFocusCallback, ScaleGestureDetector.OnScaleGestureListener {
    private final String TAG = "Preview";
    public static final String RECORDING_HINT = "recording-hint";
    public static final String FALSE = "false";
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int mCameraID;
    private boolean isFocusSucc = true;
    private Context mContext;
    private ScaleGestureDetector mScale;
    private float beginSpan;
    private boolean sufaceCreate = false;
    public static final int DEFAULT_CAPTURE_PIXELS = 1280 * 720;
    private int mPreviewWidth;
    private int mPreviewHeight;

    public CameraPreview(Context context) {
        super(context);
        init(context);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
    }

    /**
     * 设置当前使用的照相机
     * 
     * @param camera
     * @param cameraID
     */
    public void setCamera(Camera camera, int cameraID) {
        mCamera = camera;
        mCameraID = cameraID;
        if (mCamera != null) {
            setCameraDisplayOrientation(0, mCamera);
            Parameters cameraParam = mCamera.getParameters();
            setupCaptureParams(cameraParam);
            mSurfaceView.setOnTouchListener(this);
            mScale = new ScaleGestureDetector(mContext, this);
            if (CameraUtil.isFocusAreaSupported(cameraParam)) {
                cameraParam.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            requestLayout();
        }
    }

    /**
     * 设置相机的方向
     * 
     * @param cameraId
     * @param camera
     */
    public void setCameraDisplayOrientation(int cameraId,
            android.hardware.Camera camera) {
        int result = getDisplayOrientation(cameraId);
        camera.setDisplayOrientation(result);
    }

    /**
     * 获取当前显示的方向
     * 
     * @param cameraId
     * @return
     */
    private int getDisplayOrientation(int cameraId) {
        int degrees = CameraUtil.getDisplayRotation((Activity) mContext);
        return CameraUtil.getDisplayOrientation(degrees, cameraId);

    }

    /**
     * 切换照相机
     * 
     * @param camera
     */
    public void switchCamera(Camera camera) {
        setCamera(camera, mCameraID);
        try {
            camera.setPreviewDisplay(mHolder);
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
        Camera.Parameters parameters = camera.getParameters();
        requestLayout();

        camera.setParameters(parameters);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 测量组件大小
        final int width = resolveSize(getSuggestedMinimumWidth(),
                widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(),
                heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // 用来控制surface的布局
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = mPreviewHeight;
            int previewHeight = mPreviewWidth;

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height
                        / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width
                        / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2, width,
                        (height + scaledChildHeight) / 2);
            }

        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG, "surfaceCreated");
        // 开启预览图片
        try {
            if (mCamera != null) {
                holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
        sufaceCreate = true;
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(TAG, "surfaceDestroyed");
        // 停止照相功能
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        sufaceCreate = false;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.e(TAG, "surfaceChanged");
        // surface 改变的时候重新设置照片的属性参数与尺寸
        // if (mCamera != null) {
        // Camera.Parameters parameters = mCamera.getParameters();
        // parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        // requestLayout();
        // mCamera.setParameters(parameters);
        // mCamera.startPreview();
        // }

    }

    public Size getmPreviewSize() {
        return mCamera.getParameters().getPreviewSize();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && sufaceCreate) {
            focusOnTouch(event);
            return true;
        }
        if (mScale != null) {
            mScale.onTouchEvent(event);
        }
        return false;
    }

    /**
     * 点击预览图位置时做处理，一是移动焦点，二是设置光照
     * 
     * @param event
     */
    protected void focusOnTouch(MotionEvent event) {
        if (mCamera != null) {
            mCamera.cancelAutoFocus();
            Parameters parameters = mCamera.getParameters();
            if (CameraUtil.isFocusAreaSupported(parameters)) {
                parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
                initializeFocusAreas(Math.round(event.getX()),
                        Math.round(event.getY()), parameters);
            }

            if (CameraUtil.isMeteringAreaSupported(parameters)) {
                initializeMeteringAreas(Math.round(event.getX()),
                        Math.round(event.getY()), parameters);
            }
            mCamera.setParameters(parameters);

            // 三星i9268 出现错误
            mCamera.autoFocus(this);
        }
    }

    /**
     * 把焦点设置到触摸位置附近的矩形区域
     * 
     * @param x
     * @param y
     * @param parameters
     */
    private void initializeFocusAreas(int x, int y, Parameters parameters) {
        // 有时候会报数字转化错误。部分手机直接获取会报错。其中小米与nexus 4
        // MessageQueue-JNIjava.lang.NumberFormatException: Invalid
        // int: " 0"
        List<Camera.Area> listFocusArea = null;
        try {
            listFocusArea = parameters.getFocusAreas();
        } catch (NumberFormatException e) {
            Log.i(TAG, "some devices parameters getfocusareas exception");
            e.printStackTrace();
        }

        Rect rect = calculateTapArea(100, 100, 1f, x, y);
        if (listFocusArea == null) {
            listFocusArea = new ArrayList<Camera.Area>();
            Area touchArea = new Camera.Area(rect, 1);
            listFocusArea.add(touchArea);
        } else {
            Area touchArea = listFocusArea.get(0);
            touchArea.rect = rect;
        }
        parameters.setFocusAreas(listFocusArea);
    }

    /**
     * 设置感光区域
     * 
     * @param x
     * @param y
     * @param parameters
     */
    private void initializeMeteringAreas(int x, int y, Parameters parameters) {
        List<Camera.Area> listMeterArea = new ArrayList<Camera.Area>();
        Rect rect = calculateTapArea(100, 100, 1.5f, x, y);
        Area touchArea = new Camera.Area(rect, 1);
        listMeterArea.add(touchArea);
        parameters.setMeteringAreas(listMeterArea);
    }

    /**
     * 计算焦点实现的位置，主要为焦点的矩形区域位置
     * 
     * @param focusWidth
     * @param focusHeight
     * @param areaMultiple
     * @param x
     * @param y
     * @return
     */
    private Rect calculateTapArea(int focusWidth, int focusHeight,
            float areaMultiple, int x, int y) {

        // 获取sufview的大小尺寸，不一定是全屏，
        int sufWidth = mSurfaceView.getWidth();
        int sufHeight = mSurfaceView.getHeight();
        // 计算出想要设置的区域的长宽
        int areaWidth = (int) (focusWidth * areaMultiple);
        int areaHeight = (int) (focusHeight * areaMultiple);
        // 计算显示区域左边在view中的像素位置。这个位置不是绝对位置，是相对sufacevice的距离。
        int left = CameraUtil.clamp(x - areaWidth / 2, 0, sufWidth - areaWidth);
        int right = left + areaWidth;
        int top = CameraUtil.clamp(y - areaHeight / 2, 0, sufHeight
                - areaHeight);
        int bottom = top + areaHeight;
        // 由于要把sufaceview分割成一个长宽都是-1000，1000的范围，满足照相设置区域功能。计算出当前sufaceview的像素比例
        float widthScale = 2000 / (float) sufWidth;
        float heightScale = 2000 / (float) sufHeight;
        // 分别计算出转化成-1000，1000显示的区域范围下，区域所占位置的大小
        int convertLeft = CameraUtil.clamp(Math.round(left * widthScale), 0,
                2000) - 1000;
        int convertRight = CameraUtil.clamp(Math.round(right * widthScale), 0,
                2000) - 1000;
        int convertTop = CameraUtil.clamp(Math.round(top * heightScale), 0,
                2000) - 1000;
        int convertBottom = CameraUtil.clamp(Math.round(bottom * heightScale),
                0, 2000) - 1000;
        Rect rect = new Rect(convertLeft, convertTop, convertRight,
                convertBottom);
        return rect;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        isFocusSucc = success;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float currentSpan = detector.getCurrentSpan();
        float compValue = currentSpan - beginSpan;
        if (Math.abs(compValue) > 5) {
            beginSpan = currentSpan;
            int zoomSize = 0;
            if (compValue > 0) {
                zoomSize = gestureZoomIn();
            } else {
                zoomSize = gestureZoomOut();
            }
            CameraActivity activity = (CameraActivity) mContext;
            activity.updateSeekBar(zoomSize);
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        beginSpan = detector.getCurrentSpan();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    /**
     * 手势缩小
     * 
     * @return
     */
    private int gestureZoomIn() {
        Parameters mParameters = mCamera.getParameters();
        if (mParameters.isZoomSupported()) {
            int mZoomMax = mParameters.getMaxZoom();
            int currentZoom = mParameters.getZoom();
            if (currentZoom < mZoomMax) {
                currentZoom++;
                return currentZoom;
            } else {
                return mZoomMax;
            }
        }
        return 0;
    }

    /**
     * 手势放大
     * 
     * @return
     */
    private int gestureZoomOut() {
        Parameters mParameters = mCamera.getParameters();
        if (mParameters.isZoomSupported()) {
            int currentZoom = mParameters.getZoom();
            if (0 < currentZoom) {
                currentZoom--;
                return currentZoom;
            }
        }
        return 0;
    }

    private void setupCaptureParams(Parameters parameters) {
        List<Size> supportedSizes = parameters.getSupportedPreviewSizes();
        if (!findBestPreviewSize(supportedSizes, true, true)) {
            Log.w(TAG, "No 4:3 ratio preview size supported.");
            if (!findBestPreviewSize(supportedSizes, false, true)) {
                Log.w(TAG, "Can't find a supported preview size smaller than 960x720.");
                findBestPreviewSize(supportedSizes, false, false);
            }
        }
        parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);

        List<int[]> frameRates = parameters.getSupportedPreviewFpsRange();
        int last = frameRates.size() - 1;
        int minFps = (frameRates.get(last))[Parameters.PREVIEW_FPS_MIN_INDEX];
        int maxFps = (frameRates.get(last))[Parameters.PREVIEW_FPS_MAX_INDEX];
        parameters.setPreviewFpsRange(minFps, maxFps);

        parameters.set(RECORDING_HINT, FALSE);

    }

    /**
     * 查找最好的预览图尺寸
     * 
     * @param supportedSizes
     * @param need4To3 是否需要4：3
     * @param needSmaller 是否需要最小
     * @return
     */
    private boolean findBestPreviewSize(List<Size> supportedSizes, boolean need4To3,
            boolean needSmaller) {
        int pixelsDiff = DEFAULT_CAPTURE_PIXELS;
        boolean hasFound = false;
        for (Size size : supportedSizes) {
            int h = size.height;
            int w = size.width;
            // we only want 4:3 format.
            int d = DEFAULT_CAPTURE_PIXELS - h * w;
            if (needSmaller && d < 0) { // no bigger preview than 960x720.
                continue;
            }
            if (need4To3 && (h * 4 != w * 3)) {
                continue;
            }
            d = Math.abs(d);
            if (d < pixelsDiff) {
                mPreviewWidth = w;
                mPreviewHeight = h;
                pixelsDiff = d;
                hasFound = true;
            }
        }
        return hasFound;
    }
}
