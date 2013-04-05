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

package com.itbeyond.eotrackme.loggers;

import android.os.Environment;

import com.itbeyond.common.EOTrackMe;
import com.itbeyond.eotrackme.common.AppSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileLoggerFactory
{
    public static List<IFileLogger> GetFileLoggers()
    {
        File gpxFolder = new File(Environment.getExternalStorageDirectory(), "EOLogger");
        if (!gpxFolder.exists())
        {
            gpxFolder.mkdirs();
        }

        List<IFileLogger> loggers = new ArrayList<IFileLogger>();

        
        if (AppSettings.getEOTrackMeEnabled())
        {
            loggers.add(new EOTrackMeLogger(EOTrackMe.getLogFile()));
        }

        return loggers;
    }
}
