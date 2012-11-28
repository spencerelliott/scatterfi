package ca.spencerelliott.scatterfy;

import java.util.ArrayList;
import java.util.HashMap;

import ca.spencerelliott.scatterfy.messages.MessageIntent;
import ca.spencerelliott.scatterfy.services.BluetoothSettings;
import ca.spencerelliott.scatterfy.services.ClientService;
import ca.spencerelliott.scatterfy.services.IBluetoothServerService;
import ca.spencerelliott.scatterfy.services.MessengerCallback;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ClientActivity extends Activity {
private TextView status = null;
	
	private IBluetoothServerService service = null;
	private String intentConnectionMac = null;
	
	private ProgressDialog progressDialog = null;
	
	private static final int OPEN_CONNECTING_DIALOG = 0;
	private static final int CLOSE_CONNECTING_DIALOG = 1;
	private static final int UPDATE_CONNECTING_DIALOG = 2;
	
	private ArrayList<HashMap<String,String>> chatList = new ArrayList<HashMap<String,String>>();
	private SimpleAdapter chatAdapter = null;
	
	private Handler dialogHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case OPEN_CONNECTING_DIALOG:
					//Show the progress dialog
					progressDialog = new ProgressDialog(ClientActivity.this);
					progressDialog.setIndeterminate(true);
					progressDialog.setCancelable(false);
					
					progressDialog.setMessage(getResources().getText(R.string.connecting));
					
					progressDialog.show();
					
					ConnectionThread thread = new ConnectionThread();
					thread.execute((String)msg.obj);
					break;
				case CLOSE_CONNECTING_DIALOG:
					if(progressDialog != null) progressDialog.dismiss();
					break;
				case UPDATE_CONNECTING_DIALOG:
					if(progressDialog != null) {
						progressDialog.setMessage((String)msg.obj);
					}
					break;
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.activity_client);
		
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			Intent intent = getIntent();
	        
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			
			Log.i("Scatterfi", "Received NFC message: " + intent.getDataString() + " [" + tag.describeContents() + "]");
			intentConnectionMac = intent.getData().getQueryParameter("mac");
	    }
		
		chatAdapter = new SimpleAdapter(this, chatList, R.layout.list_chat_row, new String[] { "from", "message" }, new int[] { R.id.user_addr, R.id.chat_message });
		
		Intent serverService = new Intent(this, ClientService.class);
		startService(serverService);
		
		status = (TextView)findViewById(R.id.status);
		
		final EditText chat = (EditText)findViewById(R.id.chat_message);
		chat.setText(BluetoothSettings.MY_BT_ADDR);
		
		Button send = (Button)findViewById(R.id.send_button);
		send.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if(service != null && !chat.getText().toString().trim().isEmpty()) {
					//Uri uri = Uri.parse(chat.getText().toString());
					//Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					
					Intent intent = new Intent(MessageIntent.CHAT_MESSAGE);
					intent.putExtra("message", chat.getText().toString());
					
					try {
						service.sendMessage("00:00:00:00:00:00", intent);
					} catch (RemoteException e) {
						
					}
					
					HashMap<String,String> newMessage = new HashMap<String,String>();
					
					newMessage.put("from", BluetoothSettings.MY_BT_ADDR);
					newMessage.put("message", chat.getText().toString());
					
					chatList.add(newMessage);
					chatAdapter.notifyDataSetChanged();
					
					chat.setText("");
				}
			}
		});
		
		ListView chatMessages = (ListView)findViewById(R.id.chat_list);
		chatMessages.setAdapter(chatAdapter);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		bindService(new Intent(this, ClientService.class), connection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onStop() {
		if(service != null) {
			//Make sure to remove the callback when exiting the application
			try {
				service.unregisterCallback(callback);
			} catch (RemoteException e) {
				
			}
		}
		
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
		LayoutInflater inflater = LayoutInflater.from(this);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.menu_disconnect:
	        	stopService(new Intent(this, ClientService.class));
	        	finish();
	        	break;
	        case R.id.menu_connect:
				try {
					if(!service.isConnected()) {		        		
		        		LinearLayout dialogInflated = (LinearLayout)inflater.inflate(R.layout.connect_dialog, null);
		        		final TextView deviceText = (TextView)dialogInflated.findViewById(R.id.device_mac);
		        		
		        		AlertDialog dialog = builder.setView(dialogInflated)
		        			.setTitle(R.string.connect)
		        			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									String mac = deviceText.getText().toString();
									
									Message msg = new Message();
									msg.what = OPEN_CONNECTING_DIALOG;
									msg.obj = mac;
									
									dialogHandler.sendMessage(msg);
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
	        case R.id.menu_notes:        		
        		LinearLayout dialogInflated = (LinearLayout)inflater.inflate(R.layout.note_dialog, null);
        		final TextView noteText = (TextView)dialogInflated.findViewById(R.id.note_text);
        		final Spinner userSpinner = (Spinner)dialogInflated.findViewById(R.id.user_select);
        		
        		ArrayList<HashMap<String,String>> clientList = new ArrayList<HashMap<String,String>>();
        		
        		HashMap<String,String> broadcastUser = new HashMap<String,String>();
        		broadcastUser.put("mac", "00:00:00:00:00:00");
        		
        		clientList.add(broadcastUser);
        		
        		SimpleAdapter adapter = new SimpleAdapter(ClientActivity.this, clientList, R.layout.spinner_row, new String[] { "mac" }, new int[] { R.id.spinner_text });
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
							
							Toast.makeText(ClientActivity.this, "Sending note to " + selectedUser, Toast.LENGTH_SHORT).show();
							
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
	
	private MessengerCallback.Stub callback = new MessengerCallback.Stub() {
		@Override
		public void update(String message) throws RemoteException {
			if(progressDialog != null) {
				Message msg = new Message();
				msg.what = UPDATE_CONNECTING_DIALOG;
				msg.obj = message;
				
				dialogHandler.sendMessage(msg);
			}
		}

		@Override
		public void newMessage(String from, String message) throws RemoteException {
			HashMap<String,String> newMessage = new HashMap<String,String>();
			
			newMessage.put("from", from);
			newMessage.put("message", message);
			
			chatList.add(newMessage);
			chatAdapter.notifyDataSetChanged();
		}
	};
	
	private class ConnectionThread extends AsyncTask<String, Void, Void> {		
		@Override
		protected Void doInBackground(String... params) {			
			if(service != null) {
				try {
					service.registerUser(params[0], callback);
				} catch (RemoteException e) {
					Log.i("Scatterfi", "Could not register device: " + e.getMessage());
				}
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			dialogHandler.sendEmptyMessage(CLOSE_CONNECTING_DIALOG);
	    }
		
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
			
			try {
				service.registerCallback(callback);
			} catch (RemoteException e1) {
				
			}
			
			//Connect to the MAC address passed by the intent
			if(intentConnectionMac != null) {
				Message msg = new Message();
				msg.what = OPEN_CONNECTING_DIALOG;
				msg.obj = intentConnectionMac;
				
				dialogHandler.sendMessage(msg);
				
				try {
					service.registerUser(intentConnectionMac, callback);
				} catch (RemoteException e) {
					Log.e("Scatterfi", "Failed to register on NFC connection");
				}
				
				intentConnectionMac = null;
			}
			
			try {
				//Get all previous chat messages
				ArrayList<String> previousChat = (ArrayList<String>)service.getChatMessages();
				
				if(previousChat != null) {
					for(String s : previousChat) {
						HashMap<String,String> decodedMessage = new HashMap<String,String>();
						String[] splitMessage = s.split(":::");
						
						decodedMessage.put("from", splitMessage[0]);
						decodedMessage.put("message", splitMessage[1]);
						
						//Add the message to the list
						chatList.add(decodedMessage);
					}
					
					//Tell the adapter to update
					chatAdapter.notifyDataSetChanged();
				}
			} catch (RemoteException e) {
				
			}
			
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
