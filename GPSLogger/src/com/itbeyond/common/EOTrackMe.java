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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import com.itbeyond.eotrackme.common.Utilities;

import android.os.Environment;


public class EOTrackMe
{

    public static File getLogFile()
    {
    	File gpxFolder = new File(Environment.getExternalStorageDirectory(), "EOTrackMe");
        return new File(gpxFolder.getPath(), "Sender.log");
    }
    
    public static int getLogFileLines()       
    {        

    	try {
    	    BufferedReader br = new BufferedReader(new FileReader(getLogFile()), 8192);

    	    int lineCount = 0;
    	    while ((br.readLine()) != null) {
    	        lineCount++;
    	    }
    	    br.close();
    	    
    	    return lineCount;

    	}
    	catch (IOException e) {
    	}
    	    return 0;
    	}
    
    public static void removeSentLines(int sentLocationsCount)       
    {      
    	int WriteLock_Timeout = 10;
    	while (WriteLock_Timeout > 0) 
    	{
	    	try {
	    		if (sentLocationsCount == getLogFileLines())
	    		{
	    			// Delete the log file
	    			EOTrackMe.getLogFile().delete();
	    		}
	    		else
	    		{
	    			int numtoremove = sentLocationsCount - 1;
	    			File logFile = EOTrackMe.getLogFile();
	    			// We must remove already processed lines
	    			// As the file is appended
		    		String thisline;
		    		StringBuilder fullfile = new StringBuilder();
		    	    BufferedReader br = new BufferedReader(new FileReader(logFile), 16384);
		    	    while ((thisline = br.readLine()) != null) {
		    	    	if (numtoremove < 0) {
		    	    		fullfile.append(thisline + "\n"); 
		    	    		}
		    	    	numtoremove--;
		    	    	}
		    	    br.close();
		    	    
		    	    logFile.delete();
		    	    logFile.createNewFile();
		    	    
		            FileOutputStream writer = new FileOutputStream(EOTrackMe.getLogFile(), false);
		            BufferedOutputStream output = new BufferedOutputStream(writer, 16384);
		
		            output.write(fullfile.toString().getBytes());
		            output.flush();
		            output.close();
		    	 }	
	    		break;
	    	}
	    	catch (IOException e) {
	    	    if (WriteLock_Timeout < 5) { Utilities.LogError("EOTrackMe.removeSentLines - Write Lock", e); }
	    	    try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
				}
	    	    WriteLock_Timeout -= 1;
	    	}
    	}
    }  
}
