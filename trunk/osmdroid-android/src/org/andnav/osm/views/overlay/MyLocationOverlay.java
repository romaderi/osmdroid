// Created by plusminus on 22:01:11 - 29.09.2008
package org.andnav.osm.views.overlay;

import java.util.LinkedList;

import org.andnav.osm.DefaultResourceProxyImpl;
import org.andnav.osm.ResourceProxy;
import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.NetworkLocationIgnorer;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;
import org.andnav.osm.views.OpenStreetMapViewController;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay.Snappable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

/**
 *
 * @author Manuel Stahl
 *
 */
public class MyLocationOverlay extends OpenStreetMapViewOverlay implements SensorEventListener, LocationListener, Snappable {

	public static final String DEBUGTAG = "OPENSTREETMAP";

	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected final Paint mPaint = new Paint();
	protected final Paint mCirclePaint = new Paint();

	protected final Bitmap PERSON_ICON;
	protected final Bitmap DIRECTION_ARROW;

	private final OpenStreetMapView mMapView;
	private final OpenStreetMapViewController mMapController;
	private final LocationManager mLocationManager;
	private final SensorManager mSensorManager;

	private boolean mMyLocationEnabled = false;
	private LinkedList<Runnable> mRunOnFirstFix = new LinkedList<Runnable>();
	private final Point mMapCoords = new Point();

	private Location mLocation;
	private long mLocationUpdateMinTime = 0;
	private float mLocationUpdateMinDistance = 0.0f;
	protected boolean mFollow = false;	// follow location updates
	private NetworkLocationIgnorer mIgnorer = new NetworkLocationIgnorer();

	private final Matrix directionRotater = new Matrix();

	/** Coordinates the feet of the person are located. */
	protected final android.graphics.Point PERSON_HOTSPOT = new android.graphics.Point(24,39);

	private final float DIRECTION_ARROW_CENTER_X;
	private final float DIRECTION_ARROW_CENTER_Y;

	// Compass values
	private boolean mCompassEnabled = false;

	protected final Picture mCompassFrame = new Picture();
	protected final Picture mCompassRose = new Picture();

	private float mAzimuth = 0.0f;

	private float mCompassCenterX = 35.0f;
	private float mCompassCenterY = 35.0f;
	private float mCompassRadius = 20.0f;

	private float mScale = 1.0f;

	// ===========================================================
	// Constructors
	// ===========================================================

	public MyLocationOverlay(final Context ctx, final OpenStreetMapView mapView) {
		this(ctx, mapView, new DefaultResourceProxyImpl(ctx));
	}

