package com.example.testdemo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;

public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback,
		OnTouchListener, AutoFocusCallback {
	private final String TAG = "Preview";

	private SurfaceView mSurfaceView;
	private SurfaceHolder mHolder;
	private Size mPreviewSize;
	private List<Size> mSupportedPreviewSizes;
	private Camera mCamera;
	private int mCameraID;
	private boolean isFocusSucc = true;
	private Context mContext;

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
			mSupportedPreviewSizes = cameraParam.getSupportedPreviewSizes();
			boolean flag = CameraUtil.isFocusAreaSupported(cameraParam);
			if (flag) {
				cameraParam.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				mSurfaceView.setOnTouchListener(this);
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
		parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
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

		if (mSupportedPreviewSizes != null) {
			mPreviewSize = CameraUtil.getOptimalPreviewSize(
					(Activity) mContext, mSupportedPreviewSizes, (double) width
							/ height);
			// mPreviewSize.height = 1088;
			// mPreviewSize.width = 1920;
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// 用来控制surface的布局
		if (changed && getChildCount() > 0) {
			final View child = getChildAt(0);

			final int width = r - l;
			final int height = b - t;

			int previewWidth = width;
			int previewHeight = height;
			if (mPreviewSize != null) {
				previewWidth = mPreviewSize.height;
				previewHeight = mPreviewSize.width;
			}

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
		// 开启预览图片
		try {
			if (mCamera != null) {
				mCamera.setPreviewDisplay(holder);
			}
		} catch (IOException exception) {
			Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// 停止照相功能
		if (mCamera != null) {
			mCamera.stopPreview();
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// surface 改变的时候重新设置照片的属性参数与尺寸
		if (mCamera != null) {
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
			requestLayout();

			mCamera.setParameters(parameters);
			mCamera.startPreview();
		}

	}

	public Size getmPreviewSize() {
		return mPreviewSize;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			focusOnTouch(event);
			return true;
		}
		return false;
	}

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
			mCamera.autoFocus(this);
		}
	}

	private void initializeFocusAreas(int x, int y, Parameters parameters) {
		List<Camera.Area> listFocusArea = parameters.getFocusAreas();
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

	private void initializeMeteringAreas(int x, int y, Parameters parameters) {
		List<Camera.Area> listMeterArea = new ArrayList<Camera.Area>();
		Rect rect = calculateTapArea(100, 100, 1.5f, x, y);
		Area touchArea = new Camera.Area(rect, 1);
		listMeterArea.add(touchArea);
		parameters.setMeteringAreas(listMeterArea);
	}

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

}
