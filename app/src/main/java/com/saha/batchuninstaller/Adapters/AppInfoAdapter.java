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
import androidx.recyclerview.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.saha.batchuninstaller.AppInfo;
import com.saha.batchuninstaller.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import github.nisrulz.recyclerviewhelper.RVHAdapter;
import github.nisrulz.recyclerviewhelper.RVHViewHolder;

public class AppInfoAdapter extends RecyclerView.Adapter<AppInfoAdapter.ItemViewHolder> implements RVHAdapter {
    private List<AppInfo> mAppsList;
    private Context mContext;

    public AppInfoAdapter(Context context, List<AppInfo> apps) {
        mContext = context;
        mAppsList = apps;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_list_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        holder.mLayoutItem.setBackgroundColor(mContext.getResources().getColor(mAppsList.get(position).color));
        holder.mImgIcon.setImageBitmap(mAppsList.get(position).icon);
        holder.mTvAppName.setText(mAppsList.get(position).appName);
        holder.mTvAppSize.setText(Formatter.formatShortFileSize(mContext, mAppsList.get(position).fileSize));

        Date date = new Date(mAppsList.get(position).firstInstallTime);
        SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yy");
        holder.mTvAppDate.setText(df2.format(date));
    }

    @Override
    public int getItemCount() {
        return mAppsList.size();
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        return false;
    }

    @Override
    public void onItemDismiss(int position, int direction) {
/*
        String pkg_name = mAppsList.get(position).packageName;
        mAppsList.remove(position);
        notifyItemRemoved(position);
        Uri packageUri = Uri.parse("package:" + pkg_name);
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
        uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(uninstallIntent);
*/
        notifyDataSetChanged();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder implements RVHViewHolder {
        RelativeLayout mLayoutItem;
        TextView mTvAppName, mTvAppSize, mTvAppDate;
        ImageView mImgIcon;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mTvAppName = itemView.findViewById(R.id.tv_appname);
            mTvAppSize = itemView.findViewById(R.id.tv_appsize);
            mTvAppDate = itemView.findViewById(R.id.tv_date);
            mImgIcon = itemView.findViewById(R.id.img_icon);
            mLayoutItem = itemView.findViewById(R.id.layout_appitem);
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