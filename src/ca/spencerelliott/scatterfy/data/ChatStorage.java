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
		store = new PersistentStorage(context, StorageType.CHAT, date);
	}
	
	public void saveMessage(ChatMessage msg) {
		
	}
	
	public void cleanup() {
		store.cleanup();
	}
	
	public ArrayList<HashMap<String,String>> getMessages() {
		return messages;
	}
}
