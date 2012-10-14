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

import org.openpanodroid.rest.RESTQuery;
import org.openpanodroid.rest.RESTRequestorXML;
import org.xml.sax.helpers.DefaultHandler;


public class FlickrPhotoSizeRequestor extends RESTRequestorXML {

	class MySAXHandler extends DefaultHandler {
		public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {
			if (localName.equals("rsp")) {
				String state = attributes.getValue("stat");
				
				if (state == null || !state.equals("ok")) {
					setSuccessState(false);
				} else {
					result = new FlickrImageSizes();
					setResult(result);
					setSuccessState(true);
				}
			} else if (localName.equals("size")) {
				Assert.assertFalse(result == null);
				
				String sizeLabel = attributes.getValue("source");
				String srcUrlStr = attributes.getValue("source");
				URL srcUrl;
				try {
					srcUrl = new URL(srcUrlStr);
				} catch (MalformedURLException e) {
					return;
				}
				int width = Integer.parseInt(attributes.getValue("width"));
				int height = Integer.parseInt(attributes.getValue("height"));
				
				if (sizeLabel.equalsIgnoreCase("square")) {
					result.squareSize = new ImageSize(width, height);
					result.squareURL = srcUrl;
				} else if (sizeLabel.equalsIgnoreCase("thumbnail")) {
					result.thumbnailSize = new ImageSize(width, height);
					result.thumbnailURL = srcUrl;
				} else if (sizeLabel.equalsIgnoreCase("small")) {
					result.smallSize = new ImageSize(width, height);
					result.smallURL = srcUrl;
				} else if (sizeLabel.equalsIgnoreCase("medium")) {
					result.mediumSize = new ImageSize(width, height);
					result.mediumURL = srcUrl;
				} else if (sizeLabel.equalsIgnoreCase("large")) {
					result.largeSize = new ImageSize(width, height);
					result.largeURL = srcUrl;
				} else if (sizeLabel.equalsIgnoreCase("original")) {
					result.originalSize = new ImageSize(width, height);
					result.originalURL = srcUrl;
				} else {
					Assert.fail();
				}
			}
		}
	}
	
	private FlickrImageSizes result;
	
	public FlickrPhotoSizeRequestor() {
	}

	@Override
	protected DefaultHandler createSAXHandler() {
		return new MySAXHandler();
	}

	@Override
	protected URL buildRequestURL(RESTQuery queryData) {
		Assert.assertTrue(queryData instanceof FlickrPhotoSizeQuery);
		FlickrPhotoSizeQuery q = (FlickrPhotoSizeQuery) queryData;
		
		String urlStr = FlickrConstants.FLICKR_URL + "/?"; 
		urlStr += "method=flickr.photos.getSizes" + "&";
		urlStr += "api_key=" + FlickrConstants.API_KEY_PANODROID + "&";
		urlStr += "photo_id=" + q.photoid;
		
		URL url = null;
		
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException ex) {
			Assert.fail();
		}
		
		return url;
	}

}
