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

public class RouteMapActivity extends MapActivity {

//	private LocationManager mLocMgr = null;

	private MapView mMap = null;
	private MapController mMapCtrl = null;
	
//	private CurrentLocationOverlay currentLocationOverlay;
	private TrackingMyLocationOverlay mMyLocOverlay = null;
	private RouteOverlay routeOverlay = null;
	private CrossOverlay mCrossOverlay = null;
	
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
		mMyLocOverlay = new TrackingMyLocationOverlay(this, mMap);
		mMyLocOverlay.runOnFirstFix(new Runnable() {
			
			@Override
			public void run() {
				
				mMapCtrl.animateTo(mMyLocOverlay.getMyLocation());
				startingPoint = mMyLocOverlay.getMyLocation();
				Log.d(LOG_TAG, "Fix Current Location: " + startingPoint.getLatitudeE6() + "," + startingPoint.getLongitudeE6());
			}
		});
		
		mMap.getOverlays().add(mMyLocOverlay);

		// 中心ラインを表示
		mCrossOverlay = new CrossOverlay();
		mMap.getOverlays().add(mCrossOverlay);
		
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
//		if(mMyLocOverlay != null && mMyLocOverlay.isMyLocationEnabled()){
//			mMyLocOverlay.disableMyLocation();
//			mMyLocOverlay.disableCompass();	
//		}
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
				GeoPoint dest = mMap.getMapCenter();
				Log.d(LOG_TAG, "dest: " + dest.getLatitudeE6() + "," + dest.getLongitudeE6());
				
    			// Google Directions API へのリクエストを，非同期で実行する。
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

		// データ登録リクエスト送信後、ルート情報を解析する。
		@Override
		protected void onPostExecute(String result) {
			
			Log.v("Traveler","Successfully Fetched.");
			
			Log.d(LOG_TAG, result);
			
			try {
				mRoute = RouteModel.ParseJSON(result);
				
				// ルート表示のオーバーレイを生成
				routeOverlay = new RouteOverlay();
				mMap.getOverlays().add(routeOverlay);
		
				// マップの表示更新
				mMap.invalidate();
				
				Toast.makeText(mContext, "Route Fetched.", Toast.LENGTH_LONG).show();
				
			} catch (JSONException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			
		}
    	
    }

    class TrackingMyLocationOverlay extends MyLocationOverlay
    {
        MapView mapView;
        
        public TrackingMyLocationOverlay(Context context, MapView mapView)
        {
            super(context, mapView);
            this.mapView = mapView;
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
    
    class CrossOverlay extends Overlay {

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            super.draw(canvas, mapView, shadow);
    		
            if( !shadow ) {
                Paint paint = new Paint( Paint.ANTI_ALIAS_FLAG);
                paint.setStyle( Paint.Style.STROKE);
                paint.setAntiAlias( true);
                paint.setStrokeWidth( 3);
                paint.setColor( Color.RED);

                Path path = new Path();
                int height = canvas.getHeight();
                int width = canvas.getWidth();
                path.moveTo( width / 2, 0);
                path.lineTo( width / 2, height);
                canvas.drawPath(path, paint);
                path.moveTo( 0, height / 2);
                path.lineTo( width , height / 2);
                canvas.drawPath(path, paint);
            }
        }
    }
    
	class RouteOverlay extends Overlay{

		private RouteModel.Step steps[] = null;
		private Paint mPaint = null;
		
		public RouteOverlay() {
			super();
			steps = mRoute.steps;
			
	        mPaint = new Paint();
	        mPaint.setStrokeCap(Paint.Cap.ROUND);
	        mPaint.setStrokeJoin(Paint.Join.ROUND);
	        mPaint.setStyle(Paint.Style.STROKE);
	        mPaint.setColor(Color.BLUE);
	        mPaint.setStrokeWidth(8);
	        mPaint.setAlpha(127);
	        mPaint.setAntiAlias(true);
		}

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			super.draw(canvas, mapView, shadow);
			
	        Projection projection = mapView.getProjection();
	        Path path = new Path();
	        Point pxPoint = new Point();
	        
        	projection.toPixels(steps[0].start_addr, pxPoint);
        	path.moveTo((float)pxPoint.x, (float)pxPoint.y);
	        
	        for(int i = 0; i < steps.length; i++){
	        	projection.toPixels(steps[i].end_addr, pxPoint);
	        	path.lineTo((float)pxPoint.x, (float)pxPoint.y);	        	
	        	
	        }
	        canvas.drawPath(path, mPaint);
		}

	}
    
}


