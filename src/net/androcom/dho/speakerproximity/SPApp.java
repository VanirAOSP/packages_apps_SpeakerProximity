/****
 * Created by Michel Racic (http://www.2030.tk)
 * 
 * This is the base application class that handles the global stuff
 */

package net.androcom.dho.speakerproximity;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class SPApp extends Application {
	/** Self instance reference to be pulled from an activity **/
	private static SPApp		instance;

	/** used for the sensor management **/
	private SensorEventListener	proximityListener;
	private SensorManager		sensorManager;
	private SensorEventListener	orientationListener;

	/**
	 * handling headset changes, taken from
	 * http://proximitytoolextension.googlecode.com
	 **/
	private BroadcastReceiver	headSetPlugReceiver;
	private BroadcastReceiver	bluetoothConnectReceiver;
	private BroadcastReceiver	bluetoothDisconnectReceiver;

	/** State variables **/
	private boolean				inCall;
	private boolean				inCalibration;

	// private boolean headsetConnected;

	@Override
	public void onCreate() {
		instance = this;
	}

	public static SPApp getInstance() {
		return instance;
	}

	public SensorEventListener getProximityListener() {
		return proximityListener;
	}

	public void setProximityListener(SensorEventListener proximityListener) {
		this.proximityListener = proximityListener;
	}

	public SensorManager getSensorManager() {
		return sensorManager;
	}

	public void setSensorManager(SensorManager sensorManager) {
		this.sensorManager = sensorManager;
	}

	public SensorEventListener getOrientationListener() {
		return orientationListener;
	}

	public void setOrientationListener(SensorEventListener orientationListener) {
		this.orientationListener = orientationListener;
	}

	public BroadcastReceiver getHeadSetPlugReceiver() {
		return headSetPlugReceiver;
	}

	public void setHeadSetPlugReceiver(BroadcastReceiver headSetPlugReceiver) {
		this.headSetPlugReceiver = headSetPlugReceiver;
	}

	public BroadcastReceiver getBluetoothConnectReceiver() {
		return bluetoothConnectReceiver;
	}

	public void setBluetoothConnectReceiver(
			BroadcastReceiver bluetoothConnectReceiver) {
		this.bluetoothConnectReceiver = bluetoothConnectReceiver;
	}

	public BroadcastReceiver getBluetoothDisconnectReceiver() {
		return bluetoothDisconnectReceiver;
	}

	public void setBluetoothDisconnectReceiver(
			BroadcastReceiver bluetoothDisconnectReceiver) {
		this.bluetoothDisconnectReceiver = bluetoothDisconnectReceiver;
	}

	/**
	 * This is a static logging method to handle logging in one place
	 * 
	 * @param msg
	 *            to be logged
	 */
	public static void log(String msg) {
		Log.d("SpeakerProximity", "["
				+ new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] "
				+ msg);
	}

	public boolean isInCall() {
		return inCall;
	}

	public void setInCall(boolean inCall) {
		this.inCall = inCall;
	}

	public boolean isInCalibration() {
		return inCalibration;
	}

	public void setInCalibration(boolean inCalibration) {
		this.inCalibration = inCalibration;
	}

	public boolean isHeadsetConnected() {
		return PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext())
				.getBoolean("isHeadsetConnected", false);
	}

	public void setHeadsetConnected(boolean headsetConnected) {
		Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).edit(); // get the preference writer
		prefsEditor.putBoolean("isHeadsetConnected", headsetConnected);
		prefsEditor.commit();
		prefsEditor = null;
	}

	public boolean registerProximityListener() {
		if (inCall || inCalibration) {
			log("registered proximity listener");
			return getSensorManager().registerListener(getProximityListener(),
					getSensorManager().getDefaultSensor(Sensor.TYPE_PROXIMITY),
					SensorManager.SENSOR_DELAY_FASTEST);
		} else {
			return false;
		}
	}

	public void unregisterProximityListener() {
		log("unregistered proximity listener");
		if (getProximityListener() != null) {
			try {
				getSensorManager().unregisterListener(getProximityListener());
			} catch (Exception e) {
				// do nothing as it's not probably just mot registered
			}
		}
	}
}
