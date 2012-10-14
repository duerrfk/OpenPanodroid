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

import java.net.MalformedURLException;
import java.net.URL;

import org.openpanodroid.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class PanodroidActivity extends Activity {
	final static String LOG_TAG = PanodroidActivity.class.getSimpleName();
	
	final static int CODE_CONTENT_REQUEST = 1;
	
	private EditText editTextUrl;
	private Button buttonSearchFlickr;
	private Button buttonLoadLocal; 
	private Button buttonLoadUrl;
	
	private OnClickListener buttonSearchFlickrkHandler = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Log.i(LOG_TAG, "Starting FlickrSearchActivity");
			
			Intent intent = new Intent(PanodroidActivity.this, FlickrSearchActivity.class);
			startActivity(intent);
		}
	};
	
	private OnClickListener buttonLoadLocalHandler = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("image/*"); 
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			Resources resources = getResources();
			String title = resources.getText(R.string.sourceSelect).toString();
			startActivityForResult(Intent.createChooser(intent, title), CODE_CONTENT_REQUEST);
		}
	};
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CODE_CONTENT_REQUEST && resultCode == RESULT_OK) {
			Uri uri = data.getData();
			Log.i(LOG_TAG, "Received content result: " + uri.toString());
			
			Intent intent = new Intent(PanodroidActivity.this, PanoViewerActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			intent.setData(uri);
			startActivity(intent);
		}
	}
	
	private OnClickListener buttonLoadUrlHandler = new OnClickListener() {
		@Override
		public void onClick(View v) {
			String urlText = editTextUrl.getText().toString();
			
			try {
				URL url = new URL(urlText);
				Uri uri = Uri.parse(url.toString());
				Intent intent = new Intent(PanodroidActivity.this, PanoViewerActivity.class);
				intent.setAction(Intent.ACTION_VIEW);
				intent.setData(uri);
				startActivity(intent);
			} catch (MalformedURLException e) {
				Log.e(LOG_TAG, "Invalid URL");
				Resources resources = getResources();
				String msg = resources.getText(R.string.invalidUrl).toString();
				UIUtilities.showAlert(PanodroidActivity.this, null, msg);
			}
		}
	};
	
	public PanodroidActivity() {
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panodroidlayout);
        
        editTextUrl = (EditText) findViewById(R.id.editTextUrl);
        
        buttonSearchFlickr = (Button) findViewById(R.id.buttonSearchFlickr);
        buttonSearchFlickr.setOnClickListener(buttonSearchFlickrkHandler);
        
        buttonLoadLocal = (Button) findViewById(R.id.buttonLoadLocal);
        buttonLoadLocal.setOnClickListener(buttonLoadLocalHandler);
        
        buttonLoadUrl = (Button) findViewById(R.id.buttonLoadUrl);
        buttonLoadUrl.setOnClickListener(buttonLoadUrlHandler);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.panodroid, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.about :
	    	showAbout();
	    	return true;
	    case R.id.preferences :
	    	Intent intent = new Intent(this, PanodroidPreferencesActivity.class);
			startActivity(intent);
	    	return true;
	    }
		
	    return false;
	}
	
	private void showAbout() {
		Resources resources = getResources();
		CharSequence aboutText = resources.getText(R.string.aboutText);
		UIUtilities.showTextInfo(this, getString(R.string.about), aboutText);
	}
}
