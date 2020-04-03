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

import android.content.pm.ApplicationInfo
import com.stericson.RootTools.RootTools
import java.io.IOException

/**
 * Created by sarbajit on 22/5/18.
 */
object RootUtils {
	fun uninstallSystemApp(pkg: ApplicationInfo?): Boolean {
		val sourceDir = pkg!!.sourceDir
		val dataDir = pkg.dataDir
		RootTools.debugMode = true
		return (RootTools.deleteFileOrDirectory(sourceDir, true)
				&& RootTools.deleteFileOrDirectory(dataDir, true))
	}

	fun uninstallUserApp(pkg: ApplicationInfo?): Boolean {
		var status = false
		try {
			val commandDelete = arrayOf("su", "-c", """pm uninstall ${pkg!!.packageName}""")
			val process = Runtime.getRuntime().exec(commandDelete)
			process.waitFor()
			val i = process.exitValue()
			if (i == 0) status = true
		} catch (e: InterruptedException) {
			e.printStackTrace()
		} catch (e: IOException) {
			e.printStackTrace()
		}
		return status
	}
}

fun ApplicationInfo.uninstallApp() = if (this.sourceDir.startsWith("/system"))
	RootUtils.uninstallSystemApp(this) else RootUtils.uninstallUserApp(this)
