package com.insigma.hzbikeclient.activity;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.map.ItemizedOverlay;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MKMapViewListener;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.map.OverlayItem;
import com.baidu.mapapi.map.PopupClickListener;
import com.baidu.mapapi.map.PopupOverlay;
import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.insigma.hzbikeclient.R;
import com.insigma.hzbikeclient.bean.Station;
import com.insigma.hzbikeclient.db.StationDAO;
import com.insigma.hzbikeclient.util.Tools;

public class MainActivity extends Activity {
	private LocationClient lc;
	private MyLocationListener listener;
	private GeoPoint currPoint;
	//默认经纬度，定位到武林广场
	private double defaultLongitude = 120.17231789824;
	private double defaultLatitude = 30.275887151966;
	
	private MapFragment mapFragment;//地图fragment
	private ActionBar bar;
	//1:可借; 2:可还; 3:24小时
//	private static final int TYPE_CAN_BORROW = 1;
	private static final String TYPE_NEARBY = "nearby";
	private static final String TYPE_24_HOUR = "24hour";
	
	private BMapManager mapManager;
	private MapView mapView;
	private MapController mapController;
	private int defaultZoomLevel = 16;
	private String key = "xXGAaGXZkPc07xpZcsiYNySS";
	
	private MyTabListener tabListener;
	
	private ArrayList<OverlayItem> overlayItems = new ArrayList<OverlayItem>();//地图上的站点覆盖物列表
	private ArrayList<Station> stationList;//站点数据列表
	private MyOverlay myOverlay;//覆盖物对象
	private MyLocationOverlay locationOverlay;//定位覆盖物
	private boolean is24Hour = false;
	
	private View popView;//点击站点弹出窗
//	private PopupOverlay pop;//弹出窗
	
	private MKMapViewListener mapListener = new MKMapViewListener(){
		@Override
		public void onClickMapPoi(MapPoi arg0) {
		}
		@Override
		public void onGetCurrentMap(Bitmap arg0) {
		}
		@Override
		public void onMapAnimationFinish() {
			System.out.println("MainActivity.mapListener.new MKMapViewListener() {...}.onMapAnimationFinish()");
			refreshPoints();
		}
		@Override
		public void onMapLoadFinish() {
			System.out.println("MainActivity.mapListener.new MKMapViewListener() {...}.onMapLoadFinish()");
			refreshPoints();
		}
		@Override
		public void onMapMoveFinish() {
			System.out.println("MainActivity.mapListener.new MKMapViewListener() {...}.onMapMoveFinish()");
			refreshPoints();
		}
	};
	
	private void refreshPoints(){
		//移动地图结束，重新加载覆盖物
		//获得左下角和右上角的点
		int longSpan = mapView.getLongitudeSpan();
		int latSpan = mapView.getLatitudeSpan();
		
		GeoPoint center = mapView.getMapCenter();
		double centerLongitude = center.getLongitudeE6()*1.0/1E6;
		double centerLatitude = center.getLatitudeE6()*1.0/1E6;
		
		double halfHeight = latSpan/2.0/1E6;
		double halfWidth = longSpan/2.0/1E6;
		
		double left = centerLongitude - halfWidth;
		double right = centerLongitude + halfWidth;
		double top =  centerLatitude + halfHeight;
		double bottom = centerLatitude - halfHeight;
		
		stationList = new StationDAO(MainActivity.this).queryStationByRegion(left, right, top, bottom, is24Hour);
		int size = stationList == null? 0 : stationList.size();
//		Toast.makeText(MainActivity.this, "long宽度：" + longSpan + ",lat跨度：" + latSpan + "，中心点：" + center.getLongitudeE6() + "," + center.getLatitudeE6(), Toast.LENGTH_SHORT).show();
		Toast.makeText(MainActivity.this, "共有：" + size + "个点", Toast.LENGTH_SHORT).show();
		
		//把站点以覆盖物形式展现
		overlayItems.clear();
		mapView.getOverlays().clear();
		mapView.getOverlays().add(locationOverlay);
		myOverlay.removeAll();
		if(stationList != null){
			for (Station station : stationList) {
				GeoPoint point = new GeoPoint((int)(station.latitude*1E6), (int)(station.longitude*1E6));
				OverlayItem overlayItem = new OverlayItem(point, "", "");
				Drawable drawable = Tools.getPointDrawable(this, getResources().getDrawable(R.drawable.icon_gcoding), station.total);
				overlayItem.setMarker(drawable);
				overlayItems.add(overlayItem);
			}
			myOverlay.addItem(overlayItems);
			mapView.getOverlays().add(myOverlay);
		}
		mapView.refresh();
		
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mapManager = new BMapManager(getApplication());
		boolean regResult = mapManager.init(key, null);
		Toast.makeText(this, "注册地图key" + (regResult? "成功" : "失败"), Toast.LENGTH_SHORT).show();
		
		setContentView(R.layout.activity_main);
		
		bar = getActionBar();
		//设置显示tab
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		//获得fragment
		mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.fragment_content);
		mapView = (MapView)mapFragment.getFragmentView().findViewById(R.id.bmapsView);
		mapView.setBuiltInZoomControls(true);
		myOverlay = new MyOverlay(getResources().getDrawable(R.drawable.icon_gcoding), mapView);
		//设置地图监听
		mapView.regMapViewListener(mapManager, mapListener);
		
