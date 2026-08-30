package com.perkz.data.db

import androidx.room.Entity

@Entity(tableName = "usage", primaryKeys = ["perkId", "periodKey"])
data class UsageEntity(
    val perkId: String,
    val periodKey: String
)
