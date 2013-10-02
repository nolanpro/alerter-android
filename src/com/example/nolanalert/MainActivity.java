package com.example.nolanalert;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gcm.GCMRegistrar;

import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	static TextView mDisplay;
	static TextView mStats;
	static Button resetButton;
	static Button silenceButton;
	static SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	  
		prefs = this.getPreferences(0);
		
		GCMRegistrar.checkDevice(this);
        GCMRegistrar.checkManifest(this);
		setContentView(R.layout.activity_main);
		mDisplay = (TextView) findViewById(R.id.display);
		mStats = (TextView) findViewById(R.id.stats);
	
		resetButton = (Button) findViewById(R.id.reset_button);
		silenceButton = (Button) findViewById(R.id.silence_button);
		
		CommonUtils.register(this);
		CommonUtils.readStats();
	}
	
	public void requestAgain(View view) {
	  CommonUtils.readStats();
	  //resetButton.setVisibility(View.INVISIBLE);
	}
	
	public void silence(View view) {
	  CommonUtils.vib.cancel();
	  CommonUtils.nm.cancelAll();
	  silenceButton.setVisibility(View.INVISIBLE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onResume() {
	  super.onResume();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		
		Intent intent = getIntent();
		String message = intent.getStringExtra("message");
		if (message != null) {
		  mDisplay.setText("Received alert: " + message);
		  silenceButton.setVisibility(View.VISIBLE);
		}
		CommonUtils.readStats();
	}

	@Override
	protected void onDestroy() {
	  if (CommonUtils.registerTask != null) {
	    CommonUtils.registerTask.cancel(true);
	  }
	  if (CommonUtils.statsUpdaterTask != null) {
	    CommonUtils.statsUpdaterTask.cancel(true);
	  }
	  GCMRegistrar.onDestroy(this);
	  super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	  switch (item.getItemId()) {
	  case R.id.action_register:
	    CommonUtils.reRegister(getApplicationContext());
	    return true;
	  case R.id.action_notification:
	    CommonUtils.selectNotification(this);
	  default:
	    return super.onOptionsItemSelected(item);
	  }
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent)
	{
	  if (resultCode == Activity.RESULT_OK && requestCode == 5)
	  {
	    Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString("selectedNotification", uri.toString());
	    editor.commit();
	  }            
	}

}
