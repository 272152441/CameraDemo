package com.example.testdemo;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback {
	private final String TAG = "Preview";

	private SurfaceView mSurfaceView;
	private SurfaceHolder mHolder;
	private Size mPreviewSize;
	private List<Size> mSupportedPreviewSizes;
	private Camera mCamera;
	private int mCameraID;
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
			mSupportedPreviewSizes = mCamera.getParameters()
					.getSupportedPreviewSizes();
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
			mPreviewSize = CameraUtil.getOptimalPreviewSize((Activity) mContext,
					mSupportedPreviewSizes, (double) width / height);
//			mPreviewSize.height = 1088;
//			mPreviewSize.width = 1920;
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

}