	public MyLocationOverlay(final Context ctx, final OpenStreetMapView mapView, final ResourceProxy pResourceProxy) {
		super(pResourceProxy);
		mMapView = mapView;
		mLocationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
		mSensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
		mMapController = mapView.getController();
		mCirclePaint.setARGB(0, 100, 100, 255);
		mCirclePaint.setAntiAlias(true);

		PERSON_ICON = mResourceProxy.getBitmap(ResourceProxy.bitmap.person);
		DIRECTION_ARROW = mResourceProxy.getBitmap(ResourceProxy.bitmap.direction_arrow);

		DIRECTION_ARROW_CENTER_X = DIRECTION_ARROW.getWidth() / 2 - 0.5f;
		DIRECTION_ARROW_CENTER_Y = DIRECTION_ARROW.getHeight() / 2 - 0.5f;

		mScale = ctx.getResources().getDisplayMetrics().density;

		createCompassFramePicture();
		createCompassRosePicture();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public Location getLastFix() {
		return mLocation;
	}

	/**
	 * Return a GeoPoint of the last known location, or null if not known.
	 */
	public GeoPoint getMyLocation() {
		if (mLocation == null) {
			return null;
		} else {
			return new GeoPoint(mLocation);
		}
	}

	public boolean isMyLocationEnabled() {
		return mMyLocationEnabled;
	}

	public boolean isCompassEnabled() {
		return mCompassEnabled;
	}

	public boolean isLocationFollowEnabled() {
		return mFollow;
	}

	public void followLocation(boolean enable) {
		mFollow = enable;
	}

	public long getLocationUpdateMinTime() {
		return mLocationUpdateMinTime;
	}

	/**
	 * Set the minimum interval for location updates.
	 * See {@link LocationManager.requestLocationUpdates(String, long, float, LocationListener)}.
	 * Note that you should call this before calling {@link enableMyLocation()}.
	 * @param milliSeconds
	 */
	public void setLocationUpdateMinTime(final long milliSeconds) {
		mLocationUpdateMinTime = milliSeconds;
	}

	public float getLocationUpdateMinDistance() {
		return mLocationUpdateMinDistance;
	}

	/**
	 * Set the minimum distance for location updates.
	 * See {@link LocationManager.requestLocationUpdates}.
	 * Note that you should call this before calling {@link enableMyLocation()}.
	 * @param meters
	 */
	public void setLocationUpdateMinDistance(final float meters) {
		mLocationUpdateMinDistance = meters;
	}

	public void setCompassCenter(float x, float y) {
		mCompassCenterX = x;
		mCompassCenterY = y;
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onDrawFinished(Canvas c, OpenStreetMapView osmv) {}

	@Override
	public void onDraw(final Canvas c, final OpenStreetMapView osmv) {
		if(this.mLocation != null) {
			final OpenStreetMapViewProjection pj = osmv.getProjection();
			pj.toMapPixels(new GeoPoint(mLocation), mMapCoords);
			final float radius = pj.metersToEquatorPixels(this.mLocation.getAccuracy());

			this.mCirclePaint.setAlpha(50);
			this.mCirclePaint.setStyle(Style.FILL);
			c.drawCircle(mMapCoords.x, mMapCoords.y, radius, this.mCirclePaint);

			this.mCirclePaint.setAlpha(150);
			this.mCirclePaint.setStyle(Style.STROKE);
			c.drawCircle(mMapCoords.x, mMapCoords.y, radius, this.mCirclePaint);

			float[] mtx = new float[9];
			c.getMatrix().getValues(mtx);

			if (DEBUGMODE) {
				float tx = (-mtx[Matrix.MTRANS_X]+20)/mtx[Matrix.MSCALE_X];
				float ty = (-mtx[Matrix.MTRANS_Y]+90)/mtx[Matrix.MSCALE_Y];
				c.drawText("Lat: " + mLocation.getLatitude(),  tx, ty +  5, this.mPaint);
				c.drawText("Lon: " + mLocation.getLongitude(), tx, ty + 20, this.mPaint);
				c.drawText("Alt: " + mLocation.getAltitude(),  tx, ty + 35, this.mPaint);
				c.drawText("Acc: " + mLocation.getAccuracy(),  tx, ty + 50, this.mPaint);
			}

			if (mLocation.hasSpeed() && mLocation.getSpeed() > 1) {
				/* Rotate the direction-Arrow according to the bearing we are driving. And draw it to the canvas. */
				this.directionRotater.setRotate(this.mLocation.getBearing(), DIRECTION_ARROW_CENTER_X , DIRECTION_ARROW_CENTER_Y);
				this.directionRotater.postTranslate(-DIRECTION_ARROW_CENTER_X, -DIRECTION_ARROW_CENTER_Y);
				this.directionRotater.postScale(1/mtx[Matrix.MSCALE_X], 1/mtx[Matrix.MSCALE_Y]);
				this.directionRotater.postTranslate(mMapCoords.x, mMapCoords.y);
				c.drawBitmap(DIRECTION_ARROW, this.directionRotater, this.mPaint);
			} else {
				this.directionRotater.setTranslate(-PERSON_HOTSPOT.x, -PERSON_HOTSPOT.y);
				this.directionRotater.postScale(1/mtx[Matrix.MSCALE_X], 1/mtx[Matrix.MSCALE_Y]);
				this.directionRotater.postTranslate(mMapCoords.x, mMapCoords.y);
				c.drawBitmap(PERSON_ICON, this.directionRotater, this.mPaint);
			}
		}

		if (mCompassEnabled) { // && (mAzimuth >= 0.0f)

			float[] matrix = new float[9];
			c.getMatrix().getValues(matrix);

			final float tx = (-matrix[Matrix.MTRANS_X])/matrix[Matrix.MSCALE_X];
			final float ty = (-matrix[Matrix.MTRANS_Y])/matrix[Matrix.MSCALE_Y];

			final float centerX = tx + (mCompassCenterX * mScale);
			final float centerY = ty + (mCompassCenterY * mScale) + (c.getHeight() - mMapView.getHeight());

			final int left = (int) centerX - mCompassFrame.getWidth() / 2;
			final int top = (int) centerY - mCompassFrame.getHeight() / 2;

			final Rect drawingTarget = new Rect(left, top, left + mCompassFrame.getWidth(), top + mCompassFrame.getHeight());

			c.drawPicture(mCompassFrame, drawingTarget);

			c.save();
			c.rotate(-mAzimuth, centerX, centerY);
			c.drawPicture(mCompassRose, drawingTarget);
			c.restore();
		}

	}

	@Override
	public void onLocationChanged(final Location location) {
		if (DEBUGMODE) {
			Log.d(DEBUGTAG, "onLocationChanged(" + location + ")");
		}

		// ignore temporary non-gps fix
		if (mIgnorer.shouldIgnore(location.getProvider(), System.currentTimeMillis())) {
			Log.d(DEBUGTAG, "Ignore temporary non-gps location");
			return;
		}

		mLocation = location;
		if (mFollow) {
			mMapController.animateTo(new GeoPoint(location));
		} else {
			mMapView.invalidate(); // redraw the my location icon
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if(status == LocationProvider.AVAILABLE) {
			final Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					for(Runnable runnable: mRunOnFirstFix) {
						runnable.run();
					}
					mRunOnFirstFix.clear();
				}
			});
			t.run();
		}
	}

