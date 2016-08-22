package com.m_obj.mdswbrowser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

public class SketchActivity extends Activity {
	private static final int MENU_CLEAR = 0;
	private static final int MENU_SAVE = 1;
	private static final int MENU_PEN = 2;
	private static final int MENU_ERASER = 3;
	
	SketchView view;
	static String remoteaddr = null;
	private static boolean bPaintingDone = false;
	private static Uri mcont = null;
	public static void setPaintingDone(Uri cont){
		mcont = cont;
		bPaintingDone = true;
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        AIRi.setLastGCIntent( intent );
        remoteaddr = intent.getStringExtra("com.m_obj.mdswbrowser.remoteaddr");

        if( remoteaddr == null ){
 	        switch(AIRi.getSendMode()){
			case Active:
	            if( !AIRi.getMainTarget().equals("") ) {
	            	//
	            	bPaintingDone = false;
        			Executors.newSingleThreadScheduledExecutor().submit(new Runnable() {
        				public void run() {
        					String targetURL = "http://"+AIRi.getMainTarget()+":"+AIRi.PORTSTR+"/paint";
        					AIRi.Phttp.sendUrlContent(targetURL, TranPack.encHttpPack("paint", "", "", null ));
        					while(!bPaintingDone){
         			        	try {
        							Thread.sleep(100);
        						} catch (InterruptedException e) {
        							e.printStackTrace();
         						}
        			        }
        					Intent intent = AIRi.getLastGCIntent();
        					intent.setData(mcont);
        	            	setResult(RESULT_OK, intent);
            				finish();
        				}
        			});
	            }
				return;
			case Passive:
            	//
            	// setResult(RESULT_OK, intent);
				finish();
				return;
			case Self:
				;
			}
        }
        view = new SketchView(this);
        setContentView(view);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);
	    menu.add(0, MENU_SAVE, 0, "Save");
	    menu.add(0, MENU_CLEAR, 0, "Clear");
	    menu.add(0, MENU_PEN, 0, "Pen");
	    menu.add(0, MENU_ERASER, 0, "Eraser");
	    return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	    switch ( item.getItemId() ) {
	    case MENU_CLEAR:
	    	view.clearDrawList(); 
	    	break;
	    case MENU_SAVE:
	    	String fname = view.saveToFile();
	    	if( remoteaddr==null ){
				Intent intent = getIntent();
				//
				intent.setData(TranPack.insertCP(this, fname));
				//
				setResult(RESULT_OK, intent);
	    	}else{
	    	    //
		        File file = new File(fname);
				try {
			        FileInputStream input;
					input = new FileInputStream(file);
			        byte[] data = new byte[(int)file.length()];
			        input.read(data);
			        input.close();
			        String entity = TranPack.encHttpPack("paint", fname, data, null);
		    		AIRi.Phttp.sendUrlContent("http://"+remoteaddr+":"+AIRi.PORTSTR+"/putfile", 
		    				entity, null, null, null );
				} catch (IOException e) {
					e.printStackTrace();					
				}
				bPaintingDone = true;
	    	}
			finish();
	    	break;
	    case MENU_PEN:
	    	view.setPen(); 
	    	break;
	    case MENU_ERASER:
	    	view.setEraser();
	    	break;
	    }
	    return true;
    }
}

class SketchView extends View  {
    private Paint paint;
    Bitmap bmp = null;
    Canvas bmpCanvas;
    Point oldpos = new Point(-1,-1);
    Point curpos = new Point(-1,-1);

	public SketchView(Context context) {
		super(context);
		this.setOnTouchListener(touchListener);
        
        paint = new Paint();
        setPen();
	}

	public void setPen(){
        paint.setColor(Color.DKGRAY);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(10);
	}
	public void setEraser(){
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(30);
	}

	public String saveToFile() {
		String fname = TranPack.getNewFileName();
    	//
    	try {
	    	FileOutputStream out = new FileOutputStream(fname);
	    	bmp.compress(CompressFormat.PNG, 100, out);
	    	out.flush(); 
	    	out.close();
    	} catch(Exception e) {
    		android.util.Log.d("AIRi", e.toString());
    		return "";
    	}

    	return fname;	
	}
	
    public void clearDrawList() {
    	bmpCanvas.drawColor(Color.WHITE);
		invalidate();
	}

    @Override
    protected void onDraw(Canvas canvas) {
		canvas.drawBitmap(bmp, 0, 0, null);
    }
    /**  */
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	    super.onSizeChanged(w,h,oldw,oldh);
	    bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
	    bmpCanvas = new Canvas(bmp);
	    bmpCanvas.drawColor(Color.WHITE);
    }
    
    private OnTouchListener touchListener = new OnTouchListener() {
    @Override
    public boolean onTouch(View v, MotionEvent event) {
    	curpos.x = (int)event.getX();
    	curpos.y = (int)event.getY();
    	if (oldpos.x < 0) { 
    		oldpos.x = curpos.x; 
    		oldpos.y = curpos.y;
    	}
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            //
            break;
        case MotionEvent.ACTION_MOVE:
            //
            bmpCanvas.drawLine(oldpos.x, oldpos.y, curpos.x, curpos.y, paint);
            break;
        case MotionEvent.ACTION_UP:
            //
            bmpCanvas.drawLine(oldpos.x, oldpos.y, curpos.x, curpos.y, paint);
            oldpos.x = -1; oldpos.y = -1;
            break;
        default:
        }
		oldpos.x = curpos.x; 
		oldpos.y = curpos.y;
        invalidate();
        return true;
    }
    };
}