package com.m_obj.mdswbrowser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.m_obj.mdswbrowser.SimpleHTTPsever.HTTPSession;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.widget.Toast;


public class PieHTTPservice extends Service {
	private static final int PORT = AIRi.PORT;
	private SimpleHTTPsever server;

	@Override
	public void onCreate(){
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	ScheduledExecutorService mHTTPTask;
	@Override
	public void onStart(Intent intent, int startID){
		mHTTPTask = Executors.newSingleThreadScheduledExecutor();
		mHTTPTask.submit(new Runnable() {
			public void run() {
				try {
					server = new MyHTTPD();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
//	   	Toast.makeText(getApplicationContext(), "PieHTTPserver Service onStart()", Toast.LENGTH_LONG).show();		
	}
	
	public void onDestroy(){
		mHTTPTask.shutdown();
//	   	Toast.makeText(getApplicationContext(), "PieHTTPserver Service onDestroy()", Toast.LENGTH_LONG).show();
	}
	
    private class MyHTTPD extends SimpleHTTPsever {
        public MyHTTPD() throws IOException {
          super(PORT);
        }

		@Override
		public Response serve(String uri, String method, Properties header, Properties parms, byte[] fbuf, Socket socket) {
			String remoteaddr = socket.getInetAddress().toString().replaceAll("/", "");
			if(uri.equals("/menu")||uri.equals("/")){
				String LastContent = AIRi.getSharedHttp();
				String MenuHTML = AIRi.momo.getItem("menu.html").replace("#LastIntent#", LastContent);
				return new SimpleHTTPsever.Response(HTTP_OK, MIME_HTML, MenuHTML);
			}
			if(uri.equals("/get")){
				Intent intent = AIRi.getLastIntent();
				String action = intent.getAction();
				String retstr = "";
				if (Intent.ACTION_VIEW.equals(action)) {
					retstr = TranPack.encHttpPack("view", "", intent.getData().toString(), intent);
				}else
				if (Intent.ACTION_SEND.equals(action)) {
					retstr = TranPack.encHttpPack("send", 
							intent.getStringExtra("android.intent.extra.SUBJECT"),
							intent.getStringExtra("android.intent.extra.TEXT"), 
							intent);						
				}else{
					retstr = TranPack.encHttpPack("", 
							intent.getStringExtra("android.intent.extra.SUBJECT"),
							intent.getStringExtra("android.intent.extra.TEXT"), 
							intent);											
				}
				return new SimpleHTTPsever.Response(HTTP_OK, MIME_HTML, retstr);
			}
			if(uri.equals("/favicon.ico")){
				return new SimpleHTTPsever.Response(HTTP_OK, "image/png", 
						AIRi.momo.getBinItem("intentran128.png"));
			}
			if(uri.equals("/barcode")){
				String result = AIRi.airi.scanBarcode();
				return new SimpleHTTPsever.Response(HTTP_OK, "text/plain", result);
			}
			if(uri.startsWith("/qrcode")){
				return new SimpleHTTPsever.Response(HTTP_OK, "image/png", 
						QRCodeControler.mkQRimage(parms.getProperty("s")));
			}
			if(uri.startsWith("/item/")){
				String result = AIRi.momo.getItem(uri.substring(6));
				return new SimpleHTTPsever.Response(HTTP_OK, "text/plain", result);
			}
			if(uri.startsWith("/file/")){
				FileInputStream f;
				try {
					uri = uri.substring(6);
					if(uri.startsWith("content:")){
						return new SimpleHTTPsever.Response(HTTP_OK, "text/plain", 
								getContentResolver().openInputStream(Uri.parse(uri)));
					}
					f = new FileInputStream(uri);
					return new SimpleHTTPsever.Response(HTTP_OK, "text/plain", f);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					return new SimpleHTTPsever.Response(HTTP_OK, "text/plain", e.toString());
				}
			}
			if(uri.startsWith("/pic/")){
				FileInputStream f;
				try {
					uri = uri.substring(5);

					f = new FileInputStream(PicMonActivity.getCapDir()+"/"+uri);
					return new SimpleHTTPsever.Response(HTTP_OK, "text/plain", f);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					return new SimpleHTTPsever.Response(HTTP_OK, "text/plain", e.toString());
				}
			}
			if(uri.startsWith("/preview")){
				if(AIRi.picmon==null){
					return new SimpleHTTPsever.Response(HTTP_OK, "image/png", 
						AIRi.momo.getBinItem("intentran128.png"));
				}
				return new SimpleHTTPsever.Response(HTTP_OK, "image/png", 
						AIRi.picmon.getPreview());
			}
			if(uri.startsWith("/focus")){
				if(AIRi.picmon==null){
					return new SimpleHTTPsever.Response(HTTP_OK, "image/png", 
						AIRi.momo.getBinItem("intentran128.png"));
				}
				return new SimpleHTTPsever.Response(HTTP_OK, "image/jpeg", 
						AIRi.picmon.getFocus());
			}
			if(uri.startsWith("/cap")){
				if(AIRi.picmon==null){
					return new SimpleHTTPsever.Response(HTTP_OK, "text/plain", "not ready");
				}

				return new SimpleHTTPsever.Response(HTTP_OK, "text/plain", 
						AIRi.picmon.getCap());
			}
			if(uri.startsWith("/camera.html")){
				return new SimpleHTTPsever.Response(HTTP_OK, "text/html", 
						AIRi.momo.getBinItem("camera.html"));
			}
			
			String sdata;
			try {
				sdata = new String(fbuf,"utf-8");
			} catch (UnsupportedEncodingException e) {
				sdata = new String(fbuf);
				e.printStackTrace();
			}
			Intent intent = new Intent(getApplicationContext(),com.m_obj.mdswbrowser.AIRi.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			if(uri.equals("/view")){
				String url = TranPack.decHttpStringBody(sdata);

				if(AIRi.getViewWithIntent()){
					if(!url.equals("")){
						Intent bi = new Intent(Intent.ACTION_VIEW, Uri.parse(url));  
						bi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(bi);
					}
				}else{
					intent.putExtra("com.m_obj.mdswbrowser.action", "view");
					intent.putExtra("com.m_obj.mdswbrowser.sdata", sdata);
					startActivity(intent);
				}
			}
			if(uri.equals("/send")){
				intent.putExtra("com.m_obj.mdswbrowser.action", "send");
				intent.putExtra("com.m_obj.mdswbrowser.sdata", sdata );
				startActivity(intent);
			}
			if(uri.equals("/putfile")){
				String fname = TranPack.getNewFileName();
				try {
					File file = new File(fname);
					FileOutputStream out = new FileOutputStream(file);
					out.write(TranPack.decHttpBinaryBody(new String(fbuf)));
					out.close();
					Uri cont = TranPack.insertCP(getApplicationContext(), fname);
					
					SketchActivity.setPaintingDone(cont);

				} catch (IOException e) {
					return new SimpleHTTPsever.Response(HTTP_OK, "text/plain", e.toString());
				}
			}
			if(uri.equals("/paint")){
				Intent pi = new Intent(getApplicationContext(),com.m_obj.mdswbrowser.SketchActivity.class);  
				pi.putExtra("com.m_obj.mdswbrowser.remoteaddr", remoteaddr );
				pi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(pi);
			}
//			if(uri.equals("/clip")){
//				String body = TranPack.decHttpStringBody(sdata);
//				setClip(body);
//			}
			if(uri.equals("/register")){
				intent.putExtra("com.m_obj.mdswbrowser.action", "register");
				intent.putExtra("com.m_obj.mdswbrowser.sdata", 
						AppendXML("remoteaddr",socket.getInetAddress().toString().replace("/", ""),sdata ) );
				intent.putExtra("com.m_obj.mdswbrowser.remoteaddr", remoteaddr );
				startActivity(intent);
			}

			final String html = "<html><body>"+sdata+"</body></html>";
			return new SimpleHTTPsever.Response(HTTP_OK, MIME_HTML, html);
		}
//		private void setClip(String sdata){
//			Intent ci = new Intent("com.m_obj.mdswbrowser.PieHttpReceiver");
//			ci.putExtra("com.m_obj.mdswbrowser.action", "clip");
//			ci.putExtra("com.m_obj.mdswbrowser.sdata", sdata );
//			sendBroadcast(ci);
//		}
		private String AppendXML(String tag, String value, String bodystr){
			int p = bodystr.indexOf(">");
			if(p!=-1){
				String frontstr = bodystr.substring(0, p);
				String backstr = bodystr.substring(p+1);
				return frontstr + "<"+tag+">"+value+"</"+tag+">"+backstr;
			}
			return "<"+tag+">"+value+"</"+tag+">"+bodystr;
		}
    }
}
