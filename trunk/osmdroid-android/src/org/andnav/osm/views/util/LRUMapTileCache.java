package org.andnav.osm.views.util;

import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.andnav.osm.tileprovider.OpenStreetMapTile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class LRUMapTileCache extends LinkedHashMap<OpenStreetMapTile, Drawable> {

	private static final Logger logger = LoggerFactory.getLogger(LRUMapTileCache.class);

	private static final long serialVersionUID = -541142277575493335L;

	private int mCapacity;

	public LRUMapTileCache(final int aCapacity) {
		super(aCapacity + 2, 0.1f, true);
		mCapacity = aCapacity;
	}

	public void ensureCapacity(final int aCapacity) {
		if (aCapacity > mCapacity) {
			mCapacity = aCapacity;
		}
	}

	@Override
	public Drawable remove(final Object aKey) {
		final Drawable drawable = super.remove(aKey);
		if (drawable instanceof BitmapDrawable) {
			final Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
			if (bitmap != null) {
				bitmap.recycle();
			}
		}
		return drawable;
	}

	@Override
	public void clear() {
		// remove them all individually so that they get recycled
		try {
			for (final OpenStreetMapTile key : keySet()) {
				remove(key);
			}
		} catch (final ConcurrentModificationException ignore) {
			logger.info("ConcurrentModificationException clearing tile cache");
		}

		// and then clear
		super.clear();
	}

	@Override
	protected boolean removeEldestEntry(final Entry<OpenStreetMapTile, Drawable> aEldest) {
		return size() > mCapacity;
	}

}
