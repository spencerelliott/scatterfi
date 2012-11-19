package ca.spencerelliott.scatterfy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class ViewerActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_viewer);
		
		File dir = new File(this.getFilesDir() + "/logs");
		String[] meetingList = new String[] {};
		
		if(dir.exists()) {
			meetingList = dir.list();
		}
		
		ArrayList<HashMap<String,String>> listOptions = new ArrayList<HashMap<String,String>>();
		
		for(String s : meetingList) {
			HashMap<String,String> dateMap = new HashMap<String,String>();
			dateMap.put("date", s);
			
			listOptions.add(dateMap);
		}
		
		if(listOptions.size() > 0) {
			SimpleAdapter adapter = new SimpleAdapter(this, listOptions, R.layout.spinner_row, new String[] { "date" }, new int[] { R.id.spinner_text });
			
			ListView logList = (ListView)findViewById(R.id.viewer_list);
			logList.setAdapter(adapter);
		}
	}
}
