package ca.spencerelliott.scatterfy.routing;

import java.util.ArrayList;

import ca.spencerelliott.scatterfy.messages.RoutedMessage;
import ca.spencerelliott.scatterfy.services.BluetoothSettings;
import ca.spencerelliott.scatterfy.services.BluetoothSocketDevice;

import android.content.Context;
import android.content.Intent;

public class RingOfMastersRoutingProtocol implements IRoutingProtocol {
	private BluetoothSocketDevice next = null;
	private BluetoothSocketDevice prev = null;
	
	private ArrayList<BluetoothSocketDevice> clients = new ArrayList<BluetoothSocketDevice>();
	
	private ArrayList<Integer> sendIds = new ArrayList<Integer>();
	
	private Context context = null;
	
	public RingOfMastersRoutingProtocol(Context context) {
		this.context = context;
	}
	
	@Override
	public void sendMessage(String to, Intent message) {
		byte[] toBytes = RoutedMessage.convertAddressToByteArray(to);
		
		RoutedMessage routed = new RoutedMessage(toBytes, message);
		sendIds.add(routed.getId());
		
		//Check to see if this is a broadcast and should be forwarded everywhere
		if(to.equals(BluetoothSettings.BROADCAST_MAC)) {
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
		next.writeMessage(routed.getByteMessage());
	}

	@Override
	public void receiveMessage(byte[] message) {
		RoutedMessage received = new RoutedMessage(message);
		
		//Make sure we haven't received this message before
		if(!sendIds.contains(received.getId())) {
			//Add the id to the processed ids
			sendIds.add(received.getId());
			
			//Get the to address of the message
			String address = RoutedMessage.convertByteArrayToAddress(received.getToAddress());
			
			//Check to see if this message is for this device or a broadcast
			if(address.equals(BluetoothSettings.MY_BT_ADDR) || address.equals(BluetoothSettings.BROADCAST_MAC)) {
				context.startActivity(received.getIntent());
				
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
	public void newClient(String mac) {
		
	}

	@Override
	public void disconnectClient(String mac) {
		
	}
	
	
}
