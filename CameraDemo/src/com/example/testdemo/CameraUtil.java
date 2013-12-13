/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.testdemo;

import java.io.Closeable;
import java.util.List;
import java.util.StringTokenizer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.location.Location;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

/**
 * Collection of utility functions used in this package.
 */
public class CameraUtil {
	private static final String TAG = "Util";

	// Orientation hysteresis amount used in rounding, in degrees
	public static final int ORIENTATION_HYSTERESIS = 5;

	public static final String REVIEW_ACTION = "com.android.camera.action.REVIEW";
	
	public static final String SCENE_MODE_HDR = "hdr";

	private static boolean sIsTabletUI;
	private static float sPixelDensity = 1;

	private CameraUtil() {
	}

	public static void initialize(Context context) {
		sIsTabletUI = (context.getResources().getConfiguration().smallestScreenWidthDp >= 600);

		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(metrics);
		sPixelDensity = metrics.density;
	}

	public static boolean isTabletUI() {
		return sIsTabletUI;
	}

	public static int dpToPixel(int dp) {
		return Math.round(sPixelDensity * dp);
	}

	// Rotates the bitmap by the specified degree.
	// If a new bitmap is created, the original bitmap is recycled.
	public static Bitmap rotate(Bitmap b, int degrees) {
		return rotateAndMirror(b, degrees, false);
	}

