package com.homework.hw10;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by ljm on 2016/12/1.
 */
public class MainActivity extends AppCompatActivity implements Util.OnLocationUpdateListener,
        TextToSpeech.OnInitListener{
    private MapView mapView;
    private BaiduMap baiduMap;
    private CoordinateConverter converter;
    private ToggleButton button;
    private Location mCurrentLocation;

    private Vibrator vibrator;
    private TextToSpeech toSpeech;
    private Util sensor;

    @Override
    protected void onCreate(@Nullable Bundle saveInstanceState) {
        SDKInitializer.initialize(getApplicationContext());

        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_layout);

        mapView = (MapView)findViewById(R.id.mapView);
        baiduMap = mapView.getMap();
        button = (ToggleButton)findViewById(R.id.toggle_button);
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        toSpeech = new TextToSpeech(this,this);

        initData();
        initBaiduMap();
    }

    public void initData() {
        sensor = new Util(this);
        mCurrentLocation = sensor.getCurrentLocation();
        sensor.setListener(this);
        converter = new CoordinateConverter();

        baiduMap.setOnMapTouchListener(new BaiduMap.OnMapTouchListener() {
            @Override
            public void onTouch(MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        button.setChecked(false);
                        break;
                }
            }
        });
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    updateLoc(sensor.getCurrentLocation(), sensor.getCurrentRotation());
                }
            }
        });
    }

    public void initBaiduMap() {
        Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),
                R.mipmap.pointer),80, 80, true);
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        baiduMap.setMyLocationEnabled(true);
        MyLocationConfiguration configuration = new MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.NORMAL,true, bitmapDescriptor);
        baiduMap.setMyLocationConfigeration(configuration);
        Location loc = sensor.getCurrentLocation();

        updateLoc(loc, 0f);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            toSpeech.setLanguage(Locale.CHINA);
            toSpeech.setLanguage(Locale.US);
        }
    }

    @Override
    public void update(int type, float[] values) {
        switch (type) {
            case Util.MSG_LOCATION_CHANGED:
                updateLoc(sensor.getCurrentLocation(), sensor.getCurrentRotation());
                break;
            case Util.MSG_DATA_CHANGED:
                updateLoc(sensor.getCurrentLocation(), values[0]);
                break;
            case Util.MSG_SHAKE:
                setToast("shake");
                vibrator.vibrate(500);
                if (!toSpeech.isSpeaking())
                    tellCurrentTime();
                break;
            default: break;
        }
    }

    public void updateLoc(Location location, float direc) {
        if (location == null) return;
        converter.from(CoordinateConverter.CoordType.GPS);
        converter.coord(new LatLng(location.getLatitude(), location.getLongitude()));
        LatLng curr =  converter.convert();
        MyLocationData.Builder data = new MyLocationData.Builder();
        data.latitude(curr.latitude);
        data.longitude(curr.longitude);
        data.direction(direc);
        baiduMap.setMyLocationData(data.build());
        if (button.isChecked()) {
            MapStatus status = new MapStatus.Builder().target(curr).build();
            baiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(status));
        }
    }

    public void tellCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        String str = "当前时间为 " + formatter.format(curDate);
        toSpeech.setPitch(1.0f);
        toSpeech.setSpeechRate(0.6f);
        toSpeech.speak(str, TextToSpeech.QUEUE_FLUSH, null);
        setToast(str);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        sensor.register();
        sensor.setListener(this);
    }
    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        sensor.unregister();
        sensor.resetListener(this);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (toSpeech != null) {
            toSpeech.stop();
            toSpeech.shutdown();
        }
    }

    public void setToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}
