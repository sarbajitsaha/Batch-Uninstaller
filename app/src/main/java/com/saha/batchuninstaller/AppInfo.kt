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
 * along with Batch Uninstaller.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.saha.batchuninstaller

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import com.saha.batchuninstaller.utils.PackageUtils

class AppInfo(var packageName: String?, context: Context) {
	var color: Int
	var icon: Bitmap? = PackageUtils.getIcon(context, packageName)
	var fileSize: Long = PackageUtils.getApkSize(context, packageName)
	var appName: String = PackageUtils.getAppName(context, packageName) ?: "UNKNOWN APP"
	var systemApp: Boolean = PackageUtils.isSystemApp(context, packageName)
	var info: ApplicationInfo? = null
	var firstInstallTime: Long = PackageUtils.getInstalledDate(context, packageName)

	init {
		info = try {
			context.packageManager.getApplicationInfo(packageName, 0)
		} catch (e: PackageManager.NameNotFoundException) {
			null
		}
		color = R.color.backgroundPrimary
	}
}