/**
 * @author: denzil
 */
package com.aware.plugin.sensortag;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Screen;
import com.aware.utils.Aware_Plugin;

public class Plugin extends Aware_Plugin {

    public static BLEDevicePicker bleDevicePicker = null;

    /**
     * Broadcasted event: the user has turned on his phone
     */
    public static final String ACTION_AWARE_PLUGIN_DEVICE_USAGE = "ACTION_AWARE_PLUGIN_DEVICE_USAGE";

    /**
     * Extra (double): how long was the phone OFF until the user turned it ON
     */
    public static final String SENSOR = "sensor";
    public static final String UNIT = "unit";
    public static final String UPDATE_PERIOD = "update_period";
    public static final String VALUE = "value";

    /**
     * Extra (double): how long was the phone ON until the user turned it OFF
     */


    private static String sensor = "";
    private static String unit = "";
    private static long update_period = 0;
    private static long value = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        AUTHORITY = Provider.getAuthority(this);

        TAG = "AWARE::Device Usage";

        //Shares this plugin's context to AWARE and applications
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                ContentValues context_data = new ContentValues();
                context_data.put(Provider.Sensor_Data.TIMESTAMP, System.currentTimeMillis());
                context_data.put(Provider.Sensor_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                context_data.put(Provider.Sensor_Data.SENSOR, sensor);
                context_data.put(Provider.Sensor_Data.UNIT, unit);
                context_data.put(Provider.Sensor_Data.UPDATE_PERIOD, update_period);
                context_data.put(Provider.Sensor_Data.VALUE, value);

                if (DEBUG) Log.d(TAG, context_data.toString());

                //insert data to device usage table
                getContentResolver().insert(Provider.Sensor_Data.CONTENT_URI, context_data);

                Intent sharedContext = new Intent(ACTION_AWARE_PLUGIN_DEVICE_USAGE);
                sharedContext.putExtra(SENSOR, sensor);
                sharedContext.putExtra(UPDATE_PERIOD, update_period);
                sharedContext.putExtra(UNIT, unit);
                sharedContext.putExtra(UPDATE_PERIOD, update_period);
                sharedContext.putExtra(VALUE, value);
                sendBroadcast(sharedContext);
            }
        };

        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
            Aware.setSetting(this, Settings.STATUS_PLUGIN_DEVICE_USAGE, true);
            Aware.setSetting(this, Aware_Preferences.STATUS_SCREEN, true);



            if (!Aware.isSyncEnabled(this, Provider.getAuthority(this)) && Aware.isStudy(this) && getApplicationContext().getPackageName().equalsIgnoreCase("com.aware.phone") || getApplicationContext().getResources().getBoolean(R.bool.standalone)) {
                ContentResolver.setIsSyncable(Aware.getAWAREAccount(this), Provider.getAuthority(this), 1);
                ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), true);
                ContentResolver.addPeriodicSync(
                        Aware.getAWAREAccount(this),
                        Provider.getAuthority(this),
                        Bundle.EMPTY,
                        Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60
                );
            }

            Aware.startAWARE(this);
            bleDevicePicker = new BLEDevicePicker();
            bleDevicePicker.execute();
        }
        return START_STICKY;
    }

    public class BLEDevicePicker extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {

            Intent devicePicker = new Intent(getApplicationContext(), DevicePicker.class);
            devicePicker.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(devicePicker);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!result) {
                Toast.makeText(getApplicationContext(), "Failed to find devices", Toast.LENGTH_SHORT)
                        .show();
            } else {
                Toast.makeText(getApplicationContext(), "Connected to Sensor", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_PLUGIN_DEVICE_USAGE, false);

        if (Aware.isStudy(this) && (getApplicationContext().getPackageName().equalsIgnoreCase("com.aware.phone") || getApplicationContext().getResources().getBoolean(R.bool.standalone))) {
            ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), false);
            ContentResolver.removePeriodicSync(
                    Aware.getAWAREAccount(this),
                    Provider.getAuthority(this),
                    Bundle.EMPTY
            );
        }

        Aware.stopAWARE(this);
    }
}
