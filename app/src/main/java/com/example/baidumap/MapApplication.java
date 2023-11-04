package com.example.baidumap;

import android.app.Application;

import com.baidu.location.LocationClient;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;

public class MapApplication extends Application {
 
    @Override
    public void onCreate() {
        super.onCreate();
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext

        //自4.3.0起，百度地图SDK所有接口均支持百度坐标和国测局坐标，用此方法设置您使用的坐标类型.

        //包括BD09LL和GCJ02两种坐标，默认是BD09LL坐标。
//        SDKInitializer.setCoordType(CoordType.BD09LL);
        //同意SDKInitializer使用条例
        SDKInitializer.setAgreePrivacy(this, true);
        //初始化
        SDKInitializer.initialize(this);
        //同意LocationClient使用条例
        LocationClient.setAgreePrivacy(true);
    }
}