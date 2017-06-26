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

package com.saha.batchuninstaller;

import android.content.Context;
import android.graphics.Bitmap;

import com.saha.batchuninstaller.Helpers.PackageUtils;

public class AppInfo {
    public int color;
    public String packageName;
    public Bitmap icon;
    public long fileSize;
    public String appName;
    public boolean systemApp;
    public long firstInstallTime;

    public AppInfo(String packageName, Context context) {
        this.packageName = packageName;
        icon = PackageUtils.getIcon(context, packageName);
        fileSize = PackageUtils.getApkSize(context, packageName);
        appName = PackageUtils.getAppName(context, packageName);
        systemApp = PackageUtils.isSystemApp(context, packageName);
        firstInstallTime = PackageUtils.getInstalledDate(context, packageName);
        color = R.color.backgroundPrimary;
    }
}