	@Override
	public boolean onSnapToItem(int x, int y, Point snapPoint, OpenStreetMapView mapView) {
		if(this.mLocation != null) {
			final OpenStreetMapViewProjection pj = mapView.getProjection();
			pj.toMapPixels(new GeoPoint(mLocation), mMapCoords);
			snapPoint.x = mMapCoords.x;
			snapPoint.y = mMapCoords.y;

			boolean snap = (x - mMapCoords.x)*(x - mMapCoords.x) + (y - mMapCoords.y)*(y - mMapCoords.y) < 64;
			return snap;
		} else {
			return false;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, OpenStreetMapView mapView) {
		if (event.getAction() == MotionEvent.ACTION_MOVE)
			mFollow = false;

		return super.onTouchEvent(event, mapView);
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if ((mCompassEnabled) && (event.sensor.getType() == Sensor.TYPE_ORIENTATION))
		{
			if (event.values != null)
			{
				mAzimuth = (float) event.values[0];
				mMapView.invalidate();
			}
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public void disableMyLocation() {
		mLocationManager.removeUpdates(this);
		mMyLocationEnabled = false;
	}

	/**
	 * Enable location updates so that the map follows the current location.
	 * By default this will request location updates as frequently as possible,
	 * but you can change the frequency and/or distance by calling
	 * {@link setLocationUpdateMinTime(long)} and/or {@link setLocationUpdateMinDistance(float)}
	 * before calling this method.
	 */
	public boolean enableMyLocation() {
		if (!mMyLocationEnabled) {
			for (final String provider : mLocationManager.getAllProviders()) {
				mLocationManager.requestLocationUpdates(provider, mLocationUpdateMinTime, mLocationUpdateMinDistance, this);
			}
		}
		return mMyLocationEnabled = true;
	}

	public void enableCompass() {
		if (!mCompassEnabled) {
			final Sensor sensorOrientation = this.mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			mSensorManager.registerListener(this, sensorOrientation, SensorManager.SENSOR_DELAY_UI);
		}
		mCompassEnabled = true;
	}

	public void disableCompass() {
		mSensorManager.unregisterListener(this);
		mCompassEnabled = false;
	}

	public boolean runOnFirstFix(Runnable runnable) {
		if(mMyLocationEnabled) {
			runnable.run();
			return true;
		} else {
			mRunOnFirstFix.addLast(runnable);
			return false;
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	private Point calculatePointOnCircle(float centerX, float centerY, float radius, float degrees) {
		// for trigonometry, 0 is pointing east, so subtract 90
		// compass degrees are the wrong way round
		final double dblRadians = Math.toRadians(-degrees + 90);

		final int intX = (int) (radius * Math.cos(dblRadians));
		final int intY = (int) (radius * Math.sin(dblRadians));

		return new Point((int) centerX + intX, (int) centerY - intY);
	}

	private void drawTriangle(Canvas canvas, float x, float y, float radius, float degrees, Paint paint) {
		canvas.save();
		final Point point = this.calculatePointOnCircle(x, y, radius, degrees);
		canvas.rotate(degrees, point.x, point.y);
		final Path p = new Path();
		p.moveTo(point.x - 2 * mScale, point.y);
		p.lineTo(point.x + 2 * mScale, point.y);
		p.lineTo(point.x, point.y - 5 * mScale);
		p.close();
		canvas.drawPath(p, paint);
		canvas.restore();
	}

	private void createCompassFramePicture() {
		// The inside of the compass is white and transparent
		final Paint innerPaint = new Paint();
		innerPaint.setColor(Color.WHITE);
		innerPaint.setAntiAlias(true);
		innerPaint.setStyle(Style.FILL);
		innerPaint.setAlpha(200);

		// The outer part (circle and little triangles) is gray and transparent
		final Paint outerPaint = new Paint();
		outerPaint.setColor(Color.GRAY);
		outerPaint.setAntiAlias(true);
		outerPaint.setStyle(Style.STROKE);
		outerPaint.setStrokeWidth(2.0f);
		outerPaint.setAlpha(200);

		final int picBorderWidthAndHeight = (int) ((mCompassRadius + 5) * 2 * mScale);
		final int center = picBorderWidthAndHeight / 2;

		final Canvas canvas = mCompassFrame.beginRecording(picBorderWidthAndHeight, picBorderWidthAndHeight);

		// draw compass inner circle and border
		canvas.drawCircle(center, center, mCompassRadius * mScale, innerPaint);
		canvas.drawCircle(center, center, mCompassRadius * mScale, outerPaint);

		// Draw little triangles north, south, west and east (don't move)
		// to make those move use "-bearing + 0" etc. (Note: that would mean to draw the triangles in the onDraw() method)
		drawTriangle(canvas, center, center, mCompassRadius * mScale, 0, outerPaint);
		drawTriangle(canvas, center, center, mCompassRadius * mScale, 90, outerPaint);
		drawTriangle(canvas, center, center, mCompassRadius * mScale, 180, outerPaint);
		drawTriangle(canvas, center, center, mCompassRadius * mScale, 270, outerPaint);

		mCompassFrame.endRecording();
	}

	private void createCompassRosePicture() {
		// Paint design of north triangle (it's common to paint north in blue color)
		final Paint northPaint = new Paint();
		northPaint.setColor(0xFF0000A0);
		northPaint.setAntiAlias(true);
		northPaint.setStyle(Style.FILL);
		northPaint.setAlpha(220);

		// Paint design of south triangle (red)
		final Paint southPaint = new Paint();
		southPaint.setColor(0xFFA00000);
		southPaint.setAntiAlias(true);
		southPaint.setStyle(Style.FILL);
		southPaint.setAlpha(220);

		final int picBorderWidthAndHeight = (int) ((mCompassRadius + 5) * 2 * mScale);
		final int center = picBorderWidthAndHeight / 2;

		final Canvas canvas = mCompassRose.beginRecording(picBorderWidthAndHeight, picBorderWidthAndHeight);

		// Blue triangle pointing north
		final Path pathNorth = new Path();
		pathNorth.moveTo(center, center - (mCompassRadius - 3) * mScale);
		pathNorth.lineTo(center + 4 * mScale, center);
		pathNorth.lineTo(center - 4 * mScale, center);
		pathNorth.lineTo(center, center - (mCompassRadius - 3) * mScale);
		pathNorth.close();
		canvas.drawPath(pathNorth, northPaint);

		// Red triangle pointing south
		final Path pathSouth = new Path();
		pathSouth.moveTo(center, center + (mCompassRadius - 3) * mScale);
		pathSouth.lineTo(center + 4 * mScale, center);
		pathSouth.lineTo(center - 4 * mScale, center);
		pathSouth.lineTo(center, center + (mCompassRadius - 3) * mScale);
		pathSouth.close();
		canvas.drawPath(pathSouth, southPaint);

		mCompassRose.endRecording();
	}
}
