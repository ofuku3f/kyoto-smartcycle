package com.kyoto.smartcycle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.maps.GeoPoint;

public class RouteModel {

	public Step steps[];
	
	public static RouteModel ParseJSON(String json) throws JSONException {
		RouteModel route = new RouteModel();
		
		// JSONをパース
		JSONObject obj = new JSONObject(json);
		JSONArray routelist = obj.getJSONArray("route");
		
		

		
		 
		
		
		return route;
	}
	
	
	public class Step{
		public int distance; // 区間距離[m]
		public int duration; // 区間時間[sec]
		public GeoPoint start_addr; // 開始地点
		public GeoPoint end_addr; // 終了地点
		public String instruction; // 命令文
	}
}
