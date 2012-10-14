OpenPanodroid is a panorama image viewer for Google's Android platform.

OpenPanodroid was originally developed by Frank Dürr 
(email: frank.d.durr@googlemail.com, homepage: http://www.frank-durr.de/)

The latest version of OpenPanodroid is available from GitHub:

https://github.com/duerrfk/OpenPanodroid

OpenPanodroid is released under the GNU General Public License (GNU GPLv3), 
to enable the community to extend OpenPanodroid and use it as basis for
further applications needing a high quality panorama image viewer.

= Flickr Image Import =

In order to access panorama images from Flickr, an API key is required. 
This key is not included with the open source version of OpenPandroid!
You can apply for a key here:

  http://www.flickr.com/services/apps/create/apply

This key has to be imported into the class 

  org.openpanodroid.flickrapi.FlickrConstants, 

attribute 

  API_KEY_PANODROID.

= Calling OpenPanodroid from other Apps =

OpenPanodroid can be used by other applications to display panorama images. 
To call the panorama viewer activity, you have to supply an URI pointing at 
the panorama image (remote file URL (http://), local file path (file://), 
content URI (content://)). The following code example shows how to invoke 
OpenPanodroid:

Uri panoUri = Uri.parse("http://www.frank-durr.de/foo/pano-6000.jpg");
   
ComponentName panoViewerComponent = 
    new ComponentName("org.openpanodroid", 
    "org.openpanodroid.PanoViewerActivity");
    	
Intent intent = new Intent(Intent.ACTION_VIEW);
intent.setComponent(panoViewerComponent);
intent.setData(panoUri);
    	
startActivity(intent);

