package ca.spencerelliott.scatterfy.messages;

public final class MessageIntent {
	//---- Connection intents ----
	/** Sent to a device to ask them to pair with another device */
	public final static String CONNECT = "ca.spencerelliott.scatterfi.intent.connect_to";
	/** Sent to a master/slave to notify them of an incoming connection from a device to become a slave */
	public final static String INCOMING_SLAVE = "ca.spencerelliott.scatterfi.intent.incoming_slave";
	/** Sent to a master/slave to notify them of an incoming connection from a device to become a new master/slave (<code>next</code>) */
	public final static String INCOMING_MASTER_SLAVE = "ca.spencerelliott.scatterfi.intent.incoming_ms";
	
	//---- Disconnection intents ----
	
	/** Sent to the server from a master/slave when a slave or the next master/slave in succession disconnects */
	public final static String LOST_CONNECTION = "ca.spencerelliott.scatterfi.intent.lost_connection";
	
	//---- Messaging intents ----
	
	/** Sent by a device to indicate the message contains text for the built-in chat */
	public final static String CHAT_MESSAGE = "ca.spencerelliott.scatterfi.intent.chat_message";
	/** Send by a device to indicate the message contains values to keep track of information (e.g. deadlines, new work) */
	public final static String NOTE_MESSAGE = "ca.spencerelliott.scatterfi.intent.note_message";
	
	//---- Miscellaneous intents ----
	
	/** Sent from one device to another to get information such as the users name */
	public final static String DISCOVERY = "ca.spencerelliott.scatterfi.intent.discovery";
}