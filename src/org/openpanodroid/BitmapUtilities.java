/* 
 * Copyright 2012 Frank Dürr
 * 
 * This file is part of OpenPanodroid.
 *
 * OpenPanodroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenPanodroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenPanodroid.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openpanodroid;

import java.lang.reflect.Field;

import android.graphics.BitmapFactory;
import android.util.Log;

public class BitmapUtilities {
	public static final String LOG_TAG = BitmapUtilities.class.getSimpleName();
	
	public static void setHiddenNativeAllocField(BitmapFactory.Options options) {
        Class bitmapFactoryOptionsClass = android.graphics.BitmapFactory.Options.class;
        try {
			Field inAllocNativeField = bitmapFactoryOptionsClass.getField("inNativeAlloc");
			inAllocNativeField.setBoolean(options, true);
		} catch (SecurityException e) {
			Log.e(LOG_TAG, "Could not set inNativeAlloc flag: " + e.getMessage());
		} catch (NoSuchFieldException e) {
			Log.e(LOG_TAG, "Could not set inNativeAlloc flag: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			Log.e(LOG_TAG, "Could not set inNativeAlloc flag: " + e.getMessage());
		} catch (IllegalAccessException e) {
			Log.e(LOG_TAG, "Could not set inNativeAlloc flag: " + e.getMessage());
		}
	}
}
