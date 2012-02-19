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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/**
 * @brief 目的地を設定してルート検索を行うActivity
 */
public class RouteMapActivity extends MapActivity {

	private MapView mMap = null;
	private MapController mMapCtrl = null;
	
//	private CurrentLocationOverlay currentLocationOverlay;
	private TrackingMyLocationOverlay mMyLocOverlay = null; // 現在地表示用オーバーレイ
	private RouteOverlay mRouteOverlay = null; // ルート表示用オーバーレイ
	private CrossOverlay mCrossOverlay = null; // 十字表示用オーバーレイ
	
	private Context mContext = null;
	
	private GeoPoint mStartingPoint = null; // 出発地点(mCurPointに集約できるかも)
	private GeoPoint mCurPoint = null; // 現在地点
	public GeoPoint getCurPoint(){
		return mCurPoint;
	}
	
	private RouteModel mRoute = null; // ルート情報
	public RouteModel getRoute() {
		return mRoute;
	}

	private static final String LOG_TAG = "RouteMapActivity"; // LOGCAT用TAG
	private static final String ROUTE_URL = "http://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&avoid=tolls&avoid=highways&sensor=false"; // Google Directions APIのURL 
	
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
		mMyLocOverlay = new TrackingMyLocationOverlay(this, mMap);
		mMyLocOverlay.runOnFirstFix(new Runnable() {
			@Override
			public void run() {
				// 現在地に画面中心を合わせる。
				mMapCtrl.animateTo(mMyLocOverlay.getMyLocation());
				mStartingPoint = mMyLocOverlay.getMyLocation();
				Log.d(LOG_TAG, "Fix Current Location: " + mStartingPoint.getLatitudeE6() + "," + mStartingPoint.getLongitudeE6());
			}
		});
		mMap.getOverlays().add(mMyLocOverlay);

		// 中心の十字ラインを表示
		mCrossOverlay = new CrossOverlay();
		mMap.getOverlays().add(mCrossOverlay);
		
