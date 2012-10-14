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

package org.openpanodroid.rest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.ErrorManager;

import junit.framework.Assert;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class RESTRequestorBitmap extends RESTRequestor {

	private final static int MAX_CHUNK_SIZE = 10000;
	
	private BitmapFactory.Options bitmapOptions = null;
	
	public RESTRequestorBitmap(BitmapFactory.Options bitmapOptions) {
		super();
		this.bitmapOptions = bitmapOptions;
	}
	
	@Override
	protected void parseResponse(InputStream is, int contentLength) {
		int offset = 0;
		byte[] buffer = new byte[contentLength];
		
		BufferedInputStream bs = new BufferedInputStream(is);

		while (offset < contentLength) {
			if (isCancelled()) {
				return;
			}
			
			try {
				int chunkSize = MAX_CHUNK_SIZE > contentLength-offset ? contentLength-offset : MAX_CHUNK_SIZE;
				int read = bs.read(buffer, offset, chunkSize);
				if (read == -1) {
					setSuccessState(false);
					return;
				} else {
					offset += read;
					setProgress((int) (100.0*offset/contentLength + 0.5));
				}
			} catch (IOException e) {
				setSuccessState(false);
				setErrorMsg(e.getLocalizedMessage());
				return;
			}
		}
		
		Bitmap bitmap;
		
		if (bitmapOptions != null) {
			bitmap = BitmapFactory.decodeByteArray(buffer, 0, contentLength, bitmapOptions);
		} else {
			bitmap = BitmapFactory.decodeByteArray(buffer, 0, contentLength);
		}
		
		if (bitmap == null) {
			setSuccessState(false);
		} else {
			setResult(bitmap);
			setSuccessState(true);
		}
	}

	@Override
	protected URL buildRequestURL(RESTQuery queryData) {
		Assert.assertTrue(queryData instanceof BitmapQuery);
		BitmapQuery q = (BitmapQuery) queryData;
		
		return q.url;
	}

}
