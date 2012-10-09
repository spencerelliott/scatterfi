package ca.spencerelliott.scatterfy;

import ca.spencerelliott.scatterfy.services.BluetoothSettings;
import ca.spencerelliott.scatterfy.services.ClientService;
import ca.spencerelliott.scatterfy.services.IBluetoothServerService;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ClientActivity extends Activity {
private TextView status = null;
	
	private IBluetoothServerService service = null;
	
	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_client);
		
		Intent serverService = new Intent(this, ClientService.class);
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
						service.sendMessage("00:00:00:00:00:00", intent);
					} catch (RemoteException e) {
						
					}
				}
			}
		});
	}
	
	@Override
	public void onStart() {
		super.onStart();
		bindService(new Intent(this, ClientService.class), connection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onStop() {
		unbindService(connection);
		super.onStop();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_client, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.menu_disconnect:
	        	stopService(new Intent(this, ClientService.class));
	        	finish();
	        	break;
	        case R.id.menu_connect:
				try {
					if(!service.isConnected()) {
		        		LayoutInflater inflater = LayoutInflater.from(this);
		        		
		        		LinearLayout dialogInflated = (LinearLayout)inflater.inflate(R.layout.connect_dialog, null);
		        		final TextView deviceText = (TextView)dialogInflated.findViewById(R.id.device_mac);
		        		
		        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        		AlertDialog dialog = builder.setView(dialogInflated)
		        			.setTitle(R.string.connect)
		        			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									String mac = deviceText.getText().toString();
									
									if(service != null) {
										try {
											service.registerUser(mac);
										} catch (RemoteException e) {
											Log.i("Scatterfi", "Could not register device: " + e.getMessage());
										}
									}
								}
		        			})
		        			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							})
							.create();
		        		
		        		dialog.show();
		        			
		        	}
				} catch (RemoteException e) {
					
				}
	        	break;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	    
	    return true;
	}
	
	private ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder binder) {
			ClientActivity.this.runOnUiThread(new Runnable() {
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
			ClientActivity.this.runOnUiThread(new Runnable() {
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
