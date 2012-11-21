package ca.spencerelliott.scatterfy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class TextViewerActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_textviewer);
		
		TextView display = (TextView)findViewById(R.id.text_display);
		
		if(getIntent() != null) {
			if(getIntent().hasExtra("filename")) {
				String filename = getIntent().getExtras().getString("filename");
				File file = new File(filename);
				
				try {
					FileInputStream is = new FileInputStream(file);
					
					int count = 0;
					byte[] buffer = new byte[1024];
					
					StringBuilder builder = new StringBuilder();
					
					while((count = is.read(buffer)) > 0) {
						String s = new String(buffer, 0, count);
						builder.append(s);
					}
					
					is.close();
					
					display.setText(builder.toString());
				} catch (FileNotFoundException e) {
					
				} catch (IOException e) {
					
				}
			}
		}
	}
}