	// Rotates and/or mirrors the bitmap. If a new bitmap is created, the
	// original bitmap is recycled.
	public static Bitmap rotateAndMirror(Bitmap b, int degrees, boolean mirror) {
		if ((degrees != 0 || mirror) && b != null) {
			Matrix m = new Matrix();
			// Mirror first.
			// horizontal flip + rotation = -rotation + horizontal flip
			if (mirror) {
				m.postScale(-1, 1);
				degrees = (degrees + 360) % 360;
				if (degrees == 0 || degrees == 180) {
					m.postTranslate((float) b.getWidth(), 0);
				} else if (degrees == 90 || degrees == 270) {
					m.postTranslate((float) b.getHeight(), 0);
				} else {
					throw new IllegalArgumentException("Invalid degrees="
							+ degrees);
				}
			}
			if (degrees != 0) {
				// clockwise
				m.postRotate(degrees, (float) b.getWidth() / 2,
						(float) b.getHeight() / 2);
			}

			try {
				Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(),
						b.getHeight(), m, true);
				if (b != b2) {
					b.recycle();
					b = b2;
				}
			} catch (OutOfMemoryError ex) {
				// We have no memory to rotate. Return the original bitmap.
			}
		}
		return b;
	}

	/*
	 * Compute the sample size as a function of minSideLength and
	 * maxNumOfPixels. minSideLength is used to specify that minimal width or
	 * height of a bitmap. maxNumOfPixels is used to specify the maximal size in
	 * pixels that is tolerable in terms of memory usage.
	 * 
	 * The function returns a sample size based on the constraints. Both size
	 * and minSideLength can be passed in as -1 which indicates no care of the
	 * corresponding constraint. The functions prefers returning a sample size
	 * that generates a smaller bitmap, unless minSideLength = -1.
	 * 
	 * Also, the function rounds up the sample size to a power of 2 or multiple
	 * of 8 because BitmapFactory only honors sample size this way. For example,
	 * BitmapFactory downsamples an image by 2 even though the request is 3. So
	 * we round up the sample size to avoid OOM.
	 */
	public static int computeSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		int initialSize = computeInitialSampleSize(options, minSideLength,
				maxNumOfPixels);

		int roundedSize;
		if (initialSize <= 8) {
			roundedSize = 1;
			while (roundedSize < initialSize) {
				roundedSize <<= 1;
			}
		} else {
			roundedSize = (initialSize + 7) / 8 * 8;
		}

		return roundedSize;
	}

	private static int computeInitialSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		double w = options.outWidth;
		double h = options.outHeight;

		int lowerBound = (maxNumOfPixels < 0) ? 1 : (int) Math.ceil(Math.sqrt(w
				* h / maxNumOfPixels));
		int upperBound = (minSideLength < 0) ? 128 : (int) Math.min(
				Math.floor(w / minSideLength), Math.floor(h / minSideLength));

		if (upperBound < lowerBound) {
			// return the larger one when there is no overlapping zone.
			return lowerBound;
		}

		if (maxNumOfPixels < 0 && minSideLength < 0) {
			return 1;
		} else if (minSideLength < 0) {
			return lowerBound;
		} else {
			return upperBound;
		}
	}

	public static Bitmap makeBitmap(byte[] jpegData, int maxNumOfPixels) {
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory
					.decodeByteArray(jpegData, 0, jpegData.length, options);
			if (options.mCancel || options.outWidth == -1
					|| options.outHeight == -1) {
				return null;
			}
			options.inSampleSize = computeSampleSize(options, -1,
					maxNumOfPixels);
			options.inJustDecodeBounds = false;

			options.inDither = false;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length,
					options);
		} catch (OutOfMemoryError ex) {
			Log.e(TAG, "Got oom exception ", ex);
			return null;
		}
	}

	public static void closeSilently(Closeable c) {
		if (c == null)
			return;
		try {
			c.close();
		} catch (Throwable t) {
			// do nothing
		}
	}

	public static void Assert(boolean cond) {
		if (!cond) {
			throw new AssertionError();
		}
	}

	public static <T> T checkNotNull(T object) {
		if (object == null)
			throw new NullPointerException();
		return object;
	}

	public static boolean equals(Object a, Object b) {
		return (a == b) || (a == null ? false : a.equals(b));
	}

	public static int nextPowerOf2(int n) {
		n -= 1;
		n |= n >>> 16;
		n |= n >>> 8;
		n |= n >>> 4;
		n |= n >>> 2;
		n |= n >>> 1;
		return n + 1;
	}

	public static float distance(float x, float y, float sx, float sy) {
		float dx = x - sx;
		float dy = y - sy;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	public static int clamp(int x, int min, int max) {
		if (x > max)
			return max;
		if (x < min)
			return min;
		return x;
	}

	public static int getDisplayRotation(Activity activity) {
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		switch (rotation) {
		case Surface.ROTATION_0:
			return 0;
		case Surface.ROTATION_90:
			return 90;
		case Surface.ROTATION_180:
			return 180;
		case Surface.ROTATION_270:
			return 270;
		}
		return 0;
	}

	public static int getDisplayOrientation(int degrees, int cameraId) {
		// See android.hardware.Camera.setDisplayOrientation for
		// documentation.
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		return result;
	}

	public static int getCameraOrientation(int cameraId) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		return info.orientation;
	}

	public static int roundOrientation(int orientation, int orientationHistory) {
		boolean changeOrientation = false;
		if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
			changeOrientation = true;
		} else {
			int dist = Math.abs(orientation - orientationHistory);
			dist = Math.min(dist, 360 - dist);
			changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
		}
		if (changeOrientation) {
			return ((orientation + 45) / 90 * 90) % 360;
		}
		return orientationHistory;
	}

	public static Size getOptimalPreviewSize(Activity currentActivity,
			List<Size> sizes, double targetRatio) {
		// Use a very small tolerance because we want an exact match.
		final double ASPECT_TOLERANCE = 0.001;
		if (sizes == null)
			return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		// Because of bugs of overlay and layout, we sometimes will try to
		// layout the viewfinder in the portrait orientation and thus get the
		// wrong size of mSurfaceView. When we change the preview size, the
		// new overlay will be created before the old one closed, which causes
		// an exception. For now, just get the screen size

		Display display = currentActivity.getWindowManager()
				.getDefaultDisplay();
		int targetHeight = Math.min(display.getHeight(), display.getWidth());

		if (targetHeight <= 0) {
			// We don't know the size of SurfaceView, use screen height
			targetHeight = display.getHeight();
		}

		// Try to find an size match aspect ratio and size
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		// Cannot find the one match the aspect ratio. This should not happen.
		// Ignore the requirement.
		if (optimalSize == null) {
			Log.w(TAG, "No preview size match the aspect ratio");
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	// Returns the largest picture size which matches the given aspect ratio.
	public static Size getOptimalVideoSnapshotPictureSize(List<Size> sizes,
			double targetRatio) {
		// Use a very small tolerance because we want an exact match.
		final double ASPECT_TOLERANCE = 0.001;
		if (sizes == null)
			return null;

		Size optimalSize = null;

		// Try to find a size matches aspect ratio and has the largest width
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (optimalSize == null || size.width > optimalSize.width) {
				optimalSize = size;
			}
		}

		// Cannot find one that matches the aspect ratio. This should not
		// happen.
		// Ignore the requirement.
		if (optimalSize == null) {
			Log.w(TAG, "No picture size match the aspect ratio");
			for (Size size : sizes) {
				if (optimalSize == null || size.width > optimalSize.width) {
					optimalSize = size;
				}
			}
		}
		return optimalSize;
	}

	public static void dumpParameters(Parameters parameters) {
		String flattened = parameters.flatten();
		StringTokenizer tokenizer = new StringTokenizer(flattened, ";");
		Log.d(TAG, "Dump all camera parameters:");
		while (tokenizer.hasMoreElements()) {
			Log.d(TAG, tokenizer.nextToken());
		}
	}

	private static boolean isFrontCameraIntent(int intentCameraId) {
		return (intentCameraId == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
	}

	private static boolean isBackCameraIntent(int intentCameraId) {
		return (intentCameraId == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK);
	}

	public static void dumpRect(RectF rect, String msg) {
		Log.v(TAG, msg + "=(" + rect.left + "," + rect.top + "," + rect.right
				+ "," + rect.bottom + ")");
	}

	public static void rectFToRect(RectF rectF, Rect rect) {
		rect.left = Math.round(rectF.left);
		rect.top = Math.round(rectF.top);
		rect.right = Math.round(rectF.right);
		rect.bottom = Math.round(rectF.bottom);
	}

	public static void prepareMatrix(Matrix matrix, boolean mirror,
			int displayOrientation, int viewWidth, int viewHeight) {
		// Need mirror for front camera.
		matrix.setScale(mirror ? -1 : 1, 1);
		// This is the value for android.hardware.Camera.setDisplayOrientation.
		matrix.postRotate(displayOrientation);
		// Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
		// UI coordinates range from (0, 0) to (width, height).
		matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
		matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
	}

	public static void fadeIn(View view) {
		if (view.getVisibility() == View.VISIBLE)
			return;

		view.setVisibility(View.VISIBLE);
		Animation animation = new AlphaAnimation(0F, 1F);
		animation.setDuration(400);
		view.startAnimation(animation);
	}

	public static void fadeOut(View view) {
		if (view.getVisibility() != View.VISIBLE)
			return;

		Animation animation = new AlphaAnimation(1F, 0F);
		animation.setDuration(400);
		view.startAnimation(animation);
		view.setVisibility(View.GONE);
	}

	public static void setGpsParameters(Parameters parameters, Location loc) {
		// Clear previous GPS location from the parameters.
		parameters.removeGpsData();

		// We always encode GpsTimeStamp
		parameters.setGpsTimestamp(System.currentTimeMillis() / 1000);

		// Set GPS location.
		if (loc != null) {
			double lat = loc.getLatitude();
			double lon = loc.getLongitude();
			boolean hasLatLon = (lat != 0.0d) || (lon != 0.0d);

			if (hasLatLon) {
				Log.d(TAG, "Set gps location");
				parameters.setGpsLatitude(lat);
				parameters.setGpsLongitude(lon);
				parameters.setGpsProcessingMethod(loc.getProvider()
						.toUpperCase());
				if (loc.hasAltitude()) {
					parameters.setGpsAltitude(loc.getAltitude());
				} else {
					// for NETWORK_PROVIDER location provider, we may have
					// no altitude information, but the driver needs it, so
					// we fake one.
					parameters.setGpsAltitude(0);
				}
				if (loc.getTime() != 0) {
					// Location.getTime() is UTC in milliseconds.
					// gps-timestamp is UTC in seconds.
					long utcTimeSeconds = loc.getTime() / 1000;
					parameters.setGpsTimestamp(utcTimeSeconds);
				}
			} else {
				loc = null;
			}
		}
	}

	public static Bitmap decodeSampledBitmapFromFile(byte[] data, int reqWidth,
			int reqHeight) {

		// 先进行尺寸的检测
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(data, 0, data.length, options);
		// 计算图片缩放比例
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);

		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeByteArray(data, 0, data.length, options);
	}

	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// 图片真实的尺寸
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// 计算比率
			final int heightRatio = Math.round((float) height
					/ (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// 选择最小的缩放比例，
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

			final float totalPixels = width * height;

			final float totalReqPixelsCap = reqWidth * reqHeight * 2;

			while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
				inSampleSize++;
			}
		}
		return inSampleSize;
	}

	public static Bitmap decodeYUV422P(byte[] yuv422p, int width, int height)
			throws NullPointerException, IllegalArgumentException {
		final int frameSize = width * height;
		int[] rgb = new int[frameSize];
		for (int j = 0, yp = 0; j < height; j++) {
			int up = frameSize + (j * (width / 2)), u = 0, v = 0;
			int vp = ((int) (frameSize * 1.5) + (j * (width / 2)));
			for (int i = 0; i < width; i++, yp++) {
				int y = (0xff & ((int) yuv422p[yp])) - 16;
				if (y < 0)
					y = 0;
				if ((i & 1) == 0) {
					u = (0xff & yuv422p[up++]) - 128;
					v = (0xff & yuv422p[vp++]) - 128;
				}

				int y1192 = 1192 * y;
				int r = (y1192 + 1634 * v);
				int g = (y1192 - 833 * v - 400 * u);
				int b = (y1192 + 2066 * u);

				if (r < 0)
					r = 0;
				else if (r > 262143)
					r = 262143;
				if (g < 0)
					g = 0;
				else if (g > 262143)
					g = 262143;
				if (b < 0)
					b = 0;
				else if (b > 262143)
					b = 262143;

				rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
						| ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
			}
		}
		return Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888);
	}

	public static boolean isCameraHdrSupported(Parameters params) {
		List<String> supported = params.getSupportedSceneModes();
		return (supported != null) && supported.contains(SCENE_MODE_HDR);
	}

	@TargetApi(ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static boolean isMeteringAreaSupported(Parameters params) {
		if (ApiHelper.HAS_CAMERA_METERING_AREA) {
			return params.getMaxNumMeteringAreas() > 0;
		}
		return false;
	}

	@TargetApi(ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static boolean isFocusAreaSupported(Parameters params) {
		if (ApiHelper.HAS_CAMERA_FOCUS_AREA) {
			return (params.getMaxNumFocusAreas() > 0 && isSupported(
					Parameters.FOCUS_MODE_AUTO, params.getSupportedFocusModes()));
		}
		return false;
	}

	public static boolean isSupported(String value, List<String> supported) {
		return supported == null ? false : supported.indexOf(value) >= 0;
	}
}
