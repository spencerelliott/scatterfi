package ca.spencerelliott.scatterfy.services;

import android.app.Notification;

public class ClientService extends BluetoothServerService {
	@Override
	public void onCreate() {
		@SuppressWarnings("deprecation")
		Notification noti = new Notification.Builder(this.getApplicationContext())
			.setContentTitle("Scatterfy")
			.setContentText("You are connected to a Scatterfy server")
			.setOngoing(true)
			.getNotification();
		
		nm.notify(BluetoothSettings.NOTIFICATION_ID, noti);
	}
}
