package ca.spencerelliott.scatterfy.routing;

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
	public void newClient(String mac);
	
	/**
	 * Called when a client has disconnected from the client
	 * @param mac The MAC address of the disconnected client
	 */
	public void disconnectClient(String mac);
}
