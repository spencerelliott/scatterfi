package ca.spencerelliott.scatterfy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class ViewerActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_viewer);
		
		//Create the path to the logs folder
		File dir = new File(this.getFilesDir() + "/logs");
		String[] meetingList = new String[] {};
		
		//Retrieve the list of folders for logs
		if(dir.exists()) {
			meetingList = dir.list();
		}
		
		//Build the list to store the folders
		final ArrayList<HashMap<String,String>> listOptions = new ArrayList<HashMap<String,String>>();
		
		//Create the list of folders available
		for(String s : meetingList) {
			HashMap<String,String> dateMap = new HashMap<String,String>();
			dateMap.put("date", s);
			
			listOptions.add(dateMap);
		}
		
		//Make sure the folders exist
		if(listOptions.size() > 0) {
			//Create the adapter for the list view with all folders in it
			SimpleAdapter adapter = new SimpleAdapter(this, listOptions, R.layout.spinner_row, new String[] { "date" }, new int[] { R.id.spinner_text });
			
			//Set the adapter on the list view
			ListView logList = (ListView)findViewById(R.id.viewer_list);
			logList.setAdapter(adapter);
			
			logList.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> adapter, View clickedView, int position, long id) {
					//Create the intent to load the text viewing activity with the path to the file to open
					Intent intent = new Intent(ViewerActivity.this, TextViewerActivity.class);
					intent.putExtra("filename", ViewerActivity.this.getFilesDir() + "/logs/" + (String)listOptions.get(position).get("date") + "chat.txt");
					
					startActivity(intent);
				}
			});
		}
	}
}
