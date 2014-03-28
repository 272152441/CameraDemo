
package com.example.testdemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraActivity extends Activity implements OnZoomChangeListener,
        OnSeekBarChangeListener, OnItemClickListener {

    public static final String TAG = "CameraActivity";

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
    private RelativeLayout controlCamera;
    private LinearLayout controlPicture;
    private List<Rotatable> rotateViewList;
    private PopupWindows cameraSettingPop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_main);

        requstValueInIntent();
        initButton();
        initRotateView();
        initCamera();
    }

    /**
     * 根据intent中的参数来设置当前照片的存储路径
     */
    private void requstValueInIntent() {
        // 用来初始化照片存储路径，照片保存路径是根据调用的activity提供的，如果没有提供则保存到系统默认的存储路径下
        if (getIntent() != null && getIntent().getExtras() != null) {
            fileDic = getIntent().getExtras().getString(Constances.CAMERA_PICTURE_DIR);
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
    }

    /**
     * 初始化照相机的摄像头，主要选择照相机与id
     */
    private void initCamera() {
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

    /**
     * 初始化按钮
     */
    private void initButton() {
        parentPreview = (FrameLayout) findViewById(R.id.surface_camera);
        controlCamera = (RelativeLayout) findViewById(R.id.control_camera);
        controlPicture = (LinearLayout) findViewById(R.id.control_picture);
        takePreview = (ImageView) findViewById(R.id.take_picture_preview);
        SeekBar zoomControl = (SeekBar) findViewById(R.id.zoom_control);
        zoomControl.setOnSeekBarChangeListener(this);
        cameraSettingPop = new PopupWindows(this);
        cameraSettingPop.setContentView(R.layout.setting_popup);
        ListView settingList = (ListView) cameraSettingPop.getContentView().findViewById(
                R.id.camera_setting_list);
        settingList.setOnItemClickListener(this);

    }

    /**
     * 初始化话旋转的view、
     */
    private void initRotateView() {
        rotateViewList = new ArrayList<Rotatable>();

        Rotatable view1 = (Rotatable) findViewById(R.id.take_picture);
        Rotatable view2 = (Rotatable) findViewById(R.id.take_picture_again);
        Rotatable view3 = (Rotatable) findViewById(R.id.take_picture_back);
        Rotatable view4 = (Rotatable) findViewById(R.id.take_picture_confirm);
        Rotatable view5 = (Rotatable) findViewById(R.id.take_picture_retry);
        Rotatable view6 = (Rotatable) findViewById(R.id.camera_setting);
        Rotatable view7 = (Rotatable)
                cameraSettingPop.getContentView().findViewById(
                        R.id.rotate_setting_list);

        rotateViewList.add(view1);
        rotateViewList.add(view2);
        rotateViewList.add(view3);
        rotateViewList.add(view4);
        rotateViewList.add(view5);
        rotateViewList.add(view6);
        rotateViewList.add(view7);

    }

    private class MyOrientationEventListener extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == ORIENTATION_UNKNOWN)
                return;
            int hisOrientation = mOrientation;
            mOrientation = CameraUtil.roundOrientation(orientation,
                    mOrientation);
            if (mOrientation != hisOrientation) {
                int roate = mOrientation
                        + CameraUtil.getDisplayRotation(CameraActivity.this);
                for (Rotatable item : rotateViewList) {
                    item.setOrientation(roate, true);
                }

            }

            Log.i("CameraActivity", "Orientation " + mOrientation);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 激活照相功能
        // 打开相机失败 Fail to connect to camera service
        mCamera = Camera.open();
        if (mCamera != null) {

            Parameters param = mCamera.getParameters();
            SeekBar zoomControl = (SeekBar) findViewById(R.id.zoom_control);
            int maxZoom = param.getMaxZoom();
            if (maxZoom > 0) {
                zoomControl.setVisibility(View.VISIBLE);
                zoomControl.setMax(param.getMaxZoom());
                zoomControl.setProgress(param.getZoom());
            } else {
                zoomControl.setVisibility(View.GONE);
            }

            mCamera.setZoomChangeListener(this);
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
            mCamera.setZoomChangeListener(null);
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
            case R.id.camera_setting:
                showSettingPop(v);
                break;
            default:
                break;
        }
    }

    /**
     * 初始化照片分辨率的大小
     * 
     * @param v
     */
    private void showSettingPop(View v) {
        ListView screenSize = (ListView) cameraSettingPop.getContentView().findViewById(
                R.id.camera_setting_list);
        if (screenSize.getAdapter() == null) {
            // 获取照片支持的尺寸
            Parameters paramter = mCamera.getParameters();
            List<Size> sizeList = paramter.getSupportedPictureSizes();
            Size currSize = paramter.getPictureSize();
            List<CameraSizeBean> showList = new ArrayList<CameraSizeBean>();
            int position = -1;
            // 转换数据类型
            if (sizeList != null) {
                // 排序list
                Collections.sort(sizeList, new SortByPicSize());
                for (int i = 0; i < sizeList.size(); i++) {
                    Size item = sizeList.get(i);
                    showList.add(new CameraSizeBean(item));
                    if (item.equals(currSize)) {
                        position = i;
                    }
                }
            }
            // 设置数据适配器
            ShowOneTextAdaper screenAdapter = new ShowOneTextAdaper(this, R.layout.camera_pic_size_item,
                    showList);
            screenSize.setItemsCanFocus(false);
            screenSize.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            screenSize.setAdapter(screenAdapter);
            if (position > -1) {
                screenSize.setItemChecked(position, true);
            }
            // 设置弹出的Popup大小
            cameraSettingPop.setWidth(300);
            cameraSettingPop.setHeight(300);
        }
        cameraSettingPop.showLeftPopDownward(v);
    }

    /**
     * 排序List<Size> 根据宽度与高度
     * 
     * @author dlwy
     */
    class SortByPicSize implements Comparator<Size> {
        public int compare(Size o1, Size o2) {
            if (o1.height >= o2.height) {
                return 1;
            } else if (o1.height == o2.height) {
                return o1.width - o2.width;
            } else {
                return -1;
            }
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

            // 把数据写入到文件中
            writeDataToFile(data);

            // 加载照片，主要用来压缩所照的相片
            int degrees = Exif.getOrientation(data);
            Log.i("CameraDemo", "exif data degrees" + degrees);

            Size preViewSize = mPreview.getmPreviewSize();

            int previewWidth = parentPreview.getWidth();
            int previewHeight = parentPreview.getHeight();
            int cameraWidth = preViewSize.width;
            int cameraHeight = preViewSize.height;
            int width = previewWidth < cameraWidth ? previewWidth : cameraWidth;
            int height = previewHeight < cameraHeight ? previewHeight : cameraHeight;

            Bitmap bitPreView = CameraUtil.decodeSampledBitmapFromFile(data, width, height);
            bitPreView.getWidth();
            bitPreView.getHeight();
            // if (mJpegRotation % 180 != 0) {
            // int x = r.height;
            // int y = r.width;
            // r.width = x;
            // r.height = y;
            // }

            // 设置照相预览的相片旋转的角度
            int roate = mOrientation
                    + CameraUtil.getDisplayRotation(CameraActivity.this);
            // roate = roate - degrees;
            roate = (360 - roate) % 360 + degrees;

            // 旋转照片
            Bitmap routeBitmap = CameraUtil.rotate(bitPreView, roate);

            // 设置预览图显示
            RelativeLayout.LayoutParams layoutPara = new LayoutParams(
                    mPreview.getWidth(), mPreview.getHeight());
            takePreview.setLayoutParams(layoutPara);
            takePreview.setImageBitmap(routeBitmap);
            CameraUtil.fadeIn(takePreview);
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
    private boolean writeDataToFile(byte[] data) {
        boolean writeFlag = false;
        FileOutputStream outStream = null;
        File fileDir = new File(fileDic);
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        String fileName = fileDic + "/" + CommonUtil.getTime(System.currentTimeMillis(), null)
                + ".jpg";
        currentFile = fileName;
        try {
            outStream = new FileOutputStream(fileName);
            outStream.write(data);
            outStream.flush();
            showPostCaptureAlert();
            writeFlag = true;
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
        return writeFlag;
    }

    /**
     * 显示控制窗口
     */
    public void showPostCaptureAlert() {
        View setting = findViewById(R.id.camera_setting);
        CameraUtil.fadeOut(setting);
        CameraUtil.fadeOut(controlCamera);
        CameraUtil.fadeIn(controlPicture);
    }

    /**
     * 隐藏控制窗口
     */
    public void hidePostCaptureAlert() {
        View setting = findViewById(R.id.camera_setting);
        CameraUtil.fadeIn(setting);
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

        Log.i("Camera", "take picture rotation" + rotation);
        parameters.setRotation(rotation);
    }

    @Override
    public void onZoomChange(int zoomValue, boolean stopped, Camera camera) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        Parameters param = mCamera.getParameters();
        param.setZoom(progress);
        mCamera.setParameters(param);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void updateSeekBar(int zoomSize) {
        SeekBar zoomControl = (SeekBar) findViewById(R.id.zoom_control);
        zoomControl.setProgress(zoomSize);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.camera_setting_list:
                ShowOneTextAdaper adapter = (ShowOneTextAdaper) parent.getAdapter();
                changeCameraSize((CameraSizeBean) adapter.getItem(position));
                break;

            default:
                break;
        }

    }

    private void changeCameraSize(CameraSizeBean cameraSizeBean) {
        Size selectSize = cameraSizeBean.getSize();
        Parameters cameraParam = mCamera.getParameters();

        if (!cameraParam.getPictureSize().equals(selectSize)) {
            cameraParam.setPictureSize(selectSize.width, selectSize.height);
            // cameraParam.setPreviewSize(selectSize.width, selectSize.height);
            mCamera.setParameters(cameraParam);
            mPreview.invalidate();
            mPreview.requestLayout();
        }
    }

}
