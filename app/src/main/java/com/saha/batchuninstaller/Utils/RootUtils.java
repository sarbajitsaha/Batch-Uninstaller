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
