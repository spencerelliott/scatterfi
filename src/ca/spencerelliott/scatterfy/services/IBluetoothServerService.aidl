package ca.spencerelliott.scatterfy.services;

import java.util.List;
import android.content.Intent;
import ca.spencerelliott.scatterfy.services.MessengerCallback;

interface IBluetoothServerService {
	List<String> getConnectedClients();
	boolean isConnected();
	boolean registerUser(in String mac, MessengerCallback callback);
	void removeUser(in String mac);
	void sendMessage(in String address, in Intent intent);
	List<String> getMasterSlaves();
	List<String> getSlaves(in String msMac);
}