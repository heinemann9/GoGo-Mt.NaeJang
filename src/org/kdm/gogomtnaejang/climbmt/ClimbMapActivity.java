package org.kdm.gogomtnaejang.climbmt;

import java.util.ArrayList;

import org.kdm.gogomtnaejang.navermap.NMapCalloutCustomOldOverlay;
import org.kdm.gogomtnaejang.navermap.NMapCalloutCustomOverlayView;
import org.kdm.gogomtnaejang.navermap.NMapViewerResourceProvider;
import org.kdm.gogomtnaejang.node.ManageNode;
import org.kdm.gogomtnaejang.node.Path;
import org.kdm.gogomtnaejang.node.SimpleNode;
import org.kdm.gogonaejangmt.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.nhn.android.maps.NMapActivity;
import com.nhn.android.maps.NMapCompassManager;
import com.nhn.android.maps.NMapController;
import com.nhn.android.maps.NMapLocationManager;
import com.nhn.android.maps.NMapOverlay;
import com.nhn.android.maps.NMapOverlayItem;
import com.nhn.android.maps.NMapView;
import com.nhn.android.maps.maplib.NGeoPoint;
import com.nhn.android.maps.nmapmodel.NMapError;
import com.nhn.android.maps.nmapmodel.NMapPlacemark;
import com.nhn.android.maps.overlay.NMapPOIdata;
import com.nhn.android.maps.overlay.NMapPOIitem;
import com.nhn.android.maps.overlay.NMapPathData;
import com.nhn.android.maps.overlay.NMapPathLineStyle;
import com.nhn.android.mapviewer.overlay.NMapCalloutCustomOverlay;
import com.nhn.android.mapviewer.overlay.NMapCalloutOverlay;
import com.nhn.android.mapviewer.overlay.NMapMyLocationOverlay;
import com.nhn.android.mapviewer.overlay.NMapOverlayManager;
import com.nhn.android.mapviewer.overlay.NMapPOIdataOverlay;
import com.nhn.android.mapviewer.overlay.NMapPathDataOverlay;

public class ClimbMapActivity extends NMapActivity{

	private static final boolean DEBUG = false;
	
	private static final String LOG_TAG = "ClimbMapActivity";
	private static final String NMAP_API_KEY = "2406f325c2ac640aa1db69ab84bba088";
	
	private static final NGeoPoint NMAP_LOCATION_DEFAULT = new NGeoPoint(126.889159, 35.478316);
	private static final int NMAP_ZOOMLEVEL_DEFAULT = 11;
	private static final int NMAP_VIEW_MODE_DEFAULT = NMapView.VIEW_MODE_HYBRID;
	private static final boolean NMAP_TRAFFIC_MODE_DEFAULT = false;
	private static final boolean NMAP_BICYCLE_MODE_DEFAULT = false;

	private static final String KEY_ZOOM_LEVEL = "NMapViewer.zoomLevel";
	private static final String KEY_CENTER_LONGITUDE = "NMapViewer.centerLongitudeE6";
	private static final String KEY_CENTER_LATITUDE = "NMapViewer.centerLatitudeE6";
	private static final String KEY_VIEW_MODE = "NMapViewer.viewMode";
	private static final String KEY_TRAFFIC_MODE = "NMapViewer.trafficMode";
	private static final String KEY_BICYCLE_MODE = "NMapViewer.bicycleMode";
	
	public static final int CALL_BY_TRACK_LIST_DETAIL_ACTIVITY = 0;
	
	private MapContainerView mMapContainerView;
	private NMapView mMapView = null;
	private NMapController mMapController;
	private NMapViewerResourceProvider mMapViewerResourceProvider;
	private NMapOverlayManager mOverlayManager;
	private NMapLocationManager mMapLocationManager;
	private NMapCompassManager mMapCompassManager;
	private NMapMyLocationOverlay mMyLocationOverlay;	
	private NMapPOIdataOverlay mFloatingPOIdataOverlay;
	private NMapPOIitem mFloatingPOIitem;

