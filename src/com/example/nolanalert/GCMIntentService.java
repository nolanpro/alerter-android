package com.example.nolanalert;


import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
 
import com.example.nolanalert.CommonUtils;
import com.google.android.gcm.GCMBaseIntentService;
 
public class GCMIntentService extends GCMBaseIntentService {
    private static int count = 0;
 
	/**
	 * @see com.google.android.gcm.GCMBaseIntentService#onError(android.content.Context, java.lang.String)
	 */
	@Override
	protected void onError(Context arg0, String error) {
	       Log.d("Done", "onError: " + error);
	}
 
	/**
	 * @see com.google.android.gcm.GCMBaseIntentService#onRegistered(android.content.Context, java.lang.String)
	 */
	@Override
	protected void onRegistered(Context context, String regId) {
		Log.d("Done", "onRegistered: " + regId);
		CommonUtils.regId = regId;
	}
 
	/**
	 * @see com.google.android.gcm.GCMBaseIntentService#onUnregistered(android.content.Context, java.lang.String)
	 */
	@Override
	protected void onUnregistered(Context context, String regId) {
		Log.d("Done", "onUnregistered: " + regId);
	}
 
	/**
	 * @see com.google.android.gcm.GCMBaseIntentService#onMessage(android.content.Context, android.content.Intent)
	 */
	@Override
	protected void onMessage(Context context, Intent intent) {
		// This is how to get values from the push message (data)
	  // String message = intent.getStringExtra("message");
		// long timestamp = intent.getLongExtra("timestamp", -1);
	  
		try {
			CommonUtils.gotMessage(context, intent.getStringExtra("message"));
		} catch (Exception e) { }
 
	}
}