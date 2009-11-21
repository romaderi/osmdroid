package org.andnav.osm.views.overlay;

import org.andnav.osm.services.OpenStreetMapTileProviderService;
import org.andnav.osm.services.util.OpenStreetMapTile;
import org.andnav.osm.util.MyMath;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;
import org.andnav.osm.views.util.OpenStreetMapRendererInfo;
import org.andnav.osm.views.util.OpenStreetMapTileProvider;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;

public class OpenStreetMapTilesOverlay extends OpenStreetMapViewOverlay {

	protected OpenStreetMapView mOsmv;
	protected OpenStreetMapRendererInfo mRendererInfo;

	/** Current renderer */
	protected final OpenStreetMapTileProvider mTileProvider;
	protected final Paint mPaint = new Paint();

	public OpenStreetMapTilesOverlay(final OpenStreetMapView aOsmv,
			final OpenStreetMapRendererInfo aRendererInfo,
			final OpenStreetMapTileProvider aTileProvider) {
		this.mOsmv = aOsmv;
		this.mRendererInfo = aRendererInfo;
		if(aTileProvider == null)
			mTileProvider = new OpenStreetMapTileProvider(mOsmv.getContext(), new SimpleInvalidationHandler());
		else
			this.mTileProvider = aTileProvider;
	}
	
	public OpenStreetMapRendererInfo getRendererInfo() {
		return mRendererInfo;
	}
	
	public void setRendererInfo(final OpenStreetMapRendererInfo aRendererInfo) {
		this.mRendererInfo = aRendererInfo;
	}

	public void setAlpha(int a) {
		this.mPaint.setAlpha(a);
	}
	
	@Override
	protected void onDraw(Canvas c, OpenStreetMapView osmv) {
		/*
		 * Do some calculations and drag attributes to local variables to save
		 * some performance.
		 */
		final OpenStreetMapViewProjection pj = osmv.getProjection();
		final int zoomLevel = osmv.getZoomLevel();
		final Rect viewPort = c.getClipBounds();
		final int tileSizePx = this.mRendererInfo.MAPTILE_SIZEPX;
		final int tileZoom = this.mRendererInfo.MAPTILE_ZOOM;
		final int worldSize_2 = 1 << (zoomLevel + this.mRendererInfo.MAPTILE_ZOOM - 1);
		final OpenStreetMapTile tile = new OpenStreetMapTile(0, 0, 0, 0);
		tile.rendererID = this.mRendererInfo.ordinal();	// TODO get from service
		
		/*
		 * Calculate the amount of tiles needed for each side around the center 
		 * one.
		 */
		viewPort.offset(worldSize_2, worldSize_2);
		final int tileNeededToLeftOfCenter = (viewPort.left >> tileZoom) - 1;
		final int tileNeededToRightOfCenter = viewPort.right >> tileZoom;
		final int tileNeededToTopOfCenter = (viewPort.top >> tileZoom) - 1;
		final int tileNeededToBottomOfCenter = viewPort.bottom >> tileZoom;

		final int mapTileUpperBound = 1 << zoomLevel;
//		final Point mapTileCoords = new Point();
		Point tilePos = new Point();

		/* Draw all the MapTiles (from the upper left to the lower right). */
		for (int y = tileNeededToTopOfCenter; y <= tileNeededToBottomOfCenter; y++) {
			for (int x = tileNeededToLeftOfCenter; x <= tileNeededToRightOfCenter; x++) {
				/* Construct a URLString, which represents the MapTile. */
				tile.zoomLevel = zoomLevel;
				tile.y = MyMath.mod(y, mapTileUpperBound);
				tile.x = MyMath.mod(x, mapTileUpperBound);

				pj.toPixels(x, y, tilePos);
				if (this.mTileProvider.isTileAvailable(tile)) {
				/* Draw the MapTile 'i tileSizePx' above of the centerMapTile */
					final Bitmap currentMapTile = this.mTileProvider.getMapTile(tile);
					c.drawBitmap(currentMapTile, tilePos.x, tilePos.y, this.mPaint);
				} else {
					this.mTileProvider.preCacheTile(tile);
					if (zoomLevel > 0) {
						int offX = tile.x % 2;
						int offY = tile.y % 2;
						tile.zoomLevel = zoomLevel - 1;
						tile.x >>= 1;
						tile.y >>= 1;
						if (this.mTileProvider.isTileAvailable(tile)) {
							final Bitmap currentMapTile = this.mTileProvider.getMapTile(tile);
							if (currentMapTile != null) {
								final Rect src = new Rect(offX*tileSizePx/2, offY* tileSizePx/2, (offX+1)*tileSizePx/2, (offY+1)* tileSizePx/2); 
								final Rect dst = new Rect(tilePos.x, tilePos.y, tilePos.x+tileSizePx, tilePos.y+tileSizePx);
								c.drawBitmap(currentMapTile, src, dst, mPaint);
							}
						}
					}
				}

				if (OpenStreetMapViewConstants.DEBUGMODE) {
					c.drawLine(tilePos.x, tilePos.y, tilePos.x + tileSizePx, tilePos.y, this.mPaint);
					c.drawLine(tilePos.x, tilePos.y, tilePos.x, tilePos.y + tileSizePx, this.mPaint);
				}
			}
		}
	}

	@Override
	protected void onDrawFinished(Canvas c, OpenStreetMapView osmv) {
	}

	private class SimpleInvalidationHandler extends Handler {

		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
				case OpenStreetMapTileProviderService.MAPTILE_SUCCESS_ID:
					mOsmv.invalidate();
					break;
			}
		}
	}
}
