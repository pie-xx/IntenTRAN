package com.m_obj.mdswbrowser;

import java.util.HashMap;
import java.util.concurrent.Executors;

//import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class BarcodeSettingActivity extends Activity {
	private WebView webView;//Web
	private static JSInterface jsi;
	private static boolean mScaning = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
Log.d("AIRi", "Start BarcodeSettingActivity");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
       makeWebView();
  
    	mScaning = false;

    	jsi.setPageItem("barcodesetting.html");
Log.d("AIRi", "end BarcodeSettingActivity");
	}
	
//	@SuppressLint("SetJavaScriptEnabled")
	private void makeWebView(){
		//WEB
		webView=new WebView(this);
		WebSettings settings = webView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setSavePassword(false);
		settings.setSaveFormData(false);
		settings.setSupportZoom(true);
		settings.setBuiltInZoomControls(true);
		settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

		//JavaScript
		jsi = new JSInterface(new Handler());
		webView.addJavascriptInterface( jsi, "bcd");
		webView.setWebChromeClient(new WebChromeClient());

		setContentView(webView);
	}
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getTitle().equals("Main")){
			Intent intent = new Intent(getApplicationContext(),com.m_obj.mdswbrowser.AIRi.class);
			intent.setAction(Intent.ACTION_MAIN);
			startActivity(intent);
			finish();
			return true;
		}
		return true;
	}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);
	    menu.add(0, 0, 0, "Main");
	    return true;
    }	
	public static final int REQUEST_CODE = 1;
	private static String scanResult;
	public String _scanBarcode(){
		mScaning = true;
		scanResult = "";
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        try{
            startActivityForResult(intent, REQUEST_CODE);
        }catch (ActivityNotFoundException e){
            Toast.makeText(this, "not found Barcode Scanner", Toast.LENGTH_SHORT ).show();
             return "";
        }
        while(mScaning){
        	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
	    		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
				return "";
			}
        }
       return scanResult;
	}
   @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if( requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
            scanResult = TranPack.Intent2XML(data);
        }
        mScaning = false;
    }

	public final class JSInterface {
		private HashMap<String,String> resultMap;
		//
		public JSInterface(Handler handler) {
			resultMap = new HashMap<String,String>();
		}
		//JavaScript
		public void log( String msg ){
			android.util.Log.d("AIRi", msg);
		}
		//------------ util commands -----------------------
		public String getBarcode(){
			return AIRi.getBarcode();
		}
		public String scanBarcode(){
			return _scanBarcode();
		}
		public void toast(String str){
			Toast.makeText(getApplicationContext(), "JavaScript:"+str, Toast.LENGTH_SHORT).show();
		}
		public String getSharedHttp(){
			return AIRi.getSharedHttp();
		}
		public String http(String url, String entity){
			return AIRi.http( url, entity);
		}
		
		public String getSharedBody(){
			return TranPack.decHttpStringBody(AIRi.getSharedHttp());
		}
		public String getSharedTitle(){
			return TranPack.decHttpTitle(AIRi.getSharedHttp());
		}
		//------------ service commands -----------------------

		public void jumptopage(String fname){
			Intent intent = new Intent(getApplicationContext(),com.m_obj.mdswbrowser.BarcodeSettingActivity.class);
			intent.putExtra("com.m_obj.mdswbrowser.action", "jumptopage");
			intent.putExtra("com.m_obj.mdswbrowser.sdata", fname);
			startActivity(intent);
		}

		public void setSendMode(String mstr){
			AIRi.setSendMode(mstr);
		}
		public String getSendMode(){
			return AIRi.getSendModeStr();
		}
		
		public String getIPList(){
			return AIRi.getIPList();
		}
		
		public void setMainTarget(String ipstr){
			AIRi.setMainTarget(ipstr);
		}
		public String getMainTarget(){
			return AIRi.getMainTarget();
		}

		public void setMyIP(String ipstr){
			AIRi.setMyIP(ipstr);
		}
		public String getMyIP(){
			return AIRi.getMyIP();
		}
		public String getMyName() {
			return AIRi.getMyName();
		}
		public void setMyName(String myName) {
			AIRi.setMyName(myName);
		}

		public void addTarget(String name, String ip, String ckey){
			AIRi.addTarget(name,ip,ckey);
		}
		public void delTarget(String ip){
			AIRi.delTarget(ip);
		}	
		public String getTargetList(){
			return AIRi.getTargetList();
		}
		
		//------------- setpage ---------------------------
		public void loadURL(String url){
			webView.loadUrl(url);
		}
		public boolean setPageItem(String pagename){
			String pstr=AIRi.momo.getItem(pagename);
			if( !pstr.equals("")){
				webView.loadDataWithBaseURL(AIRi.BaseURL+pagename, pstr, "text/html", "utf-8", null);
				return true;
			}
			return false;
		}
	}

}
