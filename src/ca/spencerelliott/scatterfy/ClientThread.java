package ca.spencerelliott.scatterfy;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class ClientThread extends AsyncTask<String, Void, Void> {
	private Context context = null;
	
	public ClientThread(Context context) {
		this.context = context;
	}

	@Override
	protected Void doInBackground(String... params) {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		//Needs at least two parameters (MAC, UUID)
		if(params.length <= 1) {
			return null;
		}
		
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(params[0]);
		
		try {
			BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(params[1]));
			
			socket.connect();
			OutputStream os = socket.getOutputStream();
			
			os.write(new byte[] {'h', 'i', 'y', 'o'});
			
			os.close();
			socket.close();
		} catch (IOException e) {
			Log.e("Scatterfy", e.getMessage());
		} 
		return null;
	}

}
