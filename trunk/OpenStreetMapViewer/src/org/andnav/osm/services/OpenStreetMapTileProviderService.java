package org.andnav.osm.services;

import org.andnav.osm.services.util.OpenStreetMapTile;
import org.andnav.osm.services.util.OpenStreetMapTileFilesystemProvider;
import org.andnav.osm.views.util.OpenStreetMapRendererInfo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * The OpenStreetMapTileProviderService can download map tiles from a server
 * and stores them in a file system cache.
 * @author Manuel Stahl
 */
public class OpenStreetMapTileProviderService extends Service {

	private OpenStreetMapTileFilesystemProvider mFileSystemProvider;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mFileSystemProvider = new OpenStreetMapTileFilesystemProvider(
				this.getBaseContext(), 4 * 1024 * 1024); // 4MB FSCache
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	/**
	 * The IRemoteInterface is defined through IDL
	 */
	private final IOpenStreetMapTileProviderService.Stub mBinder = new IOpenStreetMapTileProviderService.Stub() {
		@Override
		public String[] getTileProviders() throws RemoteException {
			int i = 0;
			String[] providers = new String[OpenStreetMapRendererInfo.values().length];
			for (OpenStreetMapRendererInfo info: OpenStreetMapRendererInfo.values())
				providers[i++] = info.name();
			return providers;
		}
		@Override
		public void getMapTile(int rendererID, int zoomLevel, int tileX,
				int tileY, IOpenStreetMapTileProviderCallback callback)
				throws RemoteException {

			OpenStreetMapTile tile = new OpenStreetMapTile(rendererID, zoomLevel, tileX, tileY);
			mFileSystemProvider.loadMapTileAsync(tile, callback);
		}
	};

}
