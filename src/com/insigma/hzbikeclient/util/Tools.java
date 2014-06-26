package com.insigma.hzbikeclient.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.widget.Toast;

import com.insigma.hzbikeclient.R;
import com.insigma.hzbikeclient.bean.Station;
import com.insigma.hzbikeclient.db.StationDAO;

public class Tools {
	private static SparseArray<Drawable> iconMap = new SparseArray<Drawable>();
	
	//得到2个字符串中间的文字
	public static String getSubStr(String src, String p1, String p2){
		return src.substring(src.indexOf(p1) + p1.length(), src.lastIndexOf(p2));
	}
	
	/**
	 * 根据站点编号，实时查询站点当前可借，可还车辆信息
	 * @param code 站点编号
	 * @return Station站点信息
	 */
	public static boolean refreshStation(Station station){
		//查询Station基本信息
		
		String url = Constants.CATCH_URL + "&StationID=" + station.code;
		DefaultHttpClient hc = new DefaultHttpClient();
		String html = "";
		int nums = 0;
		while("".equals(html) && nums < 3){//尝试3次
			html = Tools.getHTML(hc, url);
			nums++;
		}
		
//		System.out.println(html);
		try {
			Parser parser = new Parser(html);
			
			//获得站点的其他信息
			parser = new Parser(html);
			HasAttributeFilter attrFilter = new HasAttributeFilter("class", "pad line_28");
			NodeList nodeList = parser.parse(attrFilter);
			if(nodeList.size() == 0){//查无此站点
				return false;
			}
			
			String text = nodeList.elementAt(0).toHtml();
			//<div class="pad line_28">  <p>     可借：<span id="bike_span1">12</span>辆 可还：<span id="bike_span2">20</span>辆<br />     名称：<span>吴山广场</span><br />     编号：<span>1001</span><br />     地点：<span>吴山广场牌坊西侧</span><br />     电话：<span></span><br />     服务时间：<span>0:01-23:59</span>   </p> </div>
			String canBorrowStr = Tools.getSubStr(text, "可借：<span id=\"bike_span1\">", "</span>辆 可还");
//			System.out.println(canBorrowStr);
			String canReturnStr = Tools.getSubStr(text, "可还：<span id=\"bike_span2\">", "</span>辆<br />     名");
//			System.out.println(canReturnStr);
			
			station.canBorrow = Integer.parseInt(canBorrowStr);
			station.canReturn = Integer.parseInt(canReturnStr);
			
		} catch (ParserException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			hc.getConnectionManager().shutdown();//关闭hc
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * 获取一个连接地址上的html
	 * @param hc
	 * @param url
	 * @return
	 */
	public static String getHTML(HttpClient hc, String url){
		StringBuilder html = new StringBuilder();
		HttpGet get = new HttpGet(url);
		
		
		try {
			HttpResponse response = hc.execute(get);
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){//返回成功
				HttpEntity entity = response.getEntity();
				if(entity != null){
					BufferedReader br  = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
					String line = null;
					while((line = br.readLine()) != null){
						html.append(line);
					}
					entity.getContent().close();
				}
			}else{
				System.out.println("取回失败，返回码为：" + response.getStatusLine().getStatusCode());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch(Exception e){
			e.printStackTrace();
		}
		
		
		return html.toString();
	}
	
	// 拷贝数据库结构到/data/data/com.insigma.hzbikeclient/databases/下
	public static boolean initDB(Context context) {
		boolean isSuccess = false;
		File dest = context.getDatabasePath(Constants.DATABASE_NAME);
		dest.getParentFile().mkdirs();

		// 打开静态数据库文件的输入流
		InputStream is = context.getResources().openRawResource(R.raw.hzbike);
		// 打开目标数据库文件的输出流
		FileOutputStream os = null;

		try {
			os = new FileOutputStream(dest.getCanonicalPath());
			byte[] buffer = new byte[1024];
			int count = -1;
			// 将静态数据库文件拷贝到目的地
			while ((count = is.read(buffer)) != -1) {
				os.write(buffer, 0, count);
				os.flush();
			}
			Toast.makeText(context, "初始化数据库结构成功", Toast.LENGTH_SHORT).show();
			isSuccess = true;
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(context, "初始化数据库结构失败，系统将不能正常运行", Toast.LENGTH_SHORT)
					.show();
		} finally {
			try {
				if (is != null) {
					is.close();
				}
				if (os != null) {
					os.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return isSuccess;
	}
	
	/**
	 * 合成数字和地图标记
	 * @param activity
	 * @param drawable
	 * @param number
	 * @return 
	 */
	public static Drawable getPointDrawable(Activity activity, Drawable drawable, Integer number){
		Drawable retIcon = iconMap.get(number);
		if(retIcon != null){
			return retIcon;
		}
		
//		DisplayMetrics dm = new DisplayMetrics();
//		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
//	        Toast.makeText(this,"屏幕分辨率为:"+dm.widthPixels+" * "+dm.heightPixels, Toast.LENGTH_SHORT).show();
		
		Bitmap icon = BitmapFactory.decodeResource(activity.getResources(), R.drawable.icon_gcoding);
		int width = icon.getWidth();
		int height = icon.getHeight();
		
//		Toast.makeText(this, "高度：" + height + ",宽度：" + width, Toast.LENGTH_SHORT).show();
		Bitmap bgIcon = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);//创建一个新的图片
		Canvas canvas = new Canvas(bgIcon);//作为底图
		
		Rect src = new Rect(0, 0, width, height);//创建一个源图矩形大小 
		Rect dst = new Rect(0, 0, width, height);//创建一个目标图矩形大小  
		Paint iconPaint = new Paint();
		canvas.drawBitmap(icon, null, dst, iconPaint);//把原图画到底图上
		 
		Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
		System.out.println("width:" + width);
		float textSize = -1;
		if(number >= 10){
			textSize = DensityUtil.px2dip(activity, width) /1.2f * (width/96.0f);
		}else{
			textSize = DensityUtil.px2dip(activity, width) /1.2f * (width/96.0f);
		}
		System.out.println("TextSize=" + textSize);
		textPaint.setTextSize(textSize);
		textPaint.setTypeface(Typeface.DEFAULT_BOLD);//采用默认的宽度  
		textPaint.setColor(Color.WHITE);//采用的颜色  
		float xCoor = -1;
		float yCoor = -1;
		if(number >= 10){
			xCoor = 23*width/96;
			yCoor = 45*width/96;
		}else{
			xCoor = 35*width/96;
			yCoor = 45*width/96;
		}
		canvas.drawText("" + number, xCoor, yCoor, textPaint);//绘制上去字
		canvas.save(Canvas.ALL_SAVE_FLAG); 
		canvas.restore(); 
		
		retIcon = new BitmapDrawable(activity.getResources(), bgIcon);
		iconMap.put(number, retIcon);
		
		return retIcon;
	}
}
