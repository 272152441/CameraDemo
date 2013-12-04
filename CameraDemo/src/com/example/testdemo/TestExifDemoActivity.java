package com.example.testdemo;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TestExifDemoActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_main);
		Button btn = (Button) findViewById(R.id.test_button);

		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				File file = new File(Environment.getExternalStorageDirectory()
						.getAbsoluteFile() + "/CameraTest");
				if (!file.exists()) {
					file.mkdir();
				}
				Intent intent = new Intent();
				intent.setClass(TestExifDemoActivity.this, CameraActivity.class);
				Bundle bundle = new Bundle();
				bundle.putString("picdir", file.getAbsolutePath());
				intent.putExtras(bundle);
				TestExifDemoActivity.this.startActivity(intent);
			}
		});
	}
}
