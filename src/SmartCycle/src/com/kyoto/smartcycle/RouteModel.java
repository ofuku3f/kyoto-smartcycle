package com.kyoto.smartcycle;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.maps.GeoPoint;

public class RouteModel {

	public Step steps[];
	
	public static RouteModel ParseJSON(String json) throws JSONException {
		RouteModel route = new RouteModel();
		
		ArrayList<Step> localSteps = new ArrayList<RouteModel.Step>();
		
		// JSONをパース
		JSONObject obj = new JSONObject(json);
		JSONArray routelist = obj.getJSONArray("routes");
		JSONObject mainroute = routelist.getJSONObject(0);
		JSONArray legslist = mainroute.getJSONArray("legs");
		for(int i = 0; i < legslist.length(); i++){
			JSONArray steps = legslist.getJSONObject(i).getJSONArray("steps");
			for(int j = 0; j < steps.length(); j++){
				Step s = route.new Step();
				JSONObject step = steps.getJSONObject(i);
				s.distance = step.getJSONObject("distance").getInt("value");
				s.duration = step.getJSONObject("duration").getInt("value");
				JSONObject st = step.getJSONObject("start_location");
				s.start_addr = new GeoPoint((int)(st.getDouble("lat") * 1E6), (int)(st.getDouble("lng")));
				JSONObject en = step.getJSONObject("end_location");
				s.end_addr = new GeoPoint((int)(en.getDouble("lat") * 1E6), (int)(en.getDouble("lng")));
				s.instruction = step.getString("html_instructions");
				localSteps.add(s);
			}
		}
		
		route.steps = new Step[localSteps.size()];
		for(int i = 0; i < localSteps.size(); i++){
			route.steps[i] = localSteps.get(i);
		}
		
		

		
		 
		
		
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
