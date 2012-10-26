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
	
	/**
	 * Sets the routing protocol callback for receiving messages
	 * @param routing The routing protocol to be called when receiving messages
	 */
	public void setRoutingProtocol(IRoutingProtocol routing) {
		this.routing = routing;
	}
	
	/**
	 * Writes a message to the output stream of the Bluetooth socket
	 * @param message The message to write to the stream
	 */
	public void writeMessage(byte[] message) {
		try {
			OutputStream os = socket.getOutputStream();
			os.write(message);
		} catch (IOException e) {
			Log.e("Scatterfi", "Error sending message to client: " + e.getMessage());
		}
		
	}
	
	/**
	 * Retrieves the address of this Bluetooth device
	 * @return The address of the Bluetooth device
	 */
	public String getAddress() {
		return device.getAddress();
	}
	
	/**
	 * Retrieves the actual Bluetooth device 
	 * @return The Bluetooth device associated with this object
	 */
	public BluetoothDevice getDevice() {
		return device;
	}
	
	/**
	 * Retrieves the actual socket of the Bluetooth device
	 * @return The Bluetooth socket associated with this object
	 */
	public BluetoothSocket getSocket() {
		return socket;
	}
	
	/**
	 * Closes the socket connected to this Bluetooth device
	 */
	public void cleanup() {
		try {
			socket.getInputStream().close();
		} catch(IOException e1) {
			Log.e("Scatterfi", "Could not close input stream: " + e1.getMessage());
		}
		
		try {
			socket.getOutputStream().close();
		} catch (IOException e1) {
			Log.e("Scatterfi", "Could not close output stream: " + e1.getMessage());
		}
		
		try {
			socket.close();
		} catch (IOException e1) {
			Log.e("Scatterfi", "Could not close socket: " + e1.getMessage());
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
					Log.e("Scatterfi", "Lost connection in BluetoothSocketDevice: " + e.getMessage());
					
					//cleanup();
					
					//Let the routing protocol know that the connection was lost
					routing.lostConnection(BluetoothSocketDevice.this);
					break;
				}
			}
		}
	}
}
