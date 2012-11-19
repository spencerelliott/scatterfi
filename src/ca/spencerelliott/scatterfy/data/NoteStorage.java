package ca.spencerelliott.scatterfy.data;

import java.io.File;
import java.util.Date;

import ca.spencerelliott.scatterfy.data.PersistentStorage.StorageType;

import android.content.Context;

public class NoteStorage {
	private PersistentStorage store;
	
	public NoteStorage(Context context, Date date) {
		store = new PersistentStorage(context, StorageType.NOTE, date);
	}
	
	public void saveNote(Note note) {
		
	}
	
	public void cleanup() {
		store.cleanup();
	}
}
