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

//TODO: Simplify email logic (too many methods)
//TODO: Allow messages in IActionListener callback methods
//TODO: Handle case where a fix is not found and GPS gives up - restart alarm somehow?

package com.itbeyond.eotrackme;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;

import android.net.NetworkInfo;

import com.itbeyond.common.EOTrackMe;
import com.itbeyond.eotrackme.R;
import com.itbeyond.eotrackme.common.AppSettings;
import com.itbeyond.eotrackme.common.IActionListener;
import com.itbeyond.eotrackme.common.Session;
import com.itbeyond.eotrackme.common.Utilities;
import com.itbeyond.eotrackme.loggers.FileLoggerFactory;
import com.itbeyond.eotrackme.loggers.IFileLogger;
import com.itbeyond.eotrackme.senders.AlarmReceiver_EOTrackMe;
import com.itbeyond.eotrackme.senders.EOTrackMeHelper;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

@SuppressLint("SimpleDateFormat")
public class GpsLoggingService extends Service implements IActionListener
{
    private static NotificationManager gpsNotifyManager;
    private static int NOTIFICATION_ID = 8675309;

    private final IBinder mBinder = new GpsLoggingBinder();
    private static IGpsLoggerServiceClient mainServiceClient;

    // ---------------------------------------------------
    // Helpers and managers
    // ---------------------------------------------------
    private GeneralLocationListener gpsLocationListener;
    private GeneralLocationListener towerLocationListener;
    LocationManager gpsLocationManager;
    private LocationManager towerLocationManager;
    
    private Intent EOTrackMe_alarmIntent;
    private boolean EOTrackMe_Running = false;
    
    AlarmManager nextPointAlarmManager;
    // ---------------------------------------------------

