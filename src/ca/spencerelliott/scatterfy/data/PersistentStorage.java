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
		
		//Format the date and create the file path based on the date
		String dateFormat = (calendar.get(Calendar.MONTH) >= 9 ? calendar.get(Calendar.MONTH)+1 : "0" + calendar.get(Calendar.MONTH)+1) + "-" + (calendar.get(Calendar.DAY_OF_MONTH) >= 10 ? calendar.get(Calendar.DAY_OF_MONTH) : "0" + calendar.get(Calendar.DAY_OF_MONTH)) + "-" + calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.HOUR_OF_DAY) >= 10 ? calendar.get(Calendar.HOUR_OF_DAY) : "0" + calendar.get(Calendar.HOUR_OF_DAY)) + ":" + (calendar.get(Calendar.MINUTE) >= 10 ? calendar.get(Calendar.MINUTE) : "0" + calendar.get(Calendar.MINUTE));
		String filePath = context.getFilesDir() + "/logs/" + dateFormat + "/";
		
		//Create the directory
		storeFile = new File(filePath);
		storeFile.mkdirs();
		
		//Determine which file to put the information in
		switch(type) {
			case CHAT:
				filePath += "chat.txt";
				break;
			case NOTE:
				filePath += "notes.txt";
				break;
		}
		
		//Create the file and open the output stream
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
				//Write the message to the file
				stream.write(message.getBytes());
			} catch (IOException e) {
				Log.i("Scatterfi", e.getMessage());
			}
		}
	}
	
	public void cleanup() {
		if(stream != null) {
			try {
				//Flush the stream and close the file
				stream.flush();
				stream.close();
			} catch (IOException e) {
				Log.i("Scatterfi", e.getMessage());
			}
		}
	}
}
