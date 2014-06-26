package com.insigma.hzbikeclient.db;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.insigma.hzbikeclient.bean.Station;

public class StationDAO {
	private 	MyDBHelper dbHelper; 
	public StationDAO(Context context){
		this.dbHelper = new MyDBHelper(context);
	}
	
	/**
	 * 根据上下左右坐标，获取区域内的站点,
	 * @param left 左边界（经度）
	 * @param right 右边界（经度）
	 * @param top 上边界（纬度）
	 * @param bottom （纬度）
	 * @param is24Hour 是否24小时服务站点
	 * @return 站点列表, 如果没有则返回null
	 */
	public ArrayList<Station> queryStationByRegion(double left, double right, double top, double bottom, boolean is24Hour){
		ArrayList<Station> stationList = null;
		
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		
		try {
			String selection = "latitude<=? and latitude>=? and longitude<=? and longitude>=?" + (is24Hour? " and startService=?" : "");
			String[] args =  is24Hour? new String[]{"" + top,""+bottom, ""+right, ""+left, "00:00"}:new String[]{"" + top,""+bottom, ""+right, ""+left};
			Cursor c = db.query("station", null, selection , args, null, null, null);
			while(c.moveToNext()){
				if(stationList == null){
					stationList = new ArrayList<Station>();
				}
				Station station = new Station();
				station.id = c.getInt(c.getColumnIndex("id"));
				station.code = c.getInt(c.getColumnIndex("code"));
				station.name = c.getString(c.getColumnIndex("name"));
				station.latitude = c.getDouble(c.getColumnIndex("latitude"));
				station.longitude = c.getDouble(c.getColumnIndex("longitude"));
				station.canBorrow = c.getInt(c.getColumnIndex("canborrow"));
				station.canReturn = c.getInt(c.getColumnIndex("canreturn"));
				station.total = c.getInt(c.getColumnIndex("total"));
				station.address = c.getString(c.getColumnIndex("address"));
				station.tel = c.getString(c.getColumnIndex("tel"));
				station.startService = c.getString(c.getColumnIndex("startService"));
				station.endService = c.getString(c.getColumnIndex("endService"));
				stationList.add(station);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			db.close();
		}
		
		return stationList;
	}
	
	/**
	 * 根据上下左右坐标，获取区域内的站点
	 * @param left 左边界（经度）
	 * @param right 右边界（经度）
	 * @param top 上边界（纬度）
	 * @param bottom （纬度）
	 * @return 站点列表, 如果没有则返回null
	 */
	public ArrayList<Station> queryStationByRegion(double left, double right, double top, double bottom){
		return queryStationByRegion(left, right, top, bottom, false);
	}
	
	/**
	 * 根据站点编号查询
	 * @param code 站点编号
	 * @return 站点对象,如果没有查到，返回null
	 */
	public Station queryByCode(int code){
		Station station = null;
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		
		try {
			Cursor c = db.query("station", null, "code=?", new String[]{"" + code}, null, null, null);
			if(c.moveToNext()){
				station = new Station();
				station.id = c.getInt(c.getColumnIndex("id"));
				station.code = c.getInt(c.getColumnIndex("code"));
				station.name = c.getString(c.getColumnIndex("name"));
				station.latitude = c.getDouble(c.getColumnIndex("latitude"));
				station.longitude = c.getDouble(c.getColumnIndex("longitude"));
				station.total = c.getInt(c.getColumnIndex("total"));
				station.address = c.getString(c.getColumnIndex("address"));
				station.tel = c.getString(c.getColumnIndex("tel"));
				station.startService = c.getString(c.getColumnIndex("startService"));
				station.endService = c.getString(c.getColumnIndex("endService"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			db.close();
		}
		
		return station;
	}
	
	
}
