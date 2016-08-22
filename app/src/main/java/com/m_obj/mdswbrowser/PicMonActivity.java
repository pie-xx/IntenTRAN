package com.m_obj.mdswbrowser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

public class PicMonActivity extends Activity 
		implements SurfaceHolder.Callback, Camera.AutoFocusCallback {

	private static final String TAG = "ZXingTest";
	     
	private static final int MIN_PREVIEW_PIXCELS = 1024 * 480;
	private static final int MAX_PREVIEW_PIXCELS = 3200 * 3200;

	private Camera myCamera;
	private SurfaceView surfaceView;
	     
	private Boolean hasSurface;    
	private Boolean initialized;
	
	private Point screenPoint;
	private Point previewPoint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
         
        hasSurface = false;
        initialized = false;

        bis = null;
        inShutter = false;
        setContentView(R.layout.picmon);
    }
    @Override
    protected void onResume() {
        super.onResume();
         
        surfaceView = (SurfaceView)findViewById(R.id.preview_view);
        SurfaceHolder holder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(holder);
        } else {
            holder.addCallback(this);
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        AIRi.picmon = this;
    }
    
    @Override
    protected void onPause() {
		if( bis != null ){
			try {
				bis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			bis = null;
		}
		AIRi.picmon = null;
        closeCamera();
        if (!hasSurface) {
            SurfaceHolder holder = surfaceView.getHolder();
            holder.removeCallback(this);
        }
        super.onPause();
        finish();
    }
 
    /** SurfaceHolder.Callback */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }
 
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }
 
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
 
    }

    /** Camera.AutoFocusCallback */
	@Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (success)
            camera.takePicture(null, null, new Camera.PictureCallback() {	
				@Override
				public void onPictureTaken(byte[] data, Camera camera) {
					bis = new ByteArrayInputStream(data);
					LastCapFile = savePic(data);
					inShutter = false;
                    Intent intent = getIntent();
                    intent.setData(TranPack.insertCP(getApplicationContext(), LastCapFile));
                    setResult(RESULT_OK, intent);
					finish();
				}
			});
    }
	static public String getCapDir(){
        String capfolder = "book0";
        if(AIRi.momo != null) {
            capfolder = AIRi.momo.getItem("capfolder");
            if (capfolder.isEmpty()) {
                capfolder = "book0";
            }
        }
    	return Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_PICTURES ).getAbsolutePath()
    						+"/com.m_obj.cap/"+capfolder;
    	
    }
	String savePic(byte[] data){
    	File foutdir = new File(getCapDir());
    	if( !foutdir.exists() ){
    		foutdir.mkdirs();
    	}
	    String cappath = new SimpleDateFormat("yyyyMMddHHmmssSSS'.jpg'").format(new Date());
    	try {
        	FileOutputStream f = new FileOutputStream( foutdir.getAbsolutePath()+"/"+cappath );
			f.write(data);
			f.close();
			/***
			String[] paths = {fpath};
			String[] mimeTypes = {"image/jpeg"};
			MediaScannerConnection.scanFile(getApplicationContext(),
			                                paths,
			                                mimeTypes,
			                                new MediaScannerConnection.OnScanCompletedListener() {
			                    			    @Override
			                    			    public void onScanCompleted(String path, Uri uri) {
			                    			        Log.d("MediaScannerConnection", "Scanned " + path + ":");
			                    			        Log.d("MediaScannerConnection", "-> uri=" + uri);
			                    			    }
			                    			});

			sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, 
                     Uri.parse("file://" + Environment.getExternalStorageDirectory())));
             ***/
			return foutdir.getAbsolutePath()+"/"+cappath;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
    }
    
    boolean inShutter = false;
    boolean inPrvShutter = false;
    public void shutter(){
    	inShutter = true;
        Camera.Parameters parameters = myCamera.getParameters();
        myCamera.autoFocus(this);  	
    }
    /** devices */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (myCamera != null) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
            	shutter();
            }
        }
        return true;
    }

    public ByteArrayInputStream bis = null;
    public ByteArrayInputStream prv = null;
    public String LastCapFile = "";

    public String getCap(){
    	if(!inShutter){
    		shutter();
    	}
    	while(inShutter){
    		try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
    	}
    	return LastCapFile;
    }
    public InputStream getCap2(){
    	if(!inShutter){
    		shutter();
    	}
    	while(inShutter){
    		try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
    	}
    	return rtnIs(bis);
    }
    public InputStream getFocus(){
    	if(!inShutter){
        	myCamera.autoFocus(new Camera.AutoFocusCallback(){
				@Override
				public void onAutoFocus(boolean arg0, Camera arg1) {
					inShutter = false;
				}});
    	}
    	while(inShutter){
    		try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
    	}
    	return rtnIs(prv);
    }
    InputStream rtnIs(InputStream is){
    	if( is != null ){
    		try {
				is.reset();
			} catch (IOException e) {
				e.printStackTrace();
			}
    		return is;
    	}
    	return AIRi.momo.getBinItem("airi3.png");
    }
    public InputStream getPreview(){
    	if(!inPrvShutter){
    		inPrvShutter = true;
  
    		myCamera.setPreviewCallback(new Camera.PreviewCallback() {
				@Override
				public void onPreviewFrame(byte[] data, Camera camera) {
			        if (data == null)
			            return;
			        camera.addCallbackBuffer(data);
				    Camera.Parameters parameters = camera.getParameters();
				    Size size = camera.getParameters().getPreviewSize();

				    YuvImage image = new YuvImage(data, parameters.getPreviewFormat(),
				    		size.width, size.height, null);
				    ByteArrayOutputStream out = new ByteArrayOutputStream();

				    image.compressToJpeg(
			            new Rect(0, 0, image.getWidth(), image.getHeight()), 40, out);
				    prv = new ByteArrayInputStream(out.toByteArray());
					inPrvShutter = false;
			    }
    			}
    		);

        	while(inPrvShutter){
        		try {
    				Thread.sleep(10);
    			} catch (InterruptedException e) {
    				e.printStackTrace();
    				return null;
    			}
        	}
        	myCamera.setPreviewCallback(null);
        }
		return rtnIs(prv);
    }
    /**
     * 繧ｫ繝｡繝ｩ諠��繧貞�譛溷喧
     * @param holder
     */
    private void initCamera(SurfaceHolder holder) {
        try {
            openCamera(holder);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }
     
    private void openCamera(SurfaceHolder holder) throws IOException {
        if (myCamera == null) {
            myCamera = Camera.open();
            if (myCamera == null) {
                throw new IOException();
            }
        }
        myCamera.setPreviewDisplay(holder);
         
        if (!initialized) {
            initialized = true;
            initFromCameraParameters(myCamera);
        }
         
        setCameraParameters(myCamera);
        myCamera.startPreview();
    }
     
    /**
     * 繧ｫ繝｡繝ｩ諠��繧堤�譽�
     */
    private void closeCamera() {
        if (myCamera != null) {
            myCamera.stopPreview();
            myCamera.release();
            myCamera = null;
        }
    }
     
    /**
     * 繧ｫ繝｡繝ｩ諠��繧定ｨｭ螳�
     * @param camera
     */
    private void setCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();

        parameters.setPreviewSize(previewPoint.x, previewPoint.y);
        parameters.setPictureSize(previewPoint.x, previewPoint.y);
        camera.setParameters(parameters);
    }
     
    /**
     * 繧ｫ繝｡繝ｩ縺ｮ繝励Ξ繝薙Η繝ｼ繧ｵ繧､繧ｺ繝ｻ逕ｻ髱｢繧ｵ繧､繧ｺ繧定ｨｭ螳�
     * @param camera
     */
    private void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        WindowManager manager = (WindowManager)getApplication().getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
         
        if (width < height) {
            int tmp = width;
            width = height;
            height = tmp;
        }
        screenPoint = new Point(width, height);
        Log.d(TAG, "screenPoint = " + screenPoint);
        previewPoint = findPreviewPoint(parameters, screenPoint, false);
        Log.d(TAG, "previewPoint = " + previewPoint);
    }
     
    /**
     * 譛�←縺ｪ繝励Ξ繝薙Η繝ｼ繧ｵ繧､繧ｺ繧定ｨｭ螳�
     * @param parameters
     * @param screenPoint
     * @param portrait
     * @return
     */
    private Point findPreviewPoint(Camera.Parameters parameters, Point screenPoint, boolean portrait) {
        Point previewPoint = null;
        int diff = Integer.MAX_VALUE;
         
        for (Camera.Size supportPreviewSize : parameters.getSupportedPreviewSizes()) {
        	int pixels=supportPreviewSize.width * supportPreviewSize.height;
            if (pixels < MIN_PREVIEW_PIXCELS || pixels > MAX_PREVIEW_PIXCELS) {
                continue;
            }
             
            int supportedWidth = portrait ? supportPreviewSize.height : supportPreviewSize.width;
            int supportedHeight = portrait ? supportPreviewSize.width : supportPreviewSize.height;
            int newDiff = Math.abs(screenPoint.x * supportedHeight - supportedWidth * screenPoint.y);
             
            if (newDiff == 0) {
                previewPoint = new Point(supportedWidth, supportedHeight);
                break;
            }
             
            if (newDiff < diff) {
                previewPoint = new Point(supportedWidth, supportedHeight);
                diff = newDiff;
            }
        }
        if (previewPoint == null) {
            Camera.Size defaultPreviewSize = parameters.getPreviewSize();
            previewPoint = new Point(defaultPreviewSize.width, defaultPreviewSize.height);
        }
         
        return previewPoint;
    }

}
