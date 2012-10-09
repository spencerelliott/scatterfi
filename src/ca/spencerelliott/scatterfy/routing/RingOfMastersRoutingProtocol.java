package ca.spencerelliott.scatterfy.routing;

import java.io.IOException;
import java.util.ArrayList;

import ca.spencerelliott.scatterfy.messages.RoutedMessage;
import ca.spencerelliott.scatterfy.services.BluetoothSettings;
import ca.spencerelliott.scatterfy.services.BluetoothSocketDevice;
import ca.spencerelliott.scatterfy.services.DeviceType;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RingOfMastersRoutingProtocol implements IRoutingProtocol {
	private DeviceType type = DeviceType.MASTER_SLAVE;
	
	private BluetoothSocketDevice next = null;
	//private BluetoothSocketDevice prev = null;
	
	private ArrayList<BluetoothSocketDevice> clients = new ArrayList<BluetoothSocketDevice>();
	
	private ArrayList<Integer> sendIds = new ArrayList<Integer>();
	
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
				processIntent(received.getIntent());
				
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
	public void newClient(BluetoothSocketDevice device) {
		switch(type) {
			case SERVER:
				serverConnect(device);
				break;
			case MASTER_SLAVE:
			case SLAVE:
				clientConnect(device);
				break;
		}
		clients.add(device);
	}

	@Override
	public void disconnectClient(String mac) {
		
	}
	
	@Override
	public void lostConnection(BluetoothSocketDevice device) {
		
	}
	
	@Override
	public void destroyAndCleanUp() {
		thread.stopListening();
	}
	
	@Override
	public void setDeviceType(DeviceType type) {
		this.type = type;
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
				serverSock = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("Scatterfy server", BluetoothSettings.BT_UUID);
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
							protocol.newClient(newDevice);
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
	
	private void processIntent(Intent intent) {
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
	
	private void serverConnect(BluetoothSocketDevice device) {
		
	}
	
	private void clientConnect(BluetoothSocketDevice device) {
		
	}
}
