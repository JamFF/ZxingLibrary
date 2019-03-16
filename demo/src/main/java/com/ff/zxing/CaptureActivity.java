package com.ff.zxing;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.ff.qrcode.library.CaptureCallback;
import com.ff.qrcode.library.camera.CameraManager;
import com.ff.qrcode.library.decode.DecodeThread;
import com.ff.qrcode.library.utils.CaptureActivityHandler;
import com.ff.qrcode.library.utils.InactivityTimer;
import com.ff.zxing.utils.BeepManager;
import com.ff.zxing.utils.Utils;
import com.google.zxing.Result;

import java.io.IOException;

/**
 * description:
 * author: FF
 * time: 2019/3/15 22:06
 */
public class CaptureActivity extends Activity implements CaptureCallback, SurfaceHolder.Callback {

    private static final String TAG = "CaptureActivity";

    private SurfaceView scanPreview; // SurfaceView控件
    private RelativeLayout scanContainer; // 布局容器
    private RelativeLayout scanCropView; // 布局中的扫描框


    private ObjectAnimator objectAnimator; // 属性动画
    private boolean isPause; // 是否暂停

    private InactivityTimer inactivityTimer; // 计时器
    private BeepManager beepManager; // 蜂鸣器
    private CaptureActivityHandler handler;
    private Rect mCropRect; // 矩形
    private CameraManager cameraManager; // 相机管理类
    private boolean isHasSurface; // SurfaceView控件是否存在，surfaceCreated

