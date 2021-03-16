package com.saha.batchuninstaller

import android.content.Intent
import android.net.Uri
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.util.*

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.saha.batchuninstaller/android"


    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "uninstallApp") {
                val pkg = call.argument<String>("pkg")
                val packageUri = Uri.parse("package:" + pkg)
                val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri)
                uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
                startActivityForResult(uninstallIntent, 1)
                result.error("hello", "hello", null)
            } else {
                result.notImplemented()
            }
        }
    }
}
