package com.example.nolanalert;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gcm.GCMRegistrar;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class CommonUtils {
	static final String SERVER_URL = "http://www.nolanpro.com:85/";
    static final String SENDER_ID = "836680780112";
    static final String DISPLAY_MESSAGE_ACTION =
            "com.example.nolanalert.DISPLAY_MESSAGE";
    
    static AsyncTask<Void, Void, Boolean> registerTask;
    static AsyncTask<Void, String, String> statsUpdaterTask;
    
    static String regId;
    static Vibrator vib;
    static NotificationManager nm;

    public static void echo(String txt) {
      MainActivity.mDisplay.append(txt + " \n");
    }

    public static void register(final Context context) {
      echo("Starting Register...");
      final String regId = GCMRegistrar.getRegistrationId(context);
      if (regId.equals("")) {
        echo ("Registering with google");
        GCMRegistrar.register(context, SENDER_ID);
      } else {
        echo("Registered with Google: ..." + regId.substring(regId.length() - 5, regId.length()));
        if (GCMRegistrar.isRegisteredOnServer(context)) {
          echo("Already registered with server");
        } else {
          echo("Need to register with server, doing it now.");
          registerWithServer(regId, context);
        }
      }
    }
    
    public static void reRegister(final Context context) {
      final String regId = GCMRegistrar.getRegistrationId(context);
      registerWithServer(regId, context);
    }
    
    public static void selectNotification(MainActivity activity) {
      Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
      intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
      intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
      intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) getNotification());
      activity.startActivityForResult(intent, 5); 
    }

    public static void registerWithServer(final String regId, final Context context) {

      registerTask = new AsyncTask<Void, Void, Boolean>() {

        @Override
        protected Boolean doInBackground(Void... params) {
          Boolean registered = sendId(context, regId);
          if (!registered) {
            Log.e("Error", "Did not sync with server, unregistering!");
            GCMRegistrar.unregister(context);
          }
          return registered;
        }

        @Override
        protected void onPostExecute(Boolean result) {
          registerTask = null;
          GCMRegistrar.setRegisteredOnServer(context, result);
          if (result) {
            MainActivity.mDisplay.setText("Success!");
          } else {
            MainActivity.mDisplay.setText("Error. Not registered with server");
          }

        }

      };
      registerTask.execute(null, null, null);
    }
    
    public static boolean sendId(final Context context, final String regId) {
      JSONObject r = new JSONObject();
      boolean success = false;
      try {
        String url = SERVER_URL + "?device_id=" + regId;
        r = getFromServer(url);
        Log.i("Got response when regestering",  r.toString());
        if (r.has("result") && r.getString("result").equals("ok")) {
          Log.i("All", "Good");
          success = true;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return success;
    }
    
    private static Uri getNotification()
    {
      String pref = MainActivity.prefs.getString("selectedNotification", "none");
      if (pref != "none") {
        return Uri.parse(pref);
      }
      return null;
    }
    
    private static void generateNotification(Context context, String message) {
      
      nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
      
      long[] alarm_vib = { 0, 200, 500 };
      
      Uri notification = getNotification();
      Uri soundUri;
      if (notification != null) {
        soundUri = notification;
      } else {
        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
      }
      if (message.equals("alarm")) {
        vib.vibrate(alarm_vib, 0);
        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
      } else {
        vib.vibrate(300);
      }

      NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
      .setSmallIcon(R.drawable.ic_launcher)
      .setContentTitle("Alert!")
      .setContentText(message)
      .setSound(soundUri);

      //Display notification
      nm.notify(0, mBuilder.build());
    }
    
    public static void gotMessage(final Context context, String message) {
    	if (message != "") {
    		generateNotification(context, message);
    	}
    	startApp(context, message);
    }
    
    public static void startApp(Context context, String message) {
      PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
      WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
      wl.acquire(10000); // 10 seconds
      
      // Start our app and bring it to the front
      Intent intent = new Intent();
      intent.setClass(context, MainActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.putExtra("message", message);
      
      context.startActivity(intent);
    }

    public static String generateUrl(JSONObject json) {
    	String query = "";
    	try {
    	  query = URLEncoder.encode(json.toString(), "utf-8");
    	} catch (UnsupportedEncodingException e) { }
    	String url = SERVER_URL + "?json=" + query;
    	return url;
    }

    public static JSONObject getFromServer(String url) {
      String output = "";
      JSONObject json = new JSONObject();
      DefaultHttpClient httpClient = new DefaultHttpClient();
      
      final HttpParams httpParameters = httpClient.getParams();
      int connectionTimeOutSec = 5;
      int socketTimeoutSec = 5;
      HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
      HttpConnectionParams.setSoTimeout        (httpParameters, socketTimeoutSec * 1000);
      
      HttpGet httpGet = new HttpGet(url);

      try {
        HttpResponse httpResponse = httpClient.execute(httpGet);
        HttpEntity httpEntity = httpResponse.getEntity();
        output = EntityUtils.toString(httpEntity);
        json = new JSONObject(output);
      } catch (Exception e) { }

      return json;
    }

    public static String getString(JSONObject r) {
      try {
        return "Oven Watts: " + r.getString("watts") + "\n" + "Smoke Alarm: " + r.getString("alarm");
      } catch (JSONException e) {}
      return r.toString();
    }


    public static void readStats() {
      if (statsUpdaterTask != null) {
        // a stats request is still running
        //return;
        statsUpdaterTask.cancel(true);
      }
      
      statsUpdaterTask = new AsyncTask<Void, String, String>() {
        @Override
        protected String doInBackground(Void... params) {
          int i = 0;
          while (i < 10) {
            JSONObject r = CommonUtils.getFromServer(CommonUtils.SERVER_URL);
            publishProgress(CommonUtils.getString(r));
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) { }
            i++;
          }
          return "\nChecked 10 times, no more calls";
        }

        @Override
        protected void onPostExecute(String result) {
          MainActivity.mDisplay.append(result);
          statsUpdaterTask = null;
          //MainActivity.resetButton.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(String... result) {
          MainActivity.mStats.setText(result[0]);
        }

      };
      statsUpdaterTask.execute(null, null, null);
    }

}

