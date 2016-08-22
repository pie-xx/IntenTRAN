package com.m_obj.mdswbrowser;

//import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.ClipboardManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import com.m_obj.mdswbrowser.R;
import com.m_obj.mdswbrowser.PieHTTP;

public class AIRi extends Activity {
	public static PicMonActivity picmon = null;
	public static Momo momo;
	private WebView webView;//Web�r���[

	private AiriMenu kmenu;
	private static JSInterface jsi;
	private String pageparam;
	private String lastpage;
	private int debugKeyCount;

	public static final String BarcodeResultAction = "BarcodeResultAction";
	public static final int PORT = 8626;
	public static final String BaseURL = "http://localhost:8626/";
	public static AIRi airi;
	public static final String PORTSTR = Integer.toString(PORT);
	private ExecutorService mHTTPTask;
	public static PieHTTP Phttp = null;

	private static List<TargetHost> targetHostList = new ArrayList<TargetHost>();

	private static String MyName = "";
	public static String getMyName() {
		MyName = momo.getItem("MyName");
		if( MyName.equals("")){
			MyName = Build.MODEL;
		}
		return MyName;
	}
	public static void setMyName(String myName) {
		MyName = myName;
		momo.setItem("MyName", MyName);
	}
	private static String MyIP = "";
	private static String MainTarget = "";

	public static enum ESMode {Active,Passive,Self};
	private static ESMode mSendMode = ESMode.Self;	// ���M���[�h direct, indirect, self
	public static enum EVMode {Direct,Self};
	private static EVMode mShowMode = EVMode.Self;		// �\�����[�h direct, self

	private static boolean mPeriodicWiFiReset = false;	// ���I��WiFi����
//	private static boolean mViewWithIntent = false;		// view �A�N�V������intent�ŏ���
	
	private static Intent LastIntent = null;
	private static Intent LastGCIntent = null;
	public synchronized static Intent getLastGCIntent() {
		return LastGCIntent;
	}
	public synchronized static void setLastGCIntent(Intent lastGCIntent) {
		LastGCIntent = lastGCIntent;
	}
	private static String SharedHttp = "";
	private static long LastSharedTime;
	
	public void setClip(String str){
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
		clipboard.setText(str);
	}
	public String getClip(){
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
		return (String) clipboard.getText();
	}
	
	public static void setBarcode(String barcodestr){
		scanResult = barcodestr;
	}
	public static String getBarcode(){
		return scanResult;
	}
	
	public static void setSharedHttp(String str){
		SharedHttp = str;
		LastSharedTime= System.currentTimeMillis();
		momo.setItem("SharedHttp", SharedHttp);
	}
	public static String getSharedHttp(){
		if(SharedHttp==null){
			SharedHttp="";
		}
		if(SharedHttp.equals("")){
			SharedHttp = momo.getItem("SharedHttp");
		}
		return SharedHttp;
	}
	public static void clearSharedHttp(){
		SharedHttp="";
		momo.setItem("SharedHttp","");
	}
	public static String getSharedTime(){
		return Integer.toString((int) LastSharedTime);
	}
	// IntenTRAN���j���[�o�R��
	public static boolean getViewWithIntent(){
	//	return mViewWithIntent;
		return mShowMode != EVMode.Self;
	}
	public static boolean isDirectSend(){
		return mSendMode != ESMode.Self;
	}
	
	public static void setLastIntent( Intent intent ){
		LastIntent = intent;
	}
	public static Intent getLastIntent(){
		return LastIntent;
	}
	public static void  setItem(String key, String value){
		momo.setItem(key, value);
	}
	public static String getMyIP(){
		return MyIP;
	}
	public static void setMyIP(String ipstr){
		MyIP = ipstr;
	}
	public static String getMainTarget(){
		return MainTarget;
	}
	public static void setMainTarget(String ipstr){
		if(ipstr.startsWith("/"))
			ipstr = ipstr.replaceAll("\\/", "");
		MainTarget = ipstr;
		momo.setItem("MainTarget", MainTarget);
	}
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		airi = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent intent = getIntent();
        String action = intent.getAction();

        makeWebView();
        Phttp = new PieHTTP();
        MainTarget = momo.getItem("MainTarget");
        MyName = momo.getItem("MyName");
        if( MyName.equals("")){
        	MyName=Build.MODEL;
        }

        setSendMode(momo.getItem("SendMode"));
        
