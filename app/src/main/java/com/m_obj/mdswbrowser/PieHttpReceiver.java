package com.m_obj.mdswbrowser;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;

public class PieHttpReceiver  extends BroadcastReceiver {

	  @Override
	public void onReceive(Context context, Intent intent) {
		String sdata = intent.getStringExtra("com.m_obj.mdswbrowser.sdata");
		ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Activity.CLIPBOARD_SERVICE); 
		clipboard.setText(sdata);
	}

}
