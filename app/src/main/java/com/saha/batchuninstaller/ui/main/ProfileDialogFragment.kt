package com.saha.batchuninstaller.ui.main

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.RoomDatabase
import com.saha.batchuninstaller.AppInfo
import com.saha.batchuninstaller.R
import com.saha.batchuninstaller.RoomStorage.AppDatabase
import com.saha.batchuninstaller.RoomStorage.Profile
import com.saha.batchuninstaller.ui.main.adapters.AppInfoAdapter
import com.saha.batchuninstaller.utils.PackageUtils
import com.saha.batchuninstaller.utils.PackageUtils.moveBatchUninstallerToEnd
import com.stericson.RootTools.RootTools
import github.nisrulz.recyclerviewhelper.RVHItemClickListener
import github.nisrulz.recyclerviewhelper.RVHItemDividerDecoration
import github.nisrulz.recyclerviewhelper.RVHItemTouchHelperCallback
import kotlin.collections.ArrayList


class ProfileDialogFragment : DialogFragment () {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setMessage("Create/Use separate profiles to instantly delete multiple apps, instead of selecting them every time\n\n"+
                    "E.g. Use a social profile to instantly delete multiple social apps")
            .setPositiveButton("Create Profile") { _,_ ->
                dismiss()
                createProfileDialogPrompt()
            }
            .setNegativeButton("Run Profile") { _,_ ->
                dismiss()
                runProfileDialogPrompt()
            }
            .setNeutralButton("Delete profile") { _,_ ->
                dismiss()
                deleteProfileDialogPrompt()
            }
            .setCancelable(true)
            .create()

    private fun deleteProfileDialogPrompt() {
        val profiles = mDb.profileDao().getAll()
        val builder = AlertDialog.Builder(requireContext())
        val profileNames = ArrayList<String>()
        lateinit var profileToBeDeleted : Profile
        if (profiles.isEmpty()) {
            Toast.makeText(mContext, "No profiles found", Toast.LENGTH_SHORT).show()
        } else {
            for (p in profiles) {
                profileNames.add(p.profileName)
            }
            profileToBeDeleted = mDb.profileDao().loadProfile(profileNames[0])
            builder.setSingleChoiceItems(profileNames.toTypedArray(), 0) { _, which ->
                profileToBeDeleted = mDb.profileDao().loadProfile(profileNames[which])
            }.setPositiveButton("Delete") { _, _ ->
                mDb.profileDao().delete(profileToBeDeleted)
                Toast.makeText(mContext, "${profileToBeDeleted.profileName} profile deleted", Toast.LENGTH_SHORT).show()
                dismiss()
            }.setNegativeButton("Cancel") { _, _ ->
                dismiss()
            }.setCancelable(true).show()
        }
    }

    private fun runProfileDialogPrompt() {
        val profiles = mDb.profileDao().getAll()
        val builder = AlertDialog.Builder(requireContext())
        val profileNames = ArrayList<String>()
        val mFreeApps = ArrayList<ApplicationInfo>()
        var packages: ArrayList<String>

        if (profiles.isEmpty()) {
            Toast.makeText(mContext, "No profiles found", Toast.LENGTH_SHORT).show()
        } else {
            for (p in profiles) {
                profileNames.add(p.profileName)
            }
            var pkg_string = mDb.profileDao().loadProfilePackages(profileNames[0])
            packages = ArrayList(pkg_string.split(","))
            builder.setSingleChoiceItems(profileNames.toTypedArray(), 0) { _, which ->
                pkg_string = mDb.profileDao().loadProfilePackages(profileNames[which])
                packages = ArrayList(pkg_string.split(","))
            }.setPositiveButton("Run") { _, _ ->
                for (i in mApps.indices) {
                    if (packages.contains(mApps[i].packageName)) {
                        mFreeApps.add(mApps[i].info!!)
                    }
                }
                val ma : MainActivity = mContext as MainActivity
                ma.uninstallAppsFromProfile(mFreeApps)
                dismiss()
            }.setNegativeButton("Cancel") { _, _ ->
                dismiss()
            }.setCancelable(false).show()

        }
    }

    private fun createProfileDialogPrompt() {
        var mFreeApps : ArrayList<String> = ArrayList()
        var mAdapter = AppInfoAdapter(requireContext(), mApps)
        val dialogView = layoutInflater.inflate(
            R.layout.profile_create_dialog, null) as View
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView).setCancelable(false)
        val dialog = builder.create()
        val mRvAppList = dialogView.findViewById<RecyclerView>(R.id.rv_profile_create_applist)
        mRvAppList.layoutManager = LinearLayoutManager(requireContext())
        mRvAppList.adapter = mAdapter
        val callback: ItemTouchHelper.Callback = RVHItemTouchHelperCallback(
            mAdapter, true, true, true)
        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(mRvAppList)
        mRvAppList.addItemDecoration(
            RVHItemDividerDecoration(context, LinearLayoutManager.VERTICAL)
        )
        mRvAppList.addOnItemTouchListener(RVHItemClickListener(
            context, RVHItemClickListener.OnItemClickListener { _, position ->
                mApps[position].color = if (mApps[position].color == R.color.backgroundSelected) R.color.backgroundPrimary else R.color.backgroundSelected
                mAdapter.notifyItemChanged(position)
                if (mFreeApps.contains(mApps[position].packageName)) {
                    mFreeApps.remove(mApps[position].packageName)
                } else {
                    mApps[position].packageName?.let { mFreeApps.add(it) }
                }
            }))
        val cancelProfileButton = dialogView.findViewById<AppCompatButton>(R.id.cancel_profile_btn)
        cancelProfileButton.setOnClickListener{
            dialog.dismiss()
        }
        val saveProfileButton = dialogView.findViewById<AppCompatButton>(R.id.save_profile_btn)
        saveProfileButton.setOnClickListener{
            val profileEditText = dialogView.findViewById<EditText>(R.id.profile_name)
            if (profileEditText.text.isEmpty()) {
                Toast.makeText(mContext, "Give a profile name", Toast.LENGTH_SHORT).show()
            } else {
                val profileName = profileEditText.text.toString()
                if (mDb.profileDao().isProfileExist(profileName) != 0) {
                    Toast.makeText(mContext, "Profile name already exists", Toast.LENGTH_SHORT).show()
                } else {
                    val packages = mFreeApps.joinToString(separator = ",")
                    mDb.profileDao().insert(Profile(profileName, packages))
                    Toast.makeText(mContext, "Profile created with ${mFreeApps.size} apps", Toast.LENGTH_SHORT).show()
                    mFreeApps.clear()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    companion object {
        const val TAG = "Profile Dialog Fragment"
        private lateinit var mApps: ArrayList<AppInfo>
        private lateinit var mContext : Context
        private lateinit var mDb : AppDatabase
        fun newInstance(content: ArrayList<AppInfo>, context: Context, db: AppDatabase): ProfileDialogFragment {
            mApps = content
            mContext = context
            mDb = db
            return ProfileDialogFragment()
        }
    }

}