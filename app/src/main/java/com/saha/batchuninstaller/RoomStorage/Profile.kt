package com.saha.batchuninstaller.RoomStorage

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Profile (
    @PrimaryKey
    @ColumnInfo(name = "profile_name")
    val profileName: String,
    @ColumnInfo(name = "packages")
    val packages: String //packages separated by comma
)