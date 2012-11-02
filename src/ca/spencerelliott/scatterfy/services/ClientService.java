package ca.spencerelliott.scatterfy.services;

import ca.spencerelliott.scatterfy.ClientActivity;
import ca.spencerelliott.scatterfy.R;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class ClientService extends BluetoothServerService {
	@Override
	public void onCreate() {
		super.onCreate();
		
		Intent notificationIntent = new Intent(this, ClientActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		
		NotificationCompat.Builder notiBuilder = new NotificationCompat.Builder(this)
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle("Scatterfi")
			.setContentText("You are not connected to a device")
			.setOngoing(true)
			.setContentIntent(contentIntent);
		
		nm.notify(BluetoothSettings.NOTIFICATION_ID, notiBuilder.getNotification());
	}
}