    @Override
    public IBinder onBind(Intent arg0)
    {
        Utilities.LogDebug("GpsLoggingService.onBind");
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        Utilities.LogDebug("GpsLoggingService.onCreate");
        nextPointAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Utilities.LogInfo("GPSLoggerService created");
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        Utilities.LogDebug("GpsLoggingService.onStart");
        HandleIntent(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        Utilities.LogDebug("GpsLoggingService.onStartCommand");
        HandleIntent(intent);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy()
    {
        Utilities.LogWarning("GpsLoggingService is being destroyed by Android OS.");
        mainServiceClient = null;
        super.onDestroy();
    }

    @Override
    public void onLowMemory()
    {
        Utilities.LogWarning("Android is low on memory.");
        super.onLowMemory();
    }

    private void HandleIntent(Intent intent)
    {

        Utilities.LogDebug("GpsLoggingService.handleIntent");
        GetPreferences();

        Utilities.LogDebug("Null intent? " + String.valueOf(intent == null));

        if (intent != null)
        {
            Bundle bundle = intent.getExtras();

            if (bundle != null)
            {
                boolean stopRightNow = bundle.getBoolean("immediatestop");
                boolean startRightNow = bundle.getBoolean("immediate");
                boolean getNextPoint = bundle.getBoolean("getnextpoint");
                boolean EOTrackMeAlarm = bundle.getBoolean("EOTrackMeAlarm");
                String EOTrackMeStatus = bundle.getString("EOTrackMeStatus");
                
                if (startRightNow)
                {
                    Utilities.LogInfo("Auto starting logging");

                    StartLogging();
                }

                if (stopRightNow)
                {
                    Utilities.LogInfo("Auto stop logging");
                    StopLogging();
                }

                if (getNextPoint && Session.isStarted())
                {
                    Utilities.LogDebug("HandleIntent - getNextPoint");
                    StartGpsManager();
                }
                
                if (EOTrackMeAlarm)
                {
                	SendEOTrackMe();
                }
                if (String.valueOf(EOTrackMeStatus) != null);
                {
                 SetEOTrackMeStatus(String.valueOf(EOTrackMeStatus));
                }
            }
        }
        else
        {
            // A null intent is passed in if the service has been killed and
            // restarted.
            Utilities.LogDebug("Service restarted with null intent. Start logging.");
            StartLogging();

        }
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

    /**
     * Can be used from calling classes as the go-between for methods and
     * properties.
     */
    public class GpsLoggingBinder extends Binder
    {
        public GpsLoggingService getService()
        {
            Utilities.LogDebug("GpsLoggingBinder.getService");
            return GpsLoggingService.this;
        }
    }
   
    public void SetupEOTrackMeSendTimers()
    {
        Utilities.LogDebug("EOTrackMeEnabled - " + String.valueOf(AppSettings.getEOTrackMeEnabled()));     
        if (AppSettings.getEOTrackMeEnabled())
        {           
            Utilities.LogDebug("GpsLoggingService.SetAlarmForEOTrackMe");
            EOTrackMe_Running = true;           
            EOTrackMe_alarmIntent = new Intent(getApplicationContext(), AlarmReceiver_EOTrackMe.class);
            
            AlarmManager ea = (AlarmManager) getSystemService(ALARM_SERVICE);
            PendingIntent mi = PendingIntent.getBroadcast(this, 0, EOTrackMe_alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            ea.cancel(mi);

            Utilities.LogDebug("New EOTrackMe alarm intent");
            ea.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + (60 * 2 * 1000), mi);
  //          ea.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + (120000), mi);

            Utilities.LogDebug("EOTrackMe Alarm has been set");    	       	
        }
        else
        {
            Utilities.LogDebug("Checking if EOTrackMe alarmIntent is null");
            if (EOTrackMe_alarmIntent != null)
            {
                Utilities.LogDebug("EOTrackMe alarmIntent was null, canceling alarm");
                CancelAlarm_EOTrackMe();
            }
        }
    }

    private void CancelAlarm_EOTrackMe()
    {
        if (EOTrackMe_alarmIntent != null)
        {
            Utilities.LogDebug("GpsLoggingService.CancelAlarmForEOTrackMe");
            AlarmManager ea = (AlarmManager) getSystemService(ALARM_SERVICE);
            PendingIntent mi = PendingIntent.getBroadcast(this, 0, EOTrackMe_alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            EOTrackMe_Running = false;
            Utilities.LogDebug("Pending alarm intent was null? " + String.valueOf(mi == null));
            ea.cancel(mi);
        }
    }
    
    /**
     * Calls the EOTrackMe Helper which processes the file and sends it.
     */
    private void SendEOTrackMe()
    {
        boolean isOnline = false;
        Utilities.LogDebug("GpsLoggingService.SendEOTrackMe");

        // Check if we are online or not
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && (cm.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) ||(cm.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED)){ 
            isOnline= true;
        }
        
        // Check the server log file has data to be sent
        if (EOTrackMe.getLogFileLines() > 0 && isOnline && Session.getEOTrackMeError().length() == 0)
        {
            Utilities.LogInfo("Sending EOTrackMe Log File");
            SetEOTrackMeStatus("Sending Locations");
            EOTrackMeHelper EOTrackMeHelper = new EOTrackMeHelper(getApplicationContext(), this);
            EOTrackMeHelper.UploadFile();
        }
        
        if (Session.getEOTrackMeError() != "" && Session.getEOTrackMeError() != null) {
        	Utilities.LogWarning(Session.getEOTrackMeError());
        	SetEOTrackMeStatus(Session.getEOTrackMeError()); return;
        } 
        
        SetupEOTrackMeSendTimers();
    }
    

    /**
     * Sets the activity form for this service. The activity form needs to
     * implement IGpsLoggerServiceClient.
     *
     * @param mainForm The calling client
     */
    protected static void SetServiceClient(IGpsLoggerServiceClient mainForm)
    {
        mainServiceClient = mainForm;
    }

    /**
     * Gets preferences chosen by the user and populates the AppSettings object.
     * Also sets up email timers if required.
     */
    private void GetPreferences()
    {
        Utilities.LogDebug("GpsLoggingService.GetPreferences");
        Utilities.PopulateAppSettings(getApplicationContext());
        
    	if (!AppSettings.getEOTrackMeEnabled()) {
    		SetEOTrackMeStatus("Enter your MemberID in Settings");
    		return;
        }
        
        if (AppSettings.getEOTrackMeEnabled() && !EOTrackMe_Running )
        {
        	SendEOTrackMe();
        }

    }

    /**
     * Resets the form, resets file name if required, reobtains preferences
     */
    protected void StartLogging()
    {    

        Utilities.LogDebug("GpsLoggingService.StartLogging");
        Session.setAddNewTrackSegment(true);
      
        if (Session.isStarted())
        {
            return;
        }
        Utilities.LogInfo("Starting logging procedures");
        try
        {
            startForeground(NOTIFICATION_ID, new Notification());
        }
        catch (Exception ex)
        {
            System.out.print(ex.getMessage());
        }
        GetPreferences();
        
        Session.setStarted(true);
        Session.setEOTrackMeError("");

        Notify();
        //(true);
        ClearForm();
        StartGpsManager();

    }

    /**
     * Asks the main service client to clear its form.
     */
    private void ClearForm()
    {
        if (IsMainFormVisible())
        {
            mainServiceClient.ClearForm();
        }
    }

    /**
     * Stops logging, removes notification, stops GPS manager, stops email timer
     */
    public void StopLogging()
    {
        Utilities.LogDebug("GpsLoggingService.StopLogging");
        Session.setAddNewTrackSegment(true);

        Utilities.LogInfo("Stopping logging");
        Session.setStarted(false);
        // Email log file before setting location info to null
//        AutoSendLogFileOnStop();
//        CancelAlarm();
        CancelAlarm_EOTrackMe();
        Session.setCurrentLocationInfo(null);
        stopForeground(true);

        RemoveNotification();
        StopAlarm();
        StopGpsManager(false);
        StopMainActivity();
    }

    /**
     * Manages the notification in the status bar
     */
    private void Notify()
    {

        Utilities.LogDebug("GpsLoggingService.Notify");
        if (AppSettings.shouldShowInNotificationBar())
        {
            gpsNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            ShowNotification();
        }
        else
        {
            RemoveNotification();
        }
    }

    /**
     * Hides the notification icon in the status bar if it's visible.
     */
    private void RemoveNotification()
    {
        Utilities.LogDebug("GpsLoggingService.RemoveNotification");
        try
        {
            if (Session.isNotificationVisible())
            {
                gpsNotifyManager.cancelAll();
            }
        }
        catch (Exception ex)
        {
            Utilities.LogError("RemoveNotification", ex);
        }
        finally
        {
            Session.setNotificationVisible(false);
        }
    }

    /**
     * Shows a notification icon in the status bar for GPS Logger
     */
    private void ShowNotification()
    {
        Utilities.LogDebug("GpsLoggingService.ShowNotification");
        // What happens when the notification item is clicked
        Intent contentIntent = new Intent(this, GpsMainActivity.class);

        PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, contentIntent,
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

        Notification nfc = new Notification(R.drawable.logo_ani, null, System.currentTimeMillis());
        nfc.flags |= Notification.FLAG_ONGOING_EVENT;

        NumberFormat nf = new DecimalFormat("###.######");

        String contentText = getString(R.string.gpslogger_still_running);
        if (Session.hasValidLocation())
        {
            contentText = nf.format(Session.getCurrentLatitude()) + ","
                    + nf.format(Session.getCurrentLongitude());
        }

        nfc.setLatestEventInfo(getApplicationContext(), getString(R.string.gpslogger_still_running),
                contentText, pending);

        gpsNotifyManager.notify(NOTIFICATION_ID, nfc);
        Session.setNotificationVisible(true);
    }

    /**
     * Starts the location manager. There are two location managers - GPS and
     * Cell Tower. This code determines which manager to request updates from
     * based on user preference and whichever is enabled. If GPS is enabled on
     * the phone, that is used. But if the user has also specified that they
     * prefer cell towers, then cell towers are used. If neither is enabled,
     * then nothing is requested.
     */
    private void StartGpsManager()
    {
        Utilities.LogDebug("GpsLoggingService.StartGpsManager");

        GetPreferences();

        if (gpsLocationListener == null)
        {
            gpsLocationListener = new GeneralLocationListener(this);
        }

        if (towerLocationListener == null)
        {
            towerLocationListener = new GeneralLocationListener(this);
        }


        gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        towerLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        CheckTowerAndGpsStatus();

        if (Session.isGpsEnabled() && !AppSettings.shouldPreferCellTower())
        {
            Utilities.LogInfo("Requesting GPS location updates");
            // gps satellite based
            gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000, 0,
                    gpsLocationListener);

            gpsLocationManager.addGpsStatusListener(gpsLocationListener);

            Session.setUsingGps(true);
        }
        else if (Session.isTowerEnabled())
        {
            Utilities.LogInfo("Requesting tower location updates");
            Session.setUsingGps(false);
            // Cell tower and wifi based
            towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    1000, 0,
                    towerLocationListener);

        }
        else
        {
            Utilities.LogInfo("No provider available");
            Session.setUsingGps(false);
            SetStatus(R.string.gpsprovider_unavailable);
            SetFatalMessage(R.string.gpsprovider_unavailable);
            StopLogging();
            return;
        }

        SetStatus(R.string.started_waiting);
    }

    /**
     * This method is called periodically to determine whether the cell tower /
     * gps providers have been enabled, and sets class level variables to those
     * values.
     */
    private void CheckTowerAndGpsStatus()
    {
    	Session.setTowerEnabled(towerLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        Session.setGpsEnabled(gpsLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
     }
    /**
     * Stops the location managers
     * @param <pause>
     */
    private void StopGpsManager(boolean pause)
    {

        Utilities.LogDebug("GpsLoggingService.StopGpsManager");

        if (towerLocationListener != null)
        {
            Utilities.LogDebug("Removing towerLocationManager updates");
            towerLocationManager.removeUpdates(towerLocationListener);
        }

        if (gpsLocationListener != null)
        {
            Utilities.LogDebug("Removing gpsLocationManager updates");
            gpsLocationManager.removeUpdates(gpsLocationListener);
            gpsLocationManager.removeGpsStatusListener(gpsLocationListener);
        }
        if (pause) {SetStatus(getString(R.string.gps_stopped)); } else {SetStatus(getString(R.string.stopped)); }
    }

    /**
     * Sets the current file name based on user preference.
    
    @SuppressLint("SimpleDateFormat")
	private void ResetCurrentFileName(boolean newStart)
    {

        Utilities.LogDebug("GpsLoggingService.ResetCurrentFileName");

        String newFileName = Session.getCurrentFileName();
        if (AppSettings.shouldCreateNewFileOnceADay())
        {
            // 20100114.gpx
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            newFileName = sdf.format(new Date());
            Session.setCurrentFileName(newFileName);
        }
        else if (newStart)
        {
            // 20100114183329.gpx
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            newFileName = sdf.format(new Date());
            Session.setCurrentFileName(newFileName);
        }

        if (IsMainFormVisible())
        {
            mainServiceClient.onFileName(newFileName);
        }

    }
 */
    /**
     * Gives a status message to the main service client to display
     *
     * @param status The status message
     */
    void SetStatus(String status)
    {
        if (IsMainFormVisible())
        {
            mainServiceClient.OnStatusMessage(status);
        }
    }
    
    /**
     * Gives an EOTrackMe status message to the main service client to display
     *
     * @param status The status message
     */
    void SetEOTrackMeStatus(String status)
    {
        if (IsMainFormVisible() && status != "null")
        {
            mainServiceClient.OnEOTrackMeStatusMessage(status);
        }
    }

    /**
     * Gives an error message to the main service client to display
     *
     * @param messageId ID of string to lookup
     */
    void SetFatalMessage(int messageId)
    {
        if (IsMainFormVisible())
        {
            mainServiceClient.OnFatalMessage(getString(messageId));
        }
    }

    /**
     * Gets string from given resource ID, passes to SetStatus(String)
     *
     * @param stringId ID of string to lookup
     */
    private void SetStatus(int stringId)
    {
        String s = getString(stringId);
        SetStatus(s);
    }
    
    /**
     * Notifies main form that logging has stopped
     */
    void StopMainActivity()
    {
        if (IsMainFormVisible())
        {
            mainServiceClient.OnStopLogging();
        }
    }


    /**
     * Stops location manager, then starts it.
     */
    void RestartGpsManagers()
    {
        Utilities.LogDebug("GpsLoggingService.RestartGpsManagers");
        StopGpsManager(false);
        StartGpsManager();
    }


    /**
     * This event is raised when the GeneralLocationListener has a new location.
     * This method in turn updates notification, writes to file, reobtains
     * preferences, notifies main service client and resets location managers.
     *
     * @param loc Location object
     */
    void OnLocationChanged(Location loc)
    {
        int retryTimeout = Session.getRetryTimeout();

        if (!Session.isStarted())
        {
            Utilities.LogDebug("OnLocationChanged called, but Session.isStarted is false");
            StopLogging();
            return;
        }

        Utilities.LogDebug("GpsLoggingService.OnLocationChanged");


        long currentTimeStamp = System.currentTimeMillis();

        // Wait some time even on 0 frequency so that the UI doesn't lock up

        if ((currentTimeStamp - Session.getLatestTimeStamp()) < 1000)
        {
            return;
        }

        // Don't do anything until the user-defined time has elapsed
        if ((currentTimeStamp - Session.getLatestTimeStamp()) < (AppSettings.getMinimumSeconds() * 1000))
        {
            return;
        }

        // Don't do anything until the user-defined accuracy is reached
        if (AppSettings.getMinimumAccuracyInMeters() > 0)
        {
          if(AppSettings.getMinimumAccuracyInMeters() < Math.abs(loc.getAccuracy()))
            {
                if(retryTimeout < 50)
                {
                    Session.setRetryTimeout(retryTimeout+1);
                    SetStatus("Only accuracy of " + String.valueOf(Math.floor(loc.getAccuracy())) + " reached");
                    StopManagerAndResetAlarm(AppSettings.getRetryInterval());
                    return;
                }
                else
                {
                    Session.setRetryTimeout(0);
                    SetStatus("Only accuracy of " + String.valueOf(Math.floor(loc.getAccuracy())) + " reached and timeout reached");
                    StopManagerAndResetAlarm();
                    return;
                }
            }
        }

        //Don't do anything until the user-defined distance has been traversed
        if (AppSettings.getMinimumDistanceInMeters() > 0 && Session.hasValidLocation())
        {

            double distanceTraveled = Utilities.CalculateDistance(loc.getLatitude(), loc.getLongitude(),
                    Session.getCurrentLatitude(), Session.getCurrentLongitude());

            if (AppSettings.getMinimumDistanceInMeters() > distanceTraveled)
            {
                SetStatus("Only " + String.valueOf(Math.floor(distanceTraveled)) + " m traveled.");
                StopManagerAndResetAlarm();
                return;
            }

        }


        Utilities.LogInfo("New location obtained");
//       ResetCurrentFileName(false);
        Session.setLatestTimeStamp(System.currentTimeMillis());
        Session.setCurrentLocationInfo(loc);
        SetDistanceTraveled(loc);
        Notify();
        WriteToFile(loc);
        GetPreferences();
        StopManagerAndResetAlarm();

        if (IsMainFormVisible())
        {
            mainServiceClient.OnLocationUpdate(loc);
        }
    }

    private void SetDistanceTraveled(Location loc)
    {
        // Distance
        if (Session.getPreviousLocationInfo() == null)
        {
            Session.setPreviousLocationInfo(loc);
        }
        // Calculate this location and the previous location location and add to the current running total distance.
        // NOTE: Should be used in conjunction with 'distance required before logging' for more realistic values.
        double distance = Utilities.CalculateDistance(
                Session.getPreviousLatitude(),
                Session.getPreviousLongitude(),
                loc.getLatitude(),
                loc.getLongitude());
        Session.setPreviousLocationInfo(loc);
        Session.setTotalTravelled(Session.getTotalTravelled() + distance);
    }

    protected void StopManagerAndResetAlarm()
    {
        Utilities.LogDebug("GpsLoggingService.StopManagerAndResetAlarm");
        StopGpsManager(true);
        SetAlarmForNextPoint();
    }

    protected void StopManagerAndResetAlarm(int retryInterval)
    {
        Utilities.LogDebug("GpsLoggingService.StopManagerAndResetAlarm_retryInterval");
        StopGpsManager(true);
        SetAlarmForNextPoint(retryInterval);
    }

    private void StopAlarm()
    {
        Utilities.LogDebug("GpsLoggingService.StopAlarm");
        Intent i = new Intent(this, GpsLoggingService.class);
        i.putExtra("getnextpoint", true);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);
    }


    private void SetAlarmForNextPoint()
    {

        Utilities.LogDebug("GpsLoggingService.SetAlarmForNextPoint");

        Intent i = new Intent(this, GpsLoggingService.class);

        i.putExtra("getnextpoint", true);

        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);

        nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AppSettings.getMinimumSeconds() * 1000, pi);

    }

