package ca.spencerelliott.scatterfy.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import ca.spencerelliott.scatterfy.messages.RoutedMessage;
import ca.spencerelliott.scatterfy.routing.IRoutingProtocol;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothSocketDevice {
	private BluetoothDevice device;
	private BluetoothSocket socket;
	
	private BluetoothMessageReceiverThread thread = null;
	
	private IRoutingProtocol routing;
	
	public BluetoothSocketDevice(BluetoothDevice device, BluetoothSocket socket) {
		this.device = device;
		this.socket = socket;
		
		thread = new BluetoothMessageReceiverThread();
		thread.start();
	}
	
	public void setRoutingProtocol(IRoutingProtocol routing) {
		this.routing = routing;
	}
	
	public void writeMessage(byte[] message) {
		try {
			OutputStream os = socket.getOutputStream();
			os.write(message);
		} catch (IOException e) {
			Log.e("Scatterfi", "Error sending message to client: " + e.getMessage());
		}
		
	}
	
	public String getAddress() {
		return device.getAddress();
	}
	
	public BluetoothDevice getDevice() {
		return device;
	}
	
	public BluetoothSocket getSocket() {
		return socket;
	}
	
	public void cleanup() {
		try {
			socket.close();
		} catch(Exception e) {
			
		}
	}
	
	private class BluetoothMessageReceiverThread extends Thread {
		@Override
		public void run() {
			InputStream is = null;
			
			try {
				//Grab the input stream for the socket
				is = socket.getInputStream();
			} catch(IOException e) {
				Log.e("Scatterfi", e.getMessage());
			}
			
			while(true) {
				ByteBuffer ba = ByteBuffer.allocate(512);
				
				try {
					int eomCount = 0;
					
					//Read the data from the buffer
					while(true) {
						byte readByte = (byte)is.read();
						
						ba.put(readByte);
						
						//Grow the size of the byte buffer if needed
						if(!ba.hasRemaining()) {
							ByteBuffer temp = ByteBuffer.allocate(ba.limit()*2);
							temp.put(ba.array());
							
							ba = temp;
						}
						
						//Check the byte to see if it's part of the end of message
						if(readByte == RoutedMessage.EOM[eomCount]) {
							eomCount++;
						//Reset the count if the byte doesn't match
						} else {
							eomCount = 0;
						}
						
						//Finish reading the message and send it to the routing protocol
						if(eomCount >= RoutedMessage.EOM.length) {
							break;
						}
					}
					
					Log.i("Scatterfi", "Receiving message from " + device.getAddress());
					
					//Copy the buffer into the final array of data
					byte[] finalArray = new byte[ba.limit()-ba.remaining()];
					byte[] baArray = ba.array();
					
					for(int i = 0; i < finalArray.length; i++) {
						finalArray[i] = baArray[i];
					}
					
					//Send the message to the routing protocol
					routing.receiveMessage(finalArray);
				} catch (IOException e) {
					Log.e("Scatterfi", e.getMessage());
					
					//Let the routing protocol know that the connection was lost
					routing.lostConnection(BluetoothSocketDevice.this);
					break;
				}
			}
		}
	}
}
