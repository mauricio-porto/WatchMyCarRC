
/*
 * Copyright (c) 2017 Nathanial Freitas
 *
 *   This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.braintech.watchmycar.base;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ApplicationPreferences {
	
    private SharedPreferences appSharedPrefs;

    public static final String BT_DEVICE_NAME = "BT_device_name";
    public static final String BT_DEVICE_ADDRESS = "BT_device_address";

    private Context context;
	
    public ApplicationPreferences(Context context) {
        this.context = context;
        this.appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public SharedPreferences getSharedPreferences() {
        return appSharedPrefs;
    }

    public String getBTDeviceName() {
        return appSharedPrefs.getString(BT_DEVICE_NAME, null);
    }

    public String getBTDeviceAddress() {
        return appSharedPrefs.getString(BT_DEVICE_ADDRESS, null);
    }

    public void setBTDeviceName(String btDeviceName) {
        appSharedPrefs.edit().putString(BT_DEVICE_NAME, btDeviceName).commit();
    }

    public void setBTDeviceAddress(String btDeviceAddress) {
        appSharedPrefs.edit().putString(BT_DEVICE_ADDRESS, btDeviceAddress).commit();
    }
}