	private SharedPreferences mPreferences;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	
	setContentView(R.layout.activity_climb_map);
	mMapView = (NMapView)findViewById(R.id.climbMapView);

	// set a registered API key for Open MapViewer Library
	mMapView.setApiKey(NMAP_API_KEY);

	// initialize map view
	mMapView.setClickable(true);
	mMapView.setEnabled(true);
	mMapView.setFocusable(true);
	mMapView.setFocusableInTouchMode(true);
	mMapView.requestFocus();
	
	// register listener for map state changes
	mMapView.setOnMapStateChangeListener(onMapViewStateChangeListener);
	mMapView.setOnMapViewTouchEventListener(onMapViewTouchEventListener);
	mMapView.setOnMapViewDelegate(onMapViewTouchDelegate);

	// use map controller to zoom in/out, pan and set map center, zoom level etc.
	mMapController = mMapView.getMapController();
	
	// use built in zoom controls
	NMapView.LayoutParams lp = new NMapView.LayoutParams(LayoutParams.WRAP_CONTENT,
		LayoutParams.WRAP_CONTENT, NMapView.LayoutParams.BOTTOM_RIGHT);
	mMapView.setBuiltInZoomControls(true, lp);
	
	// create resource provider
	mMapViewerResourceProvider = new NMapViewerResourceProvider(this);

	// set data provider listener
	super.setMapDataProviderListener(onDataProviderListener);
	
	// create overlay manager
	mOverlayManager = new NMapOverlayManager(this, mMapView, mMapViewerResourceProvider);
	
	// create overlay manager
	mOverlayManager = new NMapOverlayManager(this, mMapView, mMapViewerResourceProvider);
	// register callout overlay listener to customize it.
	mOverlayManager.setOnCalloutOverlayListener(onCalloutOverlayListener);
	// register callout overlay view listener to customize it.
	mOverlayManager.setOnCalloutOverlayViewListener(onCalloutOverlayViewListener);

	// location manager
	mMapLocationManager = new NMapLocationManager(this);
	mMapLocationManager.setOnLocationChangeListener(onMyLocationChangeListener);

	// compass manager
	mMapCompassManager = new NMapCompassManager(this);

	// create my location overlay
	mMyLocationOverlay = mOverlayManager.createMyLocationOverlay(mMapLocationManager, mMapCompassManager);
	
