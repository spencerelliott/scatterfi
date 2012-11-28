package ca.spencerelliott.scatterfy.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	private IRoutingProtocol protocol = null;
	
	private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	
	protected NotificationManager nm = null;
	private DeviceType type = DeviceType.MASTER_SLAVE;
	
	@Override
	public void onCreate() {
		nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		protocol = new RingOfMastersRoutingProtocol(this.getApplicationContext());
		
		protocol.setDeviceType(type);
	}
	
	@Override
	public void onDestroy() {
		nm.cancel(BluetoothSettings.NOTIFICATION_ID);
		
		protocol.destroyAndCleanUp();
		super.onDestroy();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return stub;
	}
	
	/**
	 * Updates the type of device this service is acting on
	 * @param type The type of device this service is acting as
	 */
	protected void updateType(DeviceType type) {
		this.type = type;
		protocol.setDeviceType(type);
	}
	
	private IBluetoothServerService.Stub stub = new IBluetoothServerService.Stub() {
		@Override
		public void sendMessage(String address, Intent intent) throws RemoteException {			
			protocol.sendMessage(address, intent);
		}
		
		@Override
		public void removeUser(String mac) throws RemoteException {
			protocol.disconnectClient(mac);
		}
		
		@Override
		public boolean registerUser(String mac, MessengerCallback callback) throws RemoteException {
			try {
				if(callback != null) callback.update("Clearing connections...");
				((RingOfMastersRoutingProtocol)protocol).clearAllConnections();
				
				if(callback != null) callback.update("Attempting to connect to device...");
				//Create the remote device
				BluetoothDevice device = adapter.getRemoteDevice(mac);
				BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(BluetoothSettings.BT_UUID);
				socket.connect();
				
				BluetoothSocketDevice d = new BluetoothSocketDevice(device, socket);
				d.setRoutingProtocol(protocol);
				
				if(callback != null) callback.update("Adding to network...");
				protocol.newConnection(d, false);
				
				if(callback != null) callback.update("Connected!");
				Thread.sleep(500);
			} catch (IOException e) {
				if(callback != null) callback.update("Failed to connect...");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					
				}
				return false;
			} catch (InterruptedException e) {
				
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

		@Override
		public List<String> getMasterSlaves() throws RemoteException {
			LinkedHashMap<String,ArrayList<String>> map = protocol.getNetworkMap();
			
			return new ArrayList<String>(map.keySet());
		}

		@Override
		public List<String> getSlaves(String msMac) throws RemoteException {
			LinkedHashMap<String,ArrayList<String>> map = protocol.getNetworkMap();
			
			return map.get(msMac);
		}

		@Override
		public void registerCallback(MessengerCallback callback) throws RemoteException {
			protocol.registerCallback(callback);
		}

		@Override
		public void unregisterCallback(MessengerCallback callback) throws RemoteException {
			protocol.unregisterCallback(callback);
		}

		@Override
		public List<String> getChatMessages() throws RemoteException {
			return protocol.getChatMessages();
		}
	};

}
