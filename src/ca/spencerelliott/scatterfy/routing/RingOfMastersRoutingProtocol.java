package ca.spencerelliott.scatterfy.routing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

import ca.spencerelliott.scatterfy.messages.MessageIntent;
import ca.spencerelliott.scatterfy.messages.RoutedMessage;
import ca.spencerelliott.scatterfy.services.BluetoothSettings;
import ca.spencerelliott.scatterfy.services.BluetoothSocketDevice;
import ca.spencerelliott.scatterfy.services.DeviceType;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class RingOfMastersRoutingProtocol implements IRoutingProtocol {
	/** The maximum amount of devices that should be allocated to each master/slave node */
	public static final int MAX_DEVICES_PER_MS = 2;
	
	private DeviceType type = DeviceType.SLAVE;
	
	private BluetoothSocketDevice next = null;
	
	protected LinkedHashMap<String,ArrayList<String>> networkMap = null;
	
	private ArrayList<String> incomingClients = new ArrayList<String>();
	private String incomingMasterSlave = "";
	
	private String serverAddress = null;
	private boolean ignoreOnce = false;
	//private boolean connected = false;
	
	private LinkedHashMap<String,BluetoothSocketDevice> allDevices = new LinkedHashMap<String,BluetoothSocketDevice>();
	private ArrayList<String> ignoreList = new ArrayList<String>();
	
	private ArrayList<BluetoothSocketDevice> clients = new ArrayList<BluetoothSocketDevice>();
	
	private ArrayList<Long> sendIds = new ArrayList<Long>();
	
	private Context context = null;
	
	private BackgroundConnectionThread thread = null;
	
	public RingOfMastersRoutingProtocol(Context context) {
		this.context = context;
		
		thread = new BackgroundConnectionThread(this);
		thread.start();
	}
	
	@Override
	public synchronized void sendMessage(String to, Intent message) {		
		byte[] toBytes = RoutedMessage.convertAddressToByteArray(to);
		
		RoutedMessage routed = new RoutedMessage(toBytes, message);
		
		//Loopback message received
		if(to.equals(BluetoothSettings.MY_BT_ADDR)) {
			Log.i("Scatterfi", "Loopback message received");
			receiveMessage(routed.getByteMessage());
			return;
		}
		
		sendIds.add(routed.getId());
		
		//Check to see if this is a broadcast and should be forwarded everywhere
		if(to.equals(BluetoothSettings.BROADCAST_MAC)) {
			Log.i("Scatterfi", "Sending broadcast message from " + BluetoothSettings.MY_BT_ADDR);
			
			if(next != null) {
				next.writeMessage(routed.getByteMessage());
			}
			
			for(BluetoothSocketDevice d : clients) {
				d.writeMessage(routed.getByteMessage());
			}
			
			return;
		}
		
		//Check the list of clients and see if the message is for them
		for(BluetoothSocketDevice d : clients) {
			if(d.getAddress().equals(to)) {
				d.writeMessage(routed.getByteMessage());
				return;
			}
		}
		
		//If the client is not in this piconet, forward it
		if(next != null) {
			next.writeMessage(routed.getByteMessage());
		}
		
	}

	@Override
	public synchronized void receiveMessage(byte[] message) {
		RoutedMessage received = new RoutedMessage(message);
		
		Log.i("Scatterfi", "Message received: " + received.getIntent());
		
		//Make sure we haven't received this message before
		if(!sendIds.contains(received.getId())) {
			//Add the id to the processed ids
			sendIds.add(received.getId());
			
			//Get the to address of the message
			String address = RoutedMessage.convertByteArrayToAddress(received.getToAddress());
			
			//Check to see if this message is for this device or a broadcast
			if(address.equals(BluetoothSettings.MY_BT_ADDR) || address.equals(BluetoothSettings.BROADCAST_MAC)) {
				processIntent(received);
				
				//Resend this message if it was a broadcast
				if(address.equals(BluetoothSettings.BROADCAST_MAC)) {
					sendMessage(BluetoothSettings.BROADCAST_MAC, received.getIntent());
				}
			} else {
				sendMessage(address, received.getIntent());
			}
		}
	}

	@Override
	public void newClient(BluetoothSocketDevice device, boolean incoming) {
		//Add the device to the full list of devices
		allDevices.put(device.getAddress(), device);
		
		//Ignore the incoming connection and just accept it
		if(ignoreList.contains(device.getAddress())) {
			Log.i("Scatterfi", "Ignoring connection from " + device.getAddress());
			
			ignoreList.remove(device.getAddress());
			return;
		}
		
		//Only deal with rerouting and such if it's an incoming connection
		if(incoming) {
			if(incomingMasterSlave != null && incomingMasterSlave.equals(device.getAddress())) {
				incomingMasterSlave = null;
				
				//Set the device as the next node
				next = device;
				
				//Notify the server that the device connected successfully
				Intent intent = new Intent(MessageIntent.NEW_DEVICE_CONNECTED);
				intent.putExtra("mac", device.getAddress());
				intent.putExtra("type", DeviceType.MASTER_SLAVE.ordinal());
				
				sendMessage(serverAddress, intent);
				
				return;
			}
			
			if(incomingClients.contains(device.getAddress())) {
				incomingClients.remove(device.getAddress());
				
				//Add the device to the set of clients
				clients.add(device);
				
				//Notify the server that the device connected successfully
				Intent intent = new Intent(MessageIntent.NEW_DEVICE_CONNECTED);
				intent.putExtra("mac", device.getAddress());
				intent.putExtra("type", DeviceType.SLAVE.ordinal());
				
				sendMessage(serverAddress, intent);
	
				return;
			}
			
			if(type == DeviceType.SERVER) {
				serverConnect(device);
			}
		}
	}

	@Override
	public void disconnectClient(String mac) {
		
	}
	
	@Override
	public void lostConnection(BluetoothSocketDevice device) {
		Log.i("Scatterfi", device.getAddress() + " lost connection");
		
		if(!ignoreOnce) {
			BluetoothSocketDevice d = allDevices.get(device.getAddress());
			
			if(d != null) {
				d.cleanup();
			}
			
			allDevices.remove(device.getAddress());
			
			Log.i("Scatterfi", "All device count: " + allDevices.size());
		} else {
			ignoreOnce = false;
		}

	}
	
	@Override
	public void destroyAndCleanUp() {
		thread.stopListening();
		clearAllConnections();
	}
	
	@Override
	public void setDeviceType(DeviceType type) {
		this.type = type;
		
		//Initialize the network map if the new type is a server
		if(type == DeviceType.SERVER) {
			serverAddress = BluetoothSettings.MY_BT_ADDR;
			networkMap = new LinkedHashMap<String,ArrayList<String>>();
		}
	}
	
	private class BackgroundConnectionThread extends Thread {
		private IRoutingProtocol protocol = null;
		private boolean threadRunning = true;
		
		public BackgroundConnectionThread(IRoutingProtocol protocol) {
			this.protocol = protocol;
		}
		
		@Override
		public void run() {
			//Get the default bluetooth adapter for the device
			BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			BluetoothServerSocket serverSock = null;
			
			//Create the Bluetooth listen server using the Scatterfi UUID
			try {
				serverSock = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("Scatterfi server", BluetoothSettings.BT_UUID);
			} catch (IOException e) {
				Log.e("Scatterfi", e.getMessage());
				return;
			}
			
			Log.i("Scatterfi", "Listening for incoming connections [UUID: " + BluetoothSettings.BT_UUID.toString() + "]");
			
			//Run continuously looking for new connections
			while(threadRunning) {
				try {
					//Look for 10s
					BluetoothSocket socket = serverSock.accept(10000);
					
					if(socket != null) {
						//Create the socket device
						BluetoothSocketDevice newDevice = new BluetoothSocketDevice(socket.getRemoteDevice(), socket);
						newDevice.setRoutingProtocol(protocol);
						
						//Pass it to the protocol to handle
						if(protocol != null) {
							protocol.newClient(newDevice, true);
						}
					}
				} catch(IOException e) {
					//Log.e("Scatterfi", e.getMessage());
				}
			}
			
			Log.i("Scatterfi", "Finished listening for connections");
		}
		
		public void stopListening() {
			threadRunning = false;
		}
	}
	
	private void clearAllConnections() {
		//Disconnect from the next node if it exists
		if(next != null) {
			next.cleanup();
			next = null;
		}
		
		//Disconnect from all clients
		for(BluetoothSocketDevice d : clients) {
			d.cleanup();
		}
		
		//Create a new list of clients
		clients = new ArrayList<BluetoothSocketDevice>();
		
		//Clean up all devices
		for(String s : allDevices.keySet()) {
			allDevices.get(s).cleanup();
		}
	}
	
	private void processIntent(RoutedMessage message) {
		Intent intent = message.getIntent();
		
		if(intent.getAction().equals(MessageIntent.CONNECT)) {			
			//Get the extras from the sent intent
			Bundle extras = intent.getExtras();
			
			//Retrieve the MAC address to connect to and the new device type
			String mac = extras.getString("mac");
			type = DeviceType.values()[extras.getInt("type")];		
			
			//Clear all connections from the device
			clearAllConnections();
			
			//When losing connection to the server and reconnecting, don't panic
			if(mac.equals(serverAddress)) {
				ignoreOnce = true;
			}
			
			Log.i("Scatterfi", "Attempting to connect to " + mac + " [" + intent.getAction() + "]");
			
			try {
				//Connect to the device
				BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac);
				BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(BluetoothSettings.BT_UUID);
				socket.connect();
				
				BluetoothSocketDevice d = new BluetoothSocketDevice(device, socket);
				d.setRoutingProtocol(this);
				
				allDevices.put(device.getAddress(), d);
				
				//If this device is a slave, set it up to forward its messages to the master/slave (d)
				if(type == DeviceType.SLAVE) {
					next = d;
				} else if(type == DeviceType.MASTER_SLAVE) {
					masterConnect(d);
				}
			} catch(IOException e) { 
				Log.e("Scatterfi", "Could not connect to device! [" + intent.getAction() + "]");
			}
		} else if(intent.getAction().equals(MessageIntent.INCOMING_SLAVE) && type == DeviceType.MASTER_SLAVE) {
			//Get the extras from the intent and retrieve the incoming MAC address
			Bundle extras = intent.getExtras();
			String mac = extras.getString("mac");
			
			Log.i("Scatterfi", "Adding new incoming slave: " + mac + " [" + intent.getAction() + "]");
			
			//Add the MAC address to the incoming client list
			incomingClients.add(mac);
		} else if(intent.getAction().equals(MessageIntent.INCOMING_MASTER_SLAVE) && (type == DeviceType.MASTER_SLAVE || type == DeviceType.SERVER)) {			
			//Get the extras from the intent and retrieve the incoming MAC address
			Bundle extras = intent.getExtras();
			String mac = extras.getString("mac");
			
			Log.i("Scatterfi", "Setting new incoming master/slave to " + mac + " [" + intent.getAction() + "]");
			
			//Set the next master/slave MAC address
			incomingMasterSlave = mac;
		} else if(intent.getAction().equals(MessageIntent.INCOMING_CONNECTION_IGNORE) && type == DeviceType.SERVER) {
			ignoreList.add(intent.getExtras().getString("mac"));
		} else if(intent.getAction().equals(MessageIntent.CHAT_MESSAGE)) {
			Log.i("Scatterfi", "Chat message received" + " [" + intent.getAction() + "]");
		} else if(intent.getAction().equals(MessageIntent.NOTE_MESSAGE)) {
			Log.i("Scatterfi", "Note received" + " [" + intent.getAction() + "]");
		} else if(intent.getAction().equals(MessageIntent.DISCOVERY)) {
			Log.i("Scatterfi", "Discovery request sent by " + RoutedMessage.convertByteArrayToAddress(message.getFromAddress()) + " [" + intent.getAction() + "]");
		} else if(intent.getAction().equals(MessageIntent.LOST_CONNECTION) && type == DeviceType.SERVER) {
			
		} else if(intent.getAction().equals(MessageIntent.SERVER_MAC) && serverAddress == null) {
			Log.i("Scatterfi", "Setting server address to " + intent.getExtras().getString("mac") + " [" + intent.getAction() + "]");
			
			serverAddress = intent.getExtras().getString("mac");
		} else if(intent.getAction().equals(MessageIntent.NEW_DEVICE_CONNECTED) && type == DeviceType.SERVER) {
			DeviceType connectionType = DeviceType.values()[intent.getExtras().getInt("type")];
			
			Log.i("Scatterfi", "New " + connectionType.name() + " connected" + " [" + intent.getAction() + "]");
			
			switch(connectionType) {
				case MASTER_SLAVE:
					//Add the master/slave to the network map
					networkMap.put(intent.getExtras().getString("mac"), new ArrayList<String>());
					break;
				case SLAVE:
					//Add the slave to the master/slave node that sent the message
					networkMap.get(RoutedMessage.convertByteArrayToAddress(message.getFromAddress())).add(intent.getExtras().getString("mac"));
					break;
			}
		} else {
			//Let Android handle the intent otherwise
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		}
	}
	
	private void serverConnect(BluetoothSocketDevice device) {		
		//If this is the first node in the network
		if(next == null) {			
			Log.i("Scatterfi", "First master/slave connected to network");
			
			//Create the intent to send a loopback to say that this device will become a master/slave
			Intent intent = new Intent(MessageIntent.INCOMING_MASTER_SLAVE);
			intent.putExtra("mac", device.getAddress());
			
			//Send the loopback message
			sendMessage(BluetoothSettings.MY_BT_ADDR, intent);
			
			//Create the intent to send to the device to tell it who the server is
			intent = new Intent(MessageIntent.SERVER_MAC);
			intent.putExtra("mac", BluetoothSettings.MY_BT_ADDR);
			
			//Send the message to the new device
			RoutedMessage message = new RoutedMessage(RoutedMessage.convertAddressToByteArray(device.getAddress()), intent);
			device.writeMessage(message.getByteMessage());
			
			//Create the intent to send to the device to tell it to connect to the server again
			intent = new Intent(MessageIntent.CONNECT);
			intent.putExtra("mac", BluetoothSettings.MY_BT_ADDR);
			intent.putExtra("type", DeviceType.MASTER_SLAVE.ordinal());
			
			//Send the message to the new device
			message = new RoutedMessage(RoutedMessage.convertAddressToByteArray(device.getAddress()), intent);
			device.writeMessage(message.getByteMessage());
			return;
		}
		
		Set<String> msNodes = networkMap.keySet();
		String lastNode = null;
		
		//Check the amount of nodes on each master/slave in the map
		for(String s : msNodes) {
			lastNode = s;
			
			ArrayList<String> msList = networkMap.get(s);
			
			//If this node has more room for clients, notify the master/slave of an incoming connection
			//and tell the new client to connect to the master/slave
			if(msList.size() < MAX_DEVICES_PER_MS) {
				Log.i("Scatterfi", "Assigning " + device.getAddress() + " to " + s);
				
				Intent intent = new Intent(MessageIntent.INCOMING_SLAVE);
				intent.putExtra("mac", device.getAddress());
				
				sendMessage(s, intent);
				
				//Create the intent to send to the device to tell it who the server is
				intent = new Intent(MessageIntent.SERVER_MAC);
				intent.putExtra("mac", BluetoothSettings.MY_BT_ADDR);
				
				//Send the message to the new device
				RoutedMessage message = new RoutedMessage(RoutedMessage.convertAddressToByteArray(device.getAddress()), intent);
				device.writeMessage(message.getByteMessage());
				
				intent = new Intent(MessageIntent.CONNECT);
				intent.putExtra("mac", s);
				intent.putExtra("type", DeviceType.SLAVE.ordinal());
				
				message = new RoutedMessage(RoutedMessage.convertAddressToByteArray(device.getAddress()), intent);
				device.writeMessage(message.getByteMessage());
				
				return;
			}
		}
		
		//No room on the network, add this node as a master/slave. Send the last added node
		//a message notifying it of a new incoming master/slave
		Intent intent = new Intent(MessageIntent.INCOMING_MASTER_SLAVE);
		intent.putExtra("mac", device.getAddress());
		
		sendMessage(lastNode, intent);
		
		//Create the intent to send to the device to tell it who the server is
		intent = new Intent(MessageIntent.SERVER_MAC);
		intent.putExtra("mac", BluetoothSettings.MY_BT_ADDR);
		
		//Send the message to the new device
		RoutedMessage message = new RoutedMessage(RoutedMessage.convertAddressToByteArray(device.getAddress()), intent);
		device.writeMessage(message.getByteMessage());
		
		//Tell the server to ignore the next incoming connection from this device as it will
		//connect to the server as it's next node
		intent = new Intent(MessageIntent.INCOMING_CONNECTION_IGNORE);
		intent.putExtra("mac", device.getAddress());
		
		//Send the loopback message
		sendMessage(BluetoothSettings.MY_BT_ADDR, intent);
		
		//Create the intent to tell the new device to connect to the last node
		intent = new Intent(MessageIntent.CONNECT);
		intent.putExtra("mac", lastNode);
		intent.putExtra("type", DeviceType.MASTER_SLAVE.ordinal());
		
		message = new RoutedMessage(RoutedMessage.convertAddressToByteArray(device.getAddress()), intent);
		device.writeMessage(message.getByteMessage());
	}
	
	private void clientConnect(BluetoothSocketDevice device) {
				
	}
	
	private void masterConnect(BluetoothSocketDevice newDevice) {
		if(serverAddress == null) return;
		
		if(newDevice.getAddress().equals(serverAddress)) {
			Log.i("Scatterfi", "Server already connected. Adding as next device");
			
			next = newDevice;
			return;
		}
		
		//Connect to the server to be the next node
		try {
			Log.i("Scatterfi", "Connecting to server as next device");
			
			//Connect to the device
			BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(serverAddress);
			BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(BluetoothSettings.BT_UUID);
			socket.connect();
			
			BluetoothSocketDevice d = new BluetoothSocketDevice(device, socket);
			d.setRoutingProtocol(this);
			
			//Add the server to all devices
			allDevices.put(d.getAddress(), d);
			
			//Set the next node to be the server
			next = d;
		} catch(IOException e) { 
			Log.i("Scatterfi", "Could not connect to server");
		}
	}
}
