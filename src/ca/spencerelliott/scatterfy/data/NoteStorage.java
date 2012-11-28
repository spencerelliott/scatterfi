package ca.spencerelliott.scatterfy.data;

import java.util.Date;

import ca.spencerelliott.scatterfy.data.PersistentStorage.StorageType;

import android.content.Context;

public class NoteStorage {
	private PersistentStorage store;
	
	public NoteStorage(Context context, Date date) {
		//Create the persistent storage
		store = new PersistentStorage(context, StorageType.NOTE, date);
	}
	
	public void saveNote(Note note) {
		//Store the note in the storage
		store.store(note.NOTE + "\n\n");
	}
	
	public void cleanup() {
		//Cleanup the persistent store
		store.cleanup();
	}
}
