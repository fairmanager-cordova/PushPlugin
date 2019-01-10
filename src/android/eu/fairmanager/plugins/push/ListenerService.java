package eu.fairmanager.plugins.push;

// TODO: Drop this import and access the resources dynamically from the parent package.
import eu.fairmanager.mobile2.R;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.google.firebase.messaging.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static com.google.android.gms.common.internal.ConnectionErrorMessages.getAppName;

public class ListenerService extends FirebaseMessagingService {
	private static final String TAG = "ListenerService";

	@Override
	public void onMessageReceived(RemoteMessage message) {
		Log.v(TAG, "onMessageReceived");

		// Convert the data from the message to a Bundle, so that we can store it with
		// intents and process it further.
		Bundle extras = new Bundle();
		for (Map.Entry<String, String> entry : message.getData().entrySet()) {
			extras.putString(entry.getKey(), entry.getValue());
		}

		// If we are in the foreground, just surface the payload, else post it to the statusbar
		if (Push.isInForeground()) {
			extras.putBoolean("foreground", true);
			Push.sendExtras(extras);
		} else {
			extras.putBoolean("foreground", false);

			// Send a notification if there is a message
			String messageText = extras.getString("message");
			if (messageText != null && messageText.length() != 0) {
				createNotification(extras);
			}
		}
	}

	@Override
	public void onNewToken(String token) {
		Log.v(TAG, "onNewToken: " + token);

		JSONObject json;

		try {
			json = new JSONObject().put("event", "registered");
			json.put("regid", token);

			// Send this JSON data to the JavaScript application above EVENT should be set to the msg type
			// In this case this is the registration ID
			Push.sendJavascript(json);

		} catch (JSONException e) {
			// No message to the user is sent, JSON failed
			Log.e(TAG, "onNewToken: JSON exception");
		}
	}

	public void createNotification(Bundle extras) {
		Log.v(TAG, "createNotification");

		// Create a new intent to invoke the PushHandlerActivity.
		// This intent is processed when the user taps on the notification.
		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);

		// Create a PendingIntent to let the system invoke our intent.
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		// We allow the defaults to be controlled by the backend.
		// Leaving the defaults at DEFAULT_ALL *and* providing a sound, will not work.
		// The backend has to take care to adjust the defaults to the parameters it is providing
		// for the notification.
		int defaults = NotificationCompat.DEFAULT_ALL;
		if (extras.getString("defaults") != null) {
			try {
				defaults = Integer.parseInt(extras.getString("defaults"));
			} catch (NumberFormatException ignored) {
			}
		}

		// The tag should usually be the conversation ID.
		// If this is not a notification relating to a conversation, then we just use the app name
		// and make further distinctions based on the notId parameter.
		String tag = extras.getString("tag");
		if (tag == null) {
			tag = getAppName(this);
		}

		// Retrieve the notId. Notification with the same tag and notId replace each other in the UI.
		int notId = 0;
		try {
			notId = Integer.parseInt(extras.getString("notId"));
		} catch (NumberFormatException ignored) {
		}

		// Determine the notification channel.
		String category = extras.getString("category");
		if (category == null) {
			category = "uncategorized";
		}

		// The main message text.
		String message = extras.getString("message");
		if (message == null) {
			message = "<missing message content>";
		}

		// A timestamp for when this message was sent.
		// This is usually used to indicate when something was said in a conversation.
		// The timestamp is a UNIX epoch (ms).
		long timestamp = 0;
		if (extras.getString("timestamp") != null) {
			try {
				timestamp = Long.parseLong(extras.getString("timestamp"));
			} catch (NumberFormatException ignored) {
			}
		}

		// Construct the actual notification itself.
		String title = extras.getString("title");
		NotificationCompat.Builder notificationBuilder =
				new NotificationCompat.Builder(this, category)
						.setDefaults(defaults)
						.setSmallIcon(R.drawable.notification)
						.setColor(Color.rgb(0x00, 0x96, 0x88))
						.setWhen(System.currentTimeMillis())
						.setContentTitle(title)
						.setTicker(title)
						.setContentIntent(contentIntent)
						.setAutoCancel(true)
						.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

		// Depending on the category, display the notification with the suiting styles.
		switch (category) {
			case "messages":
				notificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
				String authorName = extras.getString("authorName");
				if (authorName != null) {
					notificationBuilder.setStyle(new NotificationCompat.MessagingStyle(getString(R.string.user_name))
							.addMessage(message, timestamp, authorName));
				} else {
					notificationBuilder.setContentText(message);
				}

				// Allow for in-line replies to be entered in the notification.
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
					String replyLabel = getString(R.string.reply);

					RemoteInput remoteInput = new RemoteInput.Builder("reply")
							.setLabel(replyLabel)
							.build();

					// Build a PendingIntent for the reply action to trigger.
					PendingIntent replyPendingIntent =
							PendingIntent.getActivity(this,
									0,
									notificationIntent,
									PendingIntent.FLAG_UPDATE_CURRENT);

					// Create the reply action and add the remote input.
					NotificationCompat.Action actionReply =
							new NotificationCompat.Action.Builder(R.drawable.ic_action_next_item, replyLabel, replyPendingIntent)
									.addRemoteInput(remoteInput)
									.build();

					// Create another action that only opens the message in the app.
					// This would also happen if the users taps on the notification directly,
					// but this way it's more explicitly distinguished from the reply action.
					String viewLabel = getString(R.string.view);

					// Create the reply action and add the remote input.
					NotificationCompat.Action actionView =
							new NotificationCompat.Action.Builder(android.R.drawable.ic_media_play, viewLabel, contentIntent)
									.build();

					notificationBuilder.addAction(actionReply);
					notificationBuilder.addAction(actionView);
				}
				break;

			case "reminder":
				notificationBuilder.setCategory(NotificationCompat.CATEGORY_REMINDER);
				notificationBuilder.setContentText(message);
				break;

			case "uncategorized":
			default:
				notificationBuilder.setContentText(message);
				break;
		}

		// Add a count to the notification.
		String msgcnt = extras.getString("msgcnt");
		if (msgcnt != null) {
			notificationBuilder.setNumber(Integer.parseInt(msgcnt));
		}

		// If a sound was given, play it.
		// This could potentially be overridden by the notification channel (category).
		String soundName = extras.getString("sound");
		if (soundName != null) {
			Resources r = getResources();
			int resourceId = r.getIdentifier(soundName, "raw", this.getPackageName());
			Uri soundUri = Uri.parse("android.resource://" + this.getPackageName() + "/" + resourceId);
			notificationBuilder.setSound(soundUri);

			notificationBuilder.setVibrate(new long[]
					{
							0,
							1000, 1000, 1000, 1000, 1000,
							1000, 1000, 1000, 1000, 1000,
							1000, 1000, 1000, 1000, 1000,
					}
			);
		}

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (notificationManager != null) {
			notificationManager.notify(tag, notId, notificationBuilder.build());
		} else {
			Log.w(TAG, "Unable to retrieve NotificationManager.");
		}
	}
}
