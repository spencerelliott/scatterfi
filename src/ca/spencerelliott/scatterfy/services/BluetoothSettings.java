package ca.spencerelliott.scatterfy.services;

import java.util.UUID;

import android.bluetooth.BluetoothAdapter;

public final class BluetoothSettings {
	//public final static UUID BT_UUID = UUID.fromString("d6e4a890-01ee-11e2-a21f-0800200c9a66");
	public final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	public final static int NOTIFICATION_ID = 832781;
	
	public final static String BROADCAST_MAC = "00:00:00:00:00:00";
	
	public static String MY_BT_ADDR = BluetoothAdapter.getDefaultAdapter().getAddress();
}
