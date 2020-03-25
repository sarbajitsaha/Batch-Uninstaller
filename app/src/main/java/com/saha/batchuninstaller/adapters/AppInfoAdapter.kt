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
package com.saha.batchuninstaller.adapters

import android.content.Context
import android.content.res.Resources
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.saha.batchuninstaller.AppInfo
import com.saha.batchuninstaller.R
import com.saha.batchuninstaller.adapters.AppInfoAdapter.ItemViewHolder
import github.nisrulz.recyclerviewhelper.RVHAdapter
import github.nisrulz.recyclerviewhelper.RVHViewHolder
import java.text.SimpleDateFormat
import java.util.*

class AppInfoAdapter(private val mContext: Context, private val mAppsList: List<AppInfo>) : RecyclerView.Adapter<ItemViewHolder>(), RVHAdapter {
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
		val view = LayoutInflater.from(parent.context)
				.inflate(R.layout.app_list_item, parent, false)
		return ItemViewHolder(view)
	}

	@Throws(Resources.NotFoundException::class)
	override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
		holder.mLayoutItem.setBackgroundColor(mContext.resources.getColor(mAppsList[position].color))
		holder.mImgIcon.setImageBitmap(mAppsList[position].icon)
		holder.mTvAppName.text = mAppsList[position].appName
		holder.mTvAppSize.text = Formatter.formatShortFileSize(mContext, mAppsList[position].fileSize)
		val date = Date(mAppsList[position].firstInstallTime)
		val df2 = SimpleDateFormat("dd/MM/yy")
		holder.mTvAppDate.text = df2.format(date)
	}

	override fun getItemCount(): Int {
		return mAppsList.size
	}

	override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
		return false
	}

	override fun onItemDismiss(position: Int, direction: Int) {
/*
        String pkg_name = mAppsList.get(position).packageName;
        mAppsList.remove(position);
        notifyItemRemoved(position);
        Uri packageUri = Uri.parse("package:" + pkg_name);
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
        uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(uninstallIntent);
*/
		notifyDataSetChanged()
	}

	inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), RVHViewHolder {
		var mLayoutItem: RelativeLayout = itemView.findViewById(R.id.layout_appitem)
		var mTvAppName: TextView = itemView.findViewById(R.id.tv_appname)
		var mTvAppSize: TextView = itemView.findViewById(R.id.tv_appsize)
		var mTvAppDate: TextView = itemView.findViewById(R.id.tv_date)
		var mImgIcon: ImageView = itemView.findViewById(R.id.img_icon)
		override fun onItemSelected(actionstate: Int) {
			//not used
		}

		override fun onItemClear() {
			//not used
		}
	}

}