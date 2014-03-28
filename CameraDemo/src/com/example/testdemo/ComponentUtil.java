
package com.example.testdemo;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class ComponentUtil {

    public static int screenWidthPx;
    public static int screenHeightPx;

    private ComponentUtil() {
    }

    /**
     * 设置radiogroup变成为初始化状态 其中有2个条件，一个需要在oncheckchange方法中做check与null的判断。 第二个
     * radiogroup中的第一个child必须是radiobutton，并且存在才可用。
     * 
     * @param radioGroup 要初始化的对象
     */
    public static void initialRadioGroup(RadioGroup radioGroup) {
        int radioButtonID = radioGroup.getCheckedRadioButtonId();
        RadioButton firstBtn = (RadioButton) radioGroup.getChildAt(0);
        int firstViewId = firstBtn.getId();
        if (firstViewId == radioButtonID) {
            radioGroup.clearCheck();
        }
        firstBtn.setChecked(true);
    }

    /**
     * 在给定的布局的中间显示菜单
     * 
     * @param view 菜单menu
     * @param height 菜单高度
     * @param width 菜单宽度
     * @param llytView 给定的布局
     */
    public static void showMenuInCenter(PopupWindows view, int height,
            int width, View llytView) {
        Context context = (Context) view.getContentView().getContext();
        if (height == 0) {
            view.setHeight(300);
        } else {
            view.setHeight(height);
        }

        if (width == 0) {
            view.setWidth(300);
        } else {
            view.setWidth(width);
        }

        view.showAtLocation(llytView, Gravity.CENTER, 0, 0);
    }

    public static void showMenuInCenterWindow(PopupWindows view, int height,
            int width) {
        Activity activity = (Activity) view.getContentView().getContext();
        if (height == 0) {
            view.setHeight(300);
        } else {
            view.setHeight(height);
        }

        if (width == 0) {
            view.setWidth(300);
        } else {
            view.setWidth(width);
        }
        view.showAtLocation(activity.getWindow().getDecorView(),
                Gravity.CENTER, 0, 0);
    }

}
