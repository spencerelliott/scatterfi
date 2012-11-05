package ca.spencerelliott.scatterfy.managers;

import ca.spencerelliott.scatterfy.routing.IRoutingProtocol;
import ca.spencerelliott.scatterfy.services.DeviceType;

public class DisconnectionManager {
	public static void handleLostConnection(IRoutingProtocol protocol, DeviceType type) {
		switch(type) {
			case SLAVE:
				break;
			case MASTER_SLAVE:
				break;
			default:
				break;
		}
	}
}
