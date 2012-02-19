package com.kyoto.smartcycle;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class RouteMapActivity extends MapActivity {

//	private LocationManager mLocMgr = null;

	private MapView mMap = null;
	private MapController mMapCtrl = null;
	
//	private CurrentLocationOverlay currentLocationOverlay;
	private MyLocationOverlay mMyLocOverlay = null;
	//private RouteOverlay routeOverlay = null;
	
	private Context mContext = null;
	
	private GeoPoint startingPoint = null;
	
	private RouteModel mRoute = null;
	public RouteModel getRoute() {
		return mRoute;
	}

	private GeoPoint mCurPoint = null;
	public GeoPoint getCurPoint(){
		return mCurPoint;
	}

	private static final String LOG_TAG = "RouteMapActivity";
	private static final String ROUTE_URL = "http://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&sensor=false"; 
	private static final int MENU_ID_SET_DESTINATION = (Menu.FIRST + 1); 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.routemap);
		
		mContext = this;
		
		// MapView の設定
		mMap = (MapView) findViewById(R.id.map);
		mMapCtrl = mMap.getController();
		mMap.setClickable(true);
		mMap.setBuiltInZoomControls(true);
		mMapCtrl.setZoom(15);
		
		// 現在地表示のオーバーレイを生成
		mMyLocOverlay = new MyLocationOverlay(this, mMap);
		mMyLocOverlay.runOnFirstFix(new Runnable() {
			
			@Override
			public void run() {
				
				mMapCtrl.animateTo(mMyLocOverlay.getMyLocation());
				startingPoint = mMyLocOverlay.getMyLocation();
				Log.d(LOG_TAG, "Fix Current Location: " + startingPoint.getLatitudeE6() + "," + startingPoint.getLongitudeE6());
			}
		});
		mMap.getOverlays().add(mMyLocOverlay);
		

//		// GPSの有効化
//        mLocMgr = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

//        // 現在地点マーカーの表示レイヤーを追加
//        currentLocationOverlay = new CurrentLocationOverlay(getResources().getDrawable(R.drawable.street_view_icon_mini));
//        mMap.getOverlays().add(currentLocationOverlay);
        
	}
	

	@Override
	protected void onResume() {
		super.onResume();
//       	mLocMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, currentLocationListener);
		
		// 現在地取得の有効化
		if(mMyLocOverlay != null && !mMyLocOverlay.isMyLocationEnabled()){
			mMyLocOverlay.enableMyLocation();
			mMyLocOverlay.enableCompass();
		}
		

	}

	@Override
	protected void onPause() {
		super.onPause();
//		mLocMgr.removeUpdates(currentLocationListener);
		
		// 現在地取得の無効化
		if(mMyLocOverlay != null && mMyLocOverlay.isMyLocationEnabled()){
			mMyLocOverlay.disableMyLocation();
			mMyLocOverlay.disableCompass();	
		}
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ID_SET_DESTINATION, Menu.NONE, getResources().getString(R.string.MENU_SET_DESTINATION));
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case MENU_ID_SET_DESTINATION:
//				Intent intent = new Intent(TravelerAppActivity.this, LocationDisplayActivity.class);
//				startActivity(intent);
				GeoPoint dest = mMap.getMapCenter();
				Log.d(LOG_TAG, "dest: " + dest.getLatitudeE6() + "," + dest.getLongitudeE6());
    			// リクエストを非同期で実行する。
    			FetchRouteDataTask task = new FetchRouteDataTask();
    			task.execute(String.format(ROUTE_URL, 
    					String.format("%f,%f", startingPoint.getLatitudeE6() * 0.000001, startingPoint.getLongitudeE6() * 0.000001),
    					String.format("%f,%f", dest.getLatitudeE6() * 0.000001, dest.getLongitudeE6() * 0.000001))
    					);
				
				break;
		}
		
		return super.onOptionsItemSelected(item);
	}

	
    // サーバへのデータ登録リクエストを投げる、非同期タスク
    private class FetchRouteDataTask extends AsyncTask<String, Void, String> {

    	// データ登録の実タスク
		@Override
		protected String doInBackground(String... urls) {
			if(urls.length > 0){
				String url = urls[0];
				HttpGet request = new HttpGet(url);
				
				DefaultHttpClient client = new DefaultHttpClient();
				HttpResponse response;
				try {
					response = client.execute( request );
					int status = response.getStatusLine().getStatusCode();
					String phrase = response.getStatusLine().getReasonPhrase();
					Log.d("Traveler", "[response] " + status + " " + phrase);
					
					if(status == HttpStatus.SC_OK){
						OutputStream outStream = new ByteArrayOutputStream(); 
						response.getEntity().writeTo(outStream);
						String json = outStream.toString();
						Log.d("Traveler", "JSON: " + json);
						
//						return TripRoute.ParseJSON(json);
						return json;
						
					}	
										
					return null;
					
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		// データ登録リクエスト送信後、レスポンスコードをトーストする。
		@Override
		protected void onPostExecute(String result) {
			
			Log.v("Traveler","Successfully Fetched.");
			
			Log.d(LOG_TAG, result);
			
			//mRoute = TripRoute.GetDebugRoute();
			
//			mRoute = result;
//			
//			// ルート表示のオーバーレイを生成
//			routeOverlay = new RouteOverlay(mRoute);
//			mMap.getOverlays().add(routeOverlay);
//			
//			// 見所表示のオーバーレイを生成
//			mSpotOverlay = new SpotOverlay(mRoute);
//			mMap.getOverlays().add(mSpotOverlay);	
//			
//			// マップの表示更新
//			mMap.invalidate();
//			
//			// 全体行程距離をToastに焼く
//			Toast.makeText(mContext, String.format("全体距離: %.1f km\n目標カロリー: %d kcal", mRoute.getDistance() * 1E-3, mRoute.getCalorie()), Toast.LENGTH_LONG).show();
		}
    	
    }

	

	

}
