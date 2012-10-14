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
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class FlickrPhotoInfoRequestor extends RESTRequestorXML {
	private FlickrImageInfo result;
	
	class MySAXHandler extends DefaultHandler {
		private boolean insideTitle = false;
		
		public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {
			if (localName.equals("rsp")) {
				String state = attributes.getValue("stat");
				
				if (state == null || !state.equals("ok")) {
					setSuccessState(false);
				} else {
					result = new FlickrImageInfo();
					setResult(result);
					setSuccessState(true);
				}
			} else if (localName.equals("owner")) {
				result.ownerRealName = attributes.getValue("realname");
				result.ownerUserName = attributes.getValue("username");
				result.ownerNsid = attributes.getValue("nsid");
			} else if (localName.equals("photo")) {
				result.id = attributes.getValue("id");
				result.server = attributes.getValue("server");
				result.farm = attributes.getValue("farm");
				result.secret = attributes.getValue("secret");
			} else if (localName.equals("title")) {
				insideTitle = true;
			}
		}
		
		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			if (insideTitle) {
				insideTitle = false;
				result.title = new String(ch, start, length);
			}
		}
	}
	
	protected DefaultHandler createSAXHandler() {
		return new MySAXHandler();
	}

	@Override
	protected URL buildRequestURL(RESTQuery queryData) {
		Assert.assertTrue(queryData instanceof FlickrPhotoInfoQuery);
		FlickrPhotoInfoQuery q = (FlickrPhotoInfoQuery) queryData;
		
		String urlStr = FlickrConstants.FLICKR_URL + "/?"; 
		urlStr += "method=flickr.photos.getInfo" + "&";
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
