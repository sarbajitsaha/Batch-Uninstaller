/*******************************************************************************
 * This file is part of Batch Uninstaller.
 *
 * Batch Uninstaller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Batch Uninstaller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Batch Uninstaller.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.saha.batchuninstaller.Utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PackageUtils {
    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmap, 40, 40, false);
        Canvas canvas = new Canvas(bitmapResized);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmapResized;
    }

    public static boolean isSystemApp(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    public static long getInstalledDate(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA).firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    public static Bitmap getIcon(Context context, String package_name) {
        try {
            Drawable icon = context.getPackageManager().getApplicationIcon(package_name);
            return drawableToBitmap(icon);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static long getApkSize(Context context, String package_name) {
        try {
            return new File(context.getPackageManager().getApplicationInfo(package_name, 0).publicSourceDir).length();
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    public static String getAppName(Context context, String package_name) {
        PackageManager pm = context.getPackageManager();
        try {
            return (String) pm.getApplicationLabel(pm.getApplicationInfo(package_name, PackageManager.GET_META_DATA));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static List<String> getPackageNames(Context context) {
        List<ApplicationInfo> pkgs = context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> pkg_names = new ArrayList<>();
        for (ApplicationInfo pkg : pkgs) {
            pkg_names.add(pkg.packageName);
        }
        return pkg_names;
    }
}