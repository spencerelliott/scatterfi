package ca.spencerelliott.scatterfy.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
				//Create a buffer and string buffer
				StringBuilder sb = new StringBuilder();
				byte[] buffer = new byte[255];
				
				try {
					//Read the data from the buffer
					while(is.read(buffer) > 0) {
						sb.append(buffer);
					}
					
					//Send the message to the routing protocol
					routing.receiveMessage(sb.toString().getBytes());
				} catch (IOException e) {
					Log.e("Scatterfi", e.getMessage());
					break;
				}
			}
		}
	}
}
