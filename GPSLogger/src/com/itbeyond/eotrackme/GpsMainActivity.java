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

//TODO: Move GPSMain email now call to gpsmain to allow closing of progress bar

package com.itbeyond.eotrackme;


import android.app.Activity;

import android.content.*;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.view.*;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.itbeyond.eotrackme.R;
import com.itbeyond.eotrackme.common.AppSettings;
import com.itbeyond.eotrackme.common.IActionListener;
import com.itbeyond.eotrackme.common.Session;
import com.itbeyond.eotrackme.common.Utilities;
import com.itbeyond.eotrackme.loggers.FileLoggerFactory;
import com.itbeyond.eotrackme.loggers.IFileLogger;

import java.util.*;

public class GpsMainActivity extends Activity implements OnCheckedChangeListener,
        IGpsLoggerServiceClient, View.OnClickListener, IActionListener
{

    /**
     * General all purpose handler used for updating the UI from threads.
     */
    private static Intent serviceIntent;
    private GpsLoggingService loggingService;

    /**
     * Provides a connection to the GPS Logging Service
     */
    private final ServiceConnection gpsServiceConnection = new ServiceConnection()
    {

        public void onServiceDisconnected(ComponentName name)
        {
            loggingService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service)
        {
            loggingService = ((GpsLoggingService.GpsLoggingBinder) service).getService();
            GpsLoggingService.SetServiceClient(GpsMainActivity.this);


            Button buttonSinglePoint = (Button) findViewById(R.id.buttonSinglePoint);

            buttonSinglePoint.setOnClickListener(GpsMainActivity.this);

            if (Session.isStarted())
            {
                if (Session.isSinglePointMode())
                {
                    SetMainButtonEnabled(false);
                }
                else
                {
                    SetMainButtonChecked(true);
                    SetSinglePointButtonEnabled(false);
                }

                DisplayLocationInfo(Session.getCurrentLocationInfo());
            }

            // Form setup - toggle button, display existing location info
            ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
            buttonOnOff.setOnCheckedChangeListener(GpsMainActivity.this);
        }
    };


    /**
     * Event raised when the form is created for the first time
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        Utilities.LogDebug("GpsMainActivity.onCreate");

        super.onCreate(savedInstanceState);

        Utilities.LogInfo("GPSLogger started");

        setContentView(R.layout.main);

        // Moved to onResume to update the list of loggers
        //GetPreferences();

        StartAndBindService();
    }

    @Override
    protected void onStart()
    {
        Utilities.LogDebug("GpsMainActivity.onStart");
        super.onStart();
        StartAndBindService();
    }

    @Override
    protected void onResume()
    {
        Utilities.LogDebug("GpsMainactivity.onResume");
        super.onResume();
        GetPreferences();
        StartAndBindService();
    }

    /**
     * Starts the service and binds the activity to it.
     */
    private void StartAndBindService()
    {
        Utilities.LogDebug("StartAndBindService - binding now");
        serviceIntent = new Intent(this, GpsLoggingService.class);
        // Start the service in case it isn't already running
        startService(serviceIntent);
        // Now bind to service
        bindService(serviceIntent, gpsServiceConnection, Context.BIND_AUTO_CREATE);
        Session.setBoundToService(true);
    }


    /**
     * Stops the service if it isn't logging. Also unbinds.
     */
    private void StopAndUnbindServiceIfRequired()
    {
        Utilities.LogDebug("GpsMainActivity.StopAndUnbindServiceIfRequired");
        if (Session.isBoundToService())
        {
            unbindService(gpsServiceConnection);
            Session.setBoundToService(false);
        }

        if (!Session.isStarted())
        {
            Utilities.LogDebug("StopServiceIfRequired - Stopping the service");
            //serviceIntent = new Intent(this, GpsLoggingService.class);
            stopService(serviceIntent);
        }

    }

    @Override
    protected void onPause()
    {

        Utilities.LogDebug("GpsMainActivity.onPause");
        StopAndUnbindServiceIfRequired();
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {

        Utilities.LogDebug("GpsMainActivity.onDestroy");
        StopAndUnbindServiceIfRequired();
        super.onDestroy();

    }

    /**
     * Called when the toggle button is clicked
     */
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        Utilities.LogDebug("GpsMainActivity.onCheckedChanged");

        if (isChecked)
        {
            GetPreferences();
            SetSinglePointButtonEnabled(false);
//            loggingService.SetupAutoSendTimers();
            loggingService.StartLogging();
        }
        else
        {
            SetSinglePointButtonEnabled(true);
            loggingService.StopLogging();
        }
    }

    /**
     * Called when the single point button is clicked
     */
    public void onClick(View view)
    {
        Utilities.LogDebug("GpsMainActivity.onClick");

        if (!Session.isStarted())
        {
            SetMainButtonEnabled(false);
            loggingService.StartLogging();
            Session.setSinglePointMode(true);
        }
        else if (Session.isStarted() && Session.isSinglePointMode())
        {
            loggingService.StopLogging();
            SetMainButtonEnabled(true);
            Session.setSinglePointMode(false);
        }
    }


    public void SetSinglePointButtonEnabled(boolean enabled)
    {
        Button buttonSinglePoint = (Button) findViewById(R.id.buttonSinglePoint);
        buttonSinglePoint.setEnabled(enabled);
    }

    public void SetMainButtonEnabled(boolean enabled)
    {
        ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
        buttonOnOff.setEnabled(enabled);
    }

    public void SetMainButtonChecked(boolean checked)
    {
        ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
        buttonOnOff.setChecked(checked);
    }

    /**
     * Gets preferences chosen by the user
     */
    private void GetPreferences()
    {
        Utilities.PopulateAppSettings(getApplicationContext());
        ShowPreferencesSummary();
    }

    /**
     * Displays a human readable summary of the preferences chosen by the user
     * on the main form
     */
    private void ShowPreferencesSummary()
    {
        Utilities.LogDebug("GpsMainActivity.ShowPreferencesSummary");
        try
        {
            TextView txtLoggingTo = (TextView) findViewById(R.id.txtLoggingTo);
            TextView txtFrequency = (TextView) findViewById(R.id.txtFrequency);
            TextView txtDistance = (TextView) findViewById(R.id.txtDistance);

            List<IFileLogger> loggers = FileLoggerFactory.GetFileLoggers();

            if (loggers.size() > 0)
            {

                ListIterator<IFileLogger> li = loggers.listIterator();
                String logTo = li.next().getName();
                while (li.hasNext())
                {
                    logTo += ", " + li.next().getName();
                }
                txtLoggingTo.setText(logTo);

            }
            else
            {

                txtLoggingTo.setText(R.string.summary_loggingto_screen);

            }

            if (AppSettings.getMinimumSeconds() > 0)
            {
                String descriptiveTime = Utilities.GetDescriptiveTimeString(AppSettings.getMinimumSeconds(),
                        getApplicationContext());

                txtFrequency.setText(descriptiveTime);
            }
            else
            {
                txtFrequency.setText(R.string.summary_freq_max);

            }


            if (AppSettings.getMinimumDistanceInMeters() > 0)
            {
                if (AppSettings.shouldUseImperial())
                {
                    int minimumDistanceInFeet = Utilities.MetersToFeet(AppSettings.getMinimumDistanceInMeters());
                    txtDistance.setText(((minimumDistanceInFeet == 1)
                            ? getString(R.string.foot)
                            : String.valueOf(minimumDistanceInFeet) + getString(R.string.feet)));
                }
                else
                {
                    txtDistance.setText(((AppSettings.getMinimumDistanceInMeters() == 1)
                            ? getString(R.string.meter)
                            : String.valueOf(AppSettings.getMinimumDistanceInMeters()) + getString(R.string.meters)));
                }
            }
            else
            {
                txtDistance.setText(R.string.summary_dist_regardless);
            }


   /*         if (AppSettings.isAutoSendEnabled())
            {
                String autoEmailResx;

                if (AppSettings.getAutoSendDelay() == 0)
                {
                    autoEmailResx = "autoemail_frequency_whenistop";
                }
                else
                {

                    autoEmailResx = "autoemail_frequency_"
                            + String.valueOf(AppSettings.getAutoSendDelay()).replace(".", "");
                }

                String autoEmailDesc = getString(getResources().getIdentifier(autoEmailResx, "string", getPackageName()));

                txtAutoEmail.setText(autoEmailDesc);
            }
            else
            {
                TableRow trAutoEmail = (TableRow) findViewById(R.id.trAutoEmail);
                trAutoEmail.setVisibility(View.INVISIBLE);
            }
*/
            onFileName(Session.getCurrentFileName());
        }
        catch (Exception ex)
        {
            Utilities.LogError("ShowPreferencesSummary", ex);
        }


    }

    /**
     * Handles the hardware back-button press
     */
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        Utilities.LogInfo("KeyDown - " + String.valueOf(keyCode));

        if (keyCode == KeyEvent.KEYCODE_BACK && Session.isBoundToService())
        {
            StopAndUnbindServiceIfRequired();
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Called when the menu is created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.optionsmenu, menu);

        return true;

    }

    /**
     * Called when one of the menu items is selected.
     */
    public boolean onOptionsItemSelected(MenuItem item)
    {

        int itemId = item.getItemId();
        Utilities.LogInfo("Option item selected - " + String.valueOf(item.getTitle()));

        switch (itemId)
        {
            case R.id.mnuSettings:
                Intent settingsActivity = new Intent(getApplicationContext(), GpsSettingsActivity.class);
                startActivity(settingsActivity);
                break;

            case R.id.mnuExit:
                loggingService.StopLogging();
                loggingService.stopSelf();
                finish();
                break;
        }
        return false;
    }


    /**
     * Clears the table, removes all values.
     */
    public void ClearForm()
    {

        Utilities.LogDebug("GpsMainActivity.ClearForm");

        TextView tvLatitude = (TextView) findViewById(R.id.txtLatitude);
        TextView tvLongitude = (TextView) findViewById(R.id.txtLongitude);
        TextView tvDateTime = (TextView) findViewById(R.id.txtDateTimeAndProvider);

        TextView tvAltitude = (TextView) findViewById(R.id.txtAltitude);

        TextView txtSpeed = (TextView) findViewById(R.id.txtSpeed);

        TextView txtSatellites = (TextView) findViewById(R.id.txtSatellites);
        TextView txtDirection = (TextView) findViewById(R.id.txtDirection);
        TextView txtAccuracy = (TextView) findViewById(R.id.txtAccuracy);
        TextView txtDistance = (TextView) findViewById(R.id.txtDistanceTravelled);

        TextView txtEOTrackMe = (TextView) findViewById(R.id.txtEOTrackMeStatus);
        
        tvLatitude.setText("");
        tvLongitude.setText("");
        tvDateTime.setText("");
        tvAltitude.setText("");
        txtSpeed.setText("");
        txtSatellites.setText("");
        txtDirection.setText("");
        txtAccuracy.setText("");
        txtDistance.setText("");
        txtEOTrackMe.setText("");
        Session.setPreviousLocationInfo(null);
        Session.setTotalTravelled(0d);
    }

    public void OnStopLogging()
    {
        Utilities.LogDebug("GpsMainActivity.OnStopLogging");
        SetMainButtonChecked(false);
    }

    /**
     * Sets the message in the top status label.
     *
     * @param message The status message
     */
    private void SetStatus(String message)
    {
        Utilities.LogDebug("GpsMainActivity.SetStatus: " + message);
        TextView tvStatus = (TextView) findViewById(R.id.textStatus);
        tvStatus.setText(message);
        Utilities.LogInfo(message);
    }
    
    /**
     * Sets the EOTrackMe Status message.
     *
     * @param message The status message
     */
    protected void SetEOTrackMeStatus(String message)
    {
        Utilities.LogDebug("GpsMainActivity.SetEOTrackMeStatus: " + message);
        TextView EOStatus = (TextView) findViewById(R.id.txtEOTrackMeStatus);
        EOStatus.setText(message);
        Utilities.LogInfo(message);
    }
    
    

    /**
     * Sets the number of satellites in the satellite row in the table.
     *
     * @param number The number of satellites
     */
    private void SetSatelliteInfo(int number)
    {
        Session.setSatelliteCount(number);
        TextView txtSatellites = (TextView) findViewById(R.id.txtSatellites);
        txtSatellites.setText(String.valueOf(number));
    }

    /**
     * Given a location fix, processes it and displays it in the table on the
     * form.
     *
     * @param loc Location information
     */
    private void DisplayLocationInfo(Location loc)
    {
        Utilities.LogDebug("GpsMainActivity.DisplayLocationInfo");
        try
        {

            if (loc == null)
            {
                return;
            }

            TextView tvLatitude = (TextView) findViewById(R.id.txtLatitude);
            TextView tvLongitude = (TextView) findViewById(R.id.txtLongitude);
            TextView tvDateTime = (TextView) findViewById(R.id.txtDateTimeAndProvider);

            TextView tvAltitude = (TextView) findViewById(R.id.txtAltitude);

            TextView txtSpeed = (TextView) findViewById(R.id.txtSpeed);

            TextView txtSatellites = (TextView) findViewById(R.id.txtSatellites);
            TextView txtDirection = (TextView) findViewById(R.id.txtDirection);
            TextView txtAccuracy = (TextView) findViewById(R.id.txtAccuracy);
            TextView txtTravelled = (TextView) findViewById(R.id.txtDistanceTravelled);
            String providerName = loc.getProvider();

            if (providerName.equalsIgnoreCase("gps"))
            {
                providerName = getString(R.string.providername_gps);
            }
            else
            {
                providerName = getString(R.string.providername_celltower);
            }

            tvDateTime.setText(new Date(Session.getLatestTimeStamp()).toLocaleString()
                    + getString(R.string.providername_using, providerName));
            tvLatitude.setText(String.valueOf(loc.getLatitude()));
            tvLongitude.setText(String.valueOf(loc.getLongitude()));

            if (loc.hasAltitude())
            {

                double altitude = loc.getAltitude();

                if (AppSettings.shouldUseImperial())
                {
                    tvAltitude.setText(String.valueOf(Utilities.MetersToFeet(altitude))
                            + getString(R.string.feet));
                }
                else
                {
                    tvAltitude.setText(String.valueOf(altitude) + getString(R.string.meters));
                }

            }
            else
            {
                tvAltitude.setText(R.string.not_applicable);
            }

            if (loc.hasSpeed())
            {

                float speed = loc.getSpeed();
                String unit;
                if (AppSettings.shouldUseImperial())
                {
                    if (speed > 1.47)
                    {
                        speed = speed * 0.6818f;
                        unit = getString(R.string.miles_per_hour);

                    }
                    else
                    {
                        speed = Utilities.MetersToFeet(speed);
                        unit = getString(R.string.feet_per_second);
                    }
                }
                else
                {
                    if (speed > 0.277)
                    {
                        speed = speed * 3.6f;
                        unit = getString(R.string.kilometers_per_hour);
                    }
                    else
                    {
                        unit = getString(R.string.meters_per_second);
                    }
                }

                txtSpeed.setText(String.valueOf(speed) + unit);

            }
            else
            {
                txtSpeed.setText(R.string.not_applicable);
            }

            if (loc.hasBearing())
            {

                float bearingDegrees = loc.getBearing();
                String direction;

                direction = Utilities.GetBearingDescription(bearingDegrees, getApplicationContext());

                txtDirection.setText(direction + "(" + String.valueOf(Math.round(bearingDegrees))
                        + getString(R.string.degree_symbol) + ")");
            }
            else
            {
                txtDirection.setText(R.string.not_applicable);
            }

            if (!Session.isUsingGps())
            {
                txtSatellites.setText(R.string.not_applicable);
                Session.setSatelliteCount(0);
            }

            if (loc.hasAccuracy())
            {

                float accuracy = loc.getAccuracy();

                if (AppSettings.shouldUseImperial())
                {
                    txtAccuracy.setText(getString(R.string.accuracy_within,
                            String.valueOf(Utilities.MetersToFeet(accuracy)), getString(R.string.feet)));

                }
                else
                {
                    txtAccuracy.setText(getString(R.string.accuracy_within, String.valueOf(accuracy),
                            getString(R.string.meters)));
                }

            }
            else
            {
                txtAccuracy.setText(R.string.not_applicable);
            }


            String distanceUnit;
            double distanceValue = Session.getTotalTravelled();
            if (AppSettings.shouldUseImperial())
            {
                distanceUnit = getString(R.string.feet);
                distanceValue = Utilities.MetersToFeet(distanceValue);
                // When it passes more than 1 kilometer, convert to miles.
                if (distanceValue > 3281)
                {
                    distanceUnit = getString(R.string.miles);
                    distanceValue = distanceValue / 5280;
                }
            }
            else
            {
                distanceUnit = getString(R.string.meters);
                if (distanceValue > 1000)
                {
                    distanceUnit = getString(R.string.kilometers);
                    distanceValue = distanceValue / 1000;
                }
            }

            txtTravelled.setText(String.valueOf(Math.round(distanceValue)) + " " + distanceUnit +
                    " (" + (Session.getNumLegs() + 1) + " points)");

        }
        catch (Exception ex)
        {
            SetStatus(getString(R.string.error_displaying, ex.getMessage()));
        }

    }

    public void OnLocationUpdate(Location loc)
    {
        Utilities.LogDebug("GpsMainActivity.OnLocationUpdate");
        DisplayLocationInfo(loc);
        ShowPreferencesSummary();
        SetMainButtonChecked(true);

        if (Session.isSinglePointMode())
        {
            loggingService.StopLogging();
            SetMainButtonEnabled(true);
            Session.setSinglePointMode(false);
        }

    }

    public void OnSatelliteCount(int count)
    {
        SetSatelliteInfo(count);

    }


    public void OnStatusMessage(String message)
    {
        SetStatus(message);
    }
    
    public void OnEOTrackMeStatusMessage(String message)
    {
        SetEOTrackMeStatus(message);
    }

    public void OnFatalMessage(String message)
    {
        Utilities.MsgBox(getString(R.string.sorry), message, this);
    }

    public Activity GetActivity()
    {
        return this;
    }


    @Override
    public void OnComplete()
    {
        Utilities.HideProgress();
    }

    @Override
    public void OnFailure()
    {
        Utilities.HideProgress();
    }

	@Override
	public void onFileName(String newFileName) {
		// TODO Auto-generated method stub
		
	}
}
