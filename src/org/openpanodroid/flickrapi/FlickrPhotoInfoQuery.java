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

import org.openpanodroid.rest.RESTQuery;

import android.os.Handler;

public class FlickrPhotoInfoQuery extends RESTQuery {

	public String photoid;
	
	public FlickrPhotoInfoQuery(Handler callbackHandler, String photoid) {
		super(callbackHandler);
		
		this.photoid = photoid;
	}
	
}
