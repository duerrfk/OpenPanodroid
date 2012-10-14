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

import org.openpanodroid.flickrapi.FlickrImageInfo;

import junit.framework.Assert;
import org.openpanodroid.R;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class FlickrPanoViewerActivity extends PanoViewerActivity {
	private static final String LOG_TAG = FlickrSearchActivity.class.getSimpleName();
	
	public static final String FLICKR_PANO_INFO = "de.frank_durr.panodroid.flickrpanoinfo";
	
	private FlickrImageInfo imageInfo = null;
	
	public FlickrPanoViewerActivity() {
		// TODO Auto-generated constructor stub
	}

	private void showFlickrPhotoPage() {
		assert(imageInfo != null);
		
		String urlStr = "http://www.flickr.com/photos/" +
			imageInfo.ownerNsid + "/" +
			imageInfo.id + "/";

		Uri uri = Uri.parse(urlStr);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}
	
	private String getImageAttribution() {
		String title = imageInfo.title != null ? imageInfo.title : "";
		
		String realname = imageInfo.ownerRealName != null ? imageInfo.ownerRealName : "";
		String nickname = imageInfo.ownerUserName != null ? imageInfo.ownerUserName : "";
		String author = realname.length() > 0 ? realname : nickname;
		
		String url = "http://www.flickr.com/photos/" + imageInfo.ownerNsid  + "/";
		
		String byStr = getString(R.string.by);
		String text = title + "\n" + byStr + "\n" + author + "\n" + url;
		
		return text;
	}
	
	private void showImageInfo() {
		CharSequence infoText = getImageAttribution();
		UIUtilities.showTextInfo(this, getString(R.string.imageInfo), infoText);
	}
	
	private void showAttributionToast() {
		Toast toast = Toast.makeText(this, getImageAttribution(), Toast.LENGTH_LONG);
		toast.show();	
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.flickrpanoviewer, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.imageInfo :
	    	showImageInfo();
	    	return true;
	    case R.id.flickrPhotoPage :
	    	showFlickrPhotoPage();
	    	return true;
	    case R.id.about :
	    	showAbout();
	    	return true;
	    default :
	    	Assert.fail();
	    	return false;
	    }
	}
	
	private void showAbout() {
		Resources resources = getResources();
		CharSequence aboutText = resources.getText(R.string.aboutText);
		UIUtilities.showTextInfo(this, getString(R.string.about), aboutText);
	}
	
	@Override
	protected void setupImageInfo() {
		Intent intent = getIntent();
        Parcelable data = intent.getParcelableExtra(FLICKR_PANO_INFO);
        Assert.assertTrue(data instanceof FlickrImageInfo);
        imageInfo = (FlickrImageInfo) data;
        
       	if (imageInfo.getOriginalURL() == null) {
    		Assert.assertTrue(imageInfo.getImage1024URL() != null);
    		panoUri = Uri.parse(imageInfo.getImage1024URL().toString());
    	} else {
    		panoUri = Uri.parse(imageInfo.getOriginalURL().toString());
    	}
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(LOG_TAG, "Creating");
    	
    	super.onCreate(savedInstanceState);
    }
    
    @Override
    protected void panoDisplaySetupFinished() {
		showAttributionToast();
	}
    
}
