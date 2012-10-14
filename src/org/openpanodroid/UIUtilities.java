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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.ScrollView;
import android.widget.TextView;

public class UIUtilities {
	static public void showAlert(Context context, String title, String text) {
			
		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
			}
		};
		
		UIUtilities.showAlert(context, title, text, listener);
	}
	
	static public void showAlert(Context context, String title, String text, DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(text);
		if (title != null) {
			builder.setTitle(title);
		}
		
		builder.setPositiveButton("OK", listener);
		
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	static public void showTextInfo(Context context, String title, CharSequence text) {
		TextView textView = new TextView(context);
		textView.append(text);
		
		ScrollView scrollView = new ScrollView(context);
		scrollView.addView(textView);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		if (title != null) {
		    builder.setTitle(title);
		}
		builder.setView(scrollView);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	        }
	    });
		
		AlertDialog alert = builder.create();
		alert.show();
	}
	
}
