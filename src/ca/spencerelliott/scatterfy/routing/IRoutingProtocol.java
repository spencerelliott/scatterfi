package ca.spencerelliott.scatterfy.routing;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

import ca.spencerelliott.scatterfy.MainActivity;
import ca.spencerelliott.scatterfy.R;
import ca.spencerelliott.scatterfy.data.ChatMessage;
import ca.spencerelliott.scatterfy.data.ChatStorage;
import ca.spencerelliott.scatterfy.data.Note;
import ca.spencerelliott.scatterfy.data.NoteStorage;
import ca.spencerelliott.scatterfy.messages.MessageIntent;
import ca.spencerelliott.scatterfy.messages.RoutedMessage;
import ca.spencerelliott.scatterfy.services.BluetoothSettings;
import ca.spencerelliott.scatterfy.services.BluetoothSocketDevice;
import ca.spencerelliott.scatterfy.services.DeviceType;
import ca.spencerelliott.scatterfy.services.MessengerCallback;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public abstract class IRoutingProtocol {
	protected Context context = null;
	protected MessengerCallback callback = null;
	
	private ChatStorage chatStore;
	private NoteStorage noteStore;
	
	private NotificationManager nm = null;
	
	public static final int HANDLE_MESSAGE = 0;
	
	protected Handler messageHandler = null;
	
	public IRoutingProtocol(Context context) {
		this.context = context;
		
		Date curDate = new Date();
		
		chatStore = new ChatStorage(context, curDate);
		noteStore = new NoteStorage(context, curDate);
		
		messageHandler = new Handler(context.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what) {
					case HANDLE_MESSAGE:
						receiveMessage((byte[])msg.obj);
						break;
				}
			}
		};
	}
	
	/**
	 * Sends a message to a specific MAC address within the network
	 * @param to The address of the device to receive the message (00:00:00:00:00:00 for broadcast)
	 * @param message The intent to execute on the remote device
	 */
	public abstract void sendMessage(String to, Intent message);
	
	/**
	 * Sends a message as is. Used mostly for resending or forwarding messages.
	 * @param message The message to relay
	 */
	public abstract void sendMessage(RoutedMessage message);
	
	/**
	 * Called when the client receives a message
	 * @param message The data received for the message
	 */
	public synchronized void handleMessage(Message msg) {
		messageHandler.sendMessage(msg);
	}
	
	/**
	 * Process the message within this call
	 * @param message The message received
	 */
	protected abstract void receiveMessage(byte[] message);
	
	/**
	 * Called when a new client is trying to connect to the device
	 * @param device The device that was connected
	 * @param incoming Determines whether this connection was made by the device or it was an incoming connection
	 */
	public abstract void newConnection(BluetoothSocketDevice device, boolean incoming);
	
	/**
	 * Called when a client has disconnected from the client
	 * @param mac The MAC address of the disconnected client
	 */
	public abstract void disconnectClient(String mac);
	
	/**
	 * Called when a client loses connection to the device
	 * @param device The device that lost the connection
	 */
	public abstract void lostConnection(BluetoothSocketDevice device);
	
	/**
	 * Destroys and closes any connections that this this routing protocol has
	 */
	public void destroyAndCleanUp() {
		chatStore.cleanup();
		noteStore.cleanup();
	}
	
	/**
	 * Sets the type of device that this protocol is running on
	 * @param type The <code>DeviceType</code> of the device
	 */
	public abstract void setDeviceType(DeviceType type);
	
	/**
	 * Returns the network map for the the routing protocol
	 * @return The network map
	 */
	public abstract LinkedHashMap<String,ArrayList<String>> getNetworkMap();
	
	/**
	 * Updates the notification in the notification bar to inform the user of
	 * important information
	 * @param message The message to display
	 */
	public void notifyUser(String message) {
		if(nm == null) {
			nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		}
		
		//Create the intent to be executed when the notification is clicked
		Intent notificationIntent = new Intent(context, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		
		//Create the notification
		NotificationCompat.Builder notiBuilder = new NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle("Scatterfi")
			.setContentText(message)
			.setOngoing(true)
			.setContentIntent(contentIntent);
		
		//Display the notification
		nm.notify(BluetoothSettings.NOTIFICATION_ID, notiBuilder.getNotification());
	}
	
	/**
	 * Processes an intent that is sent over the Scatternet. This can be overriden to
	 * handle more intents. Make sure to call <code>super.processIntent()</code> <b>after</b>
	 * your own processing
	 * @param message
	 */
	protected void processIntent(RoutedMessage message) {
		Intent intent = message.getIntent();
		
		if(intent.getAction().equals(MessageIntent.CHAT_MESSAGE)) {
			ChatMessage msg = new ChatMessage();
			
			//Save the chat information in a chat message
			msg.FROM = RoutedMessage.convertByteArrayToAddress(message.getFromAddress());
			msg.DATE = new Date(message.getId());
			msg.MESSAGE = intent.getExtras().getString("message");
			
			//Save the message to the persistent storage
			chatStore.saveMessage(msg);
			
			//Notify the application of a new message if one has provided a callback
			if(callback != null) {
				try {
					callback.newMessage(msg.FROM, msg.MESSAGE);
				} catch (RemoteException e) {
					
				}
			}
			
			Log.i("Scatterfi", "Chat message received" + " [" + intent.getAction() + "]");
		} else if(intent.getAction().equals(MessageIntent.NOTE_MESSAGE)) {
			Note note = new Note();
			
			//Save the note information in a note
			note.TO = RoutedMessage.convertByteArrayToAddress(message.getToAddress());
			note.DATE = new Date(message.getId());
			note.NOTE = intent.getExtras().getString("note");
			
			//Save the note to persistent storage
			noteStore.saveNote(note);
			Log.i("Scatterfi", "Note received" + " [" + intent.getAction() + "]");
		} else if(intent.getAction().equals(MessageIntent.DISCOVERY)) {
			//Discovery currently does not respond
			Log.i("Scatterfi", "Discovery request sent by " + RoutedMessage.convertByteArrayToAddress(message.getFromAddress()) + " [" + intent.getAction() + "]");
		} else {
			try {
				//Let Android handle the intent otherwise
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
			} catch(ActivityNotFoundException e) {
				//Make sure not to crash if the intent cannot be handled
				Log.e("Scatterfi", "Could not launch intent: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Registers a callback that will be used to communicate with the application
	 * @param callback The callback stub to use when communicating
	 */
	public void registerCallback(MessengerCallback callback) {
		this.callback = callback;
	}
	
	/**
	 * Unregisters the callback attached to this protocol
	 * @param callback The callback previously passed in
	 */
	public void unregisterCallback(MessengerCallback callback) {
		this.callback = null;
	}
	
	/**
	 * Retrieves the list of chat messages this protocol has received
	 * @return The list of chat messages received
	 */
	public ArrayList<String> getChatMessages() {
		return chatStore.getMessages();
	}
}
