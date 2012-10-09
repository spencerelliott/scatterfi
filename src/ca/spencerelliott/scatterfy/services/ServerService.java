package ca.spencerelliott.scatterfy.services;

import ca.spencerelliott.scatterfy.MainActivity;
import ca.spencerelliott.scatterfy.R;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class ServerService extends BluetoothServerService {
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate() {
		super.onCreate();
		
		int icon = R.drawable.ic_launcher;
		CharSequence tickerText = "Scatterfi is running";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "Scatterfi";
		CharSequence contentText = "Running as the server";
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		
		nm.notify(BluetoothSettings.NOTIFICATION_ID, notification);
		
		//Change the service type to a server
		updateType(DeviceType.SERVER);
	}
}
