package com.m_obj.mdswbrowser;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Time;
import android.util.Base64;

public class TranPack {
	public static String getNewFileName(){
    	File fout = Environment.getExternalStorageDirectory();
     	Date d = new Date();
    	String fname = fout.getAbsolutePath() + "/";
    	fname += String.format("%4d%02d%02d-%02d%02d%02d.png",
    			(1900+d.getYear()), d.getMonth(), d.getDate(),
    			d.getHours(), d.getMinutes(), d.getSeconds());
		return fname;
	}
	public static Uri insertCP(Context context, String fname){
		ContentResolver contentresolver = context.getContentResolver();
		
		Time mTime = new Time();
		mTime.setToNow();
		ContentValues contentvalues = new ContentValues(6);
		contentvalues.put("_data", fname);
		contentvalues.put("mime_type", "image/png");
		contentvalues.put("title", fname);
		contentvalues.put("datetaken", Long.valueOf(mTime.toMillis(false)));
		contentvalues.put("description", fname);
		return contentresolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentvalues);
	}
	public static String Intent2XML(Intent data){
		if(data==null){
			return "<intent></intent>";
		}
    	String contents;
    	Bundle bundle = data.getExtras();
    	if(bundle==null){
    		contents = "<ds>"+data.getDataString()+"</ds>";
    	}else{
    		// getStringExtra("SCAN_RESULT"); barcode scaner
        	Set<String> set = bundle.keySet();
        	StringBuffer sb = new StringBuffer();
        	for(String entry: set){
        		Object go = bundle.get(entry);
        		if(go!=null){
	        		String value = go.toString();
	        		sb.append("<"+entry+">"+value+"</"+entry+">\n");
        		}
        	}
            contents = "<ext>"+sb.toString()+"</ext>";
    	}
        return "<intent><type>"+data.getType()+"</type>"+
        			"<action>"+data.getAction()+"</action>"+
        	contents+"</intent>";
	}
	public static String encHttpPack( String action, String title, String body, Intent intent ){
		String bodystr = "";
		if(body!=null){
			bodystr = Base64.encodeToString(body.getBytes(), Base64.DEFAULT);
		}
		return "<tpack><action>"+action+"</action>"+
				"<title>"+Uri.encode(title)+"</title>"+
				"<body>"+bodystr+"</body>"+
				Intent2XML(intent)+
				"</tpack>";
	}
	public static String encHttpPack( String action, String title, byte[] data, Intent intent ){
		String bodystr = "";
		if(data!=null){
			bodystr = Base64.encodeToString(data, Base64.DEFAULT);
		}
		return "<tpack><action>"+action+"</action>"+
				"<title>"+Uri.encode(title)+"</title>"+
				"<body>"+bodystr+"</body>"+
				Intent2XML(intent)+
				"</tpack>";
	}

	public static byte[] decHttpBinaryBody(String estr){
		String ebody = getTagValue("body", estr);
		return Base64.decode(ebody, Base64.DEFAULT);
	}
	public static String decHttpStringBody(String estr){
		String ebody = getTagValue("body", estr);
		if( ebody.equals("") ){
			ebody = getTagValue("url", estr);
			if(!ebody.equals(""))
				return ebody;
			return getTagValue(Intent.EXTRA_STREAM, estr);
		}
		return new String(Base64.decode(ebody, Base64.DEFAULT));
	}
	public static String decHttpTitle(String estr){
		String etitle = getTagValue("title", estr);
		return new String(Uri.decode(etitle));
	}
	public static String decHttpAction(String estr){
		String etitle = getTagValue("action", estr);
		return new String(Uri.decode(etitle));
	}
	////////////////////////////////////////////////
	public static String getTagValue(String tag, String str){
		List<String> items = getTagValues(tag,str);
		if( items.size()==0)
			return "";
		return items.get(0);
	}
	public static List<String> getTagValues(String tag, String str){
		List<String> results = new ArrayList<String>();
		if(str!=null){
			String[] items = str.split("<"+tag+">");
			for(int n=1; n < items.length; ++n ){
				results.add(new String(items[n].split("</"+tag+">")[0]));
			}
		}
		return results;
	}
}
