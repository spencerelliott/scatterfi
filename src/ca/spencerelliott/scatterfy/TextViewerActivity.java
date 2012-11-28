package ca.spencerelliott.scatterfy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class TextViewerActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_textviewer);
		
		TextView display = (TextView)findViewById(R.id.text_display);
		
		//Make sure an intent was passed and it has a filename
		if(getIntent() != null) {
			if(getIntent().hasExtra("filename")) {
				//Retrieve the file name from the intent
				String filename = getIntent().getExtras().getString("filename");
				File file = new File(filename);
				
				try {
					//Open the file stream
					FileInputStream is = new FileInputStream(file);
					
					int count = 0;
					byte[] buffer = new byte[1024];
					
					StringBuilder builder = new StringBuilder();
					
					//Read all the contents into the string builder
					while((count = is.read(buffer)) > 0) {
						String s = new String(buffer, 0, count);
						builder.append(s);
					}
					
					is.close();
					
					//Display the text to the user
					display.setText(builder.toString());
				} catch (FileNotFoundException e) {
					Log.e("Scatterfi", "Coult not open file: " + e.getMessage());
				} catch (IOException e) {
					Log.e("Scatterfi", "Coult not open file: " + e.getMessage());
				}
			}
		}
	}
}
