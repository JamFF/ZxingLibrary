package com.ff.zxing;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Toast;

import com.ff.zxing.utils.Utils;

public class MainActivity extends Activity {

    private static final int REQUEST_SCAN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        findViewById(R.id.ll_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRuntimePermission();
            }
        });
    }

    // 获得运行时权限
    private void getRuntimePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] perms = {Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (checkSelfPermission(perms[0]) == PackageManager.PERMISSION_DENIED
                    || checkSelfPermission(perms[1]) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(perms, 200);
            } else {
                jumpScanPage();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case 200: {
                for (int i : grantResults) {
                    if (i == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, "权限拒绝", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                jumpScanPage();
                break;
            }
        }
    }

    // 跳转到扫码页
    private void jumpScanPage() {
        startActivityForResult(new Intent(this, CaptureActivity.class), REQUEST_SCAN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCAN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, data.getStringExtra(Utils.BAR_CODE), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "相机打开出错，请稍后重试", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
