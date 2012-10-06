package ca.spencerelliott.scatterfy.messages;

import java.net.URISyntaxException;

import ca.spencerelliott.scatterfy.services.BluetoothSettings;

import android.content.Intent;
import android.util.Log;

public class RoutedMessage {
	private int id = 0;
	private byte[] toAddress = null;
	private static byte[] fromAddress = null;
	private Intent message = null;
	
	/**
	 * Creates an empty <code>Routedmessage</code> with a generated id
	 */
	public RoutedMessage() {
		id = (int)(System.currentTimeMillis()/1000);
		
		//The from address only needs to be gathered the first time a message is created
		if(fromAddress == null) {
			fromAddress = convertAddressToByteArray(BluetoothSettings.MY_BT_ADDR);
		}
	}
	
	/**
	 * Creates a new <code>RoutedMessage</code> from a byte array created by <code>getByteMessage()</code>
	 * @param message The previously create message
	 */
	public RoutedMessage(byte[] message) {
		id = (message[0] << 32) | (message[1] << 24) | (message[2] << 16) | (message[3]);
		fromAddress = new byte[] { message[4], message[5], message[6], message[7], message[8], message[9] };
		toAddress = new byte[] { message[10], message[11], message[12], message[13], message[14], message[15] };
		
		StringBuilder builder = new StringBuilder();
		
		for(int i = 16; i < message.length-1; i++) {
			builder.append(message[i]);
		}
		
		try {
			this.message = Intent.parseUri(builder.toString(), 0);
		} catch (URISyntaxException e) {
			Log.e("Scatterfi", e.getMessage());
		}
	}
	
	/**
	 * Creates a new <code>RoutedMessage</code> from an address and intent meant to be sent over Bluetooth
	 * @param address The address of the device the message to intended for
	 * @param intent The intent to execute on that device
	 */
	public RoutedMessage(byte[] address, Intent intent) {
		this();
		
		this.toAddress = address;
		this.message = intent;
	}
	
	/**
	 * Sets the message to be sent
	 * @param intent The intent to use as the message
	 */
	public void setMessage(Intent intent) {
		this.message = intent;
	}
	
	/**
	 * Sets the to address for this message. Set to <code>{ 00, 00, 00, 00, 00, 00 }</code> for a broadcast message
	 * @param address The address this message is meant for
	 */
	public void setToAddress(byte[] address) {
		this.toAddress = address;
	}
	
	/**
	 * Retrieves the id of this message
	 * @return The id of the message
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Retrieves the to address
	 * @return The to address this message contains
	 */
	public byte[] getToAddress() {
		return this.toAddress;
	}
	
	/**
	 * Retrieves the intent attached to this message
	 * @return The <code>Intent</code> to be carried out by the message
	 */
	public Intent getIntent() {
		return this.message;
	}
	
	/**
	 * Creates a byte array to represent this message
	 * @return A byte array containing the id, address and intent message
	 */
	public byte[] getByteMessage() {
		byte[] header = new byte[] {
			(byte)(id >> 32),
			(byte)((id & 0x00FF0000) >> 24),
			(byte)((id & 0x0000FF00) >> 16),
			(byte)(id & 0x000000FF),
			fromAddress[0],
			fromAddress[1],
			fromAddress[2],
			fromAddress[3],
			fromAddress[4],
			fromAddress[5],
			toAddress[0],
			toAddress[1],
			toAddress[2],
			toAddress[3],
			toAddress[4],
			toAddress[5]
		};
		
		byte[] intent = message.toUri(0).getBytes();
		
		byte[] finalMessage = new byte[header.length + intent.length + 1];
		
		for(int i = 0; i < finalMessage.length; i++) {
			finalMessage[i] = i < header.length ? header[i] : intent[i - header.length];
		}
		
		finalMessage[finalMessage.length-1] = '\0';
		
		return finalMessage;
	}
	
	public static byte[] convertAddressToByteArray(String address) {
		String[] splitAddr = address.split(":");
		
		if(splitAddr.length == 6) {
			byte[] byteAddr = new byte[6];
			
			byteAddr[0] = (byte)Integer.parseInt(splitAddr[0], 16);
			byteAddr[1] = (byte)Integer.parseInt(splitAddr[1], 16);
			byteAddr[2] = (byte)Integer.parseInt(splitAddr[2], 16);
			byteAddr[3] = (byte)Integer.parseInt(splitAddr[3], 16);
			byteAddr[4] = (byte)Integer.parseInt(splitAddr[4], 16);
			byteAddr[5] = (byte)Integer.parseInt(splitAddr[5], 16);
			
			return byteAddr;
		}
		
		return null;
	}
	
	public static String convertByteArrayToAddress(byte[] address) {
		StringBuilder hexString = new StringBuilder();
		
		for(int i = 0; i < address.length; i++) {
			String hex = Integer.toHexString(0xFF & address[i]);
			
			if (hex.length() == 1) {
			    hexString.append('0');
			}
			
			hexString.append(hex);
		}
		
		return hexString.toString();
	}
}
