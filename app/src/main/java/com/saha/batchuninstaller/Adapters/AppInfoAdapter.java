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

package com.saha.batchuninstaller.Adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.saha.batchuninstaller.AppInfo;
import com.saha.batchuninstaller.R;

import java.util.List;

import github.nisrulz.recyclerviewhelper.RVHAdapter;
import github.nisrulz.recyclerviewhelper.RVHViewHolder;

public class AppInfoAdapter extends RecyclerView.Adapter<AppInfoAdapter.ItemViewHolder> implements RVHAdapter {
    private List<AppInfo> apps;
    private Context context;

    public AppInfoAdapter(Context context, List<AppInfo> apps) {
        this.context = context;
        this.apps = apps;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_list_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        holder.layout.setBackgroundColor(context.getResources().getColor(apps.get(position).color));
        holder.icon.setImageBitmap(apps.get(position).icon);
        holder.app_name.setText(apps.get(position).app_name);
        holder.app_size.setText(android.text.format.Formatter.formatShortFileSize(context, apps.get(position).file_size));
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        return false;
    }

    @Override
    public void onItemDismiss(int position, int direction) {
        String pkg_name = apps.get(position).package_name;
        apps.remove(position);
        notifyItemRemoved(position);
        Uri packageUri = Uri.parse("package:" + pkg_name);
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
        uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(uninstallIntent);
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder implements RVHViewHolder {
        RelativeLayout layout;
        TextView app_name, app_size;
        ImageView icon;

        public ItemViewHolder(View itemView) {
            super(itemView);
            app_name = (TextView) itemView.findViewById(R.id.app_name);
            app_size = (TextView) itemView.findViewById(R.id.app_size);
            icon = (ImageView) itemView.findViewById(R.id.icon_img);
            layout = (RelativeLayout) itemView.findViewById(R.id.layout);
        }

        @Override
        public void onItemSelected(int actionstate) {
            //not used
        }

        @Override
        public void onItemClear() {
            //not used
        }
    }
}