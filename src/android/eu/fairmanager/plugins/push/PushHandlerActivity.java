package eu.fairmanager.plugins.push;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.util.Log;

public class PushHandlerActivity extends Activity {
	private static String TAG = "PushHandlerActivity";

	/*
	 * this activity will be started if the user touches a notification that we own.
	 * We send it's data off to the push plugin for processing.
	 * If needed, we boot up the main activity to kickstart the application.
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(TAG, "onCreate");

		boolean isPushActive = Push.isActive();
		processPushBundle(isPushActive);

		finish();

		if (!isPushActive) {
			forceMainActivityReload();
		}
	}

	/**
	 * Takes the pushBundle extras from the intent,
	 * and sends it through to the Push for processing.
	 */
	private void processPushBundle(boolean isPushActive) {
		Log.v(TAG, "processPushBundle");

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();

		if (extras != null) {
			// The pushBundle is the data that was originally provided with the push notification
			// that caused this activity to be started.
			Bundle originalExtras = extras.getBundle("pushBundle");
			if( originalExtras == null){
				Log.w(TAG,"No pushBundle found in intent. This shouldn't happen.");
				return;
			}

			originalExtras.putBoolean("foreground", false);
			originalExtras.putBoolean("coldstart", !isPushActive);

			// Check if there is already a reply that was entered in-line in the notification.
			Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
			if (remoteInput != null) {
				CharSequence reply = remoteInput.getCharSequence("reply");
				originalExtras.putString("reply", reply != null ? reply.toString() : null);
			}

			// Send the data to the JS side. It will be processed like a push notification would
			// be received while the application is active.
			Push.sendExtras(originalExtras);
		}
	}

	/**
	 * Forces the main activity to re-launch if it's unloaded.
	 */
	private void forceMainActivityReload() {
		Log.v(TAG, "forceMainActivityReload");

		PackageManager pm = getPackageManager();
		Intent launchIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
		startActivity(launchIntent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "onResume");

		// When the application is started, we want to clear all notifications.
		final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		if (notificationManager != null) {
			notificationManager.cancelAll();
		}
	}
}