    private void SetAlarmForNextPoint(int retryInterval)
    {

        Utilities.LogDebug("GpsLoggingService.SetAlarmForNextPoint_retryInterval");

        Intent i = new Intent(this, GpsLoggingService.class);

        i.putExtra("getnextpoint", true);

        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);

        nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + retryInterval * 1000, pi);

    }

    /**
     * Calls file helper to write a given location to a file.
     *
     * @param loc Location object
     */
    private void WriteToFile(Location loc)
    {
        Utilities.LogDebug("GpsLoggingService.WriteToFile");
        List<IFileLogger> loggers = FileLoggerFactory.GetFileLoggers();
        Session.setAddNewTrackSegment(false);

        for (IFileLogger logger : loggers)
        {
            try
            {
                logger.Write(loc);
                Session.setAllowDescription(true);
            }
            catch (Exception e)
            {
                SetStatus(R.string.could_not_write_to_file);
            }
        }

    }

    /**
     * Informs the main service client of the number of visible satellites.
     *
     * @param count Number of Satellites
     */
    void SetSatelliteInfo(int count)
    {
        if (IsMainFormVisible())
        {
            mainServiceClient.OnSatelliteCount(count);
        }
    }


    private boolean IsMainFormVisible()
    {
        return mainServiceClient != null;
    }


}
