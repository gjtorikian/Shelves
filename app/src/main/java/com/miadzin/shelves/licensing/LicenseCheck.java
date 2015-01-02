package com.miadzin.shelves.licensing;

import com.miadzin.shelves.BuildConfig;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public class LicenseCheck  {
    public static boolean check(Context context) {
        if (BuildConfig.DEBUG)
            return false;
        PackageManager manager = context.getPackageManager();
        return isUnlockerInstalled(context) && manager.checkSignatures("com.miadzin.shelves", "com.miadzin.shelves.unlocker") == PackageManager.SIGNATURE_MATCH;
    }

    public static boolean isUnlockerInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo("com.miadzin.shelves.unlocker", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}