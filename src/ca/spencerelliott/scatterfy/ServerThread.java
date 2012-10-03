package ca.spencerelliott.scatterfy;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class ServerThread extends AsyncTask<Object, String, Void> {
	private Context context = null;
	
	public ServerThread(Context context) {
		this.context = context;
	}

	@Override
	protected Void doInBackground(Object... params) {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		BluetoothServerSocket serverSock = null;
		
		byte[] recvBuffer = new byte[1024];
		
		//Needs at least one parameter (UUID)
		if(params.length <= 0) {
			return null;
		}
		
		try {
			serverSock = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("Scatterfy server", UUID.fromString((String)params[0]));
			
			BluetoothSocket socket = serverSock.accept();
			
			InputStream is = socket.getInputStream();
			
			is.read(recvBuffer);
			
			publishProgress("Received " + new String(recvBuffer) + " from client");
			
			is.close();
			socket.close();
		} catch (IOException e) {
			
		}
		
		return null;
	}
	
	@Override
	public void onProgressUpdate(String... strings) {
		Toast.makeText(context, strings[0], 1000).show();
	}

}
