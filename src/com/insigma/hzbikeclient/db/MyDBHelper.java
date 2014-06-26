package com.insigma.hzbikeclient.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDBHelper extends SQLiteOpenHelper {
	private static final String DB_NAME = "hzbike.sqlite";
	private static final int START_VERSION = 1;
	public MyDBHelper(Context context) {
		super(context, DB_NAME, null, START_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

}
