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

import java.util.List;

import org.openpanodroid.flickrapi.FlickrPhotoRequestor.SortCriteria;
import org.openpanodroid.rest.RESTQuery;

import com.google.android.maps.GeoPoint;


import android.os.Handler;

public class FlickrPhotoQuery extends RESTQuery {

	public GeoPoint center;
	public float radius;
	public List<String> tags;
	public SortCriteria sortCriteria;
	public int pageNo;
	public int resultsPerPage;
	
	public FlickrPhotoQuery(Handler callbackHandler, List<String> tags, GeoPoint center, float radius, SortCriteria sortCriteria, int page, int resultsPerPage) {
		super(callbackHandler);
	
		this.tags = tags;
		
		this.center = center;
		this.radius = radius;

		this.sortCriteria = sortCriteria;
		
		this.pageNo = page;
		this.resultsPerPage = resultsPerPage;
	}
	
	public FlickrPhotoQuery(Handler callbackHandler, List<String> tags, SortCriteria sortCriteria, int page, int resultsPerPage) {
		super(callbackHandler);
	
		this.tags = tags;
		
		center = null;
		this.radius = 0;

		this.sortCriteria = sortCriteria;
		
		this.pageNo = page;
		this.resultsPerPage = resultsPerPage;
	}

}
