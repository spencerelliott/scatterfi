package ca.spencerelliott.scatterfy.managers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

import android.content.Intent;
import android.util.Log;

import ca.spencerelliott.scatterfy.messages.MessageIntent;
import ca.spencerelliott.scatterfy.messages.RoutedMessage;
import ca.spencerelliott.scatterfy.routing.IRoutingProtocol;
import ca.spencerelliott.scatterfy.services.BluetoothSettings;
import ca.spencerelliott.scatterfy.services.BluetoothSocketDevice;
import ca.spencerelliott.scatterfy.services.DeviceType;

public class ServerManager {
	/** The maximum amount of devices that should be allocated to each master/slave node */
	public static final int MAX_DEVICES_PER_MS = 2;
	
	private static void sendServerAddress(BluetoothSocketDevice device) {
		//Create the intent to send to the device to tell it who the server is
		Intent intent = new Intent(MessageIntent.SERVER_MAC);
		intent.putExtra("mac", BluetoothSettings.MY_BT_ADDR);
		
		//Send the message to the new device
		RoutedMessage message = new RoutedMessage(RoutedMessage.convertAddressToByteArray(device.getAddress()), intent);
		device.writeMessage(message.getByteMessage());
	}
	
	public static boolean assignFirstMasterSlave(IRoutingProtocol protocol, BluetoothSocketDevice device) {
		Log.i("Scatterfi", "First master/slave connected to network");
		
		//Create the intent to send a loopback to say that this device will become a master/slave
		Intent intent = new Intent(MessageIntent.INCOMING_MASTER_SLAVE);
		intent.putExtra("mac", device.getAddress());
		
		//Send the loopback message
		protocol.sendMessage(BluetoothSettings.MY_BT_ADDR, intent);
		
		//Send the server address to the new device
		sendServerAddress(device);
		
		//Create the intent to send to the device to tell it to connect to the server again
		intent = new Intent(MessageIntent.CONNECT);
		intent.putExtra("mac", BluetoothSettings.MY_BT_ADDR);
		intent.putExtra("type", DeviceType.MASTER_SLAVE.ordinal());
		
		//Send the message to the new device
		RoutedMessage message = new RoutedMessage(RoutedMessage.convertAddressToByteArray(device.getAddress()), intent);
		device.writeMessage(message.getByteMessage());
		return true;
	}
	
	public static boolean assignToExistingNode(IRoutingProtocol protocol, BluetoothSocketDevice device, LinkedHashMap<String,ArrayList<String>> networkMap) {
		Set<String> msNodes = networkMap.keySet();
		
		//Check the amount of nodes on each master/slave in the map
		for(String s : msNodes) {			
			ArrayList<String> msList = networkMap.get(s);
			
			//If this node has more room for clients, notify the master/slave of an incoming connection
			//and tell the new client to connect to the master/slave
			if(msList.size() < MAX_DEVICES_PER_MS) {
				Log.i("Scatterfi", "Assigning " + device.getAddress() + " to " + s);
				
				Intent intent = new Intent(MessageIntent.INCOMING_SLAVE);
				intent.putExtra("mac", device.getAddress());
				
				protocol.sendMessage(s, intent);
				
				sendServerAddress(device);
				
				intent = new Intent(MessageIntent.CONNECT);
				intent.putExtra("mac", s);
				intent.putExtra("type", DeviceType.SLAVE.ordinal());
				
				RoutedMessage message = new RoutedMessage(RoutedMessage.convertAddressToByteArray(device.getAddress()), intent);
				device.writeMessage(message.getByteMessage());
				
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean assignAsMasterSlave(IRoutingProtocol protocol, BluetoothSocketDevice device, String connectTo) {
		//No room on the network, add this node as a master/slave. Send the last added node
		//a message notifying it of a new incoming master/slave
		Intent intent = new Intent(MessageIntent.INCOMING_MASTER_SLAVE);
		intent.putExtra("mac", device.getAddress());
		
		protocol.sendMessage(connectTo, intent);
		
		//Send the server address to the device
		sendServerAddress(device);
		
		//Tell the server to ignore the next incoming connection from this device as it will
		//connect to the server as it's next node
		intent = new Intent(MessageIntent.INCOMING_CONNECTION_IGNORE);
		intent.putExtra("mac", device.getAddress());
		
		//Send the loopback message
		protocol.sendMessage(BluetoothSettings.MY_BT_ADDR, intent);
		
		//Create the intent to tell the new device to connect to the last node
		intent = new Intent(MessageIntent.CONNECT);
		intent.putExtra("mac", connectTo);
		intent.putExtra("type", DeviceType.MASTER_SLAVE.ordinal());
		
		RoutedMessage message = new RoutedMessage(RoutedMessage.convertAddressToByteArray(device.getAddress()), intent);
		device.writeMessage(message.getByteMessage());
		
		return true;
	}
}
