package ca.spencerelliott.scatterfy;


import java.io.IOException;
import java.util.UUID;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.content.Intent;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final int REQUEST_ENABLE_BT = 0;
	
	private static final String BT_UUID = "d6e4a890-01ee-11e2-a21f-0800200c9a66";
	private BluetoothDevice server = null;
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	
	private BluetoothServerSocket serverSock = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        TextView status = (TextView)findViewById(R.id.status);
        
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
        
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        
        if(mBluetoothAdapter.getAddress().equals("10:BF:48:BF:7A:66")) {
        	Toast.makeText(this, "You are the server", 1000).show();
        	
        	ServerThread thread = new ServerThread(this);
        	thread.execute(BT_UUID);
        } else {
        	ClientThread thread = new ClientThread(this);
        	thread.execute("10:BF:48:BF:7A:66", BT_UUID);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