		//TODO: 前回のルート情報の有無を確認して残っていれば再現し
		//      ルート表示オーバーレイを登録する。
		mRouteOverlay = new RouteOverlay();
		mMap.getOverlays().add(mRouteOverlay);
		
	}
	

	@Override
	protected void onResume() {
		super.onResume();
		
		// 現在地取得の有効化
		if(mMyLocOverlay != null && !mMyLocOverlay.isMyLocationEnabled()){
			mMyLocOverlay.enableMyLocation();
			mMyLocOverlay.enableCompass();
		}		
	}

	@Override
	protected void onPause() {
		super.onPause();
		
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
		menu.add(Menu.NONE, MENU_ID_SET_DESTINATION, Menu.NONE, getResources().getString(R.string.MENU_SET_DESTINATION)); // 目的地設定メニュー
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case MENU_ID_SET_DESTINATION:
				GeoPoint dest = mMap.getMapCenter();
				Log.d(LOG_TAG, "dest: " + dest.getLatitudeE6() + "," + dest.getLongitudeE6());
				
    			// Google Directions API へのリクエストを，非同期で実行する。
    			FetchRouteDataTask task = new FetchRouteDataTask();
    			task.execute(String.format(ROUTE_URL, 
    					String.format("%f,%f", mCurPoint.getLatitudeE6() * 1.0E-6, mCurPoint.getLongitudeE6() * 1.0E-6),
    					String.format("%f,%f", dest.getLatitudeE6() * 1.0E-6, dest.getLongitudeE6() * 1.0E-6))
    					);
				break;
		}
		
		return super.onOptionsItemSelected(item);
	}

	
    // Google Derections APIを投げる、非同期タスク
    private class FetchRouteDataTask extends AsyncTask<String, Void, String> {

    	// 問い合わせの実処理
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
					Log.d(LOG_TAG, "[response] " + status + " " + phrase);
					
					if(status == HttpStatus.SC_OK){
						OutputStream outStream = new ByteArrayOutputStream(); 
						response.getEntity().writeTo(outStream);
						String json = outStream.toString();
						Log.d(LOG_TAG, "JSON: " + json);
						
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

		// API問い合わせ後、受信レスポンスを解析する。
		@Override
		protected void onPostExecute(String result) {
			
			Log.v(LOG_TAG, "Successfully Fetched.");
			
			try {
				mRoute = RouteModel.ParseJSON(result);
				
				//TODO: ルート情報の永続化
				
				// ルート表示のオーバーレイを生成
				mRouteOverlay.setRoute(mRoute);
		
				// マップの表示更新
				mMap.invalidate();
				
				
				// 受信完了のメッセージを表示
				Toast.makeText(mContext, "Route Fetched.", Toast.LENGTH_LONG).show();				
				
			} catch (JSONException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			
		}
    	
    }

    /**
     * @brief 現在地情報(mCurPoint)を定期的に更新する。
     *
     */
    class TrackingMyLocationOverlay extends MyLocationOverlay
    {
        MapView mMapView;
        
        public TrackingMyLocationOverlay(Context context, MapView mapView)
        {
            super(context, mapView);
            mMapView = mapView;
        }

        @Override
        public void onLocationChanged(Location location)
        {
            super.onLocationChanged(location);
            mCurPoint = new GeoPoint(
                    (int) (location.getLatitude() * 1E6), 
                    (int) (location.getLongitude() * 1E6));
            Log.d(LOG_TAG, "location modified: " + mCurPoint.getLatitudeE6() + "," + mCurPoint.getLongitudeE6());
        }
    }
    
    /**
     * @brief 十字ガイドライン
     * @note 交点がMapViewの中心点となるように。
     */
    class CrossOverlay extends Overlay {
    	private Paint mPaint;
    	
    	public CrossOverlay(){
            mPaint = new Paint( Paint.ANTI_ALIAS_FLAG);
            mPaint.setStyle( Paint.Style.STROKE);
            mPaint.setAntiAlias( true);
            mPaint.setStrokeWidth( 3);
            mPaint.setColor( Color.RED);
    	}    	

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            super.draw(canvas, mapView, shadow);

            Path path = new Path();
            int height = mapView.getHeight();
            int width = mapView.getWidth();
            path.moveTo( width / 2, 0);
            path.lineTo( width / 2, height);
            canvas.drawPath(path, mPaint);
            path.moveTo( 0, height / 2);
            path.lineTo( width , height / 2);
            canvas.drawPath(path, mPaint);
        }
    }
    
    /**
     * @brief ルート経路を地図上に表示するオーバーレイ
     */
	class RouteOverlay extends Overlay{

		private RouteModel.Step mSteps[] = null;
		private Paint mPaint = null;
		
		public RouteOverlay() {
			super();
			mSteps = new RouteModel.Step[0];
			
	        mPaint = new Paint();
	        mPaint.setStrokeCap(Paint.Cap.ROUND);
	        mPaint.setStrokeJoin(Paint.Join.ROUND);
	        mPaint.setStyle(Paint.Style.STROKE);
	        mPaint.setColor(Color.BLUE);
	        mPaint.setStrokeWidth(8);
	        mPaint.setAlpha(127);
	        mPaint.setAntiAlias(true);
		}
		
		public void setRoute(RouteModel route){
			mSteps = route.steps;
		}

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			super.draw(canvas, mapView, shadow);
			
	        Projection projection = mapView.getProjection();
	        Path path = new Path();
	        Point pxPoint = new Point();
	        
	        if(mSteps.length > 0){
	        	projection.toPixels(mSteps[0].start_addr, pxPoint); // GeoPoint -> Px座標
	        	path.moveTo((float)pxPoint.x, (float)pxPoint.y);
		        
		        for(int i = 0; i < mSteps.length; i++){
		        	projection.toPixels(mSteps[i].end_addr, pxPoint);
		        	path.lineTo((float)pxPoint.x, (float)pxPoint.y);	        	
		        	
		        }
		        canvas.drawPath(path, mPaint);
		    }
		}

	}
    
}


