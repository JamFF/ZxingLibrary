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

    Rect getCropRect();// 获取矩形

    Handler getHandler();// 获取Handler

    CameraManager getCameraManager();// 获取CameraManager

    /**
     * 扫码成功之后回调的方法
     *
     * @param result
     * @param bundle
     */
    void handleDecode(Result result, Bundle bundle);

    /**
     * {@link android.app.Activity#setResult(int, Intent)}
     *
     * @param resultCode The result code to propagate back to the originating
     *                   activity, often RESULT_CANCELED or RESULT_OK
     * @param data       The data to propagate back to the originating activity.
     */
    void setResult(int resultCode, Intent data);

    /**
     * {@link android.app.Activity#finish()}
     */
    void finish();
}
