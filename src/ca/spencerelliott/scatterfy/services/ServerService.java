package ca.spencerelliott.scatterfy.services;

import ca.spencerelliott.scatterfy.R;
import ca.spencerelliott.scatterfy.ServerActivity;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class ServerService extends BluetoothServerService {
	@Override
	public void onCreate() {
		super.onCreate();
		
		//Create the intent for the notification
		Intent notificationIntent = new Intent(this, ServerActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		
		//Create the notification to tell the user that they are the server
		NotificationCompat.Builder notiBuilder = new NotificationCompat.Builder(this)
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle("Scatterfi")
			.setContentText("You are the server")
			.setOngoing(true)
			.setContentIntent(contentIntent);
		
		nm.notify(BluetoothSettings.NOTIFICATION_ID, notiBuilder.getNotification());
		
		//Change the service type to a server
		updateType(DeviceType.SERVER);
	}
}
