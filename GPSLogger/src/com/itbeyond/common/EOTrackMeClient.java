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

package com.itbeyond.common;


import android.content.Context;
import android.content.Intent;

import com.itbeyond.eotrackme.GpsLoggingService;
import com.itbeyond.eotrackme.common.IActionListener;
import com.itbeyond.eotrackme.common.Session;
import com.itbeyond.eotrackme.common.Utilities;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;




import org.apache.commons.lang.StringUtils;


/**
 * EOTrackMe Client
 *
 * @author David Martin <david @ itbeyond.com.au >
 */
public class EOTrackMeClient extends GpsLoggingService
{
	
    private Context applicationContext;
    private IActionListener callback;
    private String server;
    private Integer port;
    private String path;
    private String data;
    private AsyncHttpClient httpClient;
    private int sentLocationsCount = 0;


    public EOTrackMeClient(String server, Integer port, String path, IActionListener callback, Context applicationContext)
    {
        this.server = server;
        this.port = port;
        this.path = path;
        this.callback = callback;
        this.applicationContext = applicationContext;
    }

    public void sendHTTP(String id, String uid, String data)
    {
    	this.data = data;
        try
        {
            StringBuilder url = new StringBuilder();
            url.append("http://");
            url.append(getURL());

            httpClient = new AsyncHttpClient();
            httpClient.setUserAgent("EOTrackMe Driod");
            httpClient.setTimeout(120000);
            RequestParams params = new RequestParams();
            params.put("id", id);
            params.put("uid", uid);
			params.put("d", data);

			Utilities.LogDebug("Sending URL " + url + " with params " + params.toString());
			httpClient.post(applicationContext, url.toString(), params, new MyAsyncHttpResponseHandler(this));
        }
        catch (Exception e)
        {
            Utilities.LogError("EOTrackMeClient.sendHTTP", e);
            OnFailure();
        }
    }

    private String getURL()
    {
        StringBuilder url = new StringBuilder();
        url.append(server);
        if (port != null)
        {
            url.append(":");
            url.append(port);
        }
        if (path != null)
        {
            url.append(path);
        }
        return url.toString();
    }

    private class MyAsyncHttpResponseHandler extends AsyncHttpResponseHandler
    {
        private EOTrackMeClient callback;

        public MyAsyncHttpResponseHandler(EOTrackMeClient callback)
        {
            super();
            this.callback = callback;
        }

        @Override
        public void onSuccess(String response)
        {
            Utilities.LogInfo("Response Success :" + response);
            if (response.contains("ERROR")) {
            	updateStatus(response);
            	Session.setEOTrackMeError(response);
            } else {
            callback.OnCompleteLocation();
            }
        }
       
        @Override
        public void onFailure(Throwable e, String response)
        {
        	if (response != null) { updateStatus("ERROR: " + response);}
            Utilities.LogError("OnCompleteLocation.MyAsyncHttpResponseHandler Failure with response :" + response, new Exception(e));
            callback.OnFailure();
        }
    }

    public void OnCompleteLocation()
    {
       	EOTrackMe.removeSentLines(data);
        sentLocationsCount = StringUtils.countMatches(data, "|");
        Utilities.LogDebug("Sent locations count: " + sentLocationsCount);
        int toBeSentCount = EOTrackMe.getLogFileLines();

        updateStatus("Sent: " + sentLocationsCount + " Queue: " + toBeSentCount);

        OnComplete();
    }

    public void OnComplete()
    {
        callback.OnComplete();
    }

    public void OnFailure()
    {
        httpClient.cancelRequests(applicationContext, true);
        callback.OnFailure();
    }
    
    private void updateStatus(String msg) 
    {
        Intent serviceIntent = new Intent(applicationContext.getPackageName() + ".GpsLoggingService");
        serviceIntent.putExtra("EOTrackMeStatus", msg);
        // Start the service in case it isn't already running
        applicationContext.startService(serviceIntent);
    }
}
