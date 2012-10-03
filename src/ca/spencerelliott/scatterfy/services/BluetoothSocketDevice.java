package ca.spencerelliott.scatterfy.services;

import java.io.IOException;
import java.io.OutputStream;

import ca.spencerelliott.scatterfy.routing.IRoutingProtocol;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothSocketDevice {
	private BluetoothDevice device;
	private BluetoothSocket socket;
	
	private IRoutingProtocol routing;
	
	public BluetoothSocketDevice(BluetoothDevice device, BluetoothSocket socket) {
		this.device = device;
		this.socket = socket;
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
	
	private class BluetoothMessageReceiverThread extends Thread {
		@Override
		public void run() {
			//TODO Write code to listen for incoming messages on a Bluetooth socket
		}
	}
}
