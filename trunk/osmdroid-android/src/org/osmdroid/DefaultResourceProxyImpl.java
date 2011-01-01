package org.osmdroid;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.andnav.osm.ResourceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

public class DefaultResourceProxyImpl implements ResourceProxy {

	private static final Logger logger = LoggerFactory.getLogger(DefaultResourceProxyImpl.class);

	private DisplayMetrics mDisplayMetrics;

	/**
	 * Constructor.
	 * @param pContext Used to get the display metrics that are used for scaling the bitmaps
	 *                 returned by {@link getBitmap}.
	 *                 Can be null, in which case the bitmaps are not scaled.
	 */
	public DefaultResourceProxyImpl(final Context pContext) {
		if (pContext != null) {
			mDisplayMetrics = pContext.getResources().getDisplayMetrics();
			logger.debug("mDisplayMetrics=" + mDisplayMetrics);
		}
	}

	@Override
	public String getString(final string pResId) {
		switch(pResId) {
		case osmarender : return "Osmarender";
		case mapnik : return "Mapnik";
		case cyclemap : return "Cycle Map";
		case public_transport : return "Public transport";
		case base : return "OSM base layer";
		case topo : return "Topographic";
		case hills : return "Hills";
		case cloudmade_standard : return "CloudMade (Standard tiles)";
		case cloudmade_small : return "CloudMade (small tiles)";
		case fiets_nl : return "OpenFietsKaart overlay";
		case base_nl : return "Netherlands base overlay";
		case roads_nl : return "Netherlands roads overlay";
		case unknown : return "Unknown";
		case format_distance_meters : return "%s m";
		case format_distance_kilometers : return "%s km";
		case format_distance_miles : return "%s mi";
		case format_distance_nautical_miles : return "%s nm";
		case format_distance_feet : return "%s ft";
		default : throw new IllegalArgumentException();
		}
	}

	@Override
	public String getString(string pResId, Object... formatArgs) {
		return String.format(getString(pResId), formatArgs);
	}


	@Override
	public Bitmap getBitmap(final bitmap pResId) {
		InputStream is = null;
		try {
			is = getClass().getResourceAsStream(pResId.name() + ".png");
			if (is == null) {
				throw new IllegalArgumentException();
			}
			BitmapFactory.Options options = null;
			if (mDisplayMetrics != null) {
				options = getBitmapOptions();
			}
			return BitmapFactory.decodeStream(is, null, options);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (final IOException ignore) {
				}
			}
		}
	}

	private BitmapFactory.Options getBitmapOptions() {
		try {
			final Field density = DisplayMetrics.class.getDeclaredField("DENSITY_DEFAULT");
			final Field inDensity = BitmapFactory.Options.class.getDeclaredField("inDensity");
			final Field inTargetDensity = BitmapFactory.Options.class.getDeclaredField("inTargetDensity");
			final Field targetDensity = DisplayMetrics.class.getDeclaredField("densityDpi");
			final BitmapFactory.Options options = new BitmapFactory.Options();
			inDensity.setInt(options, density.getInt(null));
			inTargetDensity.setInt(options, targetDensity.getInt(mDisplayMetrics));
			return options;
		} catch (final IllegalAccessException ex) {
			// ignore
		} catch (final NoSuchFieldException ex) {
			// ignore
		}
		return null;
	}

	@Override
	public Drawable getDrawable(final bitmap pResId) {
		return new BitmapDrawable(getBitmap(pResId));
	}

}
