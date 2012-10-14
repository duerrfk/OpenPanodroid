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

package org.openpanodroid.flickrapi;

import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.Assert;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class FlickrImageInfo implements Parcelable {
	public static final String LOG_TAG = FlickrImageInfo.class.getSimpleName();
	
	public String id;
	public String secret;
	public String originalSecret;
	public String server;
	public String farm;
	public String title;
	public String ownerRealName;
	public String ownerUserName;
	public String ownerNsid;
	
	public ImageSize thumbnailSize;
	public ImageSize largeSize;
	public ImageSize originalSize;
	
	public String originalFormat;
	
	public static final Parcelable.Creator<FlickrImageInfo> CREATOR = new Parcelable.Creator<FlickrImageInfo>() {
		public FlickrImageInfo createFromParcel(Parcel in) {
			return new FlickrImageInfo(in);
		}

		public FlickrImageInfo[] newArray(int size) {
			return new FlickrImageInfo[size];
		}
	};

	public FlickrImageInfo() {
    }

	private String getUrlBaseStr() {
		// URL follow the scheme:
		// http://farm{farm-id}.static.flickr.com/{server-id}/{id}_{secret}_[mstzb].jpg
		
		String urlStr = "http://farm" + farm + "." + FlickrConstants.IMAGE_HOST + "/" +
			server + "/" + id + "_" + secret;
		
		return urlStr;
	}
	
	// 100px longest side
	public URL getImage100URL() {
		// URL follow the scheme:
		// http://farm{farm-id}.static.flickr.com/{server-id}/{id}_{secret}_[mstzb].jpg
		String urlStr = getUrlBaseStr() + "_t.jpg";
		try {
			URL url = new URL(urlStr);
			return url;
		} catch (MalformedURLException e) {
			Assert.fail();
			Log.e(LOG_TAG, "Invalid URL");
			return null;
		}
	}
	
	// 75px x 75px
	public URL getImage75x75URL() {
		// URL follow the scheme:
		// http://farm{farm-id}.static.flickr.com/{server-id}/{id}_{secret}_[mstzb].jpg
		String urlStr = getUrlBaseStr() + "_s.jpg";
		try {
			URL url = new URL(urlStr);
			return url;
		} catch (MalformedURLException e) {
			Assert.fail();
			Log.e(LOG_TAG, "Invalid URL");
			return null;
		}
	}
	
	// 240px longest side
	public URL getImage240() {
		// URL follow the scheme:
		// http://farm{farm-id}.static.flickr.com/{server-id}/{id}_{secret}_[mstzb].jpg
		String urlStr = getUrlBaseStr() + "_m.jpg";
		try {
			URL url = new URL(urlStr);
			return url;
		} catch (MalformedURLException e) {
			Assert.fail();
			Log.e(LOG_TAG, "Invalid URL");
			return null;
		}
	}
	
	// 500px longest side
	public URL getImage500URL() {
		// URL follow the scheme:
		// http://farm{farm-id}.static.flickr.com/{server-id}/{id}_{secret}_[mstzb].jpg
		String urlStr = getUrlBaseStr() + ".jpg";
		try {
			URL url = new URL(urlStr);
			return url;
		} catch (MalformedURLException e) {
			Assert.fail();
			Log.e(LOG_TAG, "Invalid URL");
			return null;
		}
	}
	
	public URL getImage640URL() {
		// URL follow the scheme:
		// http://farm{farm-id}.static.flickr.com/{server-id}/{id}_{secret}_[mstzb].jpg
		String urlStr = getUrlBaseStr() + "_z.jpg";
		try {
			URL url = new URL(urlStr);
			return url;
		} catch (MalformedURLException e) {
			Assert.fail();
			Log.e(LOG_TAG, "Invalid URL");
			return null;
		}
	}
	
	public URL getImage1024URL() {
		// URL follow the scheme:
		// http://farm{farm-id}.static.flickr.com/{server-id}/{id}_{secret}_[mstzb].jpg
		String urlStr = getUrlBaseStr() + "_b.jpg";
		try {
			URL url = new URL(urlStr);
			return url;
		} catch (MalformedURLException e) {
			Assert.fail();
			Log.e(LOG_TAG, "Invalid URL");
			return null;
		}
	}
	
	public URL getOriginalURL() {
		if (originalSecret == null || originalFormat == null) {
			return null;
		}
		
		// URL follow the scheme:
		// http://farm{farm-id}.static.flickr.com/{server-id}/{id}_{o-secret}_o.(jpg|gif|png)
		
		String urlStr = "http://farm" + farm + "." + FlickrConstants.IMAGE_HOST + "/" +
		server + "/" + id + "_" + originalSecret + "_o." + originalFormat;
		
		try {
			URL url = new URL(urlStr);
			return url;
		} catch (MalformedURLException e) {
			Assert.fail();
			Log.e(LOG_TAG, "Invalid URL");
			return null;
		}
	}
	
	public FlickrImageInfo(Parcel parcel) {
		id = parcel.readString();
		secret = parcel.readString();
		originalSecret = parcel.readString();
		server = parcel.readString();
		farm = parcel.readString();
		title = parcel.readString();
		ownerRealName = parcel.readString();
		ownerUserName = parcel.readString();
		ownerNsid = parcel.readString();
		
		int width, height;
		width = parcel.readInt();
		height = parcel.readInt();
		if (width > 0 && height > 0) {
			thumbnailSize = new ImageSize(width, height);
		} else {
			thumbnailSize = null;
		}
		
		width = parcel.readInt();
		height = parcel.readInt();
		if (width > 0 && height > 0) {
			largeSize = new ImageSize(width, height);
		} else {
			largeSize = null;
		}
	
		width = parcel.readInt();
		height = parcel.readInt();
		if (width > 0 && height > 0) {
			originalSize = new ImageSize(width, height);
		} else {
			originalSize = null;
		}
				
		originalFormat = parcel.readString();
	}
	
	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
    	out.writeString(id);
    	out.writeString(secret);
    	out.writeString(originalSecret);
    	out.writeString(server);
    	out.writeString(farm);
    	out.writeString(title);
    	out.writeString(ownerRealName);
    	out.writeString(ownerUserName);
    	out.writeString(ownerNsid);
    	
    	if (thumbnailSize == null) {
    		out.writeInt(-1);
    		out.writeInt(-1);
    	} else {
    		out.writeInt(thumbnailSize.width);
    		out.writeInt(thumbnailSize.height);
    	}
    	
    	if (largeSize == null) {
    		out.writeInt(-1);
    		out.writeInt(-1);
    	} else {
    		out.writeInt(largeSize.width);
    		out.writeInt(largeSize.height);
    	}
    	
    	if (originalSize == null) {
    		out.writeInt(-1);
    		out.writeInt(-1);
    	} else {
    		out.writeInt(originalSize.width);
    		out.writeInt(originalSize.height);
    	}
    	
    	out.writeString(originalFormat);
    }
}