	    if(Intent.ACTION_MAIN.equals(action)){
	    	mScaning = false;
	    	scanResult = "";
	    	jsi.setPageItem("start.html");
        }else
   	    if(BarcodeResultAction.equals(action)){
   	    	mScaning = false;
   	    	jsi.setPageItem("setting.html");
        }else
		if (Intent.ACTION_SEND.equals(action)) {
			LastIntent = intent;
			setSharedHttp(TranPack.encHttpPack("send",
					intent.getStringExtra("android.intent.extra.SUBJECT"),
					intent.getStringExtra("android.intent.extra.TEXT"),
					intent));
	      	setClip(intent.getStringExtra("android.intent.extra.TEXT"));
			switch(mSendMode){
			case Active:
	            if( !MainTarget.equals("") ) {
	    			sendCommand("send",intent.getStringExtra("android.intent.extra.SUBJECT"),
	    				intent.getStringExtra("android.intent.extra.TEXT"), intent);
	            }
				finish();
	            break;
			case Passive:
				finish();
				break;
			case Self:
				CommandDispatch("send",	getSharedHttp()	);
			}
		}else
		if (Intent.ACTION_VIEW.equals(action)) { 
			LastIntent = intent;
			String url = intent.getData().toString();
			setSharedHttp(TranPack.encHttpPack("view","",url,intent));
	      	setClip(url);

			switch(mSendMode){
			case Active:
	            if( !MainTarget.equals("") ) {
	            	setLastGCIntent(intent);
	    			sendCommand("view", "", url, intent);
	            }
				finish();
	            break;
			case Passive:
            	setLastGCIntent(intent);
				finish();
				break;
			case Self:
            	setLastGCIntent(null);
            	if(getViewWithIntent()){
					Intent bi = new Intent(Intent.ACTION_VIEW, Uri.parse(url));  
					bi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(bi);
					finish();
            	}else{
            		CommandDispatch("view",	getSharedHttp() );
            	}
			}
		}else{
            String command = intent.getStringExtra("com.m_obj.mdswbrowser.action");
            String sdata = intent.getStringExtra("com.m_obj.mdswbrowser.sdata");
        	setSharedHttp(sdata);

            setClip(TranPack.decHttpStringBody(sdata));

            switch(mSendMode){
			case Active:
           		CommandDispatch(command, sdata );
	            break;
			case Passive:
            	setLastGCIntent(intent);
				finish();
				break;
			case Self:
           		CommandDispatch(command, sdata );
			}
        }
	}

	private void CommandDispatch(String command, String sdata){
        if( command.equals("view")){
        	jsi.setPageItem("start.html");
        }else
        if( command.equals("send")){
        	jsi.setPageItem("start.html");
        }else
        if( command.equals("register")){
        	String hostname = getTagValue("hostname",sdata);
        	String cryptkey = getTagValue("cryptkey",sdata);
        	String hostip = getTagValue("remoteaddr",sdata);
        	addTarget(hostname, hostip, cryptkey);
	    	jsi.setPageItem("start.html");
        }else
        if( command.equals("restart")){
           	jsi.setPageItem("start.html");
        }else
        if( command.equals("jumptopage")){
         	jsi.setPageItem(sdata);
        }else
        if( command.equals("clip")){
           	setClip(sdata);
           	setItem("LastClip",sdata);
           	jsi.setPageItem("input.html");
        }else
        if( command.equals("putfile")){
			Intent gcintent = getLastGCIntent();
			gcintent.setData(TranPack.insertCP(this, sdata));
			setResult(RESULT_OK, gcintent);
        }
	}					

	public static String getTagValue(String tag, String str){
		List<String> items = getTagValues(tag,str);
		if( items.size()==0)
			return "";
		return items.get(0);
	}
	public static List<String> getTagValues(String tag, String str){
		List<String> results = new ArrayList<String>();
		String[] items = str.split("<"+tag+">");
		for(int n=1; n < items.length; ++n ){
			results.add(new String(items[n].split("</"+tag+">")[0]));
		}
		return results;
	}
	public static String http(String url, String entity){
		return Phttp.sendUrlContent(url, entity);
	}
	////////////////////////////////////////////////////////////////////
	public static String targetAction;
	public static String targetXML = "";
	public static void sendCommand( String action, String title, String body, Intent intent ){
		targetAction = action;
		targetXML =  TranPack.encHttpPack(action, title, body, intent );
		if(mSendMode==ESMode.Active) {
			Executors.newSingleThreadScheduledExecutor().submit(new Runnable() {
				public void run() {
					String targetURL = "http://"+MainTarget+":"+PORTSTR+"/"+targetAction;
					Phttp.sendUrlContent(targetURL, targetXML);
				}
			});
		}
	}
	public static String makeURLXML( String action, String url, String body, String stype ){
		return "<data><action>"+action+"</action><url>"+Uri.encode(url)+
			"</url><body>"+Uri.encode(body)+"</body><stype>"+stype+"</stype></data>";
	}
	public static void setSendMode(String modestr){
		modestr = modestr.toLowerCase();
		if(modestr.startsWith("a")){
			mSendMode = ESMode.Active;
		}else
		if(modestr.startsWith("p")){
			mSendMode = ESMode.Passive;
		}else
		if(modestr.startsWith("s")){
			mSendMode = ESMode.Self;
		}
		momo.setItem("SendMode", modestr);
	}
	public static String getSendModeStr(){
		switch(mSendMode){
		case Active:	return "Active";
		case Passive:	return "Passive";
		case Self:		return "Self";
		}
		return "";
	}
	public static ESMode getSendMode(){
		return mSendMode;
	}

	public static void setShowMode(String modestr){
		modestr = modestr.toLowerCase();
		if(modestr.startsWith("d")){
			mShowMode = EVMode.Direct;
		}else
		if(modestr.startsWith("s")){
			mShowMode = EVMode.Self;
		}
		momo.setItem("SendMode", modestr);
	}
	public static String getShowModeStr(){
		switch(mShowMode){
		case Direct:	return "Direct";
		case Self:		return "Self";
		}
		return "";
	}
	public static EVMode getShowMode(){
		return mShowMode;
	}
	
	public static void setPeriodicWiFiReset(boolean flag){
		mPeriodicWiFiReset = flag;
	}
	public static boolean getPeriodicWiFiReset(){
		return mPeriodicWiFiReset;
	}

	
	public static void addTarget(String name, String ip, String ckey){
		for(TargetHost t: targetHostList){
			if(t.hostip.equals(ip)){
				t.hostname = name;
				t.cryptkey = ckey;
				return;
			}
		}
		targetHostList.add(new TargetHost(name,ip,ckey));
		momo.setItem("Targets", TagetListStr());
	}
	public static void delTarget(String ip){
		setTargetList(getTargetList());
		for(TargetHost t: targetHostList){
			if(t.hostip.equals(ip)){
				targetHostList.remove(t);
				momo.setItem("Targets", TagetListStr());
				return;
			}
		}
	}
	public static String TagetListStr(){
	    StringBuffer hl = new StringBuffer();
	    for(int n=0; n<targetHostList.size(); ++n){
	    	String hostip = targetHostList.get(n).hostip;
	    	String hostname = targetHostList.get(n).hostname;
	    	String hostStr = "<target><name>"+hostname+"</name><ip>"+hostip+"</ip></target>";
	    	hl.append(hostStr);
	    }
	    return "<targets>"+hl.toString()+"</targets>";
	}
	public static String getTargetList(){
		return momo.getItem("Targets");
	}
	public static void setTargetList(String tstr){
		targetHostList.clear();
		String[] tags = tstr.split("<target>");
		for(int n=1; n<tags.length; ++n){
			String name = getTagValue("name",tags[n]);
			String ip = getTagValue("ip",tags[n]);
			targetHostList.add(new TargetHost(name,ip,""));
		}
	}
	public static void clearAllTarget(){
		targetHostList.clear();
		momo.setItem("Targets", TagetListStr());
	}
	public static String getIPList(){
	    List <String> myaddrs = SimpleHTTPsever.getIPAddressList();
	    StringBuffer myaddrStr = new StringBuffer();
	    for(int n=0; n<myaddrs.size(); ++n){
	    	String[] ipinfos = myaddrs.get(n).split(":");
	    	myaddrStr.append("<iplist><net>"+ipinfos[0]+"</net><ip>"+ipinfos[1]+"</ip></iplist>");
	    }
		return "<iplists>"+myaddrStr.toString()+"</iplists>";
	}

	public static final int REQUEST_CODE = 1;
	private static boolean mScaning;
	private static String scanResult;
	public String scanBarcode(){
	//	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		mScaning = true;
		scanResult = "";
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        try{
            startActivityForResult(intent, REQUEST_CODE);
        }catch (ActivityNotFoundException e){
            Toast.makeText(this, "not found Barcode Scanner", Toast.LENGTH_SHORT ).show();
    	//	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
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
	//	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
       return scanResult;
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if( requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
            scanResult = TranPack.Intent2XML(data);
        }
        mScaning = false;
   }	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        
        return true;
    }