	initByCallActivity();
	}
	
	private void initByCallActivity(){
		Intent intent = getIntent();
		int whichActivityCall = intent.getExtras().getInt("callActivity");
		
		switch(whichActivityCall){
		case CALL_BY_TRACK_LIST_DETAIL_ACTIVITY:
			int trackID = intent.getExtras().getInt("trackID");
			trackDataOverlayFromID(trackID);
			
			Track track = ManageTrackInfo.getInst().getOneTrack(trackID);
			TextView climbInfoTitle = (TextView) findViewById(R.id.climbInfoTitle);
			TextView climbInfoTimeDistance = (TextView) findViewById(R.id.climbInfoTimeDistance);
			TextView climbInfoDescription = (TextView) findViewById(R.id.climbInfoDescription);
			climbInfoTitle.setText(track.title);
			climbInfoTimeDistance.setText(track.time+", "+track.distance);
			climbInfoDescription.setText(track.description);

			mMapController.setMapCenter(126.869821, 35.465282, 8);			
			mMapController.setMapViewMode(NMapView.VIEW_MODE_HYBRID);
			break;
		default:
			break;
		}
		
		mMapView.displayZoomControls(true);
	}
	
	@Override
	protected void onStop() {

		stopMyLocation();

		super.onStop();
	}
	
	@Override
	protected void onDestroy() {

		// save map view state such as map center position and zoom level.
		saveInstanceState();

		super.onDestroy();
	}
	
	private void startMyLocation() {

		if (mMyLocationOverlay != null) {
			if (!mOverlayManager.hasOverlay(mMyLocationOverlay)) {
				mOverlayManager.addOverlay(mMyLocationOverlay);
			}

			if (mMapLocationManager.isMyLocationEnabled()) {

				if (!mMapView.isAutoRotateEnabled()) {
					mMyLocationOverlay.setCompassHeadingVisible(true);

					mMapCompassManager.enableCompass();

					mMapView.setAutoRotateEnabled(true, false);

					mMapContainerView.requestLayout();
				} else {
					stopMyLocation();
				}

				mMapView.postInvalidate();
			} else {
				boolean isMyLocationEnabled = mMapLocationManager.enableMyLocation(true);
				if (!isMyLocationEnabled) {
					Toast.makeText(this, "Please enable a My Location source in system settings",
						Toast.LENGTH_LONG).show();

					Intent goToSettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					startActivity(goToSettings);

					return;
				}
			}
		}
		
	}
	
	private void stopMyLocation() {
		if (mMyLocationOverlay != null) {
			mMapLocationManager.disableMyLocation();

			if (mMapView.isAutoRotateEnabled()) {
				mMyLocationOverlay.setCompassHeadingVisible(false);

				mMapCompassManager.disableCompass();

				mMapView.setAutoRotateEnabled(false, false);

				mMapContainerView.requestLayout();
			}
		}
	}
	

	
	NMapPathDataOverlay pathDataOverlay = null;
	private void initDataOverlay(){
		if(pathDataOverlay == null)
			pathDataOverlay = mOverlayManager.createPathDataOverlay();
	}
	
	private void trackDataOverlayFromID(int trackID) {
		ArrayList<Integer> trackPathIDList = ManageTrackInfo.getInst().getOneTrackList(trackID);
		for(int i=0; i<trackPathIDList.size(); i++){
			int pathID = trackPathIDList.get(i);
			pathDataOverlayFromID(pathID);
		}		
	}
	
	private void pathDataOverlayFromID(int pathID){
		Path path = ManageNode.getInst(this).getPathData(pathID);
		pathDataOverlay(path);
	}
	
	private void pathDataOverlay(Path path){
		ArrayList<SimpleNode> nodes = path.getAllSimpleNode();
		initDataOverlay();

		NMapPathData pathData = new NMapPathData(nodes.size());
		pathData.initPathData();
		pathData.addPathPoint(nodes.get(0).lng, nodes.get(0).lat, NMapPathLineStyle.TYPE_SOLID);
		for(int i=1; i<nodes.size(); i++){
			pathData.addPathPoint(nodes.get(i).lng, nodes.get(i).lat, 0);
		}
		pathData.endPathData();
		
		// set path line style
		NMapPathLineStyle pathLineStyle = new NMapPathLineStyle(mMapView.getContext());
		pathLineStyle.setLineColor(0xffbb2b, 0xff);
		pathLineStyle.setFillColor(0xFFFFFF, 0x00);
		pathData.setPathLineStyle(pathLineStyle);
		pathDataOverlay.addPathData(pathData);
		pathDataOverlay.showAllPathData(0);
	}
	
	/* NMapDataProvider Listener */
	private final NMapActivity.OnDataProviderListener onDataProviderListener = new NMapActivity.OnDataProviderListener() {

		@Override
		public void onReverseGeocoderResponse(NMapPlacemark placeMark, NMapError errInfo) {

			if (DEBUG) {
				Log.i(LOG_TAG, "onReverseGeocoderResponse: placeMark="
					+ ((placeMark != null) ? placeMark.toString() : null));
			}

			if (errInfo != null) {
				Log.e(LOG_TAG, "Failed to findPlacemarkAtLocation: error=" + errInfo.toString());

				Toast.makeText(ClimbMapActivity.this, errInfo.toString(), Toast.LENGTH_LONG).show();
				return;
			}

			if (mFloatingPOIitem != null && mFloatingPOIdataOverlay != null) {
				mFloatingPOIdataOverlay.deselectFocusedPOIitem();

				if (placeMark != null) {
					mFloatingPOIitem.setTitle(placeMark.toString());
				}
				mFloatingPOIdataOverlay.selectPOIitemBy(mFloatingPOIitem.getId(), false);
			}
		}

	};

	/* MyLocation Listener */
	private final NMapLocationManager.OnLocationChangeListener onMyLocationChangeListener = new NMapLocationManager.OnLocationChangeListener() {

		@Override
		public boolean onLocationChanged(NMapLocationManager locationManager, NGeoPoint myLocation) {

			if (mMapController != null) {
				mMapController.animateTo(myLocation);
			}

			return true;
		}

		@Override
		public void onLocationUpdateTimeout(NMapLocationManager locationManager) {

			// stop location updating
			//			Runnable runnable = new Runnable() {
			//				public void run() {										
			//					stopMyLocation();
			//				}
			//			};
			//			runnable.run();	

			Toast.makeText(ClimbMapActivity.this, "Your current location is temporarily unavailable.", Toast.LENGTH_LONG).show();
		}

		@Override
		public void onLocationUnavailableArea(NMapLocationManager locationManager, NGeoPoint myLocation) {

			Toast.makeText(ClimbMapActivity.this, "Your current location is unavailable area.", Toast.LENGTH_LONG).show();

			stopMyLocation();
		}

	};

	/* MapView State Change Listener*/
	private final NMapView.OnMapStateChangeListener onMapViewStateChangeListener = new NMapView.OnMapStateChangeListener() {

		@Override
		public void onMapInitHandler(NMapView mapView, NMapError errorInfo) {

			if (errorInfo == null) { // success
				// restore map view state such as map center position and zoom level.
				//restoreInstanceState();

			} else { // fail
				Log.e(LOG_TAG, "onFailedToInitializeWithError: " + errorInfo.toString());
				Toast.makeText(ClimbMapActivity.this, errorInfo.toString(), Toast.LENGTH_LONG).show();
			}
		}

		@Override
		public void onAnimationStateChange(NMapView mapView, int animType, int animState) {
			if (DEBUG) {
				Log.i(LOG_TAG, "onAnimationStateChange: animType=" + animType + ", animState=" + animState);
			}
		}

		@Override
		public void onMapCenterChange(NMapView mapView, NGeoPoint center) {
			if (DEBUG) {
				Log.i(LOG_TAG, "onMapCenterChange: center=" + center.toString());
			}
		}

		@Override
		public void onZoomLevelChange(NMapView mapView, int level) {
			if (DEBUG) {
				Log.i(LOG_TAG, "onZoomLevelChange: level=" + level);
			}
		}

		@Override
		public void onMapCenterChangeFine(NMapView mapView) {

		}
	};

	private final NMapView.OnMapViewTouchEventListener onMapViewTouchEventListener = new NMapView.OnMapViewTouchEventListener() {

		@Override
		public void onLongPress(NMapView mapView, MotionEvent ev) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onLongPressCanceled(NMapView mapView) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSingleTapUp(NMapView mapView, MotionEvent ev) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onTouchDown(NMapView mapView, MotionEvent ev) {

		}

		@Override
		public void onScroll(NMapView mapView, MotionEvent e1, MotionEvent e2) {
		}

		@Override
		public void onTouchUp(NMapView mapView, MotionEvent ev) {
			// TODO Auto-generated method stub

		}

	};

	private final NMapView.OnMapViewDelegate onMapViewTouchDelegate = new NMapView.OnMapViewDelegate() {

		@Override
		public boolean isLocationTracking() {
			if (mMapLocationManager != null) {
				if (mMapLocationManager.isMyLocationEnabled()) {
					return mMapLocationManager.isMyLocationFixed();
				}
			}
			return false;
		}

	};

	/* POI data State Change Listener*/
	private final NMapPOIdataOverlay.OnStateChangeListener onPOIdataStateChangeListener = new NMapPOIdataOverlay.OnStateChangeListener() {

		@Override
		public void onCalloutClick(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
			if (DEBUG) {
				Log.i(LOG_TAG, "onCalloutClick: title=" + item.getTitle());
			}

			// [[TEMP]] handle a click event of the callout
			Toast.makeText(ClimbMapActivity.this, "onCalloutClick: " + item.getTitle(), Toast.LENGTH_LONG).show();
		}

		@Override
		public void onFocusChanged(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
			if (DEBUG) {
				if (item != null) {
					Log.i(LOG_TAG, "onFocusChanged: " + item.toString());
				} else {
					Log.i(LOG_TAG, "onFocusChanged: ");
				}
			}
		}
	};

	private final NMapPOIdataOverlay.OnFloatingItemChangeListener onPOIdataFloatingItemChangeListener = new NMapPOIdataOverlay.OnFloatingItemChangeListener() {

		@Override
		public void onPointChanged(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
			NGeoPoint point = item.getPoint();

			if (DEBUG) {
				Log.i(LOG_TAG, "onPointChanged: point=" + point.toString());
			}

			findPlacemarkAtLocation(point.longitude, point.latitude);

			item.setTitle(null);

		}
	};

	private final NMapOverlayManager.OnCalloutOverlayListener onCalloutOverlayListener = new NMapOverlayManager.OnCalloutOverlayListener() {

		@Override
		public NMapCalloutOverlay onCreateCalloutOverlay(NMapOverlay itemOverlay, NMapOverlayItem overlayItem,
			Rect itemBounds) {

			// handle overlapped items
			if (itemOverlay instanceof NMapPOIdataOverlay) {
				NMapPOIdataOverlay poiDataOverlay = (NMapPOIdataOverlay)itemOverlay;

				// check if it is selected by touch event
				if (!poiDataOverlay.isFocusedBySelectItem()) {
					int countOfOverlappedItems = 1;

					NMapPOIdata poiData = poiDataOverlay.getPOIdata();
					for (int i = 0; i < poiData.count(); i++) {
						NMapPOIitem poiItem = poiData.getPOIitem(i);

						// skip selected item
						if (poiItem == overlayItem) {
							continue;
						}

						// check if overlapped or not
						if (Rect.intersects(poiItem.getBoundsInScreen(), overlayItem.getBoundsInScreen())) {
							countOfOverlappedItems++;
						}
					}

					if (countOfOverlappedItems > 1) {
						String text = countOfOverlappedItems + " overlapped items for " + overlayItem.getTitle();
						Toast.makeText(ClimbMapActivity.this, text, Toast.LENGTH_LONG).show();
						return null;
					}
				}
			}

			// use custom old callout overlay
			if (overlayItem instanceof NMapPOIitem) {
				NMapPOIitem poiItem = (NMapPOIitem)overlayItem;

				if (poiItem.showRightButton()) {
					return new NMapCalloutCustomOldOverlay(itemOverlay, overlayItem, itemBounds,
						mMapViewerResourceProvider);
				}
			}

			// use custom callout overlay
			return new NMapCalloutCustomOverlay(itemOverlay, overlayItem, itemBounds, mMapViewerResourceProvider);

			// set basic callout overlay
			//return new NMapCalloutBasicOverlay(itemOverlay, overlayItem, itemBounds);			
		}

	};

	private final NMapOverlayManager.OnCalloutOverlayViewListener onCalloutOverlayViewListener = new NMapOverlayManager.OnCalloutOverlayViewListener() {

		@Override
		public View onCreateCalloutOverlayView(NMapOverlay itemOverlay, NMapOverlayItem overlayItem, Rect itemBounds) {

			if (overlayItem != null) {
				// [TEST] 말풍선 오버레이를 뷰로 설정함
				String title = overlayItem.getTitle();
				if (title != null && title.length() > 5) {
					return new NMapCalloutCustomOverlayView(ClimbMapActivity.this, itemOverlay, overlayItem, itemBounds);
				}
			}

			// null을 반환하면 말풍선 오버레이를 표시하지 않음
			return null;
		}

	};

	/* Local Functions */

	private void restoreInstanceState() {
		mPreferences = getPreferences(MODE_PRIVATE);

		int longitudeE6 = mPreferences.getInt(KEY_CENTER_LONGITUDE, NMAP_LOCATION_DEFAULT.getLongitudeE6());
		int latitudeE6 = mPreferences.getInt(KEY_CENTER_LATITUDE, NMAP_LOCATION_DEFAULT.getLatitudeE6());
		int level = mPreferences.getInt(KEY_ZOOM_LEVEL, NMAP_ZOOMLEVEL_DEFAULT);
		int viewMode = mPreferences.getInt(KEY_VIEW_MODE, NMAP_VIEW_MODE_DEFAULT);
		boolean trafficMode = mPreferences.getBoolean(KEY_TRAFFIC_MODE, NMAP_TRAFFIC_MODE_DEFAULT);
		boolean bicycleMode = mPreferences.getBoolean(KEY_BICYCLE_MODE, NMAP_BICYCLE_MODE_DEFAULT);

		mMapController.setMapViewMode(viewMode);
		mMapController.setMapViewTrafficMode(trafficMode);
		mMapController.setMapViewBicycleMode(bicycleMode);
		mMapController.setMapCenter(new NGeoPoint(longitudeE6, latitudeE6), level);
	}

	private void saveInstanceState() {
		if (mPreferences == null) {
			return;
		}

		NGeoPoint center = mMapController.getMapCenter();
		int level = mMapController.getZoomLevel();
		int viewMode = mMapController.getMapViewMode();
		boolean trafficMode = mMapController.getMapViewTrafficMode();
		boolean bicycleMode = mMapController.getMapViewBicycleMode();

		SharedPreferences.Editor edit = mPreferences.edit();

		edit.putInt(KEY_CENTER_LONGITUDE, center.getLongitudeE6());
		edit.putInt(KEY_CENTER_LATITUDE, center.getLatitudeE6());
		edit.putInt(KEY_ZOOM_LEVEL, level);
		edit.putInt(KEY_VIEW_MODE, viewMode);
		edit.putBoolean(KEY_TRAFFIC_MODE, trafficMode);
		edit.putBoolean(KEY_BICYCLE_MODE, bicycleMode);

		edit.commit();

	}

	/* Menus */
	private static final int MENU_ITEM_CLEAR_MAP = 10;
	private static final int MENU_ITEM_MAP_MODE = 20;
	private static final int MENU_ITEM_MAP_MODE_SUB_VECTOR = MENU_ITEM_MAP_MODE + 1;
	private static final int MENU_ITEM_MAP_MODE_SUB_SATELLITE = MENU_ITEM_MAP_MODE + 2;
	private static final int MENU_ITEM_MAP_MODE_SUB_TRAFFIC = MENU_ITEM_MAP_MODE + 3;
	private static final int MENU_ITEM_MAP_MODE_SUB_BICYCLE = MENU_ITEM_MAP_MODE + 4;
	private static final int MENU_ITEM_ZOOM_CONTROLS = 30;
	private static final int MENU_ITEM_MY_LOCATION = 40;

	private static final int MENU_ITEM_TEST_MODE = 50;
	private static final int MENU_ITEM_TEST_POI_DATA = MENU_ITEM_TEST_MODE + 1;
	private static final int MENU_ITEM_TEST_PATH_DATA = MENU_ITEM_TEST_MODE + 2;
	private static final int MENU_ITEM_TEST_FLOATING_DATA = MENU_ITEM_TEST_MODE + 3;
	private static final int MENU_ITEM_TEST_AUTO_ROTATE = MENU_ITEM_TEST_MODE + 4;

	/**
	 * Invoked during init to give the Activity a chance to set up its Menu.
	 * 
	 * @param menu the Menu to which entries may be added
	 * @return true
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuItem menuItem = null;
		SubMenu subMenu = null;

		menuItem = menu.add(Menu.NONE, MENU_ITEM_CLEAR_MAP, Menu.CATEGORY_SECONDARY, "초기화");
		menuItem.setAlphabeticShortcut('c');
		menuItem.setIcon(android.R.drawable.ic_menu_revert);

		subMenu = menu.addSubMenu(Menu.NONE, MENU_ITEM_MAP_MODE, Menu.CATEGORY_SECONDARY, "지도보기");
		subMenu.setIcon(android.R.drawable.ic_menu_mapmode);

		menuItem = subMenu.add(0, MENU_ITEM_MAP_MODE_SUB_VECTOR, Menu.NONE, "일반지도");
		menuItem.setAlphabeticShortcut('m');
		menuItem.setCheckable(true);
		menuItem.setChecked(false);

		menuItem = subMenu.add(0, MENU_ITEM_MAP_MODE_SUB_SATELLITE, Menu.NONE, "위성지도");
		menuItem.setAlphabeticShortcut('s');
		menuItem.setCheckable(true);
		menuItem.setChecked(false);

		menuItem = subMenu.add(0, MENU_ITEM_MAP_MODE_SUB_TRAFFIC, Menu.NONE, "실시간교통");
		menuItem.setAlphabeticShortcut('t');
		menuItem.setCheckable(true);
		menuItem.setChecked(false);

		menuItem = subMenu.add(0, MENU_ITEM_MAP_MODE_SUB_BICYCLE, Menu.NONE, "자전거지도");
		menuItem.setAlphabeticShortcut('b');
		menuItem.setCheckable(true);
		menuItem.setChecked(false);

		menuItem = menu.add(0, MENU_ITEM_ZOOM_CONTROLS, Menu.CATEGORY_SECONDARY, "Zoom Controls");
		menuItem.setAlphabeticShortcut('z');
		menuItem.setIcon(android.R.drawable.ic_menu_zoom);

		menuItem = menu.add(0, MENU_ITEM_MY_LOCATION, Menu.CATEGORY_SECONDARY, "내위치");
		menuItem.setAlphabeticShortcut('l');
		menuItem.setIcon(android.R.drawable.ic_menu_mylocation);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu pMenu) {
		super.onPrepareOptionsMenu(pMenu);

		int viewMode = mMapController.getMapViewMode();
		boolean isTraffic = mMapController.getMapViewTrafficMode();
		boolean isBicycle = mMapController.getMapViewBicycleMode();

		pMenu.findItem(MENU_ITEM_CLEAR_MAP).setEnabled(
			(viewMode != NMapView.VIEW_MODE_VECTOR) || isTraffic || mOverlayManager.sizeofOverlays() > 0);
		pMenu.findItem(MENU_ITEM_MAP_MODE_SUB_VECTOR).setChecked(viewMode == NMapView.VIEW_MODE_VECTOR);
		pMenu.findItem(MENU_ITEM_MAP_MODE_SUB_SATELLITE).setChecked(viewMode == NMapView.VIEW_MODE_HYBRID);
		pMenu.findItem(MENU_ITEM_MAP_MODE_SUB_TRAFFIC).setChecked(isTraffic);
		pMenu.findItem(MENU_ITEM_MAP_MODE_SUB_BICYCLE).setChecked(isBicycle);

		if (mMyLocationOverlay == null) {
			pMenu.findItem(MENU_ITEM_MY_LOCATION).setEnabled(false);
		}

		return true;
	}

	/**
	 * Invoked when the user selects an item from the Menu.
	 * 
	 * @param item the Menu entry which was selected
	 * @return true if the Menu item was legit (and we consumed it), false
	 *         otherwise
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case MENU_ITEM_CLEAR_MAP:
				if (mMyLocationOverlay != null) {
					stopMyLocation();
					mOverlayManager.removeOverlay(mMyLocationOverlay);
				}

				mMapController.setMapViewMode(NMapView.VIEW_MODE_VECTOR);
				mMapController.setMapViewTrafficMode(false);
				mMapController.setMapViewBicycleMode(false);

				mOverlayManager.clearOverlays();

				return true;

			case MENU_ITEM_MAP_MODE_SUB_VECTOR:
				mMapController.setMapViewMode(NMapView.VIEW_MODE_VECTOR);
				return true;

			case MENU_ITEM_MAP_MODE_SUB_SATELLITE:
				mMapController.setMapViewMode(NMapView.VIEW_MODE_HYBRID);
				return true;

			case MENU_ITEM_MAP_MODE_SUB_TRAFFIC:
				mMapController.setMapViewTrafficMode(!mMapController.getMapViewTrafficMode());
				return true;

			case MENU_ITEM_MAP_MODE_SUB_BICYCLE:
				mMapController.setMapViewBicycleMode(!mMapController.getMapViewBicycleMode());
				return true;

			case MENU_ITEM_ZOOM_CONTROLS:
				mMapView.displayZoomControls(true);
				return true;

			case MENU_ITEM_MY_LOCATION:
				startMyLocation();
				return true;
				
			case MENU_ITEM_TEST_PATH_DATA:
				mOverlayManager.clearOverlays();

				// add path data overlay
				trackDataOverlayFromID(1);
				return true;

			case MENU_ITEM_TEST_AUTO_ROTATE:
				if (mMapView.isAutoRotateEnabled()) {
					mMapView.setAutoRotateEnabled(false, false);

					mMapContainerView.requestLayout();

					mHnadler.removeCallbacks(mTestAutoRotation);
				} else {

					mMapView.setAutoRotateEnabled(true, false);

					mMapView.setRotateAngle(30);
					mHnadler.postDelayed(mTestAutoRotation, AUTO_ROTATE_INTERVAL);

					mMapContainerView.requestLayout();
				}
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private static final long AUTO_ROTATE_INTERVAL = 2000;
	private final Handler mHnadler = new Handler();
	private final Runnable mTestAutoRotation = new Runnable() {
		@Override
		public void run() {
		}
	};

	/** 
	 * Container view class to rotate map view.
	 */
	private class MapContainerView extends ViewGroup {

		public MapContainerView(Context context) {
			super(context);
		}

		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
			final int width = getWidth();
			final int height = getHeight();
			final int count = getChildCount();
			for (int i = 0; i < count; i++) {
				final View view = getChildAt(i);
				final int childWidth = view.getMeasuredWidth();
				final int childHeight = view.getMeasuredHeight();
				final int childLeft = (width - childWidth) / 2;
				final int childTop = (height - childHeight) / 2;
				view.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
			}

			if (changed) {
				mOverlayManager.onSizeChanged(width, height);
			}
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int w = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
			int h = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
			int sizeSpecWidth = widthMeasureSpec;
			int sizeSpecHeight = heightMeasureSpec;

			final int count = getChildCount();
			for (int i = 0; i < count; i++) {
				final View view = getChildAt(i);

				if (view instanceof NMapView) {
					if (mMapView.isAutoRotateEnabled()) {
						int diag = (((int)(Math.sqrt(w * w + h * h)) + 1) / 2 * 2);
						sizeSpecWidth = MeasureSpec.makeMeasureSpec(diag, MeasureSpec.EXACTLY);
						sizeSpecHeight = sizeSpecWidth;
					}
				}

				view.measure(sizeSpecWidth, sizeSpecHeight);
			}
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}
}
