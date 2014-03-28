package com.example.testdemo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Environment;
import android.os.StatFs;

public class CommonUtil {

	/**
	 * 检测sdcard是否可用
	 * 
	 * @return true为可用，否则为不可用
	 */
	public static boolean sdCardIsAvailable() {
		String status = Environment.getExternalStorageState();
		if (!status.equals(Environment.MEDIA_MOUNTED))
			return false;
		return true;
	}

	/**
	 * Checks if there is enough Space on SDCard
	 * 
	 * @param updateSize
	 *            Size to Check
	 * @return True if the Update will fit on SDCard, false if not enough space
	 *         on SDCard Will also return false, if the SDCard is not mounted as
	 *         read/write
	 */
	public static boolean enoughSpaceOnSdCard(long updateSize) {
		String status = Environment.getExternalStorageState();
		if (!status.equals(Environment.MEDIA_MOUNTED))
			return false;
		return (updateSize < getRealSizeOnSdcard());
	}

	/**
	 * get the space is left over on sdcard
	 */
	public static long getRealSizeOnSdcard() {
		File path = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath());
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return availableBlocks * blockSize;
	}

	/**
	 * Checks if there is enough Space on phone self
	 * 
	 */
	public static boolean enoughSpaceOnPhone(long updateSize) {
		return getRealSizeOnPhone() > updateSize;
	}

	/**
	 * get the space is left over on phone self
	 */
	public static long getRealSizeOnPhone() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		long realSize = blockSize * availableBlocks;
		return realSize;
	}

	/**
	 * 根据手机分辨率从dp转成px
	 * 
	 * @param context
	 * @param dpValue
	 * @return
	 */
	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	/**
	 * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
	 */
	public static int px2dip(Context context, float pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f) - 15;
	}

	/**
	 * 写入GPS信息到图片中，其中gps信息为度分秒形式
	 * 
	 * @param imagePath
	 *            图片的路径
	 * @param latitude
	 *            图片的纬度
	 * @param longtitude
	 *            图片的经度
	 * @return
	 */
	public static boolean writeGpsToJpg(String imagePath, double latitude,
			double longtitude) {
		try {
			File file = new File(imagePath);
			ExifInterface exif = new ExifInterface(file.getCanonicalPath());
			double alat = Math.abs(latitude);
			String dms = Location.convert(alat, Location.FORMAT_SECONDS);
			String[] splits = dms.split(":");
			String[] secnds = (splits[2]).split("\\.");
			String seconds;
			if (secnds.length == 0) {
				seconds = splits[2];
			} else {
				seconds = secnds[0];
			}

			String latitudeStr = splits[0] + "/1," + splits[1] + "/1,"
					+ seconds + "/1";
			exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latitudeStr);

			exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF,
					latitude > 0 ? "N" : "S");

			double alon = Math.abs(longtitude);

			dms = Location.convert(alon, Location.FORMAT_SECONDS);
			splits = dms.split(":");
			secnds = (splits[2]).split("\\.");

			if (secnds.length == 0) {
				seconds = splits[2];
			} else {
				seconds = secnds[0];
			}
			String longitudeStr = splits[0] + "/1," + splits[1] + "/1,"
					+ seconds + "/1";

			exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, longitudeStr);
			exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF,
					longtitude > 0 ? "E" : "W");
			exif.saveAttributes();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 取得时间字符串根据匹配规则
	 * 
	 * @param time
	 *            时间long
	 * @param pattern
	 *            匹配规则 默认为 yyyy-MM-dd HH-mm-ss
	 * @return
	 */
	public static String getTime(long time, String pattern) {
		if (pattern == null) {
			pattern = "yyyy-MM-dd HH-mm-ss";
		}
		Date date = null;
		if (time == 0) {
			date = new Date(time);
		} else {
			date = new Date();
		}

		SimpleDateFormat format = new SimpleDateFormat(pattern);
		return format.format(date);
	}

	/**
	 * 从字符串中获取其中非数字的字符串。并以list形式返回回去
	 * 
	 * @param content
	 *            被处理的字符串
	 * @return 处理结果
	 */
	public static List<String> splitNotNumber(String content) {
		Pattern pattern = Pattern.compile("\\D+");
		Matcher matcher = pattern.matcher(content);
		List<String> strList = new ArrayList<String>();
		while (matcher.find()) {
			strList.add(matcher.group());
		}
		return strList;
	}

	/**
	 * 从字符串中获取其中为数字的字符串。并以list形式返回回去
	 * 
	 * @param content
	 *            被处理的字符串
	 * @return 处理结果
	 */
	public static List<String> splitNumber(String content) {
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matcher = pattern.matcher(content);
		List<String> strList = new ArrayList<String>();
		while (matcher.find()) {
			strList.add(matcher.group());
		}
		return strList;
	}
}
