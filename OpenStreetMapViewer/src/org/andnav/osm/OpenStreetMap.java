// Created by plusminus on 00:23:14 - 03.10.2008
package org.andnav.osm;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.overlay.MyLocationOverlay;
import org.andnav.osm.views.util.OpenStreetMapRendererInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;


/**
 * Default map view activity.
 * @author Manuel Stahl
 *
 */
public class OpenStreetMap extends Activity implements OpenStreetMapConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int MENU_MY_LOCATION = Menu.FIRST;
	private static final int MENU_MAP_MODE = MENU_MY_LOCATION + 1;
	private static final int MENU_ABOUT = MENU_MAP_MODE + 1;
	
	private static final int DIALOG_ABOUT_ID = 1;

	// ===========================================================
	// Fields
	// ===========================================================
	
	private SharedPreferences mPrefs;
	private OpenStreetMapView mOsmv;
	private MyLocationOverlay mLocationOverlay;

	// ===========================================================
	// Constructors
	// ===========================================================
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        final RelativeLayout rl = new RelativeLayout(this);
        
        this.mOsmv = new OpenStreetMapView(this, OpenStreetMapRendererInfo.values()[mPrefs.getInt(PREFS_RENDERER, OpenStreetMapRendererInfo.MAPNIK.ordinal())]);
        this.mLocationOverlay = new MyLocationOverlay(this.getBaseContext(), this.mOsmv);
        this.mOsmv.setBuiltInZoomControls(true);
        this.mOsmv.getOverlays().add(this.mLocationOverlay);
        rl.addView(this.mOsmv, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        this.setContentView(rl);
    
    	mOsmv.getController().setZoom(mPrefs.getInt(PREFS_ZOOM_LEVEL, 1));
    	mOsmv.scrollTo(mPrefs.getInt(PREFS_SCROLL_X, 0), mPrefs.getInt(PREFS_SCROLL_Y, 0));
    }
        
    @Override
    protected void onPause() {
    	SharedPreferences.Editor edit = mPrefs.edit();
    	edit.putInt(PREFS_RENDERER, mOsmv.getRenderer().ordinal());
    	edit.putInt(PREFS_SCROLL_X, mOsmv.getScrollX());
    	edit.putInt(PREFS_SCROLL_Y, mOsmv.getScrollY());
    	edit.putInt(PREFS_ZOOM_LEVEL, mOsmv.getZoomLevel());
    	edit.putBoolean(PREFS_SHOW_LOCATION, mLocationOverlay.isMyLocationEnabled());
    	edit.putBoolean(PREFS_FOLLOW_LOCATION, mLocationOverlay.isLocationFollowEnabled());
    	edit.commit();

    	this.mLocationOverlay.disableMyLocation();
    	
    	super.onPause();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	mOsmv.setRenderer(OpenStreetMapRendererInfo.values()[mPrefs.getInt(PREFS_RENDERER, OpenStreetMapRendererInfo.MAPNIK.ordinal())]);
    	if(mPrefs.getBoolean(PREFS_SHOW_LOCATION, false))
    		this.mLocationOverlay.enableMyLocation();
    	this.mLocationOverlay.followLocation(mPrefs.getBoolean(PREFS_FOLLOW_LOCATION, true));
    }
 
    @Override
	public boolean onCreateOptionsMenu(final Menu pMenu) {
    	pMenu.add(0, MENU_MY_LOCATION, Menu.NONE, R.string.my_location).setIcon(android.R.drawable.ic_menu_mylocation);

    	{
    	int id = 1000;
			final SubMenu mapMenu = pMenu.addSubMenu(0, MENU_MAP_MODE,
					Menu.NONE, R.string.map_mode).setIcon(
					android.R.drawable.ic_menu_mapmode);

			for (OpenStreetMapRendererInfo renderer : OpenStreetMapRendererInfo
					.values()) {
				mapMenu.add(MENU_MAP_MODE, id++, Menu.NONE, getString(renderer.NAME));
			}
			mapMenu.setGroupCheckable(MENU_MAP_MODE, true, true);
    	}
    	
    	pMenu.add(0, MENU_ABOUT, Menu.NONE, R.string.about).setIcon(android.R.drawable.ic_menu_info_details);
    	
    	return true;
	}
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	int id = mOsmv.getRenderer().ordinal();
    	menu.findItem(1000 + id).setChecked(true);
    	return true;
    }
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
			case MENU_MY_LOCATION:
				this.mLocationOverlay.followLocation(true);
	    		this.mLocationOverlay.enableMyLocation();
	    		Location lastFix = this.mLocationOverlay.getLastFix();
	    		if (lastFix != null)
	    			this.mOsmv.setMapCenter(new GeoPoint(lastFix));
				return true;
			
			case MENU_MAP_MODE:
				this.mOsmv.invalidate();
				return true;
				
			case MENU_ABOUT:
				showDialog(DIALOG_ABOUT_ID);
				return true;
								
			default:	// Map mode submenu items 
				this.mOsmv.setRenderer(OpenStreetMapRendererInfo.values()[item.getItemId() - 1000]);
		}
		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;

		switch (id) {
		case DIALOG_ABOUT_ID:
			return new AlertDialog.Builder(OpenStreetMap.this)
            .setIcon(R.drawable.icon)
            .setTitle(R.string.app_name)
            .setMessage(R.string.about_message)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {}
            }).create();

		default:
			dialog = null;
			break;
		}
		return dialog;
	}
	
	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		return this.mOsmv.onTrackballEvent(event);
	}
	
    @Override
    public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_MOVE)
			this.mLocationOverlay.followLocation(false);

    	return super.onTouchEvent(event);
    }

	// ===========================================================
	// Methods
	// ===========================================================
    
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
