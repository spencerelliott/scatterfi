package ca.spencerelliott.scatterfy.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import ca.spencerelliott.scatterfy.data.PersistentStorage.StorageType;

import android.content.Context;

public class ChatStorage {
	private ArrayList<HashMap<String,String>> messages = new ArrayList<HashMap<String,String>>();
	private PersistentStorage store;
	
	public ChatStorage(Context context, Date date) {
		//Create the persistent store to write to
		store = new PersistentStorage(context, StorageType.CHAT, date);
	}
	
	public void saveMessage(ChatMessage msg) {
		//Get the information from the passed message and add it to the entire list
		HashMap<String,String> newMessage = new HashMap<String,String>();
		newMessage.put("from", msg.FROM);
		newMessage.put("message", msg.MESSAGE);
		
		messages.add(newMessage);
		
		//Store the chat message in the persistent storage
		store.store("<" + msg.FROM + ">:" + msg.MESSAGE + "\n");
	}
	
	public void cleanup() {
		//Clean up and write the message
		store.cleanup();
	}
	
	public ArrayList<String> getMessages() {
		ArrayList<String> formattedMessages = new ArrayList<String>();
		
		//Format all the messages
		for(HashMap<String,String> h : messages) {
			formattedMessages.add(h.get("from") + ":::" + h.get("message"));
		}
		
		//Return the list of chat strings
		return formattedMessages;
	}
}
