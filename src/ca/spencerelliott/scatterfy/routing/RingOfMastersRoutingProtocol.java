package ca.spencerelliott.scatterfy.routing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import ca.spencerelliott.scatterfy.managers.DisconnectionManager;
import ca.spencerelliott.scatterfy.managers.ServerManager;
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
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class RingOfMastersRoutingProtocol extends IRoutingProtocol {		
	//---- Master/Slave - Slave specific variables ----
	private String serverAddress = null;
	private ArrayList<BluetoothSocketDevice> clients = new ArrayList<BluetoothSocketDevice>();
	//-------------------------------------------------
	
	//---- Server specific variables ----
	protected LinkedHashMap<String,ArrayList<String>> networkMap = null;
	protected ArrayList<String> disconnectedSlaves = null;
	//-----------------------------------
	
	//---- Client/Server variables ----	
	private BluetoothSocketDevice next = null;
	private DeviceType type = DeviceType.SLAVE;
	
	private ArrayList<String> incomingClients = new ArrayList<String>();
	private String incomingMasterSlave = "";
	
	private LinkedHashMap<String,BluetoothSocketDevice> allDevices = new LinkedHashMap<String,BluetoothSocketDevice>();
	private ArrayList<String> ignoreList = new ArrayList<String>();
	
	private ArrayList<Long> sendIds = new ArrayList<Long>();
	//----------------------------------

	private BackgroundConnectionThread thread = null;
	
	public RingOfMastersRoutingProtocol(Context context) {
		super(context);
		
		thread = new BackgroundConnectionThread(this);
		thread.start();
	}
	
	@Override
	public synchronized void sendMessage(String to, Intent message) {
		//Create a new routed message using the information provided
		byte[] toBytes = RoutedMessage.convertAddressToByteArray(to);
		RoutedMessage routed = new RoutedMessage(toBytes, message);
		
		//Send the message using the generic send method
		sendMessage(routed);		
	}
	
	public void sendMessage(RoutedMessage routed) {
		String to = RoutedMessage.convertByteArrayToAddress(routed.getToAddress());
		
		//Loopback message received
		if(to.equals(BluetoothSettings.MY_BT_ADDR)) {
			Log.i("Scatterfi", "Loopback message received");
			receiveMessage(routed.getByteMessage());
			return;
		}
		
		//Add the id to the list of ids so this message will not be processed again
		sendIds.add(routed.getId());
		
		//Check to see if this is a broadcast and should be forwarded everywhere
		if(to.equals(BluetoothSettings.BROADCAST_MAC)) {
			Log.i("Scatterfi", "Sending broadcast message from " + BluetoothSettings.MY_BT_ADDR);
			
			//Send message to the next node
			if(next != null) {
				next.writeMessage(routed.getByteMessage());
			}
			
			//Send the message to all slaves attached to this device
			for(BluetoothSocketDevice d : clients) {
				d.writeMessage(routed.getByteMessage());
			}
			
			return;
		}
		
		//Check the list of clients and see if the message is for them
		for(BluetoothSocketDevice d : clients) {
			//Write the message to the specific device
			if(d.getAddress().equals(to)) {
				d.writeMessage(routed.getByteMessage());
				return;
			}
		}
		
		//If the client is not in this piconet, forward it to the next one
		if(next != null) {
			next.writeMessage(routed.getByteMessage());
		}
	}

	protected synchronized void receiveMessage(byte[] message) {
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
					sendMessage(received);
				}
			} else {
				sendMessage(received);
			}
		}
	}

	@Override
	public void newConnection(BluetoothSocketDevice device, boolean incoming) {
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
			
			//Redirect the client to the appropriate address
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
		
		//Check to see if the disconnection was voluntary
		if(!device.wasVoluntaryDisconnect()) {
			Log.i("Scatterfi", "Involuntary disconnection from " + device.getAddress());
			
			BluetoothSocketDevice d = allDevices.get(device.getAddress());
			
			//Cleanup the socket
			if(d != null) {
				d.cleanup();
			}
			
			//Remove the disconnected device from the master list
			allDevices.remove(device.getAddress());
			
			//Create the intent to send to the server when a connection is lost
			Intent lostIntent = new Intent(MessageIntent.LOST_CONNECTION);
			lostIntent.putExtra("mac", device.getAddress());
			
			//Send a message to the server notifying it of the disconnect
			if(type == DeviceType.SERVER) {
				sendMessage(BluetoothSettings.MY_BT_ADDR, lostIntent);
			} else if(type == DeviceType.MASTER_SLAVE && next != null && !next.getAddress().equals(device)) {
				sendMessage(serverAddress, lostIntent);
			}
			
			Log.i("Scatterfi", "All device count: " + allDevices.size());
		} else {
			Log.i("Scatterfi", "Voluntary disconnection from " + device.getAddress());
		}
		
		//If the device that was connected was the next node, remove it
		if(next != null && device.getAddress().equals(next.getAddress())) {
			next = null;
			
			DisconnectionManager.handleLostConnection(this, type);
		}
	}
	
	@Override
	public void destroyAndCleanUp() {
		super.destroyAndCleanUp();
		
		//Stop listening for incoming connections
		thread.stopListening();
		
		//Clear any connections this device has
		clearAllConnections();
	}
	
	@Override
	public void setDeviceType(DeviceType type) {
		this.type = type;
		
		//Initialize the network map if the new type is a server
		if(type == DeviceType.SERVER) {
			serverAddress = BluetoothSettings.MY_BT_ADDR;
			networkMap = new LinkedHashMap<String,ArrayList<String>>();
			disconnectedSlaves = new ArrayList<String>();
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
							protocol.newConnection(newDevice, true);
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
	
	public void clearAllConnections() {
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
		allDevices.clear();
		
		//Reset the state
		next = null;
		clients.clear();
	}
	
	@Override
	protected void processIntent(RoutedMessage message) {		
		Intent intent = message.getIntent();
		
		if(intent.getAction().equals(MessageIntent.CONNECT)) {			
			//Get the extras from the sent intent
			Bundle extras = intent.getExtras();
			
			//Retrieve the MAC address to connect to and the new device type
			String mac = extras.getString("mac");
			type = DeviceType.values()[extras.getInt("type")];		
			
			//Clear all connections from the device
			clearAllConnections();
			
			try {
				Thread.sleep(5000);
			} catch(Exception e) {
				Log.e("Scatterfi", "Interrupted while waiting: " + e.getMessage());
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
				
				//Update the notification to show the user which device they're connected to
				notifyUser("Connected to " + mac);
			} catch(IOException e) { 
				Log.e("Scatterfi", "Could not connect to device! " + e.getMessage() + " [" + intent.getAction() + "]");
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
		} else if(intent.getAction().equals(MessageIntent.SERVER_MAC)) {
			Log.i("Scatterfi", "Setting server address to " + intent.getExtras().getString("mac") + " [" + intent.getAction() + "]");
			
			//Save the server address for use later
			serverAddress = intent.getExtras().getString("mac");
		} else if(intent.getAction().equals(MessageIntent.NEW_DEVICE_CONNECTED) && type == DeviceType.SERVER) {
			//Determine which type of device was connected
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
		} else if(intent.getAction().equals(MessageIntent.LOST_CONNECTION) && type == DeviceType.SERVER) {
			String address = intent.getExtras().getString("mac");
			
			//Try to remove this as a master/slave from the map
			ArrayList<String> removed = networkMap.remove(address);
			
			//If the device wasn't a master/slave, check all slaves
			if(removed == null) {
				Set<String> msNodes = networkMap.keySet();
				boolean foundSlave = false;
				
				//Loop through each master/slave
				for(String s : msNodes) {
					ArrayList<String> slaves = networkMap.get(s);
					Iterator<String> iSlaves = slaves.iterator();
					
					//Check each slave
					while(iSlaves.hasNext()) {
						String is = iSlaves.next();
						
						if(is.equals(address)) {
							iSlaves.remove();
							foundSlave = true;
							break;
						}
					}
					
					if(foundSlave) break;
				}
			}
		} else {
			//If the intent wasn't handled by this protocol, forward it to the super class
			super.processIntent(message);
		}
	}
	
	private void serverConnect(BluetoothSocketDevice device) {		
		//If this is the first node in the network
		if(next == null) {			
			ServerManager.assignFirstMasterSlave(this, device);
			return;
		}
		
		//Attempt to assign the new device to an existing master/slave
		if(ServerManager.assignToExistingNode(this, device, networkMap)) {
			return;
		}
		
		//Get the last master/slave node in the network map
		ArrayList<String> keys = new ArrayList<String>(networkMap.keySet());
		String lastNode = keys.get(keys.size()-1);
		
		//Assign this device as a new master/slave
		ServerManager.assignAsMasterSlave(this, device, lastNode);
	}
	
	private void masterConnect(BluetoothSocketDevice newDevice) {
		//Something is missing, do not attempt to connect
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

	@Override
	public LinkedHashMap<String, ArrayList<String>> getNetworkMap() {
		if(type != DeviceType.SERVER) return null;
		
		return networkMap;
	}
}
