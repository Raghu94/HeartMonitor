package com.raghu.heartmonitor;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

/**
 * Created by RaghuTeja on 27/02/2017.
 */

public class MonitorView extends JavaCameraView {
    public MonitorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void setFlashOn() {
        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
//        params.set("orientation", "portrait");
//        params.setRotation(90);
        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(90);


    }

    public void setFlashOff() {
        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
//        params.set("orientation", "portrait");
//        params.setRotation(90);
        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(90);
    }
}