//	@SuppressLint("SetJavaScriptEnabled")
	private void makeWebView(){
		//WEB�r���[�̐���
		webView=new WebView(this);
		WebSettings settings = webView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setSavePassword(false);
		settings.setSaveFormData(false);
		settings.setSupportZoom(true);
		settings.setBuiltInZoomControls(true);
		settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

		//JavaScript�C���^�t�F�[�X�̐���
		jsi = new JSInterface(new Handler());
		webView.addJavascriptInterface( jsi, "airi");
		webView.setWebChromeClient(new WebChromeClient());

		//DB����
		momo = new Momo( this );
		kmenu = new AiriMenu();

		setContentView(webView);
		lastpage = momo.getItem("#lastpage");

	}

	public void startService(){
	    Intent sintent=new Intent(this,com.m_obj.mdswbrowser.PieHTTPservice.class);
	    startService(sintent);		
	}
	public void stopService(){
	    Intent sintent=new Intent(this,com.m_obj.mdswbrowser.PieHTTPservice.class);
    	stopService(sintent);
 	}
    
	@Override
	public void onResume(){
		super.onResume();
		startService();
	}
	
	@Override
	public void onPause(){
		super.onPause();
		momo.setItem("#lastpage", lastpage);
		finish();
//		momo.close();
	}

	int DebugKey[]={KeyEvent.KEYCODE_VOLUME_UP,KeyEvent.KEYCODE_VOLUME_DOWN,
			KeyEvent.KEYCODE_VOLUME_UP,KeyEvent.KEYCODE_VOLUME_DOWN,
			KeyEvent.KEYCODE_SEARCH
			};
	public boolean  onKeyDown  (int keyCode, KeyEvent event){
		String url = "";
		if(DebugKey[debugKeyCount++]!=keyCode){
			debugKeyCount = 0;
		}else{
			if(debugKeyCount >= DebugKey.length ){
				debugKeyCount = 0;
				webView.loadUrl("file:///android_asset/list.html");
				return true;
			}
		}
		switch( keyCode ){
		case KeyEvent.KEYCODE_BACK:
			url = kmenu.getUrl("back");
			break;
		case KeyEvent.KEYCODE_SEARCH:
			url = kmenu.getUrl("search");
			break;
		case KeyEvent.KEYCODE_VOLUME_UP:
			url = kmenu.getUrl("vup");
			break;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			url = kmenu.getUrl("vdown");
			break;
		}
		if( url.equals("") ){
			return super.onKeyDown(keyCode, event);
		}
		jump2menuURL(url);
		return true;
	}

	void jumptoPage(String pagename){
		if( ! jsi.setPageItem(pagename))
			webView.loadUrl("file:///android_asset/"+pagename);
	}
	void jump2menuURL(String url){
		if(url.indexOf(":")==-1){
			jsi.setPageItem(url);
		}else
		if(url.startsWith(BaseURL)){
			jsi.setPageItem(url.substring(BaseURL.length()));
		}else{
			webView.loadUrl(url);
		}
	}
	//////menu/////////////////////////////////////////////////////
	@Override
	public boolean  onPrepareOptionsMenu(Menu menu){
		menu.clear();
		menu.add(0,0,0,"Stop").setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		menu.add(0,0,0,"Setting").setIcon(android.R.drawable.ic_menu_manage);
		menu.add(0,0,0,"Target").setIcon(android.R.drawable.ic_dialog_alert);
		menu.add(0,0,0,"PicMon").setIcon(android.R.drawable.ic_menu_camera);
		menu.add(0,0,0,"Start").setIcon(R.drawable.ic_menu_refresh);
		menu.add(0,0,0,"Get").setIcon(android.R.drawable.ic_input_get);
		menu.add(0,0,0,"EditScript").setIcon(android.R.drawable.stat_notify_sync);
		return true;
	}
	public static String menuCommand;
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		menuCommand = (String) item.getTitle();
		if(item.getTitle().equals("SelectFile")){
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("image/*");
			startActivityForResult(intent, REQUEST_CODE);			
			return true;
		}
		if(item.getTitle().equals("Stop")){
			stopService();
			finish();
			return true;
		}
		if(item.getTitle().equals("Setting")){
	    	jsi.setPageItem("setting.html");
			return true;
		//	Intent intent = new Intent(getApplicationContext(),com.m_obj.mdswbrowser.SettingActivity.class);
		//	startActivity(intent);
		//	return true;
		}
		if(item.getTitle().equals("EditScript")){
	    	jsi.setPageItem("maintenance.html");
			return true;
		}
		if(item.getTitle().equals("Start")){
	    	jsi.setPageItem("start.html");
			return true;
		}
		if(menuCommand.equals("Target")){
			Intent intent = new Intent(getApplicationContext(),com.m_obj.mdswbrowser.BarcodeScannerActivity.class);
			intent.setAction(Intent.ACTION_GET_CONTENT);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return true;
//	    	jsi.setPageItem("input.html");
//			return true;
		}
		if(menuCommand.equals("PicMon")){
			Intent intent = new Intent(getApplicationContext(),com.m_obj.mdswbrowser.PicMonActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return true;
		}
		if(menuCommand.equals("Get")||menuCommand.equals("Intent")){
			mHTTPTask = Executors.newSingleThreadScheduledExecutor();
			mHTTPTask.submit(new Runnable() {
				public void run() {
					final String cEntityTop = "<entity><![CDATA[";
					final String cEntityEnd = "]]>";
					String sharedStr = Phttp.sendUrlContent("http://"+MainTarget+":"+PORTSTR+"/get");
					
					int p = sharedStr.indexOf(cEntityTop);
					if(p!=-1){
						String body = sharedStr.substring(p);
						int q= body.indexOf(cEntityEnd);
						body = body.substring(cEntityTop.length(), q);

						setSharedHttp(body);
						String sdata = TranPack.decHttpStringBody(body);
	
						String action = getTagValue("action",body);
						if(action!=null){
							if(action.equals("send")){
								jsi.setPageItem("start.html");
							}else
							if(action.equals("view")){
								if(getViewWithIntent()){
									Intent bi = new Intent(Intent.ACTION_VIEW, Uri.parse(sdata));  
									bi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									startActivity(bi);
								}else{
									jsi.setPageItem("start.html");
								}
							}

							Intent ci = new Intent("com.m_obj.mdswbrowser.PieHttpReceiver");
							ci.putExtra("com.m_obj.mdswbrowser.action", "clip");
							ci.putExtra("com.m_obj.mdswbrowser.sdata", sdata );
							sendBroadcast(ci);
						}else{
						   	jsi.setPageString("<body><h1>ERROR</h1>"+sharedStr+"</body>");		
						}
					}
				}
			});
			return true;
		}
		if(item.getTitle().equals("Reset WiFi")){
			ResetWiFi(this);
			return true;
		}
    	jsi.setPageItem("setting.html");
		return true;
	}
	//////////////////////////////////////////////////////////////////////////
	void LiveWifi(){
		// �{�^���N���b�N�Ń��V�[�o�[�Z�b�g
		Intent intent = new Intent(this, LiveWiFiReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);

		// �A���[���}�l�[�W���̗p��
		long firstTime = SystemClock.elapsedRealtime();
		firstTime += 1 * 1000;
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, 10 * 1000, sender);
	}
	void NormalWifi(){
		Intent intent = new Intent(this, LiveWiFiReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);

		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.cancel(sender);
		ResetWiFi(this);
	}
	public static void ResetWiFi(Context ctx){
        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        wifiManager.reassociate();
	}

