package org.andnav.osm.views.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import org.andnav.osm.services.util.OpenStreetMapTile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.os.Handler;
import android.os.RemoteException;
import android.test.AndroidTestCase;
import android.test.IsolatedContext;

/**
 * @author Neil Boyd
 *
 */
public class OpenStreetMapTileProviderTest extends AndroidTestCase {

	OpenStreetMapTileProvider mProvider;

	@Override
	protected void setUp() throws Exception {

		final Context context = new IsolatedContext(null, getContext());
		mProvider = new OpenStreetMapTileProvider(context, new Handler());
		
		super.setUp();
	}

	public void test_getMapTile_not_found() {
		final OpenStreetMapTile tile = new OpenStreetMapTile(1, 2, 3, 4);
		
		final Bitmap bitmap = mProvider.getMapTile(tile);
		
		assertNull("Expect tile to be null", bitmap);
	}

	public void test_getMapTile_found() throws RemoteException, FileNotFoundException {
		final OpenStreetMapTile tile = new OpenStreetMapTile(1, 2, 3, 4);
		
		// create a bitmap, draw something on it, write it to a file and put it in the cache
		final String path = "/sdcard/andnav2/OpenStreetMapTileProviderTest.png";
		final Bitmap bitmap1 = Bitmap.createBitmap(60, 30, Config.ARGB_8888);
		bitmap1.eraseColor(Color.YELLOW);
		final Canvas canvas = new Canvas(bitmap1);
		canvas.drawText("test", 10, 20, new Paint());
		final FileOutputStream fos = new FileOutputStream(path);
		bitmap1.compress(CompressFormat.PNG, 100, fos);
		mProvider.mServiceCallback.mapTileRequestCompleted(tile.rendererID, tile.zoomLevel, tile.x, tile.y, path);

		// do the test
		final Bitmap bitmap2 = mProvider.getMapTile(tile);

		// compare a few things to see if it's the same bitmap
		assertEquals("Compare config", bitmap1.getConfig(), bitmap2.getConfig());
		assertEquals("Compare width", bitmap1.getWidth(), bitmap2.getWidth());
		assertEquals("Compare height", bitmap1.getHeight(), bitmap2.getHeight());

		// compare the total thing
		final ByteBuffer bb1 = ByteBuffer.allocate(bitmap1.getWidth() * bitmap1.getHeight() * 4);
		bitmap1.copyPixelsToBuffer(bb1);
		final ByteBuffer bb2 = ByteBuffer.allocate(bitmap2.getWidth() * bitmap2.getHeight() * 4);
		bitmap2.copyPixelsToBuffer(bb2);
		assertEquals("Compare pixels", bb1, bb2);
	}
}
