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

import android.content.pm.ApplicationInfo;

import com.stericson.RootTools.RootTools;

import java.io.IOException;

/**
 * Created by sarbajit on 22/5/18.
 */

public class RootUtils {
    final String TAG = "RootUtils";

    public static boolean uninstallApp(ApplicationInfo pkg) {
        if (pkg.sourceDir.startsWith("/system"))
            return uninstallSystemApp(pkg);
        else
            return uninstallUserApp(pkg);
    }

    private static boolean uninstallSystemApp(ApplicationInfo pkg) {
        final String sourceDir = pkg.sourceDir;
        final String dataDir = pkg.dataDir;
        RootTools.debugMode = true;
        return RootTools.deleteFileOrDirectory(sourceDir, true)
                && RootTools.deleteFileOrDirectory(dataDir, true);
    }

    private static boolean uninstallUserApp(ApplicationInfo pkg) {
        boolean status = false;
        try {
            String[] command_delete = {"su", "-c", "pm uninstall " + pkg.packageName + "\n"};
            Process process = Runtime.getRuntime().exec(command_delete);
            process.waitFor();
            int i = process.exitValue();
            if (i == 0)
                status = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return status;
    }
}
