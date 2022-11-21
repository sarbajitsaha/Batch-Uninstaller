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
import android.util.Log
import com.stericson.RootTools.RootTools
import java.io.IOException

/**
 * Created by sarbajit on 22/5/18.
 */
object RootUtils {
	private fun uninstallSystemAppRoot(pkg: ApplicationInfo?): Boolean {
		val sourceDir = pkg!!.sourceDir
		val dataDir = pkg.dataDir
		Log.d("UNINSTALL","Dirs : $sourceDir $dataDir")
		RootTools.debugMode = false
		//Lets try the pm uninstall method here as well,
		//at least it can revert back to the default state, or deactivate
		val pm_status = uninstallSystemAppRoot2(pkg)
		Log.d("UNINSTALL","PM UNINSTALL: $pm_status")
		/*
		TODO: There's an issue here, even if the directory or file isn't deleted
		This  deleteFileOrDirectory function is returning true, maybe need a new function.
		 */
		val delete_files_status = (RootTools.deleteFileOrDirectory(sourceDir, true)
				&& RootTools.deleteFileOrDirectory(dataDir, true))
		return delete_files_status or pm_status
	}

	private fun uninstallSystemAppRoot2(pkg: ApplicationInfo?): Boolean {
		var status = false
		try {
			val commandDelete = arrayOf("su", "-c", """pm uninstall --user 0 ${pkg!!.packageName}""")
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

	private fun uninstallUserAppRoot(pkg: ApplicationInfo?): Boolean {
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

	fun ApplicationInfo.uninstallAppRoot() = if (this.sourceDir.startsWith("/system"))
		RootUtils.uninstallSystemAppRoot(this) else RootUtils.uninstallUserAppRoot(this)
}
