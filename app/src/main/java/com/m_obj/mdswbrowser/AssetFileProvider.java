package com.m_obj.mdswbrowser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class AssetFileProvider extends ContentProvider {

	public static final String AUTHORITY = "com.m_obj.mdswbrowser.AssetFile";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	public static final String TAG = "AssetFileProvider";

	@Override
	public boolean onCreate() {
		return false;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public AssetFileDescriptor openAssetFile(Uri uri, String mode)
			throws FileNotFoundException {
		Log.i(TAG, "open Asset file");
		try {
			return getContext().getAssets().openFd("upperrock.jpg");
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "ERROR: " + e);
			throw new FileNotFoundException(e.getMessage());
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}
}