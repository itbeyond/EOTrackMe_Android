/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.itbeyond.eotrackme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.*;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

import com.itbeyond.common.DeviceUuidFactory;
import com.itbeyond.eotrackme.R;
import com.itbeyond.eotrackme.common.Utilities;

public class GpsSettingsActivity extends PreferenceActivity
{

    private final Handler handler = new Handler();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean useImperial = prefs.getBoolean("useImperial", false);

        EditTextPreference distanceBeforeLogging = (EditTextPreference) findPreference("distance_before_logging");

        if (useImperial)
        {
            distanceBeforeLogging.setDialogTitle(R.string.settings_distance_in_feet);
            distanceBeforeLogging.getEditText().setHint(R.string.settings_enter_feet);
        }
        else
        {
            distanceBeforeLogging.setDialogTitle(R.string.settings_distance_in_meters);
            distanceBeforeLogging.getEditText().setHint(R.string.settings_enter_meters);
        }
        
        EditTextPreference accuracyBeforeLogging = (EditTextPreference) findPreference("accuracy_before_logging");

        if (useImperial)
        {
            accuracyBeforeLogging.setDialogTitle(R.string.settings_accuracy_in_feet);
            accuracyBeforeLogging.getEditText().setHint(R.string.settings_enter_feet);
        }
        else
        {
            accuracyBeforeLogging.setDialogTitle(R.string.settings_accuracy_in_meters);
            accuracyBeforeLogging.getEditText().setHint(R.string.settings_enter_meters);
        }

        CheckBoxPreference imperialCheckBox = (CheckBoxPreference) findPreference("useImperial");
        imperialCheckBox.setOnPreferenceChangeListener(new ImperialPreferenceChangeListener(prefs, distanceBeforeLogging, accuracyBeforeLogging));


        Preference enableDisablePref = findPreference("enableDisableGps");
        enableDisablePref.setOnPreferenceClickListener(new AndroidLocationPreferenceClickListener());
        
         
        EditTextPreference txtEOTrackMeDeviceId = (EditTextPreference) findPreference("eotrackme_device_id");
        DeviceUuidFactory DUID = new DeviceUuidFactory(getApplicationContext());
        txtEOTrackMeDeviceId.setText(DUID.getDeviceUuid().toString());
       
            
    }


    private final Runnable updateResults = new Runnable()
    {
        public void run()
        {
            finish();

            startActivity(getIntent());
        }

    };


    /**
     * Opens the Android Location preferences screen
     */
    private class AndroidLocationPreferenceClickListener implements OnPreferenceClickListener
    {
        public boolean onPreferenceClick(Preference preference)
        {
            startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
            return true;
        }
    }


    private class ImperialPreferenceChangeListener implements Preference.OnPreferenceChangeListener
    {
        EditTextPreference distanceBeforeLogging;
        EditTextPreference accuracyBeforeLogging;
        SharedPreferences prefs;

        public ImperialPreferenceChangeListener(SharedPreferences prefs, EditTextPreference distanceBeforeLogging, EditTextPreference accuracyBeforeLogging)
        {
            this.prefs = prefs;
            this.distanceBeforeLogging = accuracyBeforeLogging;
            this.accuracyBeforeLogging = accuracyBeforeLogging;
        }

        public boolean onPreferenceChange(Preference preference, final Object newValue)
        {

            Utilities.ShowProgress(GpsSettingsActivity.this, getString(R.string.settings_converting_title),
                    getString(R.string.settings_converting_description));

            new Thread()
            {

                public void run()
                {

                    try
                    {
                        sleep(3000); // Give user time to read the message
                    }
                    catch (InterruptedException e)
                    {

                        Log.e("Settings", e.getMessage());

                    }

                    boolean useImp = Boolean.parseBoolean(newValue.toString());

                    String minimumDistanceString = prefs.getString("distance_before_logging", "0");
                    String minimumAccuracyString = prefs.getString("accuracy_before_logging", "0");
                    
                    int minimumDistance;

                    if (minimumDistanceString != null && minimumDistanceString.length() > 0)
                    {
                        minimumDistance = Integer.valueOf(minimumDistanceString);
                    }
                    else
                    {
                        minimumDistance = 0;
                    }
                    
                    int minimumAccuracy;

                    if (minimumAccuracyString != null && minimumAccuracyString.length() > 0)
                    {
                        minimumAccuracy = Integer.valueOf(minimumAccuracyString);
                    }
                    else
                    {
                        minimumAccuracy = 0;
                    }

                    SharedPreferences.Editor editor = prefs.edit();

                    if (useImp)
                    {
                        distanceBeforeLogging.setDialogTitle(R.string.settings_distance_in_feet);
                        distanceBeforeLogging.getEditText().setHint(R.string.settings_enter_feet);

                        minimumDistance = Utilities.MetersToFeet(minimumDistance);
                        
                        accuracyBeforeLogging.setDialogTitle(R.string.settings_accuracy_in_feet);
                        accuracyBeforeLogging.getEditText().setHint(R.string.settings_enter_feet);

                        minimumAccuracy = Utilities.MetersToFeet(minimumAccuracy);
                    }
                    else
                    {
                        minimumDistance = Utilities.FeetToMeters(minimumDistance);
                        distanceBeforeLogging.setDialogTitle(R.string.settings_distance_in_meters);
                        distanceBeforeLogging.getEditText().setHint(R.string.settings_enter_meters);
                                                    
                        minimumAccuracy = Utilities.FeetToMeters(minimumAccuracy);
                        accuracyBeforeLogging.setDialogTitle(R.string.settings_accuracy_in_meters);
                        accuracyBeforeLogging.getEditText().setHint(R.string.settings_enter_meters);
                        

                    }

                    if (minimumDistance >= 9999)
                    {
                        minimumDistance = 9999;
                    }
                    
                    if (minimumAccuracy >= 9999)
                    {
                        minimumAccuracy = 9999;
                    }

                    editor.putString("distance_before_logging", String.valueOf(minimumDistance));
                    
                    editor.putString("accuracy_before_logging", String.valueOf(minimumAccuracy));
                    editor.commit();

                    handler.post(updateResults);
                    Utilities.HideProgress();
                }
            }.start();

            return true;
        }

    }
 
    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        Utilities.LogDebug("GpsSettingsActivity.onWindowFocusChanged");
        if (hasFocus)
        {

       
        }
    }
}
