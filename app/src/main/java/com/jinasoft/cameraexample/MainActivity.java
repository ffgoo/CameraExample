package com.jinasoft.cameraexample;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraDevice;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
//
//    Camera camera;
//    SurfaceView surfaceView;
//    SurfaceHolder surfaceHolder;
//    boolean previewing = false;


private CameraDevice camera;
private SurfaceView mCameraView;
private SurfaceHolder mCameraHolder;
private Camera mCamera;
private Button mStart;
private boolean recording = false;
private MediaRecorder mediaRecorder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = findViewById(R.id.cameraView);
//
        init();
    }

    private void init(){
        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);

        //surfaceView setting
        mCameraHolder = mCameraView.getHolder();
        mCameraHolder.addCallback(this);
        mCameraHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    //surface 관련 구현
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try{
            if(mCamera == null){
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //view 가 존재하지 않음
        if(mCameraHolder.getSurface() == null){
            return;
        }
        try{
            mCamera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Camera.Parameters parameters = mCamera.getParameters();

        //오토포커스 지원 여부
        List<String> focusModes = parameters.getSupportedFocusModes();
        if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        mCamera.setParameters(parameters);
        try{
            mCamera.setPreviewDisplay(mCameraHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
            if(mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
    }
}
