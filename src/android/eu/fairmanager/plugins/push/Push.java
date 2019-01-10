package eu.fairmanager.plugins.push;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.engine.SystemWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author awysocki
 */

public class Push extends CordovaPlugin {
	public static final String TAG = "Push";

	private static CordovaWebView gWebView;
	private static String callbackNameEvent;
	private static Bundle gCachedExtras = null;
	private static boolean gForeground = false;

	/**
	 * Gets the application context from cordova's main activity.
	 *
	 * @return the application context
	 */
	private Context getApplicationContext() {
		return this.cordova.getActivity().getApplicationContext();
	}

	@Override
	public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {

		boolean result = false;

		Log.v(TAG, "execute: action=" + action);

		switch (action) {
			case "register":

				Log.v(TAG, "execute: data=" + data.toString());

				try {
					JSONObject jo = data.getJSONObject(0);

					gWebView = this.webView;
					Log.v(TAG, "execute: jo=" + jo.toString());

					callbackNameEvent = (String) jo.get("ecb");
					//gSenderID = (String) jo.get("senderID");

					//Log.v(TAG, "execute: ECB=" + callbackNameEvent + " senderID=" + gSenderID);

					Task<InstanceIdResult> tokenTask = FirebaseInstanceId.getInstance().getInstanceId();
					tokenTask.addOnSuccessListener((InstanceIdResult instanceIdResult) -> {
						String token = instanceIdResult.getToken();
						try {
							JSONObject json = new JSONObject().put("event", "registered");
							json.put("regid", token);
							Log.v(TAG, "onNewToken: " + json.toString());

							// Send this JSON data to the JavaScript application above EVENT should be set to the msg type
							// In this case this is the registration ID
							Push.sendJavascript(json);

							callbackContext.success(token);

						} catch (JSONException e) {
							Log.e(TAG, "execute: Got JSON Exception " + e.getMessage());
							callbackContext.error(e.getMessage());
						}
					});

					result = true;

				} catch (JSONException e) {
					Log.e(TAG, "execute: Got JSON Exception " + e.getMessage());
					result = false;
					callbackContext.error(e.getMessage());
				}

				if (gCachedExtras != null) {
					Log.v(TAG, "sending cached extras");
					sendExtras(gCachedExtras);
					gCachedExtras = null;
				}

				break;
			case "unregister":
				Log.v(TAG, "UNREGISTER");
				result = true;
				callbackContext.success();
				try {
					FirebaseInstanceId.getInstance().deleteInstanceId();
				} catch (IOException e) {
					Log.e(TAG, "execute: Got deleteInstanceId Exception " + e.getMessage());
					result = false;
					callbackContext.error(e.getMessage());
				}

				break;
			default:
				result = false;
				Log.e(TAG, "Invalid action : " + action);
				callbackContext.error("Invalid action : " + action);
				break;
		}

		return result;
	}

	/*
	 * Sends a json object to the client as parameter to a method which is defined in callbackNameEvent.
	 */
	public static void sendJavascript(JSONObject _json) {
		String _d = "javascript:" + callbackNameEvent + "(" + _json.toString() + ")";
		Log.v(TAG, "sendJavascript: " + _d);

		if (callbackNameEvent != null && gWebView != null) {
			gWebView.getView().post(() -> ((SystemWebView)gWebView.getView()).evaluateJavascript(_d, null));
		}
	}

	/*
	 * Sends the pushbundle extras to the client application.
	 * If the client application isn't currently active, it is cached for later processing.
	 */
	public static void sendExtras(Bundle extras) {
		if (extras != null) {
			if (callbackNameEvent != null && gWebView != null) {
				sendJavascript(convertBundleToJson(extras));
			} else {
				Log.v(TAG, "sendExtras: caching extras to send at a later time.");
				gCachedExtras = extras;
			}
		}
	}

	private void setupNotificationChannels() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			return;
		}

		NotificationManager mNotificationManager =
				(NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

		NotificationChannel channelMessages = new NotificationChannel("messages", "Messages", NotificationManager.IMPORTANCE_HIGH);
		channelMessages.enableLights(true);
		channelMessages.setLightColor(Color.valueOf(0x00, 0x96, 0x88, 0xff).toArgb());
		channelMessages.enableVibration(true);
		channelMessages.setVibrationPattern(new long[]
				{
						0, 1000, 1000, 1000, 1000, 1000,
						1000, 1000, 1000, 1000, 1000,
						1000, 1000, 1000, 1000, 1000
				});
		channelMessages.setShowBadge(true);
		Context context = this.getApplicationContext();
		Resources r = context.getResources();
		int resourceId = r.getIdentifier("message", "raw", context.getPackageName());
		Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + resourceId);
		channelMessages.setSound(soundUri, new AudioAttributes.Builder()
				.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
				.setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
				.setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST)
				.build());
		mNotificationManager.createNotificationChannel(channelMessages);

		NotificationChannel channelReminder = new NotificationChannel("reminder", "Reminder", NotificationManager.IMPORTANCE_DEFAULT);
		mNotificationManager.createNotificationChannel(channelReminder);

		NotificationChannel channelUncategorized = new NotificationChannel("uncategorized", "Uncategorized", NotificationManager.IMPORTANCE_DEFAULT);
		mNotificationManager.createNotificationChannel(channelUncategorized);
	}

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		gForeground = true;

		this.setupNotificationChannels();
	}

	@Override
	public void onPause(boolean multitasking) {
		super.onPause(multitasking);
		gForeground = false;
		final NotificationManager notificationManager = (NotificationManager) cordova.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
	}

	@Override
	public void onResume(boolean multitasking) {
		super.onResume(multitasking);
		gForeground = true;
		final NotificationManager notificationManager = (NotificationManager) cordova.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		gForeground = false;
		callbackNameEvent = null;
		gWebView = null;
	}

	/*
	 * serializes a bundle to JSON.
	 */
	private static JSONObject convertBundleToJson(Bundle extras) {
		try {
			JSONObject json;
			json = new JSONObject().put("event", "message");

			JSONObject jsondata = new JSONObject();
			for (String key : extras.keySet()) {
				Object value = extras.get(key);

				// System data from Android
				switch (key) {
					case "from":
					case "collapse_key":
					case "category":
						json.put(key, value);
						break;
					case "foreground":
						json.put(key, extras.getBoolean("foreground"));
						break;
					case "coldstart":
						json.put(key, extras.getBoolean("coldstart"));
						break;
					default:
						// Maintain backwards compatibility
						if (key.equals("message") || key.equals("msgcnt") || key.equals("soundname")) {
							json.put(key, value);
						}

						if (value instanceof String) {
							// Try to figure out if the value is another JSON object

							String strValue = (String) value;
							if (strValue.startsWith("{")) {
								try {
									JSONObject json2 = new JSONObject(strValue);
									jsondata.put(key, json2);
								} catch (Exception e) {
									jsondata.put(key, value);
								}
								// Try to figure out if the value is another JSON array
							} else if (strValue.startsWith("[")) {
								try {
									JSONArray json2 = new JSONArray(strValue);
									jsondata.put(key, json2);
								} catch (Exception e) {
									jsondata.put(key, value);
								}
							} else {
								jsondata.put(key, value);
							}
						}
						break;
				}
			} // while
			json.put("payload", jsondata);

			Log.v(TAG, "extrasToJSON: " + json.toString());

			return json;
		} catch (JSONException e) {
			Log.e(TAG, "extrasToJSON: JSON exception");
		}
		return null;
	}

	public static boolean isInForeground() {
		return gForeground;
	}

	public static boolean isActive() {
		return gWebView != null;
	}
}
