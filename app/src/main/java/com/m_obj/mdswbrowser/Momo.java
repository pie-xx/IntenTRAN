package com.m_obj.mdswbrowser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;

import android.content.*;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.*;

public class Momo  {
	private static final int IDITEM = 0;
	private static final int DTCLASSITEM = 1;
	private static final int TITLEITEM = 2;
	private static final int BODYITEM = 3;
	private static MomoHelper mhelper;
	private static final int MAXver = 3;
	private static SQLiteDatabase momodb;
//	private static HashMap<String,String> momoCache = new HashMap<String,String>();
	private static Context context;
	public Momo( Context _context ){
		context = _context;
		mhelper = new MomoHelper( context );
		momodb = null;
	}

	public void open(){
		if( momodb == null || !momodb.isOpen() ){
			momodb = mhelper.getDB();
		}
	}
	public void close(){
		mhelper.close();
	}

	public void put( String dtclass, String title, String body ){
//	   	SQLiteDatabase momodb = mhelper.getDB();
    	open();
    	ContentValues insData = new ContentValues();
    	insData.put("dtclass", dtclass);
    	insData.put("title", title);
    	insData.put("body", body);
    	momodb.insert("momos", null, insData);

    	Cursor cur = momodb.query("momos", null, "title='"+title+"' and dtclass='"+dtclass+"'", null, null, null, "dtclass, title, id desc", null);
    	int count = cur.getCount();
    	for( int n=MAXver; n < count; ++n ){
    		cur.moveToPosition(n);
    		remove( cur.getInt( IDITEM ) );
    	}
//    	mhelper.close();
   }
    public void remove( int id ){
//	   	SQLiteDatabase momodb = mhelper.getDB();
    	open();
    	String key = key( id );
//    	momoCache.remove(key);
    	momodb.delete("momos", "id="+Integer.toString(id), null);
//    	mhelper.close();
    }

///////// WebStrage ///////////////////////////////////////////////////////////////////////
    public int length(){
//	   	SQLiteDatabase momodb = mhelper.getDB();
    	open();
    	Cursor cur = momodb.query(true, "momos", new String[]{"title"}, null, null, null, null, null, null);
    	int rtv = cur.getCount();
//    	mhelper.close();
    	return rtv;
    }
    public String key( int n ){
//	   	SQLiteDatabase momodb = mhelper.getDB();
    	open();
       	Cursor cur = momodb.query(true, "momos", new String[]{"title"}, null, null, null, null, null, null);
    	String rtv = "";
    	if(cur.moveToPosition(n))
    		rtv = cur.getString(0);
//    	mhelper.close();
    	return rtv;
    }
    public InputStream getBinItem( String key ){
	    AssetManager am = context.getResources().getAssets();
	    InputStream inp;
	    try {
	        inp = am.open(key);
	        return inp;
	    }catch(IOException e) {
	        return null;
	    }
    }
    public String getItem( String key ){
//		SQLiteDatabase momodb = mhelper.getDB();
//		if( momoCache.containsKey(key)){
//			return momoCache.get(key);
//		}
		open();
		Cursor cur = momodb.query("momos", null, "title='"+key+"'", null, null, null, "dtclass, title, id desc", "1");
		String rtv = "";
		if( cur.getCount()!=0 ){
			if(cur.moveToFirst())
				rtv = cur.getString(BODYITEM);
//			momoCache.put(key, rtv);
		}else{
			rtv = mhelper.getAssetToString(key.replace('/', '-'));
		}
//		mhelper.close();
    	return rtv;
    }
    public void setItem( String title, String body ){
//	   	SQLiteDatabase momodb = mhelper.getDB();
    	open();
    	ContentValues insData = new ContentValues();
    	Cursor cur = momodb.query("momos", null, "title='"+title+"'", null, null, null, "dtclass, title, id desc", "1");
    	int id=0;
    	if( cur.moveToFirst() ){
    		insData.put("dtclass", cur.getString(DTCLASSITEM));
    		id = cur.getInt(IDITEM);
    	}
    	insData.put("title", title);
    	insData.put("body", body);
    	if( id > 0 ){
    		momodb.update("momos", insData, "id="+id, null);
    	}else{
    		momodb.insert("momos", null, insData);
    	}
//       	momoCache.put(title, body);
//    	mhelper.close();
    }

    public void removeItem( String key ){
//	   	SQLiteDatabase momodb = mhelper.getDB();
    	open();
    	momodb.delete("momos", "title='"+key+"'", null);
//    	momoCache.remove(key);
//    	mhelper.close();
    }
    
    public void clear(){
//	   	SQLiteDatabase momodb = mhelper.getDB();
    	open();
    	momodb.execSQL("DROP TABLE momos;");
//    	mhelper.close();
   }
////////////////////////////////////////////////////

    public void resetDB(){
//	   	SQLiteDatabase momodb = mhelper.getDB();
    	open();
    	momodb.execSQL("DROP TABLE momos;");
    	mhelper.onCreate(momodb);
//    	momoCache.clear();
//    	mhelper.close();
    }
}

class MomoHelper extends SQLiteOpenHelper {
	private final static String DB_NAME = "momo.db";
	private final static int DB_VERSION = 1;
	private AssetManager asset;
	
	public MomoHelper( Context context ){
		super(context, DB_NAME, null, DB_VERSION );
		asset = context.getAssets();
	}

	public SQLiteDatabase getDB(){
		try {
			return getWritableDatabase();
		}catch( SQLiteException e ){
			return getReadableDatabase();
		}
	}

	public String getAssetToString( String assetfile ){
		try {
			InputStreamReader isr = new InputStreamReader(asset.open(assetfile), "utf-8");
            int readBytes = 0;
            char[] sBuffer = new char[2048];
            StringWriter sw = new StringWriter( 2048 );
            while ((readBytes = isr.read(sBuffer)) != -1) {
            	sw.write(sBuffer,0,readBytes);
            }
            return sw.toString();
		} catch (IOException e) {
	        return "";
		}
	}
	@Override
	public void onCreate(SQLiteDatabase db){
		db.execSQL("CREATE TABLE momos " +
			"(id INTEGER PRIMARY KEY AUTOINCREMENT, dtclass TEXT, title TEXT, body TEXT)");
//		ContentValues insData = new ContentValues();
//
	}
	@Override
	public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ){
	}
}