//JavaScript�C���^�t�F�[�X----------------------------------------------------
	public final class JSInterface {
		private HashMap<String,String> resultMap;
		//�R���X�g���N�^
		public JSInterface(Handler handler) {
			resultMap = new HashMap<String,String>();
		}
		//JavaScript�C���^�t�F�[�X�̃��\�b�h�̏���(5)
		public void log( String msg ){
			android.util.Log.d("AIRi", msg);
		}
		//------------ util commands -----------------------
		public String getBarcode(){
			return AIRi.getBarcode();
		}
		public void setBarcode(String str){
			AIRi.setBarcode(str);
		}
		
		public String scanBarcode(){
			return airi.scanBarcode();
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
		public String getSharedTime(){
			return Integer.toString((int) LastSharedTime);
		}
		
		public String getSharedBody(){
			return TranPack.decHttpStringBody(AIRi.getSharedHttp());
		}
		public void clearSharedBody(){
			AIRi.clearSharedHttp();
		}
		public String getSharedAction(){
			return TranPack.decHttpAction(AIRi.getSharedHttp());
		}
		public String getSharedTitle(){
			return TranPack.decHttpTitle(AIRi.getSharedHttp());
		}
		public void reIntent(String str){
			Intent intent = new Intent();
			String istr = TranPack.getTagValue("intent", str);
			String action = TranPack.getTagValue("action", istr);
			intent.setAction(action);
			
			if(action.endsWith(".VIEW")){
				intent.setData(Uri.parse(TranPack.getTagValue("ds", istr)));
			}else{
				String estr = TranPack.getTagValue("ext", istr);
				String[] items = estr.split(">");
				for(int n=0; n < items.length; n=n+2){
					String tag = items[n].substring(1);
					String value = items[n+1].split("<")[0];
					intent.putExtra(tag, value);
				}
			}
			
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			finish();			
		}
		//------------ service commands -----------------------
		public void sendClip(String str){
			Intent intent = new Intent(getApplicationContext(),com.m_obj.mdswbrowser.AIRi.class);
			intent.putExtra("com.m_obj.mdswbrowser.action", "clip");
			intent.putExtra("com.m_obj.mdswbrowser.sdata", str );
			LastIntent = intent;

			switch(mSendMode){
			case Active:
	            if( !MainTarget.equals("") ) {
	    			sendCommand("clip", "", str, intent );
	            }
	            break;
			case Passive:
				break;
			case Self:
				CommandDispatch("clip",str);
			}
			setItem("LastClip",str);
		}
		public void setClip(String str){
			ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
			clipboard.setText(str);
		}
		public String getClip(){
			ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
			return (String) clipboard.getText();
		}
		public void jumptopage(String fname){
			Intent intent = new Intent(getApplicationContext(),com.m_obj.mdswbrowser.AIRi.class);
			intent.putExtra("com.m_obj.mdswbrowser.action", "jumptopage");
			intent.putExtra("com.m_obj.mdswbrowser.sdata", fname);
			startActivity(intent);
		}
		public void restart(){
			Intent intent = new Intent(getApplicationContext(),com.m_obj.mdswbrowser.AIRi.class);
			intent.putExtra("com.m_obj.mdswbrowser.action", "restart");
			startActivity(intent);
		}
		public void startService(){
			airi.startService();
		}
		public void stopService(){
			airi.stopService();			
		}

		public void setSendMode(String mstr){
			AIRi.setSendMode(mstr);
		}
		public String getSendMode(){
			return AIRi.getSendModeStr();
		}
		public void setShowMode(String mstr){
			AIRi.setShowMode(mstr);
		}
		public String getShowMode(){
			return AIRi.getShowModeStr();
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
		public void clearAllTarget(){
			AIRi.clearAllTarget();
		}
		//------------ momo commands -----------------------
		public void put( String dtclass, String title, String body ){
			momo.put(dtclass, title, body);
		}
		public void remove( int id ){
			momo.remove( id );
		}
		public void resetDB(){
			momo.resetDB();
		}
		public String filelist(String path){
			String flist[];
			try {
				flist = getAssets().list(path);
			} catch (IOException e) {
				flist = new String[0];
			}
			StringBuffer liststr = new StringBuffer();
			liststr.append("<nymfiles>");
			for( int n = 0; n < flist.length; ++n ){
				liststr.append("<file>").append(flist[n]).append("</file>");
			}
			liststr.append("</nymfiles>");
			return liststr.toString();
		}
		//-----------Strage---------------------------
		public int length() {
			int length = momo.length();
			return length;
		}
		public String key( int n ){
			String key = momo.key(n);
			return key;
		}
		public String getItem( String key ){
			String value = momo.getItem(key);
			return value;
		}
		public void setItem( String key, String value ) {
			AIRi.setItem(key, value);
		}
		public void removeItem( String key ) {
			momo.removeItem(key);
		}
		public void clear(){
			momo.clear();
		}
		//------------- nym ---------------------------
		public String nymrun( String nymtitle ){
			String scr = momo.getItem(nymtitle);
			return runscript( scr, null );
		}
		public String nymrun( String nymtitle, String param ){
			String scr = momo.getItem(nymtitle);
			return runscript( scr, param );
		}
		public void nymrun( final String nymtitle, final String param, final String callbackParam, final String callbackFunc ){
			final String scr = momo.getItem(nymtitle);
			Handler handler=new Handler();
			handler.post( new Runnable() {
				public void run() {
					String rtv = runscript( scr, param );
					resultMap.put(callbackParam, rtv);
					if( callbackFunc == null || callbackFunc.equals("")) {
						webView.loadUrl("javascript:callback()");
					}else{
						webView.loadUrl("javascript:"+callbackFunc);                		
					}
				}
			});
		}
		public String getNymResult( String keystr ){
			String result = resultMap.get(keystr);
			resultMap.remove(keystr);
			return result;
		}
		public void setNymResult( String keystr, String resultstr ){
			resultMap.put(keystr, resultstr);
		}
		public String runscript( String script, String param ){
			String result = "";
				Nym nym;
				try {
					nym = new Nym( script, momo, param );
					result = nym.Run();
				} catch (SAXException e) {
					result = "SAXException " + e.getMessage();
				} catch (IOException e) {
					result = "IOException " + e.getMessage();
				} catch (ParserConfigurationException e) {
					result = "ParserConfigurationException " + e.getMessage();
				}
			return result;
		}
		//------------- setpage ---------------------------
		public void loadURL(String url){
			webView.loadUrl(url);
		}
		public boolean setPageItem(String pagename){
			debugKeyCount = 0;
			String pstr=momo.getItem(pagename);
			if( !pstr.equals("")){
				clearMenu();
				webView.loadDataWithBaseURL(BaseURL+pagename, pstr, "text/html", "utf-8", null);
				lastpage = pagename;
				return true;
			}
			return false;
		}
		public boolean setPageString(String pstr){
			debugKeyCount = 0;
			if( !pstr.equals("")){
				clearMenu();
				webView.loadDataWithBaseURL(BaseURL, pstr, "text/html", "utf-8", null);
				return true;
			}
			return false;
		}
		// �y�[�W�Ԃ̃p�����[�^�n���p
		public boolean setPageItem(String pagename, String param){
			pageparam = param;
			return setPageItem(pagename);
		}
		public String getPageParam(){
			return pageparam;
		}
		//------------- menu ---------------------------
		public String setMenu(String title, String icon, String url){
		//	return amenu.add(title, icon, url);
			return "";
		}
		public void clearMenu(){
		//	amenu.clear();
			kmenu.clear();
		}
		public void setKeyMenu(String keystr, String url){
			kmenu.add(keystr, "", url);
		}
	}
//------------------------------------------------------------
	class AiriMenu {
		private List<String> titles;
		private List<String> icons;
		private List<String> urls;
	
		AiriMenu(){
			titles = new ArrayList<String>();
			icons = new ArrayList<String>();
			urls = new ArrayList<String>();
		}
		public void set( int ix, String title, String icon, String url ){
			titles.set(ix, title);
			icons.set(ix,icon);
			urls.set(ix,url);
		}
		public String add( String title, String icon, String url ){
			int ix = index(title);
			if(ix == -1){
				titles.add(title);
				icons.add(icon);
				urls.add(url);
				return Integer.toString(titles.size()-1);
			}
			set( ix, title, icon, url );
			return Integer.toString(ix);
		}
		int index( String title ){
			for( int n=0;n<titles.size();++n){
				if( getTitle(n).equals(title))
					return n;
			}
			return -1;
		}
		public String getTitle(int n){
			return titles.get(n);
		}
		public String getIcon(int n){
			return icons.get(n);
		}
		public String getUrl(int n){
			return urls.get(n);
		}
		public String getUrl( String title ){
			for( int n=0;n<titles.size();++n){
				if( getTitle(n).equals(title))
					return getUrl(n);
			}
			return "";
		}
		public int length(){
			return titles.size();
		}
		void clear(){
			titles.clear();
			icons.clear();
			urls.clear();
		}
	}
}
//host�C���^�t�F�[�X----------------------------------------------------
class TargetHost {
	public String hostname;
	public String hostip;
	public String cryptkey;

	public TargetHost(String hname, String addr, String key){
		hostname = hname;
		hostip = addr;
		cryptkey = key;
	}
}
