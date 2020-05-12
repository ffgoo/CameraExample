package com.jinasoft.cameraexample;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(!hasPermission(PERMISSIONS)){
                requestPermissions(PERMISSIONS,PERMISSIONS_REQUEST_CODE);
            }else{
                Intent intent = new Intent(LaunchActivity.this,Main2Activity.class);
                startActivity(intent);
                finish();
            }
        }else{
            Intent intent = new Intent(LaunchActivity.this,Main2Activity.class);
            startActivity(intent);
            finish();
        }
    }

    static final int PERMISSIONS_REQUEST_CODE = 1000;
    String[] PERMISSIONS = {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private boolean hasPermission(String[] permissions){
        int result;
        for (String perms : permissions){
            result = ContextCompat.checkSelfPermission(this,perms);
            if(result == PackageManager.PERMISSION_DENIED){
                return false;
            }
        }
        return true;
    }

    @Override
    public  void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions,grantResults);

        switch(requestCode) {
            case PERMISSIONS_REQUEST_CODE:

                if(grantResults.length > 0) {
                    boolean cameraPermissionAccepted = grantResults[0]
                            ==PackageManager.PERMISSION_GRANTED;
                    boolean diskPermissionAccepted = grantResults[1]
                            == PackageManager.PERMISSION_GRANTED;

                    if(!cameraPermissionAccepted || !diskPermissionAccepted)
                        showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
                    else{
                        Intent intent = new Intent(LaunchActivity.this,Main2Activity.class);
                        startActivity(intent);
                        finish();
                    }
                }
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg){

        AlertDialog.Builder builder = new AlertDialog.Builder(LaunchActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestPermissions(PERMISSIONS,PERMISSIONS_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.create().show();
    }

}
