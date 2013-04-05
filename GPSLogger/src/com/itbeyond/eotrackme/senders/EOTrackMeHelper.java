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

package com.itbeyond.eotrackme.senders;

import android.content.Context;

import com.itbeyond.common.EOTrackMe;
import com.itbeyond.common.EOTrackMeClient;
import com.itbeyond.eotrackme.common.AppSettings;
import com.itbeyond.eotrackme.common.IActionListener;
import com.itbeyond.eotrackme.common.Utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class EOTrackMeHelper implements IActionListener
{
    Context applicationContext;
    IActionListener callback;

    public EOTrackMeHelper(Context applicationContext, IActionListener callback)
    {
        this.applicationContext = applicationContext;
        this.callback = callback;
    }

    public void UploadFile()
    {
        Thread t = new Thread(new EOTrackMeHandler(applicationContext, this));
        t.start();
     }

    public void OnComplete()
    {
        callback.OnComplete();
    }

    public void OnFailure()
    {
    	
        callback.OnFailure();
    }

}

class EOTrackMeHandler implements Runnable
{

    Context applicationContext;
    final IActionListener helper;

    public EOTrackMeHandler(Context applicationContext, IActionListener helper)
    {
        this.applicationContext = applicationContext;
        this.helper = helper;
    }

    public void run()
    {
        try
        {

         // Read up to the first 500 lines from the LogFile and send
        	StringBuilder data = new StringBuilder();
        	String thisline = "";
        	try {
        	    BufferedReader br = new BufferedReader(new FileReader(EOTrackMe.getLogFile()), 8192);

        	    int lineCount = 0;
        	    while ((thisline = br.readLine()) != null && lineCount < 500) {
        	    	data.append("{" + thisline + "}|");
        	        lineCount++;
        	    }
        	    br.close();
        	}
        	catch (IOException e) {
        		Utilities.LogError("EOTrackMeHandler.run - ReadFile: ", e);
        		helper.OnFailure();
        	}
        	
            String server = "eotrackme.com"; //AppSettings.getOpenGTSServer();
            int port = 80; //Integer.parseInt(AppSettings.getOpenGTSServerPort());
            String path = "/track/position.aspx"; // AppSettings.getOpenGTSServerPath();
            String deviceId = AppSettings.getEOTrackMeDeviceId();
            String uid = AppSettings.getEOTrackMeUserId();
            
            EOTrackMeClient EOTrackMeClient = new EOTrackMeClient(server, port, path, helper, applicationContext);
            EOTrackMeClient.sendHTTP(deviceId, uid, data.toString());
            
        }
        catch (Exception e)
        {
            Utilities.LogError("EOTrackMeHandler.run", e);
            helper.OnFailure();
        }

    }



}