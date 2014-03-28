
package com.example.testdemo;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.PopupWindow;

public class PopupWindows {
    protected Drawable mBackground = null;
    protected Context mContext;
    protected View mRootView;
    protected PopupWindow mWindow;
    private boolean outsideDismiss = true;
    private boolean isFullScrean = false;

    /**
     * 实例化构造方法。
     * 
     * @param context 上下文内容
     * @param outsideDismiss 是否允许点击外部关闭popup
     */
    public PopupWindows(Context context, boolean outsideDismissFlag) {
        mContext = context;
        mWindow = new PopupWindow(context);
        this.outsideDismiss = outsideDismissFlag;
        mWindow.setTouchInterceptor(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent event) {
                int viewWidth = view.getWidth();
                int viewHeight = view.getHeight();
                float x = event.getX();
                float y = event.getY();
                if (x < 0 || x > viewWidth || y < 0 || y > viewHeight) {
                    return !outsideDismiss;
                }
                return false;
            }
        });
        mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

    }

    public PopupWindows(Context context) {
        mContext = context;
        mWindow = new PopupWindow(context);
        mWindow.setTouchInterceptor(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent event) {
                return false;
            }
        });
        mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
    }

    public void dismiss() {
        if (mWindow.isShowing()) {
            mWindow.dismiss();
        }
    }

    protected void onDismiss() {
    }

    public void setSoftInputMode(int type) {
        mWindow.setSoftInputMode(type);
    }

    public void setImputMethodMode(int type) {
        mWindow.setInputMethodMode(type);
    }

    protected void preShow() {
        if (mRootView == null) {
            throw new IllegalStateException(
                    "setContentView was not called with a view to display.");
        }
        if (mBackground == null)
            mWindow.setBackgroundDrawable(new BitmapDrawable(mContext.getResources()));
        else {
            mWindow.setBackgroundDrawable(mBackground);
        }
        mWindow.setFocusable(true);
        mWindow.setOutsideTouchable(true);
        mWindow.setTouchable(true);
    }

    public void showAsDropDown(View view) {
        preShow();
        mWindow.showAsDropDown(view);
    }

    public void showAsDropDown(View view, int x, int y) {
        preShow();
        mWindow.showAsDropDown(view, x, y);
    }

    // 左方显示窗口 窗口方向为向上
    public void showLeftPopUpward(View view) {
        preShow();
        int width = -getViewWidth();
        int height = -getViewHeight();
        mWindow.showAsDropDown(view, width, height);
    }

    // 左方显示窗口 窗口方向为向上
    public void showDownPopRightward(View view) {
        preShow();
        int width = view.getWidth();
        mWindow.showAsDropDown(view, width, 0);
    }

    // 左方显示窗口 窗口方向为向上
    public void showDownPopRightward(View view, int x, int y) {
        preShow();
        int width = view.getWidth() - getWidth() - x;
        int height = y;
        mWindow.showAsDropDown(view, width, height);
    }

    // 左方显示窗口 窗口
    public void showLeft(View view) {
        preShow();
        int width = -getViewWidth();
        int height = -view.getHeight();
        mWindow.showAsDropDown(view, width, height);
    }

    // 左方显示窗口 x，y为偏移量 窗口方向为向上
    public void showLeftPopUpward(View view, int x, int y) {
        preShow();
        int width = -getViewWidth() + x;
        int height = -getViewHeight() + y;
        mWindow.showAsDropDown(view, width, height);
    }

    // 左上方显示窗口 窗口方向为向上
    public void showLeftUpPopUpward(View view) {
        preShow();
        int width = -getViewWidth();
        int height = -(view.getHeight() + getViewHeight());
        mWindow.showAsDropDown(view, width, height);
    }

    // 左上方显示窗口 x，y为偏移量 窗口方向为向上
    public void showLeftUpPopUpward(View view, int x, int y) {
        preShow();
        int width = -getViewWidth() + x;
        int height = -(view.getHeight() + getViewHeight()) + y;
        mWindow.showAsDropDown(view, width, height);
    }

    // 左下方显示窗口 窗口向下显示
    public void showLeftPopDownward(View view) {
        preShow();
        int width = view.getWidth() - getWidth();
        int height = 0;
        mWindow.showAsDropDown(view, width, height);
    }

    // 左方显示窗口 窗口方向向下
    public void showLeftDownPopDownward(View view) {
        preShow();
        int width = -view.getWidth();
        mWindow.showAsDropDown(view, width, 0);
    }

    // 右方显示窗口 窗口方向向下
    public void showRightPopDownWard(View view) {
        preShow();
        int width = view.getWidth();
        int height = -view.getHeight();
        mWindow.showAsDropDown(view, width, height);
    }

    // 左下方显示窗口 x，y为偏移量 窗口方向向下
    public void showLeftPopLeftward(View view, int x, int y) {
        preShow();
        int width = -getViewWidth() + x;
        mWindow.showAsDropDown(view, width, y);
    }

    // 上方显示窗口
    public void showUp(View view) {
        preShow();
        int height = -(view.getHeight() + getHeight());
        mWindow.showAsDropDown(view, 0, height);
    }

    // 上方显示窗口 x，y为偏移量
    public void showUp(View view, int x, int y) {
        preShow();
        int height = -(view.getHeight() + getViewHeight()) + y;
        mWindow.showAsDropDown(view, x, height);
    }

    // 上方显示窗口 x，y为偏移量
    public void showDown(View view) {

        // 右方显示窗口 窗口方向向下
        preShow();
        int height = -view.getHeight();
        mWindow.showAsDropDown(view, 0, height);
    }

    public void showAtLocation(View parent, int gravity, int x, int y) {
        preShow();
        mWindow.showAtLocation(parent, gravity, x, y);
    }

    public void setHeight(int height) {
        mWindow.setHeight(height);
    }

    public int getHeight() {
        return mWindow.getHeight();
    }

    public void setWidth(int width) {
        mWindow.setWidth(width);
    }

    public int getWidth() {
        return mWindow.getWidth();
    }

    public int getViewHeight() {
        return mRootView == null ? 0 : mRootView.getMeasuredHeight();
    }

    public int getViewWidth() {
        return mRootView == null ? 0 : mRootView.getMeasuredWidth();
    }

    public void setBackgroundDrawable(Drawable drawable) {
        mBackground = drawable;
    }

    public void setContentView(int res) {
        View view = ((LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                res, null);
        view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        setContentView(view);
    }

    /**
     * 横竖屏设置isfullscrean必须提前设置好才能生效
     * 
     * @param view
     */
    public void setContentView(View view) {
        mRootView = view;
        view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        mWindow.setContentView(mRootView);
        if (isFullScrean) {
            mWindow.setWidth(LayoutParams.MATCH_PARENT);
            mWindow.setHeight(LayoutParams.MATCH_PARENT);
        } else {
            mWindow.setWidth(LayoutParams.WRAP_CONTENT);
            mWindow.setHeight(LayoutParams.WRAP_CONTENT);
        }

    }

    public View getContentView() {

        return mRootView;
    }

    public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        mWindow.setOnDismissListener(listener);
    }

    public void measureView() {
        mWindow.getContentView().measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }

    public boolean isShowing() {
        return mWindow.isShowing();
    }

    public boolean isFullScrean() {
        return isFullScrean;
    }

    /**
     * 横竖屏设置isfullscrean必须提前设置好才能生效
     * 
     * @param isFullScrean
     */
    public void setFullScrean(boolean isFullScrean) {
        this.isFullScrean = isFullScrean;
    }

}
