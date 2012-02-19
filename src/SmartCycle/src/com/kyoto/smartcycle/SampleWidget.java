/*
 Copyright (c) 2011, Sony Ericsson Mobile Communications AB

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB nor the names
 of its contributors may be used to endorse or promote products derived from
 this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.kyoto.smartcycle;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.google.android.maps.GeoPoint;
import com.sonyericsson.extras.liveware.aef.widget.Widget;
import com.sonyericsson.extras.liveware.extension.util.SmartWatchConst;
import com.sonyericsson.extras.liveware.extension.util.widget.SmartWatchWidgetImage;
import com.sonyericsson.extras.liveware.extension.util.widget.WidgetExtension;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.text.format.DateUtils;
import android.util.Log;

/**
 * The sample widget handles the widget on an accessory. This class exists in
 * one instance for every supported host application that we have registered to.
 */
class SampleWidget extends WidgetExtension {

    public static final int WIDTH = 128;

    public static final int HEIGHT = 110;

    private static final long UPDATE_INTERVAL = 10 * DateUtils.SECOND_IN_MILLIS;

    // 画面切り替えカウント
    private int viewCount = 0;
    
    // 画面の総数
    private static int SUM_VIEW = 3;
    
/*    RouteMapActivity routeMap;
    RouteModel.Step[] stepList;
    GeoPoint geoPoint;
    RouteModel routeModel;*/
    
    /**
     * Create sample widget.
     *
     * @param hostAppPackageName Package name of host application.
     * @param context The context.
     */
    SampleWidget(final String hostAppPackageName, final Context context) {
        super(context, hostAppPackageName);
/*        routeMap = new RouteMapActivity();
        routeModel = routeMap.getRoute();
        stepList = routeModel.steps;
        geoPoint = routeMap.getCurPoint();*/
    }

    /**
     * Start refreshing the widget. The widget is now visible.
     */
    @Override
    public void onStartRefresh() {
        Log.d(SampleExtensionService.LOG_TAG, "startRefresh");
        // Update now and every 10th second
        cancelScheduledRefresh(SampleExtensionService.EXTENSION_KEY);
        scheduleRepeatingRefresh(System.currentTimeMillis(), UPDATE_INTERVAL,
                SampleExtensionService.EXTENSION_KEY);
    }

    /**
     * Stop refreshing the widget. The widget is no longer visible.
     */
    @Override
    public void onStopRefresh() {
        Log.d(SampleExtensionService.LOG_TAG, "stopRefesh");

        // Cancel pending clock updates
        cancelScheduledRefresh(SampleExtensionService.EXTENSION_KEY);
    }

    @Override
    public void onScheduledRefresh() {
        Log.d(SampleExtensionService.LOG_TAG, "scheduledRefresh()");
        
        // 情報を定期更新
/*        routeModel = routeMap.getRoute();
        stepList = routeModel.steps;
        geoPoint = routeMap.getCurPoint();*/
        
        updateWidget();
    }

    /**
     * Unregister update clock receiver, cancel pending updates
     */
    @Override
    public void onDestroy() {
        Log.d(SampleExtensionService.LOG_TAG, "onDestroy()");
        onStopRefresh();
    }

    /**
     * The widget has been touched.
     *
     * @param type The type of touch event.
     * @param x The x position of the touch event.
     * @param y The y position of the touch event.
     */
    @Override
    public void onTouch(final int type, final int x, final int y) {
        Log.d(SampleExtensionService.LOG_TAG, "onTouch() " + type);
        if (!SmartWatchConst.ACTIVE_WIDGET_TOUCH_AREA.contains(x, y)) {
            Log.d(SampleExtensionService.LOG_TAG, "Ignoring touch outside active area x: " + x
                    + " y: " + y);
            return;
        }

        if (type == Widget.Intents.EVENT_TYPE_SHORT_TAP) {
        	// 画面切り替えカウントをインクリメントする
        	viewCount++;
        	// カウントが画面総数を超えた場合はリセットする
        	if(viewCount >= SUM_VIEW){
        		viewCount = 0;
        	}
        	
            // タッチされたら画面を更新する
            updateWidget();
        }
    }

    /**
     * Update the widget.
     */
    private void updateWidget() {
        Log.d(SampleExtensionService.LOG_TAG, "updateWidget");
        
        SmartWatchWidgetImage instance;
    	Model model = new Model();
    	
        switch(viewCount)
        {
        	case 0:
        	default:
        		model.x = "右150m";
        		instance = new SmartWatchSampleWidgetImageNokori(mContext, model);
        		break;
        	case 1:
        		model.y = "2000m";
        		instance = new SmartWatchSampleWidgetImageFinalGoal(mContext, model);
        		break;
        	case 2:
        		int latitude = -32;//geoPoint.getLatitudeE6();
        		int longitude = 132;//geoPoint.getLongitudeE6();
        		String locationInfo = "";
            	try{
            		// 座標を住所に変換する
            		locationInfo = ConvertAddress(latitude,longitude,mContext);
            	}
            	catch(IOException ex){
        			// エラーログ出力
        			Log.e("err",ex.toString());
        		}
            	model.z = "京都市";//locationInfo;
        		instance = new SmartWatchSampleWidgetImageNow(mContext, model);
        		break;
        }
        showBitmap(instance.getBitmap());
    }
    
	/**
	 * 座標を住所に変換する。
	 * 
	 * @param latitude
	 * @param longitude
	 * @param context
	 * @return
	 * @throws IOException
	 */
	private String ConvertAddress(int latitude, int longitude, Context context) throws IOException{
		// 戻り値用結果
		String result = new String();

		//　geocoedrの実体化
		Geocoder geocoder = new Geocoder(context, Locale.getDefault());
		// 住所情報を取得(最大5項目)
		List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 5);

		// 空でない場合
		if (!addressList.isEmpty()){
			// 住所情報
			Address address = addressList.get(0);
			StringBuffer strbuf = new StringBuffer();

			//　住所情報をつなげていく
			String buf;
/*			for (int i = 0; i <= address.getMaxAddressLineIndex(); i++){
				buf = address.getAddressLine(i);
				strbuf.append(buf+"　");
			}*/
			buf = address.getAddressLine(1);
			strbuf.append(buf+"　");

			result = strbuf.toString();
		}
		
		return result;
	}
}
