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

package com.saha.batchuninstaller.Activities;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.chrisplus.rootmanager.RootManager;
import com.chrisplus.rootmanager.container.Result;
import com.marcoscg.easylicensesdialog.EasyLicensesDialogCompat;
import com.saha.batchuninstaller.Adapters.AppInfoAdapter;
import com.saha.batchuninstaller.AppInfo;
import com.saha.batchuninstaller.Helpers.PackageUtils;
import com.saha.batchuninstaller.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import github.nisrulz.recyclerviewhelper.RVHItemClickListener;
import github.nisrulz.recyclerviewhelper.RVHItemDividerDecoration;
import github.nisrulz.recyclerviewhelper.RVHItemTouchHelperCallback;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView mRvAppList;
    private AppInfoAdapter mAdapter;
    private SwipeRefreshLayout mSwipeLayout;
    private List<AppInfo> mApps;
    private List<String> mPkgs;
    private TextView mTvFreeSize;
    private List<String> mFreeApps;
    private ImageButton mImgBtnBack, mImgBtnDelete;
    private FloatingActionButton mFabSort, mFabFilter;
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //local variables
        Toolbar mToolbar;

        //link xml ids
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mTvFreeSize = (TextView) findViewById(R.id.tv_free_size);
        mImgBtnBack = (ImageButton) findViewById(R.id.imgbtn_back);
        mImgBtnDelete = (ImageButton) findViewById(R.id.imgbtn_delete);
        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.layout_swiperefresh);
        mRvAppList = (RecyclerView) findViewById(R.id.rv_applist);
        mFabSort = (FloatingActionButton) findViewById(R.id.fab_sort);
        mFabFilter = (FloatingActionButton) findViewById(R.id.fab_filter);

        //initialization
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mEditor = mPrefs.edit();
        mApps = new ArrayList<>();
        mFreeApps = new ArrayList<>();

        //toolbar icons visibility
        mTvFreeSize.setVisibility(View.INVISIBLE);
        mImgBtnBack.setVisibility(View.GONE);
        mImgBtnDelete.setVisibility(View.GONE);
        setSupportActionBar(mToolbar);

        //show dialog for root and non-root phones. If root ask for permission
        if (!mPrefs.getBoolean("ask_again", false)) {
            if (checkForRoot()) {
                new MaterialDialog.Builder(MainActivity.this)
                        .title(R.string.important)
                        .content(R.string.root_check_content)
                        .neutralText(R.string.understood)
                        .onAny(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                mEditor.putBoolean("ask_again", dialog.isPromptCheckBoxChecked());
                                mEditor.commit();
                                if (RootManager.getInstance().obtainPermission()) {
                                    Toast.makeText(getApplicationContext(), R.string.granted, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getApplicationContext(), R.string.denied, Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .checkBoxPromptRes(R.string.dont_show_again, false, null)
                        .show();
            } else {
                new MaterialDialog.Builder(MainActivity.this)
                        .title(R.string.important)
                        .content(R.string.non_root_content)
                        .neutralText(R.string.understood)
                        .checkBoxPromptRes(R.string.dont_show_again, false, null)
                        .onAny(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                mEditor.putBoolean("ask_again", dialog.isPromptCheckBoxChecked());
                                mEditor.commit();
                            }
                        })
                        .show();
            }
        }


        //populate lists with installed apps and details
        mPkgs = PackageUtils.getPackageNames(getApplicationContext());
        for (String pkg : mPkgs) {
            mApps.add(new AppInfo(pkg, getApplicationContext()));
        }

        mAdapter = new AppInfoAdapter(getApplicationContext(), mApps);
        mRvAppList.setLayoutManager(new LinearLayoutManager(this));
        mRvAppList.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new RVHItemTouchHelperCallback(
                mAdapter, true, true, true);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(mRvAppList);

        mRvAppList.addItemDecoration(
                new RVHItemDividerDecoration(this, LinearLayoutManager.VERTICAL));

        mRvAppList.addOnItemTouchListener(new RVHItemClickListener(
                this, new RVHItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (mApps.get(position).systemApp && mFreeApps.contains(mApps.get(position).packageName)) {
                    if (!RootManager.getInstance().obtainPermission()) {
                        Toast.makeText(getApplicationContext(), R.string.no_root_system_app, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.root_system_app, Toast.LENGTH_SHORT).show();
                    }
                }

                mApps.get(position).color = (
                        mApps.get(position).color == R.color.backgroundSelected
                ) ? R.color.backgroundPrimary : R.color.backgroundSelected;
                mAdapter.notifyItemChanged(position);

                if (mFreeApps.contains(mApps.get(position).packageName)) {
                    mFreeApps.remove(mApps.get(position).packageName);
                } else {
                    mFreeApps.add(mApps.get(position).packageName);
                }

                if (mFreeApps.size() == 0) {
                    mImgBtnBack.setVisibility(View.GONE);
                    mImgBtnDelete.setVisibility(View.GONE);
                    mTvFreeSize.setVisibility(View.INVISIBLE);
                    mFabSort.setVisibility(View.VISIBLE);
                    mFabFilter.setVisibility(View.VISIBLE);
                } else {
                    mImgBtnDelete.setVisibility(View.VISIBLE);
                    mImgBtnBack.setVisibility(View.VISIBLE);
                    mTvFreeSize.setVisibility(View.VISIBLE);
                    mFabSort.setVisibility(View.INVISIBLE);
                    mFabFilter.setVisibility(View.INVISIBLE);
                    long total_bytes = 0;
                    for (String pkg : mFreeApps) {
                        total_bytes += PackageUtils.getApkSize(getApplicationContext(), pkg);
                    }
                    mTvFreeSize.setText(Formatter
                            .formatShortFileSize(
                                    getApplicationContext(),
                                    total_bytes
                            ));
                }
            }
        }));

        mImgBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFreeApps.clear();
                mImgBtnBack.setVisibility(View.GONE);
                mTvFreeSize.setVisibility(View.INVISIBLE);
                mFabSort.setVisibility(View.VISIBLE);
                mFabFilter.setVisibility(View.VISIBLE);
                mImgBtnDelete.setVisibility(View.GONE);
                for (int i = 0; i < mApps.size(); i++)
                    mApps.get(i).color = R.color.backgroundPrimary;
                mAdapter.notifyDataSetChanged();
            }
        });

        mImgBtnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(MainActivity.this)
                        .content(R.string.uninstall_confirmation)
                        .positiveText(R.string.yes)
                        .negativeText(R.string.no)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                // delete apps
                                dialog.dismiss();
                                if (!RootManager.getInstance().obtainPermission()) {
                                    Uri packageUri = Uri.parse("package:" + mFreeApps.get(0));
                                    Intent uninstallIntent =
                                            new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
                                    uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                                    startActivityForResult(uninstallIntent, 1);
                                } else {
                                    uninstallAppRoot();
                                }

                            }
                        })
                        .show();
            }
        });

        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList();
            }
        });

        mFabFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(MainActivity.this)
                        .title(R.string.filter)
                        .items(R.array.filter)
                        .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                switch (which) {
                                    case 0:
                                        mApps.clear();
                                        mPkgs = PackageUtils.getPackageNames(getApplicationContext());
                                        for (String pkg : mPkgs) {
                                            if (PackageUtils.isSystemApp(getApplicationContext(), pkg))
                                                mApps.add(new AppInfo(pkg, getApplicationContext()));
                                        }
                                        mAdapter.notifyDataSetChanged();
                                        break;
                                    case 1:
                                        mApps.clear();
                                        mPkgs = PackageUtils.getPackageNames(getApplicationContext());
                                        for (String pkg : mPkgs) {
                                            if (!PackageUtils.isSystemApp(getApplicationContext(), pkg))
                                                mApps.add(new AppInfo(pkg, getApplicationContext()));
                                        }
                                        mAdapter.notifyDataSetChanged();
                                        break;
                                    case 2:
                                        mApps.clear();
                                        mPkgs = PackageUtils.getPackageNames(getApplicationContext());
                                        for (String pkg : mPkgs) {
                                            mApps.add(new AppInfo(pkg, getApplicationContext()));
                                        }
                                        mAdapter.notifyDataSetChanged();
                                        break;
                                    default:
                                        break;

                                }
                                return true;
                            }
                        })
                        .positiveText("Done")
                        .show();
            }
        });

        mFabSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(MainActivity.this)
                        .title(R.string.sort)
                        .items(R.array.sort)
                        .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                switch (which) {
                                    case 0:
                                        Collections.sort(mApps, new Comparator<AppInfo>() {
                                            @Override
                                            public int compare(AppInfo o1, AppInfo o2) {
                                                return o1.appName.compareToIgnoreCase(o2.appName);
                                            }
                                        });
                                        mAdapter.notifyDataSetChanged();
                                        break;
                                    case 1:
                                        Collections.sort(mApps, new Comparator<AppInfo>() {
                                            @Override
                                            public int compare(AppInfo o1, AppInfo o2) {
                                                return o2.appName.compareToIgnoreCase(o1.appName);
                                            }
                                        });
                                        mAdapter.notifyDataSetChanged();
                                        break;
                                    case 2:
                                        Collections.sort(mApps, new Comparator<AppInfo>() {
                                            @Override
                                            public int compare(AppInfo o1, AppInfo o2) {
                                                return (o1.fileSize > o2.fileSize ? 1 : -1);
                                            }
                                        });
                                        mAdapter.notifyDataSetChanged();
                                        break;
                                    case 3:
                                        Collections.sort(mApps, new Comparator<AppInfo>() {
                                            @Override
                                            public int compare(AppInfo o1, AppInfo o2) {
                                                return (o1.fileSize < o2.fileSize ? 1 : -1);
                                            }
                                        });
                                        mAdapter.notifyDataSetChanged();
                                        break;
                                    case 4:
                                        Collections.sort(mApps, new Comparator<AppInfo>() {
                                            @Override
                                            public int compare(AppInfo o1, AppInfo o2) {
                                                return (o1.firstInstallTime > o2.firstInstallTime ? 1 : -1);
                                            }
                                        });
                                        mAdapter.notifyDataSetChanged();
                                        break;
                                    case 5:
                                        Collections.sort(mApps, new Comparator<AppInfo>() {
                                            @Override
                                            public int compare(AppInfo o1, AppInfo o2) {
                                                return (o1.firstInstallTime < o2.firstInstallTime ? 1 : -1);
                                            }
                                        });
                                        mAdapter.notifyDataSetChanged();
                                        break;
                                    default:
                                        break;

                                }
                                return true;
                            }
                        })
                        .positiveText("Done")
                        .show();
            }
        });

        mRvAppList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    mFabSort.hide();
                    mFabFilter.hide();
                } else if (dy < 0) {
                    if (mFreeApps.size() == 0) {
                        mFabSort.show();
                        mFabFilter.show();
                    }
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                String packageName = mFreeApps.get(0);
                for (int i = 0; i < mApps.size(); i++) {
                    if (mApps.get(i).packageName.compareTo(packageName) == 0) {
                        mApps.remove(i);
                        mAdapter.notifyDataSetChanged();
                    }
                }
            } else {
                String packageName = mFreeApps.get(0);
                for (int i = 0; i < mApps.size(); i++) {
                    if (mApps.get(i).packageName.compareTo(packageName) == 0) {
                        mApps.get(i).color = R.color.backgroundPrimary;
                        mAdapter.notifyDataSetChanged();
                    }
                }
                Toast.makeText(getApplication().getApplicationContext(), R.string.uninstall_fail, Toast.LENGTH_SHORT).show();
            }
            mFreeApps.remove(0);
            if (mFreeApps.size() != 0) {
                Uri packageUri = Uri.parse("package:" + mFreeApps.get(0));
                Intent uninstallIntent =
                        new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
                uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                startActivityForResult(uninstallIntent, 1);
            } else {
                mFreeApps.clear();
                mImgBtnDelete.setVisibility(View.GONE);
                mImgBtnBack.setVisibility(View.GONE);
                mImgBtnDelete.setVisibility(View.INVISIBLE);
                mTvFreeSize.setVisibility(View.INVISIBLE);
                mFabSort.setVisibility(View.VISIBLE);
                mFabFilter.setVisibility(View.VISIBLE);
                mAdapter.notifyDataSetChanged();
                Toast.makeText(getApplication().getApplicationContext(), R.string.uninstall_complete, Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                about();
                return true;
            case R.id.feedback:
                feedback();
                return true;
            case R.id.rate:
                rate();
                return true;
            case R.id.donate:
                donate();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean checkForRoot() {
        return RootManager.getInstance().hasRooted();
    }

    public void refreshList() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                mApps.clear();
                mPkgs = PackageUtils.getPackageNames(getApplicationContext());
                for (String pkg : mPkgs) {
                    mApps.add(new AppInfo(pkg, getApplicationContext()));
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                        mImgBtnBack.setVisibility(View.GONE);
                        mImgBtnDelete.setVisibility(View.GONE);
                        mTvFreeSize.setVisibility(View.INVISIBLE);
                        mFabSort.setVisibility(View.VISIBLE);
                        mFabFilter.setVisibility(View.VISIBLE);
                        mFreeApps.clear();
                        mSwipeLayout.setRefreshing(false);
                    }
                });
            }
        };
        thread.start();
    }

    private void feedback() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "batchuninstaller@protonmail.com", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.feedback));
        startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.send_email)));
    }

    private void rate() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void about() {
        new MaterialDialog.Builder(MainActivity.this)
                .title(R.string.about)
                .content(R.string.about_text)
                .positiveText(R.string.github)
                .negativeText(R.string.licenses)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sarbajitsaha/Batch-Uninstaller"));
                        startActivity(intent);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        new EasyLicensesDialogCompat(MainActivity.this)
                                .setTitle(R.string.licenses)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                })
                .show();
    }

    private void donate() {
        new MaterialDialog.Builder(MainActivity.this)
                .title(R.string.donate)
                .content(R.string.donate_content)
                .positiveText(R.string.copy)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Bitcoin Address", "3GRYNKRUFsefuvKuTycgbMjB4DFxUXVys4");
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getApplicationContext(), R.string.copy_successful, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        if (mFreeApps.size() == 0)
            super.onBackPressed();
        else {
            mImgBtnBack.callOnClick();
        }
    }

    public void uninstallAppRoot() {
        final String packageName = mFreeApps.get(0);
        Result res = RootManager.getInstance().runCommand("pm uninstall " + packageName);
        if (res.getMessage().toLowerCase().contains("success")) {
            Toast.makeText(getApplicationContext(), "Uninstalled " + packageName, Toast.LENGTH_SHORT).show();
            for (int i = 0; i < mApps.size(); i++) {
                if (mApps.get(i).packageName.compareTo(packageName) == 0) {
                    mApps.remove(i);
                    mAdapter.notifyDataSetChanged();
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), "Failed to uninstall " + packageName, Toast.LENGTH_SHORT).show();
            for (int i = 0; i < mApps.size(); i++) {
                if (mApps.get(i).packageName.compareTo(packageName) == 0) {
                    mApps.get(i).color = R.color.backgroundPrimary;
                    mAdapter.notifyDataSetChanged();
                }
            }
        }
        mFreeApps.remove(0);
        if (mFreeApps.size() != 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    uninstallAppRoot();
                }
            }, Toast.LENGTH_LONG);
        } else {
            mFreeApps.clear();
            mImgBtnDelete.setVisibility(View.GONE);
            mImgBtnBack.setVisibility(View.GONE);
            mImgBtnDelete.setVisibility(View.INVISIBLE);
            mTvFreeSize.setVisibility(View.INVISIBLE);
            mFabSort.setVisibility(View.VISIBLE);
            mFabFilter.setVisibility(View.VISIBLE);
            mAdapter.notifyDataSetChanged();
            Toast.makeText(getApplication().getApplicationContext(), R.string.uninstall_complete, Toast.LENGTH_SHORT).show();
        }


    }
}
