package org.coolreader.crengine;

import org.coolreader.crengine.Engine.HyphDict;
import org.coolreader.crengine.reader.ReaderView;

import android.os.Handler;

public class Services {

	public static final Logger log = L.create("sv");
	
	private static Engine mEngine;
	private static Scanner mScanner;
	private static History mHistory;
	private static CoverpageManager mCoverpageManager;
    private static FileSystemFolders mFSFolders;

	public static Engine getEngine() { return mEngine; }
	public static Scanner getScanner() { return mScanner; }
	public static History getHistory() { return mHistory; }
    public static CoverpageManager getCoverpageManager() { return mCoverpageManager; }
    public static FileSystemFolders getFileSystemFolders() { return mFSFolders; }

    private static volatile  boolean inited = false;

	public static void startServices(BaseActivity activity) {
		log.i("First activity is created");
		// testing background thread
		//mSettings = activity.settings();
		if(!inited) {
			inited = true;
			BackgroundThread.instance().setGUIHandler(new Handler());

			mEngine = Engine.getInstance(activity);

			String code = activity.settings().getProperty(ReaderView.PROP_HYPHENATION_DICT, Engine.HyphDict.RUSSIAN.toString());
			Engine.HyphDict dict = HyphDict.byCode(code);
			mEngine.setHyphenationDictionary(dict);

			mScanner = new Scanner(activity, mEngine);
			mScanner.initRoots(Engine.getMountedRootsMap());

			mHistory = new History(mScanner);
			mScanner.setDirScanEnabled(activity.settings().getBool(ReaderView.PROP_APP_BOOK_PROPERTY_SCAN_ENABLED, true));
			mCoverpageManager = new CoverpageManager();

			mFSFolders = new FileSystemFolders(mScanner);
		}
	}

	public static void stopServices() {
		log.i("Last activity is destroyed");
		if (mCoverpageManager == null) {
			log.i("Will not destroy services: finish only activity creation detected");
			return;
		}
		mCoverpageManager.clear();
		BackgroundThread.instance().postBackground(() -> {
			log.i("Stopping background thread");
			if (mEngine == null)
				return;
			mEngine.uninit();
			BackgroundThread.instance().quit();
			mEngine = null;
		});
		mHistory = null;
		mScanner = null;
		mCoverpageManager = null;
	}
}
