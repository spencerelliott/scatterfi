package ca.spencerelliott.scatterfy;

import java.util.ArrayList;
import java.util.HashMap;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class MainActivity extends Activity {
	private static final int REQUEST_ENABLE_BT = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
        
        //Send an intent to enable Bluetooth if it is not enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        
        ArrayList<HashMap<String,String>> adapterList = new ArrayList<HashMap<String,String>>();
        
        HashMap<String,String> option = new HashMap<String,String>();
        option.put("title", "Start a server");
        
        adapterList.add(option);
        
        option = new HashMap<String,String>();
        option.put("title", "Join a server");
        
        adapterList.add(option);
        
        option = new HashMap<String,String>();
        option.put("title", "View previous notes/chat logs");
        
        adapterList.add(option);
        
        SimpleAdapter adapter = new SimpleAdapter(this, adapterList, R.layout.option_listitem, new String[] { "title" }, new int[] { R.id.itemtext });
    
        ListView optionList = (ListView)findViewById(R.id.launch_list);
        optionList.setAdapter(adapter);
        
        optionList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				switch(position) {
					//Start the server activity
					case 0:
						Intent serverIntent = new Intent(MainActivity.this, ServerActivity.class);
						startActivity(serverIntent);
						break;
					//Start the client activity
					case 1:
						Intent clientIntent = new Intent(MainActivity.this, ClientActivity.class);
						startActivity(clientIntent);
						break;
					//Start the activity to view notes
					case 2:
						Intent viewerIntent = new Intent(MainActivity.this, ViewerActivity.class);
						startActivity(viewerIntent);
						break;
				}
			}
        });
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
