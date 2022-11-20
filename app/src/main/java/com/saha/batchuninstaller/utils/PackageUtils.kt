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
package com.saha.batchuninstaller.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.File
import java.util.*

object PackageUtils {
	private fun drawableToBitmap(drawable: Drawable): Bitmap {
		if (drawable is BitmapDrawable) drawable.bitmap ?: return drawable.bitmap
		val bitmap: Bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0)
			Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
		else
			Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)

		val bitmapResized = Bitmap.createScaledBitmap(bitmap, 40, 40, false)
		val canvas = Canvas(bitmapResized)
		drawable.setBounds(0, 0, canvas.width, canvas.height)
		drawable.draw(canvas)
		return bitmapResized
	}

	fun isSystemApp(context: Context, packageName: String?): Boolean = try {
		val pm = context.packageManager
		val ai = pm.getApplicationInfo(packageName!!, 0)
		ai.flags and ApplicationInfo.FLAG_SYSTEM != 0
	} catch (e: PackageManager.NameNotFoundException) {
		false
	}

	fun getInstalledDate(context: Context, packageName: String?): Long = try {
		context.packageManager.getPackageInfo(packageName!!, PackageManager.GET_META_DATA).firstInstallTime
	} catch (e: PackageManager.NameNotFoundException) {
		0
	}

	fun getIcon(context: Context, package_name: String?): Bitmap? = try {
		val icon = context.packageManager.getApplicationIcon(package_name!!)
		drawableToBitmap(icon)
	} catch (e: PackageManager.NameNotFoundException) {
		null
	}

	fun getApkSize(context: Context, package_name: String?): Long = try {
		File(context.packageManager.getApplicationInfo(package_name!!, 0).publicSourceDir).length()
	} catch (e: PackageManager.NameNotFoundException) {
		0
	}

	fun getAppName(context: Context, package_name: String?): String? = try {
		val pm = context.packageManager
		pm.getApplicationLabel(pm.getApplicationInfo(package_name!!, PackageManager.GET_META_DATA)) as String
	} catch (e: PackageManager.NameNotFoundException) {
		null
	}

	fun getPackageNames(context: Context): List<String> {
		val pkgs = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
		val pkgNames: MutableList<String> = ArrayList()
		pkgs.forEach { pkgNames.add(it.packageName) }
		return pkgNames
	}
}