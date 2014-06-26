package com.insigma.hzbikeclient.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.widget.ListView;

import com.insigma.hzbikeclient.R;
import com.insigma.hzbikeclient.R.layout;
import com.insigma.hzbikeclient.R.menu;
import com.insigma.hzbikeclient.util.Tools;

public class StartActivity extends Activity {
	private final int SPLASH_DISPLAY_LENGHT = 3000; // 延迟时间
	
	private String confFileName = "sysConf";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
		
		SharedPreferences sp = getSharedPreferences(confFileName,MODE_PRIVATE);
		String isFirstTime = sp.getString("isFirstTime", null);

		if(isFirstTime == null){
			//拷贝数据库文件
			Tools.initDB(this);
			//设置不是第一次运行
			Editor editor = sp.edit();
			editor.putString("isFirstTime", "yes");
			editor.commit();
		}
		
		
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				Intent mainIntent = new Intent(StartActivity.this,MainActivity.class);
				startActivity(mainIntent);
				StartActivity.this.finish();
			}

		}, SPLASH_DISPLAY_LENGHT);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is
		// present.
		getMenuInflater().inflate(R.menu.start, menu);
		return true;
	}

}
