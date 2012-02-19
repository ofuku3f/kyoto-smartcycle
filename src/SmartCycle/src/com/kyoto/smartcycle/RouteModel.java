package com.kyoto.smartcycle;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.maps.GeoPoint;

public class RouteModel {

	public Step steps[]; // ルートに含まれる区間情報の配列
	
	/**
	 * @brief Google Destination APIの戻り値(JSON)をパースする
	 * @param json Google Destination APIの戻り値
	 * @return ルート情報
	 * @throws JSONException
	 */
	public static RouteModel ParseJSON(String json) throws JSONException {
		RouteModel route = new RouteModel();
		
		ArrayList<Step> localSteps = new ArrayList<RouteModel.Step>();
		
		// JSONをパース
		JSONObject obj = new JSONObject(json);
		JSONArray routes = obj.getJSONArray("routes");
		JSONObject mainroute = routes.getJSONObject(0); // 複数ルートがある場合は最初のルートだけ解析する。
		JSONArray legs = mainroute.getJSONArray("legs"); // 中継点単位
		for(int i = 0; i < legs.length(); i++){
			JSONArray steps = legs.getJSONObject(i).getJSONArray("steps");
			for(int j = 0; j < steps.length(); j++){
				Step s = route.new Step();
				JSONObject step = steps.getJSONObject(j);
				s.distance = step.getJSONObject("distance").getInt("value");
				s.duration = step.getJSONObject("duration").getInt("value");
				JSONObject st = step.getJSONObject("start_location");
				s.start_addr = new GeoPoint((int)(st.getDouble("lat") * 1E6), (int)(st.getDouble("lng") * 1E6));
				JSONObject en = step.getJSONObject("end_location");
				s.end_addr = new GeoPoint((int)(en.getDouble("lat") * 1E6), (int)(en.getDouble("lng") * 1E6));
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
	
	/**
	 * @brief 各区間情報
	 */
	public class Step{
		public int distance; // 区間距離[m]
		public int duration; // 区間時間[sec]
		public GeoPoint start_addr; // 開始地点
		public GeoPoint end_addr; // 終了地点
		public String instruction; // 終了地点での命令文(HTML?)
	}
}
