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

import java.io.Serializable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.openpanodroid.R;

import junit.framework.Assert;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

public class FlickrSearchActivity extends Activity {
	private static final String LOG_TAG = FlickrSearchActivity.class.getSimpleName();
	
	private RadioButton radioButtonInterestingness;
	private RadioButton radioButtonDate;
	private Button buttonSearch;
	private EditText editTextSearchTags;
	private CheckBox checkBoxIncludeLowResImages;
	
	private OnClickListener searchButtonClickHandler = new OnClickListener() {
		@Override
		public void onClick(View v) {
			startPanoListActivity();
		}
	};
		
	private View.OnKeyListener editTextSearchTagsKeyistener = new View.OnKeyListener() {
		// When user hits return, start search.
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			// If the event is a key-down event on the "enter" button
	        if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
	          startPanoListActivity();	          
	          return true;
	        }
	        
	        return false;
		}
	};
	
	private void startPanoListActivity() {
		// Start the PanoListActivity with the search parameters.
		SearchParameters searchParams = new SearchParameters();
		
		if (radioButtonInterestingness.isChecked()) {
			searchParams.sortCriteria = SearchParameters.SortCriteria.INTERESTING;
		} else {
			Assert.assertTrue(radioButtonDate.isChecked());
			searchParams.sortCriteria = SearchParameters.SortCriteria.DATE;
		}
		
		String searchTagsStr = editTextSearchTags.getText().toString();
		List<String> searchTags = tokenizeString(searchTagsStr);
		searchParams.tags = new String[searchTags.size()];
		searchParams.tags = searchTags.toArray(searchParams.tags);
		
		searchParams.includeLowResImages = checkBoxIncludeLowResImages.isChecked();
		
		Log.i(LOG_TAG, "Starting PanoListActivity");
		
		Intent intent = new Intent(this, FlickrPanoListActivity.class);
		intent.putExtra(FlickrPanoListActivity.SEARCH_PARAMETERS, (Serializable) searchParams);
		startActivity(intent);
	}
	
	private List<String> tokenizeString(String searchTags) {
		List<String> tokens = new Vector<String>();
		
		StringTokenizer tokenizer = new StringTokenizer(searchTags, " ,");
		while (tokenizer.hasMoreTokens()) {
			tokens.add(tokenizer.nextToken());
		}
		
		return tokens;
	}
	
 	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        radioButtonInterestingness = (RadioButton) findViewById(R.id.radioButtonInterestingness);
        radioButtonInterestingness.setChecked(true);
        radioButtonDate = (RadioButton) findViewById(R.id.radioButtonDate);
        
        editTextSearchTags = (EditText) findViewById(R.id.editTextSearchTags);
        editTextSearchTags.setOnKeyListener(editTextSearchTagsKeyistener);
        
        checkBoxIncludeLowResImages = (CheckBox) findViewById(R.id.checkBoxLowResImages);
        
        buttonSearch = (Button) findViewById(R.id.buttonSearch);
        buttonSearch.setOnClickListener(searchButtonClickHandler);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
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