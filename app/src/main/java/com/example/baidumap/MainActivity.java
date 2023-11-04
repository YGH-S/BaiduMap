package com.example.baidumap;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

public class MainActivity extends Activity{

    //需要申请的权限数组
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    //权限代码编号
    private static final int PERMISSION_REQUEST_CODE = 1;

    //百度地图视图
    MapView mv_bmapView;

    //com.baidu.mapapi.map
    //百度地图类，用于将视图控件转为对象，方便操作
    private BaiduMap mBaiduMap;

    //com.baidu.mapapi.map
    private LocationClient mLocClient = null;

    //com.baidu.mapapi.map
    //需要用这个类把图片包装成对象
    private BitmapDescriptor bitmap;
    private double markerLatitude;//标记点纬度
    private double markerLongitude;//标记点经度

    //标记点
    private Marker marker;

    //当前位置
    private ImageButton ibLocation;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();//初始化视图

        //版本23以上需要动态获取权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            //检查授权结果，true为已获得授权
            if(PermissionUtil.checkPermission(this,PERMISSIONS,PERMISSION_REQUEST_CODE)){
                try {
                    initLocation();//初始化定位
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else {
                Toast.makeText(this, "未获得权限", Toast.LENGTH_SHORT).show();
            }
        }else {
            //版本23一下不需要动态获取权限
            try {
                initLocation();//初始化定位点
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mapOnClick();//地图点击监听

    }

    //初始化视图
    private void initView() {

        mv_bmapView = (MapView) findViewById(R.id.mv_bmapView);
        ibLocation = findViewById(R.id.ib_location);

        //点击图标回到第一次定位的位置
        ibLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markerLatitude = 0;//标记点纬度置为0，触发自动定位
                try {
                    initLocation();//初始化定位点，自动定位（定位到当前所在位置）
                } catch (Exception e) {
                    e.printStackTrace();
                }
                marker.remove();//清除标记点
            }
        });

        mv_bmapView.showScaleControl(true);  // 设置比例尺是否可见（true 可见/false不可见）
        //mMapView.showZoomControls(false);  // 设置缩放控件是否可见（true 可见/false不可见）
        mv_bmapView.removeViewAt(1);// 删除百度地图Logo

        //获得百度地图实例化对象
        mBaiduMap = mv_bmapView.getMap();

        //设置标记点时的事件监听（这里是提示标记点的经纬度）
        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                String info = (String) marker.getExtraInfo().get("info");
                Toast.makeText(MainActivity.this, info, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void mapOnClick() {
        // 设置marker图标（标记点的图标）
        bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka);

        //地图点击事件
        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {
                //获取经纬度
                markerLatitude = latLng.latitude;
                markerLongitude = latLng.longitude;
                //先清除图层
                mBaiduMap.clear();
                // 定义Maker坐标点
                LatLng point = new LatLng(markerLatitude, markerLongitude);
                // 构建MarkerOption，用于在地图上添加Marker
                MarkerOptions options = new MarkerOptions().position(point)
                        .icon(bitmap);
                // 在地图上添加Marker，并显示
                //mBaiduMap.addOverlay(options);
                marker = (Marker) mBaiduMap.addOverlay(options);
                Bundle bundle = new Bundle();
                bundle.putSerializable("info", "纬度：" + markerLatitude + "   经度：" + markerLongitude);
                marker.setExtraInfo(bundle);//将bundle值传入marker中，给baiduMap设置监听时可以得到它

                //点击地图之后重新定位
                try {
                    initLocation();//初始化定位点到标记点的位置
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            //此方法就是点击地图监听
            @Override
            public void onMapPoiClick(MapPoi mapPoi) {

            }
        });
    }

    public void initLocation() throws Exception {

        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        // 定位初始化
        mLocClient = new LocationClient(this);
        //获取监听
        MyLocationListener myListener = new MyLocationListener();
        //注册监听
        mLocClient.registerLocationListener(myListener);
        //获取定位点
        LocationClientOption option = new LocationClientOption();
        //设置定位点
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);// 设置高精度定位
        //自4.3.0起，百度地图SDK所有接口均支持百度坐标和国测局坐标，用此方法设置您使用的坐标类型.
        //包括BD09LL和GCJ02两种坐标，默认是BD09LL坐标。
        option.setCoorType("bd09ll");
        option.setScanSpan(5000);// 可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);// 可选，设置是否需要地址信息，默认不需要
        option.setOpenGnss(true);//使用GNSS卫星定位，GPS用法已淘汰
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要

        //装载定位点
        mLocClient.setLocOption(option);
        mLocClient.start();//开始定位
    }

    //定位监听
    public class MyLocationListener extends BDAbstractLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            //弹出当前位置信息（x国x省x市x县（区）x街道x路）
            Toast.makeText(MainActivity.this,location.getAddrStr(),Toast.LENGTH_SHORT).show();
            // MapView 销毁后不再处理新接收的位置
            if (location == null || mv_bmapView == null) {
                return;
            }

            //经纬度
            double resultLatitude;
            double resultLongitude;

            if (markerLatitude == 0) {//自动定位
                resultLatitude = location.getLatitude();
                resultLongitude = location.getLongitude();
                ibLocation.setVisibility(View.GONE);
            } else {//标点定位
                resultLatitude = markerLatitude;
                resultLongitude = markerLongitude;
                ibLocation.setVisibility(View.VISIBLE);
            }

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())// 设置定位数据的精度信息，单位：米
                    .direction(location.getDirection()) // 此处设置开发者获取到的方向信息，顺时针0-360
                    .latitude(resultLatitude)
                    .longitude(resultLongitude)
                    .build();

            mBaiduMap.setMyLocationData(locData);// 设置定位数据, 只有先允许定位图层后设置数据才会生效
            LatLng latLng = new LatLng(resultLatitude, resultLongitude);
            MapStatus.Builder builder = new MapStatus.Builder();
            builder.target(latLng).zoom(20.0f);
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSION_REQUEST_CODE && PermissionUtil.checkGrant(grantResults)){
            try {
                initLocation();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else {
            Toast.makeText(this, "未获得权限", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        //在activity执行onResume（）时执行mv_bmapView.onResume（）,实现地图生命周期管理
        mv_bmapView.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        //在activity执行onPause（）时执行mv_bmapView.onPause（）,实现地图生命周期管理
        mv_bmapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 退出时销毁定位
        mLocClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        // 在activity执行onDestroy时必须调用mMapView.onDestroy()
        mv_bmapView.onDestroy();
    }
}

