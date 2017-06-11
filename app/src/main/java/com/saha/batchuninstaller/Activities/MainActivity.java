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
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
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

    private RecyclerView recyclerView;
    private AppInfoAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<AppInfo> apps;
    private List<String> pkgs;
    private Toolbar toolbar;
    private TextView free_size_tv;
    private List<String> free_apps;
    private ImageButton back, delete;
    private FloatingActionButton sort;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = sharedPreferences.edit();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        free_size_tv = (TextView) findViewById(R.id.free_size);
        back = (ImageButton) findViewById(R.id.imgbtn_back);
        delete = (ImageButton) findViewById(R.id.delete);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        sort = (FloatingActionButton) findViewById(R.id.sort);

        free_size_tv.setVisibility(View.INVISIBLE);
        back.setVisibility(View.GONE);
        delete.setVisibility(View.GONE);
        setSupportActionBar(toolbar);

        if (!sharedPreferences.getBoolean("ask_again", false)) {
            if (checkForRoot()) {
                new MaterialDialog.Builder(MainActivity.this)
                        .title(R.string.important)
                        .content(R.string.root_check_content)
                        .neutralText(R.string.understood)
                        .onAny(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                editor.putBoolean("ask_again", dialog.isPromptCheckBoxChecked());
                                editor.commit();

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
                                editor.putBoolean("ask_again", dialog.isPromptCheckBoxChecked());
                                editor.commit();
                            }
                        })
                        .show();
            }
        }

        apps = new ArrayList<>();
        free_apps = new ArrayList<>();

        pkgs = PackageUtils.getPackageNames(getApplicationContext());
        for (String pkg : pkgs) {
            apps.add(new AppInfo(pkg, getApplicationContext()));
        }

        adapter = new AppInfoAdapter(getApplicationContext(), apps);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new RVHItemTouchHelperCallback(
                adapter, true, true, true);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(recyclerView);

        recyclerView.addItemDecoration(
                new RVHItemDividerDecoration(this, LinearLayoutManager.VERTICAL));

        recyclerView.addOnItemTouchListener(new RVHItemClickListener(
                this, new RVHItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                if(apps.get(position).system_app && free_apps.contains(apps.get(position).package_name))
                {
                    if(!RootManager.getInstance().obtainPermission())
                    {
                        Toast.makeText(getApplicationContext(),R.string.no_root_system_app,Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(),R.string.root_system_app, Toast.LENGTH_SHORT).show();
                    }
                }


                apps.get(position).color = (
                        apps.get(position).color == R.color.backgroundSelected
                ) ? R.color.backgroundPrimary : R.color.backgroundSelected;
                adapter.notifyItemChanged(position);

                if (free_apps.contains(apps.get(position).package_name)) {
                    free_apps.remove(apps.get(position).package_name);
                } else {
                    free_apps.add(apps.get(position).package_name);
                }

                if (free_apps.size() == 0) {
                    back.setVisibility(View.GONE);
                    delete.setVisibility(View.GONE);
                    free_size_tv.setVisibility(View.INVISIBLE);
                    sort.setVisibility(View.VISIBLE);
                } else {
                    delete.setVisibility(View.VISIBLE);
                    back.setVisibility(View.VISIBLE);
                    free_size_tv.setVisibility(View.VISIBLE);
                    sort.setVisibility(View.INVISIBLE);
                    long total_bytes = 0;
                    for (String pkg : free_apps) {
                        total_bytes += PackageUtils.getApkSize(getApplicationContext(), pkg);
                    }
                    free_size_tv.setText(android.text.format.Formatter
                            .formatShortFileSize(
                                    getApplicationContext(),
                                    total_bytes
                            ));
                }
            }
        }));

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                free_apps.clear();
                back.setVisibility(View.GONE);
                free_size_tv.setVisibility(View.INVISIBLE);
                sort.setVisibility(View.VISIBLE);
                delete.setVisibility(View.GONE);
                for (int i = 0; i < apps.size(); i++)
                    apps.get(i).color = R.color.backgroundPrimary;
                adapter.notifyDataSetChanged();
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
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
                                int count = 0;
                                for (int i = 0; i < free_apps.size(); i++) {
                                    int index = -1;
                                    for (int j = 0; j < apps.size(); j++) {
                                        if (apps.get(j).package_name.compareTo(free_apps.get(i)) == 0) {
                                            index = j;
                                            break;
                                        }
                                    }

                                    if (RootManager.getInstance().obtainPermission()) {
                                        Result res = RootManager.getInstance().runCommand("pm uninstall " + free_apps.get(i));
                                        if (res.getMessage().toLowerCase().contains("success")) {
                                            Toast.makeText(getApplicationContext(),"Uninstalled "+apps.get(index).app_name,Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Failed to uninstall " + apps.get(index).app_name, Toast.LENGTH_SHORT).show();

                                        }
                                    } else {
                                        Uri packageUri = Uri.parse("package:" + free_apps.get(i));
                                        Intent uninstallIntent =
                                                new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
                                        startActivity(uninstallIntent);
                                    }
                                    if (index != -1)
                                        apps.remove(index);
                                    adapter.notifyDataSetChanged();
                                }
                                free_apps.clear();
                                delete.setVisibility(View.GONE);
                                back.setVisibility(View.GONE);
                                delete.setVisibility(View.INVISIBLE);
                                free_size_tv.setVisibility(View.INVISIBLE);

                            }
                        })
                        .show();
            }
        });


        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList();
            }
        });

        sort.setOnClickListener(new View.OnClickListener() {
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
                                        Collections.sort(apps, new Comparator<AppInfo>() {
                                            @Override
                                            public int compare(AppInfo o1, AppInfo o2) {
                                                return o1.app_name.compareToIgnoreCase(o2.app_name);
                                            }
                                        });
                                        adapter.notifyDataSetChanged();
                                        break;
                                    case 1:
                                        Collections.sort(apps, new Comparator<AppInfo>() {
                                            @Override
                                            public int compare(AppInfo o1, AppInfo o2) {
                                                return o2.app_name.compareToIgnoreCase(o1.app_name);
                                            }
                                        });
                                        adapter.notifyDataSetChanged();
                                        break;
                                    case 2:
                                        Collections.sort(apps, new Comparator<AppInfo>() {
                                            @Override
                                            public int compare(AppInfo o1, AppInfo o2) {
                                                return (o1.file_size > o2.file_size ? 1 : -1);
                                            }
                                        });
                                        adapter.notifyDataSetChanged();
                                        break;
                                    case 3:
                                        Collections.sort(apps, new Comparator<AppInfo>() {
                                            @Override
                                            public int compare(AppInfo o1, AppInfo o2) {
                                                return (o1.file_size < o2.file_size ? 1 : -1);
                                            }
                                        });
                                        adapter.notifyDataSetChanged();
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

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    sort.hide();
                } else if (dy < 0) {
                    if (free_apps.size() == 0)
                        sort.show();
                }
            }
        });

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
                apps.clear();
                pkgs = PackageUtils.getPackageNames(getApplicationContext());
                for (String pkg : pkgs) {
                    apps.add(new AppInfo(pkg, getApplicationContext()));
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        back.setVisibility(View.GONE);
                        delete.setVisibility(View.GONE);
                        free_size_tv.setVisibility(View.INVISIBLE);
                        sort.setVisibility(View.VISIBLE);
                        free_apps.clear();
                        swipeRefreshLayout.setRefreshing(false);
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
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/sarbajitsaha/Batch-Uninstaller"));
                        startActivity(intent);
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
                        Toast.makeText(getApplicationContext(),R.string.copy_successful,Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}