		//设置显示选项
		setDisplayOptions();
		//增加Tab标签页
		addTabs();
		
		//初始化地图
		initMap();
	}
	
	private void doLocation(){
		lc = new LocationClient(this);
		lc.setAK(key);
	
		listener = new MyLocationListener();
		lc.registerLocationListener(listener);
		
		setLocatonOptions();//设置定位选项
		
		lc.start();
	}
	
	private void setLocatonOptions(){
		    LocationClientOption option = new LocationClientOption();
		    option.setOpenGps(true); //打开GPS
		    option.setAddrType("all");//返回的定位结果包含地址信息
		    option.setCoorType("bd09ll");//返回的定位结果是百度经纬度,默认值gcj02
		    option.setScanSpan(5000);//设置发起定位请求的间隔时间为5000ms
		    option.disableCache(true);//禁止启用缓存定位
		    option.setPoiDistance(1000); //poi查询距离
		    option.setPoiExtraInfo(true); //是否需要POI的电话和地址等详细信息
		    lc.setLocOption(option);
	}
	
	private void initMap(){
		//获得地图控制器
		mapController = mapView.getController();
		if(currPoint == null){//如果没有定位好，则显示默认地点：武林广场.否则，不做改变
			currPoint = new GeoPoint((int)(defaultLatitude*1E6), (int)(defaultLongitude*1E6));
			mapController.setCenter(currPoint);
			mapController.setZoom(defaultZoomLevel);
		}
		
//		pop = new PopupOverlay(mapView, new PopupClickListener() {
//			@Override
//			public void onClickedPopup(int arg0) {
//			}
//		});
		
		popView = LayoutInflater.from(this).inflate(R.layout.dialog_pop, null);
	}
	
	private void setDisplayOptions(){
		//设置标题是否显示
//		bar.setDisplayShowTitleEnabled(false);
		//设置是否显示返回标记
//		bar.setDisplayHomeAsUpEnabled(true);
		
	}

	private void addTabs(){
//		Drawable icon = Tools.getPointDrawable(this, getResources().getDrawable(R.drawable.icon_gcoding), 11);
		
		tabListener = new MyTabListener();
		bar.addTab(bar.newTab().setText("附近").setTabListener(tabListener).setTag(TYPE_NEARBY));
		bar.addTab(bar.newTab().setText("24小时").setTabListener(tabListener).setTag(TYPE_24_HOUR));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is
		// present.
		//获得ActionBar显示的选项
		getMenuInflater().inflate(R.menu.main, menu);
		//查询事件监听
		SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
		searchView.setQueryHint("输入站点名称或者编号");
		searchView.setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				Toast.makeText(MainActivity.this, "开始查询：" + query, Toast.LENGTH_SHORT).show();
				return false;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				return false;
			}
		});
		
		System.out.println("MainActivity.onCreateOptionsMenu()");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId()  == R.id.action_share){
			//TODO:分享
			Toast.makeText(MainActivity.this, "分享应用", Toast.LENGTH_SHORT).show();
		}
		return super.onOptionsItemSelected(item);
	}


	public class MyTabListener implements ActionBar.TabListener{
		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			Toast.makeText(MainActivity.this, "选中：" + tab.getText(), Toast.LENGTH_SHORT).show();
			if(TYPE_NEARBY.equals(tab.getTag())){
				is24Hour = false;
				doLocation();
			}else if(TYPE_24_HOUR.equals(tab.getTag())){
				is24Hour = true;
				refreshPoints();
			}
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
//			ft.remove(fragment);
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			if(TYPE_NEARBY.equals(tab.getTag())){
				is24Hour = false;
				doLocation();
			}else if(TYPE_24_HOUR.equals(tab.getTag())){
				is24Hour = true;
				refreshPoints();
			}
			
		}
		
	}
	
	public static class MapFragment extends Fragment{
		private View view;
		public MapFragment(){
			System.out.println();
		}
		@Override
		public View onCreateView(LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {
			if(view == null){
				view = inflater.inflate(R.layout.fragment_map, container, false);
			}
			return view;
//			return super.onCreateView(inflater, container, savedInstanceState);
		}
		
		public View getFragmentView(){
			return view;
		}
		
	}
	
	public class MyLocationListener implements BDLocationListener{

		@Override
		public void onReceiveLocation(BDLocation location) {//回调的位置信息
			System.out.println("纬度：" + location.getLatitude());
			System.out.println("经度：" + location.getLongitude() );
			System.out.println("error code:" + location.getLocType());
			
			currPoint = new GeoPoint((int)(location.getLatitude()*1E6), (int)(location.getLongitude()*1E6)); 
			mapController = mapView.getController();
			mapController.setCenter(currPoint);
			mapController.setZoom(defaultZoomLevel);
			locationOverlay = new MyLocationOverlay(mapView);
			LocationData locationData = new LocationData();
			locationData.latitude = location.getLatitude();
			locationData.longitude = location.getLongitude();
			//如果不显示定位精度圈，将accuracy赋值为0即可
			locationData.accuracy = location.getRadius();
			// 此处可以设置 locData的方向信息, 如果定位 SDK 未返回方向信息，用户可以自己实现罗盘功能添加方向信息。
			locationData.direction = location.getDerect();
			locationOverlay.setData(locationData);
			mapView.getOverlays().add(locationOverlay);
			
			refreshPoints();
			//定位成功后，取消监听
			if(lc != null && lc.isStarted()){
				lc.stop();
			}
		}

		@Override
		public void onReceivePoi(BDLocation arg0) {//回调的兴趣点
		}
		
	}
	
	public class MyOverlay extends ItemizedOverlay<OverlayItem>{

		public MyOverlay(Drawable defaultMark, MapView mapView) {
			super(defaultMark, mapView);
		}

		@Override
		public boolean onTap(GeoPoint point, MapView mapView) {
			return super.onTap(point, mapView);
		}

		@Override
		protected boolean onTap(int index) {
			//设置界面，并显示
			ViewGroup parent = (ViewGroup)popView.getParent();
			if(parent != null){
				parent.removeView(popView);
			}
			
			Station station = stationList.get(index);
			TextView tvCode = (TextView)popView.findViewById(R.id.tv_code);
			TextView tvName = (TextView)popView.findViewById(R.id.tv_name);
			TextView tvAddress = (TextView)popView.findViewById(R.id.tv_address);
			TextView tvCanBorrow = (TextView)popView.findViewById(R.id.tv_can_borrow);
			tvCanBorrow.setTextColor(station.canBorrow==0? Color.RED : station.canBorrow < 3? Color. MAGENTA: Color.GREEN);
			TextView tvCanReturn = (TextView)popView.findViewById(R.id.tv_can_return);
			tvCanReturn.setTextColor(station.canReturn==0? Color.RED : station.canReturn < 3? Color.MAGENTA : Color.GREEN);
			TextView tvServiceTime = (TextView)popView.findViewById(R.id.tv_service_time);
			
			//TODO:获得实时数据
			
			tvCode.setText("" + station.code);
			tvName.setText(station.name);
			tvAddress.setText(station.address);
			tvCanBorrow.setText(station.canBorrow + "辆");
			tvCanReturn.setText(station.canReturn + "辆");
			tvServiceTime.setText(station.startService + "-" + station.endService);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this).setView(popView).setPositiveButton("关闭", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			
			AlertDialog dialog = builder.create();
			dialog.setCanceledOnTouchOutside(true);
			dialog.show();
			
			return super.onTap(index);
		}
		
		
	}

	@Override
	protected void onDestroy() {
		if(lc != null && lc.isStarted()){
			lc.stop();
		}
		super.onDestroy();
	}
	
	
}
