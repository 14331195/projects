package com.homework.hw10;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

/**
 * Created by ljm on 2016/12/1.
 */
public class Util  {
    private Context context;
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;

    private Sensor mMagneticSensor;
    private Sensor mAccelerometerSensor;
    private OnLocationUpdateListener mListener;

    private Location mCurrentLocation;
    private float mCurrentRotation;
    private final String GPS = "gps";
//    private final String NETWORK = "network";
    private final float PER_ROTATION_DEGREE = 1f;

    //这些参数用于检测手机摇晃
    private static final int SPEED_SHRESHOLD = 15;        // 速度阈值，当摇晃速度达到这值后产生作用
    private static final int UPTATE_INTERVAL_TIME = 1000;// 两次检测的时间间隔
    private long lastUpdateTime = 0;
    private float[] lastValues = {0f, 0f, 0f};

    public static final int MSG_LOCATION_CHANGED = 0;
    public static final int MSG_DATA_CHANGED = 1;
    public static final int MSG_SHAKE = 2;

    //位置服务事件监听器
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mCurrentLocation = location;
            if (mListener != null) {
                float[] values = new float[]{
                        (float) mCurrentLocation.getLongitude(),
                        (float) mCurrentLocation.getLatitude(),
                        (float) mCurrentLocation.getAltitude()
                };
                mListener.update(MSG_LOCATION_CHANGED, values);
            }
            setToast("位置更新！(GPS)");
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //setToast("GPS不可用！");
        }
        @Override
        public void onProviderEnabled(String provider) {
            setToast("GPS已启用！");
        }
        @Override
        public void onProviderDisabled(String provider) {
            setToast("GPS不可用！");
        }
    };
    //传感器事件监听器
    private SensorEventListener mEventListener = new SensorEventListener() {
        float[] acc_data, mag_data;
        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    acc_data = event.values.clone();
                    long currentUpdateTime = System.currentTimeMillis();
                    if (currentUpdateTime - lastUpdateTime >= UPTATE_INTERVAL_TIME) {
                        lastUpdateTime = currentUpdateTime;
                        float deltaX = lastValues[0] - acc_data[0];
                        float deltaY = lastValues[1] - acc_data[1];
                        float deltaZ = lastValues[2] - acc_data[2];
                        lastValues = event.values.clone();
                        double speed = Math.sqrt(deltaX*deltaX + deltaY*deltaY + deltaZ*deltaZ);
                        if (speed >= SPEED_SHRESHOLD) {
                            mListener.update(MSG_SHAKE, acc_data);
                        }
                    }
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mag_data = event.values.clone();
                    break;
            }
            //计算手机转动方向
            if (acc_data != null && mag_data != null) {
                float[] tmp = new float[9];
                float[] values = new float[3];
                SensorManager.getRotationMatrix(tmp, null, acc_data, mag_data);
                SensorManager.getOrientation(tmp, values);
                float degree = (float) Math.toDegrees(values[0]);
                //若转动幅度大于设定的值，就通知更新
                if (Math.abs(degree - mCurrentRotation) > PER_ROTATION_DEGREE) {
                    mCurrentRotation = degree;
                    if (mListener != null) {
                        mListener.update(MSG_DATA_CHANGED, new float[]{degree, 0f, 0f});
                    }
                }
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public Util (Context context) {
        this.context = context;
        mSensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        mLocationManager = (LocationManager)this.context.getSystemService(Context.LOCATION_SERVICE);

        initSensor();
        register();
    }

    public void initSensor() {
        mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void register() throws SecurityException {
        mSensorManager.registerListener(mEventListener, mMagneticSensor,
                SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(mEventListener, mAccelerometerSensor,
                SensorManager.SENSOR_DELAY_GAME);
        if (mLocationManager.isProviderEnabled(GPS)) {
            mLocationManager.requestLocationUpdates(GPS, 500, 0.5f, mLocationListener);
        }

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        String provider = mLocationManager.getBestProvider(criteria, true);
        mLocationManager.getLastKnownLocation(provider);
        mCurrentLocation = mLocationManager.getLastKnownLocation(provider);

    }
    public void unregister() throws SecurityException {
        mSensorManager.unregisterListener(mEventListener);
    }

    public void setListener(OnLocationUpdateListener listener) {
        this.mListener = listener;
    }

    public void resetListener(OnLocationUpdateListener listener) {
        if (listener == mListener) {
            mListener = null;
        }
    }

    public Location getCurrentLocation() {
        return mCurrentLocation;
    }

    public float getCurrentRotation() {
        return mCurrentRotation;
    }

    public interface OnLocationUpdateListener {
        void update(int i, float[] f);
    }

    public void setToast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
}
