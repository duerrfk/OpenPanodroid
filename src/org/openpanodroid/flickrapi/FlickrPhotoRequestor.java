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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.openpanodroid.rest.RESTQuery;
import org.openpanodroid.rest.RESTRequestorXML;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;


public class FlickrPhotoRequestor extends RESTRequestorXML {
	private static final String LOG_TAG = FlickrPhotoRequestor.class.getSimpleName();
	
	public enum SortCriteria {interestingness, date};

	private List<FlickrImageInfo> result;
	
	class MySAXHandler extends DefaultHandler {
		public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {
			if (localName.equals("rsp")) {
				String state = attributes.getValue("stat");
				
				if (state == null || !state.equals("ok")) {
					setSuccessState(false);
				} else {
					result = new LinkedList<FlickrImageInfo>();
					setResult(result);
					setSuccessState(true);
				}
			} else if (localName.equals("photo")) {
				FlickrImageInfo imageInfo = new FlickrImageInfo();
				imageInfo.id = attributes.getValue("id");
				imageInfo.ownerUserName = attributes.getValue("ownername");
				imageInfo.ownerNsid = attributes.getValue("owner");
				imageInfo.farm = attributes.getValue("farm");
				imageInfo.server = attributes.getValue("server");
				imageInfo.secret = attributes.getValue("secret");
				imageInfo.title = attributes.getValue("title");
				
				try {
					String widthStr = attributes.getValue("width_l");
					int width = Integer.parseInt(widthStr);
					String heightStr = attributes.getValue("height_l");
					int height = Integer.parseInt(heightStr);
					imageInfo.largeSize = new ImageSize(width, height);
				} catch (NumberFormatException ex) {
					imageInfo.largeSize = null;
				}
				
				imageInfo.originalFormat = attributes.getValue("originalformat");
				imageInfo.originalSecret = attributes.getValue("originalsecret");
				try {
					String widthStr = attributes.getValue("width_o");
					int width = Integer.parseInt(widthStr);
					String heightStr = attributes.getValue("height_o");
					int height = Integer.parseInt(heightStr);
					imageInfo.originalSize = new ImageSize(width, height);
				} catch (NumberFormatException ex) {
					imageInfo.originalSize = null;
				}
				
				result.add(imageInfo);
			}
		}
	}

	protected DefaultHandler createSAXHandler() {
		return new MySAXHandler();
	}

	@Override
	protected URL buildRequestURL(RESTQuery queryData) {
		Assert.assertTrue(queryData instanceof FlickrPhotoQuery);
		FlickrPhotoQuery q = (FlickrPhotoQuery) queryData;
		
		String urlStr = FlickrConstants.FLICKR_URL + "/?"; 
		urlStr += "method=flickr.photos.search" + "&";
		urlStr += "api_key=" + FlickrConstants.API_KEY_PANODROID + "&";

		if (q.tags != null && q.tags.size() > 0) {
			String tagStr = "";
			for (Iterator<String> it = q.tags.iterator(); it.hasNext(); ) {
				String tag = it.next();
				tagStr += tag;
				if (it.hasNext()) {
					tagStr += ",";
				}
			}
			urlStr += "tags=" + tagStr + "&";
			urlStr += "tag_mode=all" + "&";
		}
		
		if (q.center != null && q.radius > 0) {
			double latitude = q.center.getLatitudeE6()/1000000.0;
			double longitude = q.center.getLongitudeE6()/1000000.0;
			float radius = q.radius/1000.0f;
			
			urlStr += "lat=" + Double.toString(latitude) + "&";
			urlStr += "lon=" + Double.toString(longitude) + "&";
			urlStr += "radius=" + Float.toString(radius) + "&";
			urlStr += "radius_units=" + "km" + "&";
			// Accept world accuracy levels for coordinates.
			urlStr += "accuracy=" + "1" + "&";
		}
		
		// Only photos (content type 1)
		urlStr += "content_type=" + "1" + "&";
		
		// Don't care about license (show 'em all)
		//urlStr += "license=" + "1,2,3,4,5,6,7" + "&";
		
		urlStr += "per_page=" + Integer.toString(q.resultsPerPage) + "&";
		
		if (q.pageNo > 0) {
			urlStr += "page=" + Integer.toString(q.pageNo) + "&";
		}
		
		switch (q.sortCriteria) {
		case interestingness :
			urlStr += "sort=" + "interestingness-desc" + "&";
			break;
		case date :
			urlStr += "sort=" + "date-posted-desc" + "&";
			break;
		default:
			Assert.fail();
		}
		
		// Need a little bit of extra information for each image---in particular size and URL infos---to avoid further queries for each image.
		urlStr += "extras=owner_name,url_o,url_l,url_t,original_format,o_dims" + "&";
		
		// Always add "fake" min_upload_date since Flickr requires at least one
		// search criteria (besides location).
		urlStr += "min_upload_date=" + "1990-01-01";
		
		URL url = null;
		
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException ex) {
			assert(false);
		}
		
		return url;
	}

}
