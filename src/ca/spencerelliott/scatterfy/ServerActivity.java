package ca.spencerelliott.scatterfy;

import java.nio.charset.Charset;

import ca.spencerelliott.scatterfy.services.BluetoothSettings;
import ca.spencerelliott.scatterfy.services.IBluetoothServerService;
import ca.spencerelliott.scatterfy.services.ServerService;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ServerActivity extends Activity {
	private TextView status = null;
	
	private IBluetoothServerService service = null;
	private NfcAdapter nfcAdapter = null;
	
	private NdefMessage nfcMessage = null;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_server);
		
		Intent serverService = new Intent(this, ServerService.class);
		startService(serverService);
		
		status = (TextView)findViewById(R.id.status);
		
		final EditText chat = (EditText)findViewById(R.id.chat_message);
		
		Button send = (Button)findViewById(R.id.send_button);
		send.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if(service != null) {
					Uri uri = Uri.parse(chat.getText().toString());
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					
					try {
						//service.sendMessage(BluetoothSettings.MY_BT_ADDR, intent);
						service.sendMessage("00:00:00:00:00:00", intent);
					} catch (RemoteException e) {
						
					}
				}
			}
		});
		
		//Set up NFC, if available
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		
		if(nfcAdapter != null) {
			//Create the intent to send over NFC
			String uri = "scatterfi://client?mac=" + BluetoothSettings.MY_BT_ADDR;
			
			NdefRecord uriRecord = new NdefRecord(
				    NdefRecord.TNF_ABSOLUTE_URI ,
				    uri.getBytes(Charset.forName("US-ASCII")),
				    new byte[0], new byte[0]);
			
			//Create the message to be sent
			nfcMessage = new NdefMessage(new NdefRecord[] { uriRecord });
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onResume() {
		if(nfcAdapter != null && nfcMessage != null) {
			//Set the NFC adapter to use our message
			nfcAdapter.enableForegroundNdefPush(this, nfcMessage);
		}
		
		super.onResume();
	}
	
	@SuppressWarnings("deprecation")
	public void onPause() {
		if(nfcAdapter != null) {
			//Disable the NFC message from the adapter
			nfcAdapter.disableForegroundNdefPush(this);
		}
		
		super.onPause();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		bindService(new Intent(this, ServerService.class), connection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onStop() {
		unbindService(connection);
		super.onStop();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_server, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.menu_disconnect:
	        	stopService(new Intent(this, ServerService.class));
	        	break;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	    
	    finish();
	    
	    return true;
	}
	
	private ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder binder) {
			ServerActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					status.setText("Connected to service (MAC " + BluetoothSettings.MY_BT_ADDR + ")");
					status.invalidate();
				}
			});
			
			service = IBluetoothServerService.Stub.asInterface(binder);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			ServerActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					status.setText("Not connected (MAC " + BluetoothSettings.MY_BT_ADDR + ")");
					status.invalidate();
				}
			});
			
			service = null;
		}
		
	};
}
