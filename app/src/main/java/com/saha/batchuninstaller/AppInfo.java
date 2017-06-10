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
    public String package_name;
    public Bitmap icon;
    public long file_size;
    public String app_name;

    public AppInfo(String package_name, Context context) {
        this.package_name = package_name;
        icon = PackageUtils.getIcon(context, package_name);
        file_size = PackageUtils.getApkSize(context, package_name);
        app_name = PackageUtils.getAppName(context, package_name);
        color = R.color.backgroundPrimary;
    }
}