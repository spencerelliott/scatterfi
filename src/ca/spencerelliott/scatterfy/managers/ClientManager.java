package ca.spencerelliott.scatterfy.managers;

import ca.spencerelliott.scatterfy.routing.IRoutingProtocol;
import ca.spencerelliott.scatterfy.services.BluetoothSocketDevice;

public class ClientManager {
	/**
	 * Called when a slave loses connection to a master/slave
	 * @param protocol The protocol dealing with this device
	 * @param device The device that lost connection
	 * @return True if the message was successfully sent to the server
	 */
	public static boolean lostConnectionToSlave(IRoutingProtocol protocol, BluetoothSocketDevice device) {
		return true;
	}
}
