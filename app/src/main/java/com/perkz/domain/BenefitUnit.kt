package com.perkz.domain

import com.perkz.data.db.PerkEntity
import java.util.Locale

/**
 * Strongly typed unit describing what a perk's numeric value represents: currency, a count
 * of some discrete quantity (uses, passes, points, ...), plain access/status, or no value at
 * all. Persisted on [PerkEntity.benefitUnit] as a stable lowercase string ([code]) so it
 * round-trips through Room and the CSV parser without depending on enum ordinal/name.
 */
internal enum class BenefitUnit(val code: String) {
    /**
     * Legacy sheets that have no "Units value" column at all. The old currency-vs-uses
     * heuristic (see [isCountUnit]) is applied at display time based on the raw
     * max-value-or-uses text, exactly as before this feature existed.
     */
    AUTO("auto"),

    /** Explicit blank cell (or an unrecognized nonblank value): unitless, never gets a "$". */
    NONE("none"),

    USD("usd"),
    USES("uses"),
    PASSES("passes"),
    POINTS("points"),
    MILES("miles"),
    NIGHTS("nights"),
    MONTHS("months"),
    GUESTS("guests"),

    /** Simple access/status/membership perks: used/not-used, no numeric meter. */
    ACCESS("access");

    companion object {
        /** Decodes a value persisted via [code]. Unknown/blank codes safely fall back to [AUTO]. */
        fun fromCode(code: String?): BenefitUnit =
            entries.firstOrNull { it.code == code?.trim()?.lowercase(Locale.US) } ?: AUTO

        /**
         * Parses a raw "Units value" cell from the source sheet. A blank cell explicitly means
         * "no unit" and maps to [NONE] -- never [AUTO], which is reserved for sheets missing the
         * column entirely. Unrecognized nonblank text also safely maps to [NONE] so it never
         * causes a dollar sign to be shown.
         */
        fun fromSource(raw: String?): BenefitUnit {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isBlank()) return NONE
            val normalized = trimmed.lowercase(Locale.US)
            return when {
                normalized.contains("usd") || normalized.contains("$") || normalized.contains("dollar") -> USD
                normalized.contains("pass") -> PASSES
                normalized.contains("point") -> POINTS
                normalized.contains("mile") -> MILES
                normalized.contains("night") -> NIGHTS
                normalized.contains("month") -> MONTHS
                normalized.contains("guest") -> GUESTS
                normalized.contains("access") || normalized.contains("status") ||
                    normalized.contains("membership") -> ACCESS
                normalized.contains("use") -> USES
                else -> NONE
            }
        }
    }
}

private val QUANTITY_UNITS = setOf(
    BenefitUnit.USES,
    BenefitUnit.PASSES,
    BenefitUnit.POINTS,
    BenefitUnit.MILES,
    BenefitUnit.NIGHTS,
    BenefitUnit.MONTHS,
    BenefitUnit.GUESTS,
)

/** True for units that represent a discrete quantity (not currency, unitless, or access). */
internal fun BenefitUnit.isQuantity(): Boolean = this in QUANTITY_UNITS

/** Plural display label shown after quantity amounts, e.g. "3 of 5 passes". Null otherwise. */
internal fun BenefitUnit.quantityLabel(): String? = when (this) {
    BenefitUnit.USES -> "uses"
    BenefitUnit.PASSES -> "passes"
    BenefitUnit.POINTS -> "points"
    BenefitUnit.MILES -> "miles"
    BenefitUnit.NIGHTS -> "nights"
    BenefitUnit.MONTHS -> "months"
    BenefitUnit.GUESTS -> "guests"
    else -> null
}

/** Decodes this perk's persisted [PerkEntity.benefitUnit] string into a [BenefitUnit]. */
internal fun PerkEntity.resolvedBenefitUnit(): BenefitUnit = BenefitUnit.fromCode(benefitUnit)
