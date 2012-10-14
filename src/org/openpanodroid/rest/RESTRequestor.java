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

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;

import junit.framework.Assert;

import android.os.Message;
import android.util.Log;

abstract public class RESTRequestor extends Thread {
	public static final String LOG_TAG = RESTRequestor.class.getSimpleName();
	
	public final static int RESPONSE_QUERY_FINISHED = 1;
	public final static int RESPONSE_QUERY_FAILED = 2;
	public final static int RESPONSE_PROGRESS = 3;
	
	private Object result;
	private String errorMsg;
	private RESTQuery currentQuery;
	private LinkedList<RESTQuery> queue = new LinkedList<RESTQuery>();
	
	private boolean success;
	private boolean cancelled;
	private boolean terminate;
	
	private int progress;
	
	public RESTRequestor() {
		terminate = false;
	}
	
	abstract protected URL buildRequestURL(RESTQuery queryData);
	abstract protected void parseResponse(InputStream is, int contentLength);
	
	protected void setResult(Object result) {
		this.result = result;
	}
	
	protected void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	
	protected void setSuccessState(boolean success) {
		this.success = success;
	}
	
	protected void setProgress(int percent) {
		Assert.assertTrue(currentQuery != null);
		
		if (percent <= progress) {
			return;
		}
		
		progress = percent;
		
		Message msg = Message.obtain(currentQuery.callbackHandler, RESPONSE_PROGRESS);
		msg.arg1 = progress;
		currentQuery.callbackHandler.sendMessage(msg);
	}
	
	private void notifyClient() {
		Message msg = null;
		
		RESTResponse response = new RESTResponse(currentQuery, result, errorMsg);
		
		if (success) {
			msg = Message.obtain(currentQuery.callbackHandler, RESPONSE_QUERY_FINISHED);
		} else {
			msg = Message.obtain(currentQuery.callbackHandler, RESPONSE_QUERY_FAILED);
		}
		
		msg.obj = response;
		
		currentQuery.callbackHandler.sendMessage(msg);
	}
	
	private void doRequest() {
		InputStream is = null;
		
		try {
			initQuery();
			Assert.assertTrue(currentQuery != null);
			
			URL url = buildRequestURL(currentQuery);
			if (url == null) {
				setSuccessState(false);
				notifyClient();
				return;
			}
	
			Log.i(LOG_TAG, "Doing REST request with the following URL: " + url.toString());
			
			URLConnection connection = url.openConnection();
			is = connection.getInputStream();
			parseResponse(is, connection.getContentLength());

			notifyClient();
		} catch (InterruptedException ex) {
			// Don't notify client if thread was interrupted by client.
		} catch (Exception e) {
			Log.e(LOG_TAG, "REST query failed: " + e.getMessage());
			setSuccessState(false);
			setErrorMsg(e.getLocalizedMessage());
			notifyClient();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e2) {}
			}
			
			finishQuery();
		}
	}

	private synchronized void initQuery() throws InterruptedException {
		cancelled = false;
		success = false;
		result = null;
		errorMsg = null;
		progress = 0;
		
		dequeueQuery();
	}

	private synchronized void finishQuery() {
		currentQuery = null;
	}
	
	@Override
	public void run() {
		while (!isTerminated()) {
			doRequest();
		}
	}
	
	public synchronized void cancelQuery(RESTQuery query) {
		queue.remove(query);
			
		if (query == currentQuery) {
			cancelled = true;
			interrupt();
		}
	}
	
	public synchronized boolean isCancelled() {
		return cancelled;
	}
	
	public synchronized void terminate() {
		terminate = true;
		interrupt();
	}
	
	private synchronized boolean isTerminated() {
		return terminate;
	}
		
	private synchronized void dequeueQuery() throws InterruptedException {
		while (queue.size() <= 0) {
			wait();
		}
		
		currentQuery = queue.remove();
	}
	
	private synchronized void enqueueQuery(RESTQuery query) {
		queue.add(query);
		notify();
	}
	
	public synchronized void addQuery(RESTQuery query) {
		enqueueQuery(query);
	}
}
