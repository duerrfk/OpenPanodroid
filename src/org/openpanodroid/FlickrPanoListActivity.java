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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openpanodroid.flickrapi.FlickrImageInfo;
import org.openpanodroid.flickrapi.FlickrPhotoInfoQuery;
import org.openpanodroid.flickrapi.FlickrPhotoInfoRequestor;
import org.openpanodroid.flickrapi.FlickrPhotoQuery;
import org.openpanodroid.flickrapi.FlickrPhotoRequestor;
import org.openpanodroid.flickrapi.FlickrPhotoRequestor.SortCriteria;
import org.openpanodroid.rest.BitmapQuery;
import org.openpanodroid.rest.RESTQuery;
import org.openpanodroid.rest.RESTRequestor;
import org.openpanodroid.rest.RESTRequestorBitmap;
import org.openpanodroid.rest.RESTResponse;

import junit.framework.Assert;

import org.openpanodroid.R;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class FlickrPanoListActivity extends ListActivity implements OnItemClickListener {	
	private static final String LOG_TAG = FlickrPanoListActivity.class.getSimpleName();
	
	public final static String SEARCH_PARAMETERS = "de.frank_durr.panodroid.flickrsearchparameters";
	private static final String TAG_EQUIRECTANGULAR = "equirectangular";

	public static final int RESULTS_PER_PAGE = 50;
	public static final int MAX_TRY_CNT = 3;
	public static final int REQUIRED_RESULTS_PER_SEARCH = 10;
	
	public static final int HIGH_QUALITY_MIN_WIDTH = 3000;
	
	private final static String IMG_INFO_KEY = "imageInfo";
	private final static String THUMBNAILS_KEY = "thumbnails";
	private final static String CURRENT_PAGE_KEY ="currentPage";

	private static final float THUMBNAIL_WIDTH_ABSOLUTE = 25; // [mm]
	private static final int THUMBNAIL_WIDTH_PIXEL = 240; // [px]
	
	private Button buttonSearch;
	
	private ArrayList<FlickrImageInfo> imgInfos;
	private ArrayList<Bitmap> thumbnails;
	
	private RESTRequestorBitmap bitmapRequestor;
	private ThumbnailMsgHandler thumbnailMsgHandler;
	
	private Map<RESTQuery, Integer> queryToPosition = new HashMap<RESTQuery, Integer>();
	private Set<Integer> requestedImages = new HashSet<Integer>();
	
	private ImageInfoAdapter imageInfoAdapter;
	
	private ProgressDialog waitSearchDialog;
	
	private FlickrPhotoInfoRequestor photoInfoRequestor;
	private PhotoInfoMsgHandler photoInfoMsgHandler;
	private FlickrPhotoInfoQuery photoInfoQuery;
	
	private QueryMsgHandler queryMsgHandler;
	private FlickrPhotoQuery photoQuery;
	private FlickrPhotoRequestor photosRequestor;
	
	private FlickrImageInfo selectedImageInfo;
	
	private SearchParameters searchParameters;
	private int currentPage;
	private int tryCnt;
	private int resultCnt;
	
	
	private OnClickListener searchButtonClickHandler = new OnClickListener() {
		@Override
		public void onClick(View v) {
			startPhotoSearch();
		}
	};
	
	private class ClickListenerErrorDialog implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// After an (fatal) error dialog, the activity will be dismissed.
			finish();
		}
	}
	
	private class QueryMsgHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			RESTResponse response;
			
			switch (msg.what) {
			case FlickrPhotoRequestor.RESPONSE_QUERY_FINISHED :
				Log.i(LOG_TAG, "Received query response");
				Assert.assertTrue(msg.obj instanceof RESTResponse);
				response = (RESTResponse) msg.obj;
				Assert.assertTrue(response != null);
				Assert.assertTrue(response.result instanceof List<?>);
				List<FlickrImageInfo> imgList = (List<FlickrImageInfo>) response.result;
				Assert.assertTrue(imgList != null);
				processNewImages(imgList);
				break;
			case FlickrPhotoRequestor.RESPONSE_QUERY_FAILED :
				Log.e(LOG_TAG, "Query failed");
				response = (RESTResponse) msg.obj;
				Assert.assertTrue(response != null);
				waitSearchDialog.dismiss();
				ClickListenerErrorDialog clickListener = new ClickListenerErrorDialog();
				String errorMsg = getString(R.string.photoQueryFailed);
				if (response.errorMsg != null) {
					errorMsg = errorMsg + " (" + errorMsg + ")";
				}
				UIUtilities.showAlert(FlickrPanoListActivity.this, null, errorMsg, clickListener);
				break;
			default :
				super.handleMessage(msg);
			}
		}
	}
	
	private class ThumbnailMsgHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case RESTRequestor.RESPONSE_QUERY_FINISHED :
				Log.i(LOG_TAG, "Received thumbnail");
				Assert.assertTrue(msg.obj != null);
				Assert.assertTrue(msg.obj instanceof RESTResponse);
				RESTResponse response = (RESTResponse) msg.obj;
			    Assert.assertTrue(response.result != null);
			    Assert.assertTrue(response.result instanceof Bitmap);
				Bitmap bitmap = (Bitmap) response.result;
			    int position = queryToPosition.get(response.query);
			    thumbnails.set(position, bitmap);
				imageInfoAdapter.notifyDataSetChanged();
				break;
			case RESTRequestor.RESPONSE_QUERY_FAILED :
				Log.e(LOG_TAG, "Thumbnail query failed");
				break;
			case RESTRequestor.RESPONSE_PROGRESS :
				break;
			default :
				super.handleMessage(msg);
			}
		}
	}
	
	private class PhotoInfoMsgHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			RESTResponse response;
			
			switch (msg.what) {
			case FlickrPhotoInfoRequestor.RESPONSE_QUERY_FINISHED :
				Log.i(LOG_TAG, "Received photo infos");
				waitSearchDialog.dismiss();
				Assert.assertTrue(msg.obj instanceof RESTResponse);
				response = (RESTResponse) msg.obj;
				Assert.assertTrue(response != null);
				Assert.assertTrue(response.result instanceof FlickrImageInfo);
				FlickrImageInfo imageInfo = (FlickrImageInfo) response.result;
				selectedImageInfo.ownerRealName = imageInfo.ownerRealName;
				startPanoViewerActivity();
				break;
			case FlickrPhotoRequestor.RESPONSE_QUERY_FAILED :
				Log.e(LOG_TAG, "Photo info query failed");
				response = (RESTResponse) msg.obj;
				Assert.assertTrue(response != null);
				waitSearchDialog.dismiss();
				String errorMsg = getString(R.string.photoInfoQueryFailed);
				if (response.errorMsg != null) {
					errorMsg = errorMsg + " (" + errorMsg + ")";
				}
				UIUtilities.showAlert(FlickrPanoListActivity.this, null, errorMsg);
				break;
			default :
				super.handleMessage(msg);
			}
		}
	}
	
	private class ImageInfoAdapter extends ArrayAdapter<FlickrImageInfo> {	
		public ImageInfoAdapter(Context context, int textViewResourceId, ArrayList<FlickrImageInfo> items) {
            super(context, textViewResourceId, items);
		}
	
		@Override
		public int getCount() {
			return imgInfos.size();
		}

		@Override
		public FlickrImageInfo getItem(int position) {
			Assert.assertTrue(imgInfos != null);
			Assert.assertTrue(imgInfos.size() > position);
			return imgInfos.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
        public View getView(int position, View view, ViewGroup parent) {
			Assert.assertTrue(imgInfos != null);
			Assert.assertTrue(imgInfos.size() > position);
			FlickrImageInfo info = imgInfos.get(position);
			
			if (view == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(R.layout.panolistitem, null);
            }
			
			TextView caption = (TextView) view.findViewById(R.id.imagecaption);
			String title = info.title;
			if (title != null ) {
				caption.setText(info.title);
			} else {
				Resources resources = getResources();
				caption.setText(resources.getText(R.string.untitled));
			}
			
			ImageView thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
			Bitmap bitmap = thumbnails.get(position);
			if (bitmap == null) {
				thumbnail.setImageResource(R.drawable.wait);
				loadThumbnail(position);
			} else {
				thumbnail.setImageBitmap(bitmap);
			}
			
			ImageView star = (ImageView) view.findViewById(R.id.star);
			if (info.originalSize != null && info.originalSize.width >= HIGH_QUALITY_MIN_WIDTH) {
				star.setVisibility(ImageView.VISIBLE);
			} else {
				star.setVisibility(ImageView.INVISIBLE);
			}
			
			return view;
		}
	}
	
	private class AlertListener implements DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int id) {
			finish();
		}
	}
	
	public FlickrPanoListActivity() {
	}

	private void processNewImages(List<FlickrImageInfo> images) {
		filterImageInfos(images);
		
		for (Iterator<FlickrImageInfo> it = images.iterator(); it.hasNext(); ) {
			imgInfos.add(it.next());
			thumbnails.add(null);
		}
		
		resultCnt += images.size();
		
		if (resultCnt < REQUIRED_RESULTS_PER_SEARCH) {
			// Still haven't found enough images in this iteration.
			
			if (tryCnt >= MAX_TRY_CNT) {
				// Reached maximum try count. Give up.
				if (resultCnt == 0) {
					// Found no results in this search iteration.
					if (imgInfos.size() == 0) {
						// Found no images at all. Quit activity.
						waitSearchDialog.dismiss();
						UIUtilities.showAlert(FlickrPanoListActivity.this, null, getString(R.string.noImages), new AlertListener());
					} else {
						// Found no further images. Disable further search.
						waitSearchDialog.dismiss();
						buttonSearch.setEnabled(false);
						imageInfoAdapter.notifyDataSetChanged();
						UIUtilities.showAlert(FlickrPanoListActivity.this, null, getString(R.string.noMoreImages));
					}
				} else {
					// Found less than requested number of images, but more than 0. 
					// Allow for further search requests.
					waitSearchDialog.dismiss();
					imageInfoAdapter.notifyDataSetChanged();	
				}
			} else {
				// Still have tries left. Let's try again.
				continuePhotoSearch();
			}
		} else {
			// Found enough images in this iteration.
			waitSearchDialog.dismiss();
			imageInfoAdapter.notifyDataSetChanged();
		}
	}
	
	private void startPanoViewerActivity() {
		Assert.assertTrue(selectedImageInfo != null);
		Log.i(LOG_TAG, "Starting PanoViewerActivity");
		Intent intent = new Intent(this, FlickrPanoViewerActivity.class);
		intent.putExtra(FlickrPanoViewerActivity.FLICKR_PANO_INFO, (Parcelable) selectedImageInfo);
		
		startActivity(intent); 
	}
	
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// Request extended image info (until now we are only operating on coarse image information).
		Assert.assertTrue(position < imgInfos.size());
		selectedImageInfo = imgInfos.get(position);
		photoInfoQuery = new FlickrPhotoInfoQuery(photoInfoMsgHandler, selectedImageInfo.id);
		
		photoInfoRequestor.addQuery(photoInfoQuery);
    	
    	waitSearchDialog = new ProgressDialog(FlickrPanoListActivity.this);
    	waitSearchDialog.setMessage(getString(R.string.imageInfoRequest));
    	waitSearchDialog.setCancelable(false);
    	waitSearchDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int id) {
    			photoInfoRequestor.cancelQuery(photoInfoQuery);
    		}
    	});
    	waitSearchDialog.show();
    }
	
	private void loadThumbnail(int position) {
		if (requestedImages.contains(new Integer(position))) {
			// We have already requested this image. Response is pending.
			return;
		}
		
		Log.i(LOG_TAG, "Requesting thumbnail");
		
		requestedImages.add(new Integer(position));
		
		BitmapQuery query = new BitmapQuery(thumbnailMsgHandler, imgInfos.get(position).getImage240());
		queryToPosition.put(query, new Integer(position));
		bitmapRequestor.addQuery(query);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.panolistlayout);

		Intent intent = getIntent();
		Serializable data = intent.getSerializableExtra(SEARCH_PARAMETERS);
		Assert.assertTrue(data instanceof SearchParameters);
		searchParameters = (SearchParameters) data;

	    if (savedInstanceState != null) {
			imgInfos = savedInstanceState.getParcelableArrayList(IMG_INFO_KEY);
			thumbnails = savedInstanceState.getParcelableArrayList(THUMBNAILS_KEY);
			currentPage = savedInstanceState.getInt(CURRENT_PAGE_KEY);
		} else {
			imgInfos = new ArrayList<FlickrImageInfo>();
			thumbnails = new ArrayList<Bitmap>();
			currentPage = 0;
		}
	    
        imageInfoAdapter = new ImageInfoAdapter(this, R.layout.panolistitem, imgInfos);
        setListAdapter(imageInfoAdapter);

        ListView lv = getListView();
        lv.setOnItemClickListener(this);
        
        buttonSearch = (Button) findViewById(R.id.buttonFurtherResults);
        buttonSearch.setOnClickListener(searchButtonClickHandler);
        
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = false;
        
        // Scale the thumbnail to an absolute with of THUMBNAIL_WIDTH_ABSOLUTE [mm].
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        options.inDensity = (int) ((float) THUMBNAIL_WIDTH_PIXEL / (THUMBNAIL_WIDTH_ABSOLUTE/(10*2.54)) + 0.5f);
        options.inScaled = true; 
        options.inTargetDensity = metrics.densityDpi;
        
        bitmapRequestor = new RESTRequestorBitmap(options);
		bitmapRequestor.start();
		thumbnailMsgHandler = new ThumbnailMsgHandler();

		photoInfoRequestor = new FlickrPhotoInfoRequestor();
		photoInfoRequestor.start();
		photoInfoMsgHandler = new PhotoInfoMsgHandler();
		
	    queryMsgHandler = new QueryMsgHandler();
	    photosRequestor = new FlickrPhotoRequestor();
	    photosRequestor.start();
	    
	    if (savedInstanceState == null) {
	    	startPhotoSearch();
	    }
	}
	
    @Override
    public void onDestroy() {
    	bitmapRequestor.terminate();
    	photoInfoRequestor.terminate();
    	photosRequestor.terminate();
    	super.onDestroy();
    }
    
    private FlickrPhotoQuery createPhotoQuery() {
    	List<String> searchTags = new LinkedList<String>();
		for (int i = 0; i < searchParameters.tags.length; i++) {
			searchTags.add(searchParameters.tags[i]);
		}
		searchTags.add(TAG_EQUIRECTANGULAR);
		
		SortCriteria sortCritertia = SortCriteria.date;;
		switch (searchParameters.sortCriteria) {
		case DATE :
			sortCritertia = SortCriteria.date;
			break;
		case INTERESTING :
			sortCritertia = SortCriteria.interestingness;
			break;
		default :
			Assert.fail();
		}
		
		Log.i(LOG_TAG, "Requesting image page " + Integer.toString(currentPage));
		FlickrPhotoQuery query = new FlickrPhotoQuery(queryMsgHandler, searchTags, sortCritertia, currentPage, RESULTS_PER_PAGE);
    	
		return query;
    }
    
    private void startPhotoSearch() {
		currentPage++;
		tryCnt = 1;
		resultCnt = 0;
				
    	photoQuery = createPhotoQuery();
		photosRequestor.addQuery(photoQuery);
		
		waitSearchDialog = new ProgressDialog(FlickrPanoListActivity.this);
    	waitSearchDialog.setMessage(getString(R.string.imageSearch));
    	waitSearchDialog.setCancelable(false);
    	waitSearchDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int id) {
    			photosRequestor.cancelQuery(photoQuery);
    		}
    	});
    	waitSearchDialog.show();
    }
    
    private void continuePhotoSearch() {
    	currentPage++;
    	tryCnt++;
    	
    	photoQuery = createPhotoQuery();
		photosRequestor.addQuery(photoQuery);
    }
    
    private boolean isOriginalFormatCompatible(FlickrImageInfo imageInfo) {
    	if (imageInfo == null || imageInfo.originalFormat == null) {
    		return false;
    	}
    	
    	return (imageInfo.originalFormat.compareToIgnoreCase("jpg") == 0);
    }
    
	private void filterImageInfos(List<FlickrImageInfo> list) {
		for (Iterator<FlickrImageInfo> it = list.iterator(); it.hasNext(); ) {
			FlickrImageInfo imageInfo = it.next();
			
			if (imageInfo.largeSize == null || imageInfo.getImage1024URL() == null) {
				// Need at least large size images (with complete image info).
				it.remove();
				continue;
			}
			
			if (imageInfo.largeSize.width != 2*imageInfo.largeSize.height) {
				// Wrong aspect ration (!= 2/1) --> no equirectangular panorama
				it.remove();
				continue;
			}
			
			if (imageInfo.getOriginalURL() == null || imageInfo.originalSize == null) {
				// Just to be sure: if there is original image info, it must be complete.
				imageInfo.originalSecret = null;
				imageInfo.originalFormat = null;
				imageInfo.originalSize = null;
			}
			
			if (imageInfo.getOriginalURL() != null && !isOriginalFormatCompatible(imageInfo)) {
				// Original image format not compatible.
				imageInfo.originalSecret = null;
				imageInfo.originalFormat = null;
				imageInfo.originalSize = null;
			}
			
			if (imageInfo.originalSize != null && imageInfo.originalSize.width > GlobalConstants.MAX_PANO_IMAGE_WIDTH) {
				// Original image size too large.
				imageInfo.originalSecret = null;
				imageInfo.originalFormat = null;
				imageInfo.originalSize = null;
			}
			
			if (!searchParameters.includeLowResImages && (imageInfo.originalSize == null || imageInfo.originalSize.width < HIGH_QUALITY_MIN_WIDTH )) {
				// High quality image required, but original image not present or original image has too low resolution.
				it.remove();
				continue;
			}			
		}
	}

	@Override
    protected void onSaveInstanceState(Bundle outState) {
    	Log.i(LOG_TAG, "Saving instance state.");
    	
    	super.onSaveInstanceState(outState);
	
    	outState.putParcelableArrayList(IMG_INFO_KEY, imgInfos);
    	outState.putParcelableArrayList(THUMBNAILS_KEY, thumbnails);
    	outState.putInt(CURRENT_PAGE_KEY, currentPage);
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
