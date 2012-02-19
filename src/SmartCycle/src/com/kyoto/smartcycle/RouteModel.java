package com.kyoto.smartcycle;

import com.google.android.maps.GeoPoint;

public class RouteModel {

	public Step steps[];
	
	public class Step{
		public int distance; // 区間距離[m]
		public int duration; // 区間時間[sec]
		public GeoPoint start_addr; // 開始地点
		public GeoPoint end_addr; // 終了地点
		public String instruction; // 命令文
	}
}
