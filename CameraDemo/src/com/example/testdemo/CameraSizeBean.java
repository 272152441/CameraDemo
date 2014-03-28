
package com.example.testdemo;

import android.hardware.Camera.Size;

public class CameraSizeBean extends ShowTextBean {

    private Size size;

    public CameraSizeBean(Size size) {
        this.size = size;
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    @Override
    public String getShowText() {
        return size.width + "x" + size.height
        // + "     "
        // + FileUtils.FormetFileSize(size.width * size.height)
        ;
    }

}
