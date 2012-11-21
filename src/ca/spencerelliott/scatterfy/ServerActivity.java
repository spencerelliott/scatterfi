package ca.spencerelliott.scatterfy;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

import ca.spencerelliott.scatterfy.messages.MessageIntent;
import ca.spencerelliott.scatterfy.services.BluetoothSettings;
import ca.spencerelliott.scatterfy.services.IBluetoothServerService;
import ca.spencerelliott.scatterfy.services.MessengerCallback;
import ca.spencerelliott.scatterfy.services.ServerService;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
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
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ServerActivity extends Activity {
	private TextView status = null;
	
	private IBluetoothServerService service = null;
	private NfcAdapter nfcAdapter = null;
	
	private NdefMessage nfcMessage = null;
	
	private ArrayList<HashMap<String,String>> chatList = new ArrayList<HashMap<String,String>>();
	private SimpleAdapter chatAdapter = null;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_server);
		
		Intent serverService = new Intent(this, ServerService.class);
		startService(serverService);
		
		chatAdapter = new SimpleAdapter(this, chatList, R.layout.list_chat_row, new String[] { "from", "message" }, new int[] { R.id.user_addr, R.id.chat_message });
		
		status = (TextView)findViewById(R.id.status);
		
		final EditText chat = (EditText)findViewById(R.id.chat_message);
		chat.setText(BluetoothSettings.MY_BT_ADDR);
		
		Button send = (Button)findViewById(R.id.send_button);
		send.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if(service != null) {
					//Uri uri = Uri.parse(chat.getText().toString());
					//Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					
					Intent intent = new Intent(MessageIntent.CHAT_MESSAGE);
					intent.putExtra("message", chat.getText().toString());
					
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
		
		ListView chatMessages = (ListView)findViewById(R.id.chat_list);
		chatMessages.setAdapter(chatAdapter);
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
		LayoutInflater inflater = LayoutInflater.from(this);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.menu_disconnect:
	        	stopService(new Intent(this, ServerService.class));
	        	finish();
	        	break;
	        case R.id.menu_network_map:
	        	displayNetworkMap();
	        	break;
	        case R.id.menu_notes:        		
        		LinearLayout dialogInflated = (LinearLayout)inflater.inflate(R.layout.note_dialog, null);
        		final TextView noteText = (TextView)dialogInflated.findViewById(R.id.note_text);
        		final Spinner userSpinner = (Spinner)dialogInflated.findViewById(R.id.user_select);
        		
        		ArrayList<HashMap<String,String>> clientList = new ArrayList<HashMap<String,String>>();
        		
        		HashMap<String,String> broadcastUser = new HashMap<String,String>();
        		broadcastUser.put("mac", "00:00:00:00:00:00");
        		
        		clientList.add(broadcastUser);
        		
        		SimpleAdapter adapter = new SimpleAdapter(ServerActivity.this, clientList, R.layout.spinner_row, new String[] { "mac" }, new int[] { R.id.spinner_text });
        		userSpinner.setAdapter(adapter);
        		
        		AlertDialog dialog = builder.setView(dialogInflated)
        			.setTitle(R.string.note_hint)
        			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String note = noteText.getText().toString();
							@SuppressWarnings("unchecked")
							String selectedUser = ((HashMap<String,String>)userSpinner.getSelectedItem()).get("mac");
							
							Intent intent = new Intent(MessageIntent.NOTE_MESSAGE);
							intent.putExtra("note", note);
							
							Toast.makeText(ServerActivity.this, "Sending note to " + selectedUser, Toast.LENGTH_SHORT).show();
							
							try {
								service.sendMessage(selectedUser, intent);
							} catch (RemoteException e) {
								
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
	        	break;
	        case R.id.menu_send:        		
        		LinearLayout intentInflated = (LinearLayout)inflater.inflate(R.layout.intent_dialog, null);
        		final TextView intentText = (TextView)intentInflated.findViewById(R.id.intent_text);
        		
        		AlertDialog intentDialog = builder.setView(intentInflated)
        			.setTitle(R.string.send)
        			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Uri uri = Uri.parse(intentText.getText().toString());
							Intent intent = new Intent(Intent.ACTION_VIEW, uri);
							
							try {
								service.sendMessage("00:00:00:00:00:00", intent);
							} catch (RemoteException e) {
								
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
        		
        		intentDialog.show();
	        	break;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	    
	    return true;
	}
	
	private void displayNetworkMap() {
		ScrollView scroller = new ScrollView(this);
		
		LinearLayout dialogInflated = new LinearLayout(this);
		dialogInflated.setGravity(LinearLayout.VERTICAL);
		
		try {
			ArrayList<String> msNodes = (ArrayList<String>) service.getMasterSlaves();
			
			//Loop through each master/slave
			for(String s : msNodes) {
				TextView view = new TextView(this);
				view.setText(s);
				
				//Add the master/slave to the layout
				dialogInflated.addView(view);
				
				ArrayList<String> sNodes = (ArrayList<String>) service.getSlaves(s);
				
				//Loop through each client attached to the master/slave
				for(String is : sNodes) {
					TextView innerView = new TextView(this);
					innerView.setText("--- " + is);
					
					dialogInflated.addView(innerView);
				}
			}
		} catch (RemoteException e) {
			Log.e("Scatterfi", "Error getting network map: " + e.getMessage());
		}
		
		scroller.addView(dialogInflated);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		AlertDialog dialog = builder.setView(scroller)
			.setTitle(R.string.network_map)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.create();
		
		dialog.show();
	}
	
	private MessengerCallback.Stub callback = new MessengerCallback.Stub() {
		@Override
		public void update(String message) throws RemoteException {
			
		}

		@Override
		public void newMessage(String from, String message) throws RemoteException {
			Log.i("Scatterfi", "Chat message [" + from + ": " + message + "]");
			
			HashMap<String,String> newMessage = new HashMap<String,String>();
			
			newMessage.put("from", from);
			newMessage.put("message", message);
			
			chatList.add(newMessage);
			chatAdapter.notifyDataSetChanged();
		}
	};
	
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
