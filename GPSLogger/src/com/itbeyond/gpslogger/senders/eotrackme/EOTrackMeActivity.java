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

package com.itbeyond.gpslogger.senders.eotrackme;

import android.os.Bundle;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.KeyEvent;
import com.itbeyond.eologger.R;
import com.mendhak.gpslogger.common.Utilities;
import com.itbeyond.common.DeviceUuidFactory;



public class EOTrackMeActivity extends PreferenceActivity implements
        OnPreferenceChangeListener,
        OnPreferenceClickListener
{

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.eotrackmesettings);

        EditTextPreference txtEOTrackMeUserId = (EditTextPreference) findPreference("eotrackme_user_id");
        EditTextPreference txtEOTrackMeDeviceId = (EditTextPreference) findPreference("eotrackme_device_id");

        txtEOTrackMeUserId.setOnPreferenceChangeListener(this);
        txtEOTrackMeDeviceId.setOnPreferenceChangeListener(this);
        
        DeviceUuidFactory DUID = new DeviceUuidFactory(getApplicationContext());
        txtEOTrackMeDeviceId.setText(DUID.getDeviceUuid().toString());

    }

    public boolean onPreferenceClick(Preference preference)
    {
        if (!IsFormValid())
        {
            Utilities.MsgBox(getString(R.string.autoeotrackme_invalid_form),
                    getString(R.string.autoeotrackme_invalid_form_message),
                    EOTrackMeActivity.this);
            return false;
        }
        return true;
    }

    private boolean IsFormValid()
    {
   //     EditTextPreference txtEOTrackMeUserId = (EditTextPreference) findPreference("eotrackme_user_id");
   //     EditTextPreference txtEOTrackMeDeviceId = (EditTextPreference) findPreference("eotrackme_device_id");

   //     return txtEOTrackMeUserId.getText() != null && txtEOTrackMeUserId.getText().length() > 0
   //            && txtEOTrackMeDeviceId.getText() != null && txtEOTrackMeDeviceId.getText().length() > 0;
    	return true;
    }


    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            if (!IsFormValid())
            {
                Utilities.MsgBox(getString(R.string.autoeotrackme_invalid_form),
                        getString(R.string.autoeotrackme_invalid_form_message),
                        this);
                return false;
            }
            else
            {
                return super.onKeyDown(keyCode, event);
            }
        }
        else
        {
            return super.onKeyDown(keyCode, event);
        }
    }


    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        return true;
    }

}
