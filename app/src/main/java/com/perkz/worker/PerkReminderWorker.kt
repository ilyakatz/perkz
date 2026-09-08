package com.perkz.worker

import android.content.Context
import androidx.core.text.HtmlCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.perkz.data.db.PerkDatabase
import com.perkz.data.db.PerkEntity
import com.perkz.data.repository.PerkRepository
import com.perkz.domain.classifyPerkStatus
import com.perkz.domain.usageAmountFor
import com.perkz.notification.PerkNotificationManager
import com.perkz.ui.model.PerkStatus
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PerkReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = androidx.room.Room.databaseBuilder(
            applicationContext,
            PerkDatabase::class.java,
            "perkz.db"
        ).build()
        val repository = PerkRepository(db.perkDao())
        val allPerks = repository.allPerks.first()
        val usage = repository.allUsage.first()
        val today = LocalDate.now()
        val usageByKey = usage.associateBy { it.perkId to it.periodKey }

        val expiringSoon = allPerks.filter { perk ->
            val key = com.perkz.domain.periodKeyFor(perk, today)
            val amount = usageByKey[perk.id to key]?.amount
            val totalAmount = usageAmountFor(perk, amount)
            classifyPerkStatus(perk, today, totalAmount) == PerkStatus.ExpiringSoon
        }

        if (expiringSoon.isNotEmpty()) {
            val notificationManager = PerkNotificationManager(applicationContext)
            expiringSoon.forEach { perk ->
                val sb = StringBuilder()
                val daysLeft = ChronoUnit.DAYS.between(today, today.withDayOfMonth(today.lengthOfMonth()))
                sb.append("⏳ ${daysLeft.coerceAtLeast(0)} days left • ${perk.interval} benefit")
                
                val styledMessage = HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
                notificationManager.showNotification(
                    title = "${perk.title} (${perk.card})",
                    message = styledMessage,
                    notificationId = perk.sourceRowNumber
                )
            }
        }

        return Result.success()
    }
}
