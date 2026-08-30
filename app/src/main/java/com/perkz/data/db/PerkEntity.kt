package com.perkz.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "perks")
data class PerkEntity(
    @PrimaryKey val id: String,
    val title: String,
    val card: String,
    val interval: String,
    val sourceRowNumber: Int,
    val resetPeriod: String,
    val deadlineTrigger: String,
    val maxValueOrUses: String,
    val details: String,
    val usedFromSheet: Boolean
)
