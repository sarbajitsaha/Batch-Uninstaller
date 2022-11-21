package com.saha.batchuninstaller.RoomStorage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile")
    fun getAll(): List<Profile>

    @Query("SELECT packages FROM profile WHERE profile_name LIKE :profileName")
    fun loadProfilePackages(profileName: String): String

    @Query("SELECT * FROM profile WHERE profile_name LIKE :profileName")
    fun loadProfile(profileName: String): Profile

    @Query("SELECT COUNT(*) FROM profile WHERE profile_name LIKE :profileName")
    fun isProfileExist(profileName: String): Int

    @Insert
    fun insert(profile: Profile)

    @Delete
    fun delete(profile: Profile)
}
