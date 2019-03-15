package com.ff.qrcode.library;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;

import com.ff.qrcode.library.camera.CameraManager;
import com.google.zxing.Result;

/**
 * 通过接口解藕Activity
 */
public interface CaptureCallback {

    Rect getCropRect();

    Handler getHandler();

    CameraManager getCameraManager();

    void handleDecode(Result result, Bundle bundle);

    void setResult(int resultCode, Intent intent);

    void finish();
}
