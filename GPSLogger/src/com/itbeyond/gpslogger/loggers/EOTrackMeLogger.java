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

package com.itbeyond.gpslogger.loggers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

import android.location.Location;

import com.mendhak.gpslogger.loggers.IFileLogger;


/**
 * Writes a comma separated plain text file.<br/>
 * This file is used as a temporary store for EOTrackMe data.
 *
 * @author David Martin
 *         
 */
public class EOTrackMeLogger implements IFileLogger
{

    private File file;
    protected final String name = "EOTrackMe";

    public EOTrackMeLogger(File file)
    {
        this.file = file;
    }

    @Override
    public void Write(Location loc) throws Exception
    {
        if (!file.exists())
        {
            file.createNewFile();
        }
        
        FileOutputStream writer = new FileOutputStream(file, true);
        BufferedOutputStream output = new BufferedOutputStream(writer);

      //  String dateTimeString = Utilities.GetIsoDateTime(new Date(loc.getTime()));

        String outputString = String.format(Locale.US, "%s %f %f %f %f %f %f\n", loc.getTime(),
                loc.getLatitude(),
                loc.getLongitude(),
                loc.getAltitude(),
                loc.getAccuracy(),
                loc.getSpeed(),
                loc.getBearing());


        output.write(outputString.getBytes());
        output.flush();
        output.close();
    }

    @Override
    public void Annotate(String description, Location loc) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public String getName()
    {
        return name;
    }

}
