package ca.spencerelliott.scatterfy.routing;

import ca.spencerelliott.scatterfy.services.BluetoothSocketDevice;
import ca.spencerelliott.scatterfy.services.DeviceType;
import android.content.Intent;

public interface IRoutingProtocol {
	/**
	 * Sends a message to a specific MAC address within the network
	 * @param to The address of the device to receive the message (00:00:00:00:00:00 for broadcast)
	 * @param message The intent to execute on the remote device
	 */
	public void sendMessage(String to, Intent message);
	
	/**
	 * Called when the client receives a message
	 * @param message The data received for the message
	 */
	public void receiveMessage(byte[] message);
	
	/**
	 * Called when a new client is trying to connect to the device
	 * @param mac The MAC address of the new client
	 */
	public void newClient(BluetoothSocketDevice device);
	
	/**
	 * Called when a client has disconnected from the client
	 * @param mac The MAC address of the disconnected client
	 */
	public void disconnectClient(String mac);
	
	/**
	 * Called when a client loses connection to the device
	 * @param device The device that lost the connection
	 */
	public void lostConnection(BluetoothSocketDevice device);
	
	/**
	 * Destroys and closes any connections that this this routing protocol has
	 */
	public void destroyAndCleanUp();
	
	/**
	 * Sets the type of device that this protocol is running on
	 * @param type The <code>DeviceType</code> of the device
	 */
	public void setDeviceType(DeviceType type);
}