    private ProgressDialog dialog;// 扫描相册加载框

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 保持屏幕常量
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_capture);
        initView();
        initScan();
        initEvent();
    }

    private void initView() {
        scanPreview = findViewById(R.id.capture_preview);
        scanContainer = findViewById(R.id.capture_container);
        scanCropView = findViewById(R.id.capture_crop_view);
    }

    // 扫码初始化
    private void initScan() {
        ImageView scanLine = findViewById(R.id.scan_line);

        // 扫描线性动画(属性动画可暂停)
        float curTranslationY = scanLine.getTranslationY();
        objectAnimator = ObjectAnimator.ofFloat(scanLine, "translationY",
                curTranslationY, Utils.dp2px(this, 170));
        // 动画持续的时间
        objectAnimator.setDuration(4000);
        // 线性动画 Interpolator 匀速
        objectAnimator.setInterpolator(new LinearInterpolator());
        // 动画重复次数
        objectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        // 动画如何重复，从下到上，还是重新开始从上到下
        objectAnimator.setRepeatMode(ValueAnimator.RESTART);
    }

    private void initEvent() {
        final TextView tvLight = findViewById(R.id.tv_light);
        ToggleButton tbLight = findViewById(R.id.tb_light);

        // 闪光灯控制
        tbLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    tvLight.setText("关灯");
                    Utils.openFlashlight(cameraManager);
                } else {
                    tvLight.setText("开灯");
                    Utils.closeFlashlight();
                }
            }
        });

        // 打开相册
        findViewById(R.id.ll_album).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 打开相册，做权限判断
                Utils.openAlbum(CaptureActivity.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startScan();
    }

    @Override
    protected void onPause() {
        pauseScan();
        super.onPause();
    }

    // 开始扫描
    private void startScan() {
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);

        if (isPause) {
            // 如果是暂停，扫描动画应该要暂停
            objectAnimator.resume();
            isPause = false;
        } else {
            // 开始扫描动画
            objectAnimator.start();
        }

        // 初始化相机管理
        cameraManager = new CameraManager(this);
        handler = null; // 重置handler
        if (isHasSurface) {
            initCamera(scanPreview.getHolder());
        } else {
            // 等待surfaceCreated来初始化相机
            scanPreview.getHolder().addCallback(this);
        }
        // 开启计时器
        if (inactivityTimer != null) {
            inactivityTimer.onResume();
        }
    }

    // 暂停扫描
    private void pauseScan() {
        if (handler != null) {
            // handler退出同步并置空
            handler.quitSynchronously();
            handler = null;
        }
        // 计时器的暂停
        if (inactivityTimer != null) {
            inactivityTimer.onPause();
        }
        // 关闭蜂鸣器
        beepManager.close();
        // 关闭相机管理器驱动
        cameraManager.closeDriver();
        if (!isHasSurface) {
            // remove等待
            scanPreview.getHolder().removeCallback(this);
        }
        // 动画暂停
        objectAnimator.pause();
        isPause = true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "surfaceCreated: SurfaceHolder is null");
            return;
        }

        if (!isHasSurface) {
            isHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isHasSurface = false;
    }

    // 初始化相机
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("SurfaceHolder is null");
        }
        if (cameraManager.isOpen()) {
            Log.e(TAG, "surfaceCreated: camera is open");
            return;
        }

        try {
            cameraManager.openDriver(surfaceHolder);
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager, DecodeThread.ALL_MODE);
            }
            initCrop();
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            Utils.displayFrameworkBugMessageAndExit(this);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.lang.RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            Utils.displayFrameworkBugMessageAndExit(this);
        }
    }

    // 初始化截取的矩形区域
    private void initCrop() {
        // 获取相机的宽高
        int cameraWidth = cameraManager.getCameraResolution().y;
        int cameraHeight = cameraManager.getCameraResolution().x;

        // 获取布局中扫描框的位置信息
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - Utils.getStatusBarHeight(this);

        // 获取截取的宽高
        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        // 获取布局容器的宽高
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        // 计算最终截取的矩形的左上角顶点x坐标
        int x = cropLeft * cameraWidth / containerWidth;
        // 计算最终截取的矩形的左上角顶点y坐标
        int y = cropTop * cameraHeight / containerHeight;

        // 计算最终截取的矩形的宽度
        int width = cropWidth * cameraWidth / containerWidth;
        // 计算最终截取的矩形的高度
        int height = cropHeight * cameraHeight / containerHeight;

        // 生成最终的截取的矩形
        mCropRect = new Rect(x, y, width + x, height + y);
    }

    @Override
    public Rect getCropRect() {
        return mCropRect;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void handleDecode(Result result, Bundle bundle) {
        // 扫码成功之后回调的方法
        if (inactivityTimer != null) {
            inactivityTimer.onActivity();
        }
        // 播放蜂鸣声
        beepManager.playBeepSoundAndVibrate();

        // 将扫码的结果返回到MainActivity
        Intent intent = new Intent();
        intent.putExtra(Utils.BAR_CODE, result.getText());
        Utils.setResultAndFinish(CaptureActivity.this, RESULT_OK, intent);
    }


@Override
protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
    // 相册返回
    if (requestCode == Utils.SELECT_PIC_KITKAT // 4.4及以上图库
            && resultCode == Activity.RESULT_OK) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                showProgressDialog();
                Uri uri = data.getData();
                String path = Utils.getPath(CaptureActivity.this, uri);
                Result result = Utils.scanningImage(path);
                Intent intent = new Intent();
                if (result == null) {
                    intent.putExtra(Utils.BAR_CODE, "未发现二维码/条形码");
                } else {
                    // 数据返回
                    intent.putExtra(Utils.BAR_CODE, Utils.recode(result.getText()));
                }
                Utils.setResultAndFinish(CaptureActivity.this, RESULT_OK, intent);
                dismissProgressDialog();
            }
        }).start();
    }
}

    private void showProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialog == null) {
                    dialog = new ProgressDialog(CaptureActivity.this);
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                }
                dialog.setMessage("扫描中");    //设置内容
                dialog.setCancelable(false);//点击屏幕和按返回键都不能取消加载框
                dialog.show();
            }
        });
    }

    private void dismissProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (objectAnimator != null) {
            objectAnimator.end();
        }
        if (inactivityTimer != null) {
            inactivityTimer.shutdown();
        }
        super.onDestroy();
    }
}
