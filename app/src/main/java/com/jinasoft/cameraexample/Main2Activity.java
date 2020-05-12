package com.jinasoft.cameraexample;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;

public class Main2Activity extends AppCompatActivity {

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Handler mHandler;
    private ImageReader mImageReader;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mSession;
    private int mDeviceRotation;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private SensorManager mSensorManager;
    private DeviceOrientation deviceOrientation;
    int mDSI_height, mDSI_width;

    int CAMERA_STATE;
    String mCameraId;



    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(ExifInterface.ORIENTATION_NORMAL, 0);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_90, 90);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_180, 180);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_270, 270);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        CAMERA_STATE = Camera.CameraInfo.CAMERA_FACING_FRONT;


        Button button = findViewById(R.id.take_photo);
        button.setOnClickListener(new View.OnClickListener() {
            @TargetApi(21)
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });


        mSurfaceView = findViewById(R.id.surfaceView);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        deviceOrientation = new DeviceOrientation();

        initSurfaceView();

        Button Change_camera = findViewById(R.id.change_camera);
        Change_camera.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                String id = mCameraDevice.getId();
                Log.d("cameraID@@@",id);
                if(id.equals("0")){
                    mCameraDevice.close();
                   CAMERA_STATE = 1;
                    initCameraAndPreview();
                }else{
                    mCameraDevice.close();
                    CAMERA_STATE=0;
                    initCameraAndPreview();
                }

            }
        });



    }
    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(deviceOrientation.getEventListener(),mAccelerometer,SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(deviceOrientation.getEventListener(),mMagnetometer,SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause(){
        super.onPause();

        mSensorManager.unregisterListener(deviceOrientation.getEventListener());
    }
    public void initSurfaceView(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mDSI_height = displayMetrics.heightPixels;
        mDSI_width = displayMetrics.widthPixels;

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCameraAndPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {


            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if(mCameraDevice != null){
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
            }
        });
//        Button changeCamera =findViewById(R.id.change_camera);
//        changeCamera.setOnClickListener(new View.OnClickListener() {
//            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//            @Override
//            public void onClick(View v) {
//
//                }
//            }
//        });

    }
    @TargetApi(21)
    public void initCameraAndPreview() {


        HandlerThread handlerThread = new HandlerThread("CAMERA2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        Handler mainHandler = new Handler(getMainLooper());
        try {

            if(CAMERA_STATE==0) {
                mCameraId = "" + CameraCharacteristics.LENS_FACING_FRONT; // 후면 카메라 사용
            }else{
                mCameraId = "" + CameraCharacteristics.LENS_FACING_BACK; // 후면 카메라 사용
            }

            CameraManager mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size largestPreviewSize = map.getOutputSizes(ImageFormat.JPEG)[0];
            Log.i("LargestSize", largestPreviewSize.getWidth() + " " + largestPreviewSize.getHeight());
/**카메라 사이즈**/
            setAspectRatioTextureView(largestPreviewSize.getHeight(),largestPreviewSize.getWidth());

            mImageReader = ImageReader.newInstance(largestPreviewSize.getWidth(), largestPreviewSize.getHeight(), ImageFormat.JPEG,/*maxImages*/7);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mainHandler);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mCameraManager.openCamera(mCameraId, deviceStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Toast.makeText(this, "카메라를 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }





    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            new SaveImageTask().execute(bitmap);

        }
    };

    @TargetApi(21)
    private CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            try{
                takePreview();
            }catch(CameraAccessException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if(mCameraDevice != null){
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Toast.makeText(Main2Activity.this,"카메라를 열지 못함",Toast.LENGTH_SHORT).show();
        }
    };

    @TargetApi(21)
    public void takePreview() throws CameraAccessException {
        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewBuilder.addTarget(mSurfaceHolder.getSurface());
        mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceHolder.getSurface(), mImageReader.getSurface()), mSessionPreviewStateCallback, mHandler);
    }
    @TargetApi(21)
    private CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mSession = session;

            try{
                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                mSession.setRepeatingRequest(mPreviewBuilder.build(),null,mHandler);
            }catch (CameraAccessException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Toast.makeText(Main2Activity.this,"카메라 실패",Toast.LENGTH_SHORT).show();
        }
    };

    @TargetApi(21)
    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result){
            mSession = session;
            unlockFocus();
        }
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            mSession = session;
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };

    @TargetApi(21)
    public void takePicture(){
        try{
            /**플래시 설정**/
            CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);


            mDeviceRotation = ORIENTATIONS.get(deviceOrientation.getOrientation());
            Log.d("@@@",mDeviceRotation+"");

            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,mDeviceRotation);
                CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            mSession.capture(mCaptureRequest, mSessionCaptureCallback , mHandler);

            MediaActionSound mediaActionSound = new MediaActionSound();
            mediaActionSound.play(MediaActionSound.SHUTTER_CLICK);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //비트맵 변환
    public Bitmap getRotatedBitmap(Bitmap bitmap, int degrees) throws Exception{
        if(bitmap == null) return null;
        if(degrees ==0) return bitmap;

        Matrix m = new Matrix();
        m.setRotate(degrees, (float) bitmap.getWidth()/2, (float)bitmap.getHeight()/2);

        return Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m, true);
    }

    @TargetApi(21)
    private void unlockFocus(){
        try{
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mSession.capture(mPreviewBuilder.build(),mSessionCaptureCallback,mHandler);
            mSession.setRepeatingRequest(mPreviewBuilder.build(), mSessionCaptureCallback,
                    mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public static final String insertImage(ContentResolver cr,
                                           Bitmap source,
                                           String title,
                                           String description){

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DISPLAY_NAME,title);
        values.put(MediaStore.Images.Media.DESCRIPTION,description);
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

        Uri uri = null;
        String StringURL = null;

        try{
            uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

            if(source != null){
                OutputStream imageOut = cr.openOutputStream(uri);
                try{
                    source.compress(Bitmap.CompressFormat.JPEG,50,imageOut);
                }finally {
                    imageOut.close();
                }
            }else{
                cr.delete(uri,null,null);
                uri = null;
            }
        } catch (Exception e) {
            if(uri !=null) {
                cr.delete(uri,null,null);
                uri =null;
            }
        }
        if(uri !=null){
            StringURL = uri.toString();
        }
        return StringURL;
    }

    private class SaveImageTask extends AsyncTask<Bitmap, Void,Void>{
        @Override
        protected void onPostExecute(Void aVoid){
            super.onPostExecute(aVoid);

            Toast.makeText(Main2Activity.this,"사진 저장 완료",Toast.LENGTH_SHORT).show();
        }
        @Override
        protected Void doInBackground(Bitmap... data){
            Bitmap bitmap = null;
            try{
                bitmap = getRotatedBitmap(data[0], mDeviceRotation);
            } catch (Exception e) {
                e.printStackTrace();
            }
            insertImage(getContentResolver(), bitmap,""+System.currentTimeMillis(),"");

            return null;
        }
    }
    private void setAspectRatioTextureView(int ResolutionWidth, int ResolutionHeight)
    {
        if(ResolutionWidth > ResolutionHeight){
            int newWidth = mDSI_width;
            int newHeight = ((mDSI_width * ResolutionWidth)/ResolutionHeight);
            updateTextureViewSize(newWidth,newHeight);
        }else{
            int newWidth = mDSI_width;
            int newHeight = ((mDSI_width * ResolutionHeight)/ResolutionWidth);
            updateTextureViewSize(newWidth,newHeight);
        }
    }
    private void updateTextureViewSize(int viewWidth,int viewHeight){
        Log.d("@@@","TextureView Width : " + viewWidth + "TextureView Height : "+viewHeight);
        mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(viewWidth,viewHeight));
    }
}

