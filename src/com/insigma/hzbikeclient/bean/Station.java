package com.insigma.hzbikeclient.bean;

import java.sql.Timestamp;

public class Station {
	public int id;
	public int code;
	public String name;
	public double latitude;
	public double longitude;
	public int canBorrow;
	public int canReturn;
	public int total;
	public String address;
	public String tel;
	public String startService;
	public String endService;
	public Timestamp updateTime;
}
