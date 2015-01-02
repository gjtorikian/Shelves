package com.miadzin.shelves.util.backup;

import android.annotation.TargetApi;
import android.app.backup.BackupManager;
import android.content.Context;
import android.util.Log;

@TargetApi(8)
public class BackupManagerWrapper {
	private static final String LOG_TAG = "BackupManagerWrapper";

	// If this throws an exception, then we don't support BackupManager

	/* calling here forces class initialization */
	public static void checkAvailable() {
	}

	public void dataChanged() {
		Log.d(LOG_TAG, "dataChanged()");
		mInstance.dataChanged();
	}

	private BackupManager mInstance;

	public BackupManagerWrapper(Context ctx) {
		mInstance = new BackupManager(ctx);
	}
}
