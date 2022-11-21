/*
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
package com.saha.batchuninstaller.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.*
import android.content.SharedPreferences.Editor
import android.content.pm.ApplicationInfo
import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.text.format.Formatter
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.afollestad.materialdialogs.MaterialDialog
import com.marcoscg.easylicensesdialog.EasyLicensesDialogCompat
import com.saha.batchuninstaller.AppInfo
import com.saha.batchuninstaller.BuildConfig
import com.saha.batchuninstaller.R
import com.saha.batchuninstaller.RoomStorage.AppDatabase
import com.saha.batchuninstaller.databinding.ActivityMainBinding
import com.saha.batchuninstaller.ui.main.adapters.AppInfoAdapter
import com.saha.batchuninstaller.utils.PackageUtils
import com.saha.batchuninstaller.utils.PackageUtils.moveBatchUninstallerToEnd
import com.saha.batchuninstaller.utils.RootUtils.uninstallAppRoot
import com.stericson.RootTools.RootTools
import github.nisrulz.recyclerviewhelper.RVHItemClickListener
import github.nisrulz.recyclerviewhelper.RVHItemDividerDecoration
import github.nisrulz.recyclerviewhelper.RVHItemTouchHelperCallback
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
	private var mAdapter: AppInfoAdapter? = null
	private var mApps: ArrayList<AppInfo> = arrayListOf()
    private lateinit var mAppsCopy : ArrayList<AppInfo>
	private var mPkgs: List<String> = arrayListOf()
	private var mFreeApps: ArrayList<ApplicationInfo> = arrayListOf()

	private lateinit var mPrefs: SharedPreferences
	private lateinit var mEditor: Editor
    private lateinit var binding: ActivityMainBinding

    private var progressDialog //deprecated, need to replace this later
			: ProgressDialog? = null
	private var appUninstallCancelled = false
	private var systemAppUninstalled = false

    private lateinit var db : AppDatabase

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val mRvAppList = findViewById<RecyclerView>(R.id.rv_applist)
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this)
		mEditor = mPrefs.edit()
		//initialization
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "profiledb"
        ).allowMainThreadQueries().build()
        //Bad practice to allow main thread queries but db should be small,
        //its a headache to now support co routines for a simple db operation
		appUninstallCancelled = false

		//day night mode
		when {
			mPrefs.getInt("night_mode", 0) == 0 //auto
			-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
			mPrefs.getInt("night_mode", 0) == 1 //day
			-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
			else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
		}


		//toolbar icons visibility
		binding.toolbar.tvFreeSize.visibility = View.INVISIBLE
		binding.toolbar.imgbtnBack.visibility = View.GONE
        binding.toolbar.imgbtnDelete.visibility = View.GONE
        binding.toolbar.root.popupTheme = R.style.AppTheme
		setSupportActionBar(binding.toolbar.toolbar)

		//show dialog for root and non-root phones. If root ask for permission
		if (!mPrefs.getBoolean("ask_again", false)) {
			MaterialDialog.Builder(this@MainActivity)
					.title(R.string.important)
					.content(R.string.root_grant_ask)
					.neutralText(R.string.understood)
					.onAny { dialog, _ ->
						mEditor.putBoolean("ask_again", dialog.isPromptCheckBoxChecked)
						mEditor.apply()
						try {
							when {
								RootTools.isAccessGiven() -> Toast.makeText(applicationContext, R.string.granted, Toast.LENGTH_SHORT).show()
								else -> Toast.makeText(applicationContext, R.string.denied, Toast.LENGTH_SHORT).show()
							}
						} catch (e: Exception) {
							Log.e(javaClass.simpleName, "UnhandledException", e)
						}
					}
					.checkBoxPromptRes(R.string.dont_show_again, false, null)
					.show()
		}

        //populate lists with installed apps and details
        mPkgs = PackageUtils.getPackageNames(applicationContext)
        Log.d(TAG, "All-> " + mPkgs.size)
        for (pkg in mPkgs) {
            mApps.add(AppInfo(pkg, this))
        }
        //sort by installed data descending as default
        mApps.sortWith(Comparator { o1: AppInfo, o2: AppInfo -> if (o1.firstInstallTime < o2.firstInstallTime) 1 else -1 })
        mAppsCopy = mApps

        //populate the recyclerview
        mAdapter = AppInfoAdapter(this, mApps)
        mRvAppList.layoutManager = LinearLayoutManager(this)
        mRvAppList.adapter = mAdapter
        val callback: ItemTouchHelper.Callback = RVHItemTouchHelperCallback(
                mAdapter, true, true, true)
        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(mRvAppList)
        mRvAppList.addItemDecoration(
                RVHItemDividerDecoration(this, LinearLayoutManager.VERTICAL))
        mRvAppList.addOnItemTouchListener(RVHItemClickListener(
                this, RVHItemClickListener.OnItemClickListener { _, position ->
            if (mApps[position].systemApp && mFreeApps.contains(mApps[position].info)) {
                try {
                    when {
                        !RootTools.isAccessGiven() -> Toast.makeText(applicationContext, R.string.no_root_system_app, Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(applicationContext, R.string.root_system_app, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NotFoundException) {
                    Log.e(javaClass.simpleName, "UnhandledException", e)
                }
            }
            mApps[position].color = if (mApps[position].color == R.color.backgroundSelected) R.color.backgroundPrimary else R.color.backgroundSelected
            mAdapter!!.notifyItemChanged(position)
            if (mFreeApps.contains(mApps[position].info)) {
                mFreeApps.remove(mApps[position].info)
            } else {
                mFreeApps.add(mApps[position].info!!)
            }
            if (mFreeApps.size == 0) {
                binding.toolbar.imgbtnBack.visibility = View.GONE
                binding.toolbar.imgbtnDelete.visibility = View.GONE
                binding.toolbar.tvFreeSize.visibility = View.INVISIBLE
                binding.fabSort.visibility = View.VISIBLE
                binding.fabFilter.visibility = View.VISIBLE
            } else {
                binding.toolbar.imgbtnBack.visibility = View.VISIBLE
                binding.toolbar.imgbtnDelete.visibility = View.VISIBLE
                binding.toolbar.tvFreeSize.visibility = View.VISIBLE
                binding.fabSort.visibility = View.INVISIBLE
                binding.fabFilter.visibility = View.INVISIBLE
                var totalBytes: Long = 0
                for (pkg in mFreeApps) {
                    totalBytes += PackageUtils.getApkSize(applicationContext, pkg.packageName)
                }
                binding.toolbar.tvFreeSize.text = Formatter
                        .formatShortFileSize(
                                applicationContext,
                                totalBytes
                        )
            }
        }))
        binding.toolbar.imgbtnBack.setOnClickListener {
            mFreeApps.clear()
            systemAppUninstalled = false
            binding.toolbar.imgbtnBack.visibility = View.GONE
            binding.toolbar.tvFreeSize.visibility = View.INVISIBLE
            binding.fabSort.visibility = View.VISIBLE
            binding.fabFilter.visibility = View.VISIBLE
            binding.toolbar.imgbtnDelete.visibility = View.GONE
            for (i in mApps.indices) mApps[i].color = R.color.backgroundPrimary
            mAdapter!!.notifyDataSetChanged()
        }
        binding.toolbar.imgbtnDelete.setOnClickListener {
            MaterialDialog.Builder(this@MainActivity)
                    .content(R.string.uninstall_confirmation)
                    .positiveText(R.string.yes)
                    .negativeText(R.string.no)
                    .onPositive { dialog, _ ->
                        // delete apps
                        dialog.dismiss()
                        // If batch uninstaller itself is chosen, delete it at the end
                        moveBatchUninstallerToEnd(mFreeApps)
                        if (!RootTools.isAccessGiven()) {
                            val packageUri = Uri.parse("package:" + mFreeApps[0].packageName)
                            val uninstallIntent = Intent(Intent.ACTION_DELETE, packageUri)
                            //changed to ACTION_DELETE from ACTION_UNINSTALL_PACKAGE
                            uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
                            startActivityForResult(uninstallIntent, 1)
                        } else {
                            appUninstallCancelled = false
                            progressDialog = ProgressDialog(this@MainActivity)
                            progressDialog!!.max = mFreeApps.size
                            progressDialog!!.setTitle(R.string.uninstall_trailing)
                            progressDialog!!.setCancelable(false)
                            progressDialog!!.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel)
                            ) { _, _ ->
                                Log.i(TAG, "Uninstall cancelled")
                                mFreeApps.clear()
                                appUninstallCancelled = true
                            }
                            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                            progressDialog!!.show()
                            uninstallAppRoot()
                        }
                    }
                    .show()
        }
        binding.layoutSwiperefresh.setOnRefreshListener { refreshList() }
        binding.fabFilter.setOnClickListener {
            MaterialDialog.Builder(this@MainActivity)
                    .title(R.string.filter)
                    .items(R.array.filter)
                    .itemsCallbackSingleChoice(-1) { _, _, which, _ ->
                        when (which) {
                            0 -> {
                                val tSystem = Thread(Runnable {
                                    mApps.clear()
                                    mPkgs = PackageUtils.getPackageNames(applicationContext)
                                    var count = 0
                                    for (pkg in mPkgs) {
                                        if (PackageUtils.isSystemApp(applicationContext, pkg)) {
                                            mApps.add(AppInfo(pkg, applicationContext))
                                            count++
                                        }
                                    }
                                    Log.d(TAG, "System-> $count")
                                    runOnUiThread {
                                        mAdapter!!.notifyDataSetChanged()
                                        binding.layoutSwiperefresh.isRefreshing = false
                                    }
                                })
                                binding.layoutSwiperefresh.isRefreshing = true
                                tSystem.start()
                            }
                            1 -> {
                                val tUser = Thread(Runnable {
                                    mApps.clear()
                                    mPkgs = PackageUtils.getPackageNames(applicationContext)
                                    var count = 0
                                    for (pkg in mPkgs) {
                                        if (!PackageUtils.isSystemApp(applicationContext, pkg)) {
                                            mApps.add(AppInfo(pkg, applicationContext))
                                            count++
                                        }
                                    }
                                    Log.d(TAG, "User-> $count")
                                    runOnUiThread {
                                        mAdapter!!.notifyDataSetChanged()
                                        binding.layoutSwiperefresh.isRefreshing = false
                                    }
                                })
                                binding.layoutSwiperefresh.isRefreshing = true
                                tUser.start()
                            }
                            2 -> {
                                val tAll = Thread(Runnable {
                                    mApps.clear()
                                    mPkgs = PackageUtils.getPackageNames(applicationContext)
                                    var count = 0
                                    for (pkg in mPkgs) {
                                        mApps.add(AppInfo(pkg, applicationContext))
                                        count++
                                    }
                                    Log.d(TAG, "All-> $count")
                                    runOnUiThread {
                                        mAdapter!!.notifyDataSetChanged()
                                        binding.layoutSwiperefresh.isRefreshing = false
                                    }
                                })
                                binding.layoutSwiperefresh.isRefreshing = true
                                tAll.start()
                            }
                            else -> {
                            }
                        }
                        true
                    }
                    .positiveText(R.string.done)
                    .show()
        }
        binding.fabSort.setOnClickListener {
            MaterialDialog.Builder(this@MainActivity)
                    .title(R.string.sort)
                    .items(R.array.sort)
                    .itemsCallbackSingleChoice(-1) { _, _, which, _ ->
                        when (which) {
                            0 -> {
                                mApps.sortWith(Comparator { o1: AppInfo, o2: AppInfo -> o1.appName.compareTo(o2.appName, ignoreCase = true) })
                                mAdapter!!.notifyDataSetChanged()
                            }
                            1 -> {
                                mApps.sortWith(Comparator { o1: AppInfo, o2: AppInfo -> o2.appName.compareTo(o1.appName, ignoreCase = true) })
                                mAdapter!!.notifyDataSetChanged()
                            }
                            2 -> {
                                mApps.sortWith(Comparator { o1: AppInfo, o2: AppInfo -> if (o1.fileSize > o2.fileSize) 1 else -1 })
                                mAdapter!!.notifyDataSetChanged()
                            }
                            3 -> {
                                mApps.sortWith(Comparator { o1: AppInfo, o2: AppInfo -> if (o1.fileSize < o2.fileSize) 1 else -1 })
                                mAdapter!!.notifyDataSetChanged()
                            }
                            4 -> {
                                mApps.sortWith(Comparator { o1: AppInfo, o2: AppInfo -> if (o1.firstInstallTime < o2.firstInstallTime) -1 else 1 })
                                mAdapter!!.notifyDataSetChanged()
                            }
                            5 -> {
                                mApps.sortWith(Comparator { o1: AppInfo, o2: AppInfo -> if (o1.firstInstallTime < o2.firstInstallTime) 1 else -1 })
                                mAdapter!!.notifyDataSetChanged()
                            }
                            else -> {
                            }
                        }
                        true
                    }
                    .positiveText("Done")
                    .show()
        }
        mRvAppList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                when {
                    dy > 0 -> {
                        binding.fabSort.hide()
                        binding.fabFilter.hide()
                    }
                    dy < 0 && mFreeApps.size == 0 -> {
                        binding.fabSort.show()
                        binding.fabFilter.show()
                    }
                }
            }
        })
    }

    fun uninstallAppsFromProfile(freeAppsFromProfile: ArrayList<ApplicationInfo>) {
        if (freeAppsFromProfile.size == 0) {
            Toast.makeText(this, "Application already deleted", Toast.LENGTH_SHORT).show()
            return
        }
        mFreeApps = freeAppsFromProfile
        moveBatchUninstallerToEnd(mFreeApps)
        if (!RootTools.isAccessGiven()) {
            val packageUri = Uri.parse("package:" + mFreeApps[0].packageName)
            val uninstallIntent = Intent(Intent.ACTION_DELETE, packageUri)
            //changed to ACTION_DELETE from ACTION_UNINSTALL_PACKAGE
            uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
            startActivityForResult(uninstallIntent, 1)
        } else {
            appUninstallCancelled = false
            progressDialog = ProgressDialog(this@MainActivity)
            progressDialog!!.max = mFreeApps.size
            progressDialog!!.setTitle(R.string.uninstall_trailing)
            progressDialog!!.setCancelable(false)
            progressDialog!!.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel)
            ) { _, _ ->
                Log.i(TAG, "Uninstall cancelled")
                mFreeApps.clear()
                appUninstallCancelled = true
            }
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog!!.show()
            uninstallAppRoot()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                if (mFreeApps.isEmpty()) {
                    Toast.makeText(application.applicationContext,
                        R.string.uninstall_complete, Toast.LENGTH_SHORT).show()
                    return
                }
                val packageName = mFreeApps[0].packageName
                for (i in mApps.indices) {
                    if (mApps[i].packageName!!.compareTo(packageName) == 0) {
                        mApps.removeAt(i)
                        mAdapter!!.notifyDataSetChanged()
                        break
                    }
                }
            } else {
                if (mFreeApps.isEmpty()) {
                    Toast.makeText(application.applicationContext,
                        R.string.uninstall_fail, Toast.LENGTH_SHORT).show()
                    return
                }
                val packageName = mFreeApps[0].packageName
                for (i in mApps.indices) {
                    if (mApps[i].packageName!!.compareTo(packageName) == 0) {
                        mApps[i].color = R.color.backgroundPrimary
                        mAdapter!!.notifyDataSetChanged()
                        break
                    }
                }
                try {
                    Toast.makeText(application.applicationContext,
                            R.string.uninstall_fail, Toast.LENGTH_SHORT).show()
                } catch (e: NotFoundException) {
                    Log.e(javaClass.simpleName, "UnhandledException", e)
                }
            }
            mFreeApps.removeAt(0)
            Log.d("MainActivity","${mFreeApps.size} + size of free apps")
            if (mFreeApps.size != 0) {
                val packageUri = Uri.parse("package:" + mFreeApps[0].packageName)
                val uninstallIntent = Intent(Intent.ACTION_DELETE, packageUri)
                uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
                startActivityForResult(uninstallIntent, 1)
            } else {
                mFreeApps.clear()
                binding.toolbar.imgbtnDelete.visibility = View.GONE
                binding.toolbar.imgbtnBack.visibility = View.GONE
                binding.toolbar.imgbtnDelete.visibility = View.INVISIBLE
                binding.toolbar.tvFreeSize.visibility = View.INVISIBLE
                binding.fabSort.visibility = View.VISIBLE
                binding.fabFilter.visibility = View.VISIBLE
                mAdapter!!.notifyDataSetChanged()
                try {
                    Toast.makeText(application.applicationContext,
                            R.string.uninstall_complete, Toast.LENGTH_SHORT).show()
                } catch (e: NotFoundException) {
                    Log.e(javaClass.simpleName, "UnhandledException", e)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.about -> {
                about()
                true
            }
            R.id.nightmode -> {
                selectNightMode()
                true
            }
            R.id.feedback -> {
                feedback()
                true
            }
            R.id.rate -> {
                rate()
                true
            }
            R.id.profiles -> {
                val profileDialogFragment = ProfileDialogFragment.newInstance(mAppsCopy, this@MainActivity, db)
                profileDialogFragment.show(supportFragmentManager, ProfileDialogFragment.TAG)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun selectNightMode() {
        MaterialDialog.Builder(this@MainActivity)
                .title(R.string.night_mode)
                .items(R.array.night_mode_options)
                .itemsCallbackSingleChoice(-1) { _, _, which, _ ->
                    when (which) {
                        0, 1, 2 -> {
                            mEditor.putInt("night_mode", which)
                            mEditor.commit()
                            startActivity(intent)
                            finish()
                        }
                        else -> {
                        }
                    }
                    true
                }.show()
    }

    private fun refreshList() {
        val thread: Thread = object : Thread() {
            override fun run() {
                mApps.clear()
                mPkgs = PackageUtils.getPackageNames(applicationContext)
                for (pkg in mPkgs) {
                    mApps.add(AppInfo(pkg, applicationContext))
                }
                //sort by installed data descending as default
                mApps.sortWith(Comparator { o1: AppInfo, o2: AppInfo -> if (o1.firstInstallTime < o2.firstInstallTime) 1 else -1 })
                mAppsCopy = mApps
                runOnUiThread {
                    mAdapter!!.notifyDataSetChanged()
                    binding.toolbar.imgbtnBack.visibility = View.GONE
                    binding.toolbar.imgbtnDelete.visibility = View.GONE
                    binding.toolbar.tvFreeSize.visibility = View.INVISIBLE
                    binding.fabSort.visibility = View.VISIBLE
                    binding.fabFilter.visibility = View.VISIBLE
                    mFreeApps.clear()
                    systemAppUninstalled = false
                    binding.layoutSwiperefresh.isRefreshing = false
                }
            }
        }
        thread.start()
    }

    private fun feedback() {
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "sarbajitsaha1@gmail.com", null))
        try {
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.email_subject))
            startActivity(Intent.createChooser(emailIntent, resources.getString(R.string.send_email)))
        } catch (e: NotFoundException) {
            Log.e(javaClass.simpleName, "UnhandledException", e)
        }
    }

    private fun rate() {
        val uri = Uri.parse("market://details?id=$packageName")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        // TODO Fix version issue here
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    private fun about() {
        MaterialDialog.Builder(this@MainActivity)
                .title(R.string.about)
                .content(R.string.about_text)
                .positiveText(R.string.github)
                .negativeText(R.string.licenses)
                .onPositive { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/sarbajitsaha/Batch-Uninstaller"))
                    startActivity(intent)
                }
                .onNegative { _, _ ->
                    EasyLicensesDialogCompat(this@MainActivity)
                            .setTitle(R.string.licenses)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                }
                .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (mFreeApps.size == 0) super.onBackPressed() else {
            binding.toolbar.imgbtnBack.callOnClick()
        }
    }

    /*possibly a very bad solution. need to rewrite this later*/
    @SuppressLint("StaticFieldLeak")
    fun uninstallAppRoot() {
        if (!appUninstallCancelled) {
            val pkg = mFreeApps[0]
            if (pkg.sourceDir.startsWith("/system")) systemAppUninstalled = true
            progressDialog!!.setTitle("Uninstalling " + pkg.packageName)
            runAsyncTask(object : AsyncTask<Void, Void, Boolean>() {
                @Deprecated("Deprecated in Java")
                override fun doInBackground(vararg voids: Void): Boolean = pkg.uninstallAppRoot()

                @Deprecated("Deprecated in Java")
                override fun onPostExecute(result: Boolean) {
                    Log.d("UNINSTALL","Result: $result")
                    if (result) {
                        progressDialog!!.setMessage(getString(R.string.uninstall_past_tense) + pkg.packageName)
                        progressDialog!!.incrementProgressBy(1)
                        // Toast.makeText(getApplicationContext(),, Toast.LENGTH_SHORT).show();
                        for (i in mApps.indices) {
                            if (mApps[i].packageName!!.compareTo(pkg.packageName) == 0) {
                                mApps.removeAt(i)
                                mAdapter!!.notifyDataSetChanged()
                                break
                            }
                        }
                    } else {
                        progressDialog!!.incrementProgressBy(1)
                        try {
                            Toast.makeText(applicationContext, getString(R.string.uninstall_fail_toast)
                                    + pkg.packageName, Toast.LENGTH_SHORT).show()
                        } catch (e: NotFoundException) {
                            Log.e(javaClass.simpleName, "UnhandledException", e)
                        }
                        for (i in mApps.indices) {
                            if (mApps[i].packageName!!.compareTo(pkg.packageName) == 0) {
                                mApps[i].color = R.color.backgroundPrimary
                                mAdapter!!.notifyDataSetChanged()
                            }
                        }
                    }
                    super.onPostExecute(result)
                }
            })
            mFreeApps.removeAt(0)
            if (mFreeApps.size != 0) {
                Handler().postDelayed({ uninstallAppRoot() }, Toast.LENGTH_LONG.toLong())
            } else {
                if (progressDialog!!.isShowing) progressDialog!!.dismiss()
                mFreeApps.clear()
                for (i in mApps.indices) {
                    mApps[i].color = R.color.backgroundPrimary
                }
                binding.toolbar.imgbtnDelete.visibility = View.GONE
                binding.toolbar.imgbtnBack.visibility = View.GONE
                binding.toolbar.imgbtnDelete.visibility = View.INVISIBLE
                binding.toolbar.tvFreeSize.visibility = View.INVISIBLE
                binding.fabSort.visibility = View.VISIBLE
                binding.fabFilter.visibility = View.VISIBLE
                mAdapter!!.notifyDataSetChanged()
                if (systemAppUninstalled) {
                    MaterialDialog.Builder(this@MainActivity)
                            .title(R.string.important)
                            .content(R.string.system_app_reboot)
                            .positiveText(R.string.reboot_now)
                            .negativeText(R.string.reboot_later)
                            .onPositive { _, _ -> if (RootTools.isAccessGiven()) RootTools.restartAndroid() }
                            .show()
                }
                try {
                    Toast.makeText(application.applicationContext,
                            R.string.uninstall_complete, Toast.LENGTH_SHORT).show()
                } catch (e: NotFoundException) {
                    Log.e(javaClass.simpleName, "UnhandledException", e)
                }
                systemAppUninstalled = false
            }
        } else {
            binding.layoutSwiperefresh.isRefreshing = true
            val thread = Thread(Runnable {
                mFreeApps.clear()
                for (i in mApps.indices) {
                    mApps[i].color = R.color.backgroundPrimary
                }
                appUninstallCancelled = false
                runOnUiThread {
                    if (progressDialog!!.isShowing) progressDialog!!.dismiss()
                    binding.toolbar.imgbtnDelete.visibility = View.GONE
                    binding.toolbar.imgbtnBack.visibility = View.GONE
                    binding.toolbar.imgbtnDelete.visibility = View.INVISIBLE
                    binding.toolbar.tvFreeSize.visibility = View.INVISIBLE
                    binding.fabSort.visibility = View.VISIBLE
                    binding.fabFilter.visibility = View.VISIBLE
                    mAdapter!!.notifyDataSetChanged()
                    binding.layoutSwiperefresh.isRefreshing = false
                    systemAppUninstalled = false
                }
            })
            thread.start()
        }
    }

    @SafeVarargs
    private fun <T> runAsyncTask(asyncTask: AsyncTask<T, *, *>, vararg params: T) {
        try {
            asyncTask.execute(*params).get()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}