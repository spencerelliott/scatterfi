package ca.spencerelliott.scatterfy.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.util.Log;

public class PersistentStorage {
	public enum StorageType { CHAT, NOTE };
	private File storeFile;
	private FileOutputStream stream;
	
	public PersistentStorage(Context context, StorageType type, Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		
		String dateFormat = (calendar.get(Calendar.MONTH) >= 9 ? calendar.get(Calendar.MONTH)+1 : "0" + calendar.get(Calendar.MONTH)+1) + "-" + (calendar.get(Calendar.DAY_OF_MONTH) >= 10 ? calendar.get(Calendar.DAY_OF_MONTH) : "0" + calendar.get(Calendar.DAY_OF_MONTH)) + "-" + calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.HOUR_OF_DAY) >= 10 ? calendar.get(Calendar.HOUR_OF_DAY) : "0" + calendar.get(Calendar.HOUR_OF_DAY)) + ":" + (calendar.get(Calendar.MINUTE) >= 10 ? calendar.get(Calendar.MINUTE) : "0" + calendar.get(Calendar.MINUTE));
		String filePath = context.getFilesDir() + "/logs/" + dateFormat + "/";
		
		storeFile = new File(filePath);
		storeFile.mkdirs();
		
		switch(type) {
			case CHAT:
				filePath += "chat.txt";
				break;
			case NOTE:
				filePath += "notes.txt";
				break;
		}
		
		storeFile = new File(filePath);
		
		try {
			storeFile.createNewFile();
			stream = new FileOutputStream(storeFile);
		} catch (FileNotFoundException e) {
			Log.i("Scatterfi", e.getMessage());
		} catch (IOException e) {
			Log.i("Scatterfi", e.getMessage());
		}
	}
	
	public void store(String message) {
		if(stream != null) {
			try {
				stream.write(message.getBytes());
			} catch (IOException e) {
				Log.i("Scatterfi", e.getMessage());
			}
		}
	}
	
	public void cleanup() {
		if(stream != null) {
			try {
				stream.flush();
				stream.close();
			} catch (IOException e) {
				Log.i("Scatterfi", e.getMessage());
			}
		}
	}
}
