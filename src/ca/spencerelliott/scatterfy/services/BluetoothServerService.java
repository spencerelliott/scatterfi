package ca.spencerelliott.scatterfy.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ca.spencerelliott.scatterfy.routing.IRoutingProtocol;
import ca.spencerelliott.scatterfy.routing.RingOfMastersRoutingProtocol;

import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class BluetoothServerService extends Service {
	private ArrayList<String> connectedClients = new ArrayList<String>();
	private HashMap<BluetoothDevice, BluetoothSocket> connectedDevices = new HashMap<BluetoothDevice, BluetoothSocket>();
	
	private IRoutingProtocol protocol = new RingOfMastersRoutingProtocol(getBaseContext());
	
	private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	
	protected NotificationManager nm = null;
	
	@Override
	public void onCreate() {
		nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return stub;
	}
	
	private IBluetoothServerService.Stub stub = new IBluetoothServerService.Stub() {
		@Override
		public void sendMessage(String address, Intent intent) throws RemoteException {			
			protocol.sendMessage(address, intent);
		}
		
		@Override
		public void removeUser(String mac) throws RemoteException {
			if(connectedClients.remove(mac)) {
				BluetoothDevice rmDevice = null;
				
				for(BluetoothDevice d : connectedDevices.keySet()) {
					if(d.getAddress().equals(mac)) {
						rmDevice = d;
						break;
					}
				}
				
				if(rmDevice != null) {
					BluetoothSocket socket = connectedDevices.get(rmDevice);
					
					//Make sure the socket is closed
					try {
						socket.close();
					} catch (IOException e) {
						
					}
					
					connectedDevices.remove(rmDevice);
				}
			}
		}
		
		@Override
		public boolean registerUser(String mac) throws RemoteException {
			try {
				//Add the mac address to the list
				connectedClients.add(mac);
				
				//Create the remote device
				BluetoothDevice device = adapter.getRemoteDevice(mac);
				BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(BluetoothSettings.BT_UUID);
				
				connectedDevices.put(device, socket);
			} catch (IOException e) {
				
			}
			
			return true;
		}
		
		@Override
		public boolean isConnected() throws RemoteException {
			return false;
		}
		
		@Override
		public List<String> getConnectedClients() throws RemoteException {
			return null;
		}
	};

}
