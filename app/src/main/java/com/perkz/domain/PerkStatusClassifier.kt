package com.perkz.domain

import com.perkz.data.db.PerkEntity
import com.perkz.ui.model.PerkStatus
import java.time.LocalDate

internal fun usageAmountFor(perk: PerkEntity, localUsageAmount: Double?): Double =
    localUsageAmount
        ?: perk.usedAmountFromSheet
        ?: if (perk.usedFromSheet) parseAmount(perk.maxValueOrUses) ?: 1.0 else 0.0

internal fun classifyPerkStatus(
    perk: PerkEntity,
    date: LocalDate,
    usageAmount: Double
): PerkStatus {
    val maxAmount = parseAmount(perk.maxValueOrUses)
    val used = usageAmount > 0.0
    return when {
        maxAmount != null && usageAmount >= maxAmount -> PerkStatus.Used
        isExpired(perk, date) -> PerkStatus.Expired
        maxAmount != null && usageAmount > 0.0 && usageAmount < maxAmount -> PerkStatus.PartiallyUsed
        used -> PerkStatus.Used
        isExpiringSoon(perk, date) -> PerkStatus.ExpiringSoon
        periodKeyFor(perk, date) == "%04d-%02d".format(date.year, date.monthValue) ->
            PerkStatus.NeedsUse
        else -> PerkStatus.Upcoming
    }
}
