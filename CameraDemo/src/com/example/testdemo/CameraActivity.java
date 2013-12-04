package com.example.testdemo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class CameraActivity extends Activity {

	private CameraPreview mPreview;
	private FrameLayout parentPreview;
	private Camera mCamera;
	private int numberOfCameras;
	private int cameraCurrentlyLocked;
	private int defaultCameraId = -1;
	private String currentFile;
	private String fileDic;
	private ImageView takePreview;
	private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
	private MyOrientationEventListener mOrientationListener;
	private LinearLayout controlCamera;
	private LinearLayout controlPicture;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_main);

		// 用来初始化照片存储路径，照片保存路径是根据调用的activity提供的，如果没有提供则保存到系统默认的存储路径下
		if (getIntent() != null && getIntent().getExtras() != null) {
			fileDic = getIntent().getExtras().getString("picdir");
			if (fileDic == null) {
				File file = new File(Environment
						.getExternalStoragePublicDirectory(
								Environment.DIRECTORY_PICTURES)
						.getAbsolutePath());
				if (!file.exists()) {
					file.mkdir();
				}
				fileDic = file.getAbsolutePath();
			}
		}

		parentPreview = (FrameLayout) findViewById(R.id.surface_camera);
		controlCamera = (LinearLayout) findViewById(R.id.control_camera);
		controlPicture = (LinearLayout) findViewById(R.id.control_picture);
		takePreview = (ImageView) findViewById(R.id.take_picture_preview);
		// 找到所有可以利用的摄像头
		numberOfCameras = Camera.getNumberOfCameras();
		// 初始化默认的摄像头id主要是找到后置摄像头
		CameraInfo cameraInfo = new CameraInfo();
		mOrientationListener = new MyOrientationEventListener(this);
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				defaultCameraId = i;
			}
		}
	}

	private class MyOrientationEventListener extends OrientationEventListener {
		public MyOrientationEventListener(Context context) {
			super(context);
		}

		@Override
		public void onOrientationChanged(int orientation) {
			if (orientation == ORIENTATION_UNKNOWN)
				return;
			mOrientation = CameraUtil.roundOrientation(orientation, mOrientation);

			Log.i("CameraActivity", "Orientation " + mOrientation);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		// 激活照相功能
		mCamera = Camera.open();
		if (mCamera != null) {
			cameraCurrentlyLocked = defaultCameraId;
			mPreview = new CameraPreview(this);
			if (cameraCurrentlyLocked > -1) {
				mPreview.setCamera(mCamera, cameraCurrentlyLocked);
			} else {
				Log.e("CameraActivity", "no camera ");
			}
			parentPreview.addView(mPreview);
		}
		mOrientationListener.enable();

	}

	@Override
	protected void onPause() {
		super.onPause();
		// 暂停照相功能
		if (mCamera != null) {
			if (cameraCurrentlyLocked > -1) {
				mPreview.setCamera(null, cameraCurrentlyLocked);
			} else {
				Log.e("CameraActivity", "no camera ");
			}
			mCamera.release();
			parentPreview.removeView(mPreview);
			mCamera = null;
		}
		mOrientationListener.disable();
	}

	/**
	 * 照相界面按钮处理事件
	 * 
	 * @param v
	 */
	public void cameraOnClick(View v) {
		switch (v.getId()) {
		case R.id.take_picture:
			takePicture();
			break;
		case R.id.take_picture_again:
			againTakePicture();
			hidePostCaptureAlert();
			break;
		case R.id.take_picture_back:
			cancleCamera();
			hidePostCaptureAlert();
			break;
		case R.id.take_picture_confirm:
			confirmCamera();
			hidePostCaptureAlert();
			break;
		case R.id.take_picture_retry:
			retryTakePic();
			hidePostCaptureAlert();
			break;

		default:
			break;
		}
	}

	/**
	 * 取消照相
	 */
	private void cancleCamera() {
		deleteCurrentFile();
		setResult(RESULT_CANCELED);
		finish();
	}

	/**
	 * 确定照相
	 */
	private void confirmCamera() {
		setResult(RESULT_OK);
		finish();
	}

	/**
	 * 再拍一张
	 */
	private void againTakePicture() {
		resetCamera();
	}

	/**
	 * 重拍
	 */
	private void retryTakePic() {
		deleteCurrentFile();
		againTakePicture();
	}

	/**
	 * 删除文件
	 * 
	 * @return
	 */
	private boolean deleteCurrentFile() {
		if (currentFile == null) {
			return true;
		}
		File pic = new File(currentFile);
		if (pic.exists()) {
			return pic.delete();
		}
		return true;
	}

	/**
	 * 重启启动照相机，在照相完以后启动再次拍照
	 */
	private void resetCamera() {
		if (parentPreview.getVisibility() == View.GONE) {
			parentPreview.setVisibility(View.VISIBLE);
		}
		takePreview.setVisibility(View.GONE);
		mPreview.setCamera(mCamera, cameraCurrentlyLocked);
		mCamera.startPreview();
	}

	/**
	 * 照相
	 */
	public void takePicture() {
		setCameraRotaParams(mCamera);
		CameraUtil.fadeOut(parentPreview);
		mCamera.takePicture(null, null, jpegCallback);

	}

	/**
	 * 相片回调处理逻辑
	 */
	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			FileOutputStream outStream = null;
			try {
				// 加载照片，主要用来压缩所照的相片
				Bitmap cameraBitmap = CameraUtil.makeBitmap(data, 50 * 1024);
				// 设置照相预览的相片旋转的角度
				int roate = mOrientation
						+ CameraUtil.getDisplayRotation(CameraActivity.this);
				roate = (360 - roate) % 360;
				// 旋转照片
				Bitmap routeBitmap = CameraUtil.rotate(cameraBitmap, roate);
				// 把数据写入到文件中
				outStream = writeDataToFile(data);
				// 设置预览图显示
				RelativeLayout.LayoutParams layoutPara = new LayoutParams(
						mPreview.getWidth(), mPreview.getHeight());
				takePreview.setLayoutParams(layoutPara);
				takePreview.setImageBitmap(routeBitmap);
				CameraUtil.fadeIn(takePreview);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (outStream != null) {
					try {
						outStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

	};

	/**
	 * 把照片的数据数组写入到文件中
	 * 
	 * @param data
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private FileOutputStream writeDataToFile(byte[] data)
			throws FileNotFoundException, IOException {
		FileOutputStream outStream;
		File fileDir = new File(fileDic);
		if (!fileDir.exists()) {
			fileDir.mkdir();
		}
		final String fileName = fileDic + "/" + System.currentTimeMillis()
				+ ".jpg";
		currentFile = fileName;
		outStream = new FileOutputStream(fileName);
		outStream.write(data);
		outStream.flush();
		showPostCaptureAlert();
		return outStream;
	}

	/**
	 * 显示控制窗口
	 */
	public void showPostCaptureAlert() {
		CameraUtil.fadeOut(controlCamera);
		CameraUtil.fadeIn(controlPicture);
	}

	/**
	 * 隐藏控制窗口
	 */
	public void hidePostCaptureAlert() {
		CameraUtil.fadeIn(controlCamera);
		CameraUtil.fadeOut(controlPicture);
	}

	// 设置摄像头参数
	protected void setCameraRotaParams(Camera camera) {
		if (camera == null) {
			return;
		}
		Camera.Parameters params = camera.getParameters();
		setRotationParameter(params, cameraCurrentlyLocked, mOrientation);
		camera.setParameters(params);
	}

	/**
	 * 用来设置当前照相的方向，主要为照相采集到方向，都是默认同一个方向的。
	 * 
	 * @param parameters
	 * @param cameraId
	 * @param orientation
	 */
	public void setRotationParameter(Parameters parameters, int cameraId,
			int orientation) {
		int rotation = 0;
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
			if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
				rotation = (info.orientation - orientation + 360) % 360;
			} else { // back-facing camera
				rotation = (info.orientation + orientation) % 360;
			}
		}
		parameters.setRotation(rotation);
	}

}
