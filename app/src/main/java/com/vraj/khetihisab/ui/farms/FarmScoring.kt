package com.vraj.khetihisab.ui.farms

import com.vraj.khetihisab.data.model.FarmPnlRow
import com.vraj.khetihisab.data.repo.LedgerMath

/** One farm on the scorecard (doc 07 "Business logic"). Money in paise. */
data class FarmScore(
    val farmId: Long,
    val name: String,
    val crop: String?,
    val vigha: Double,
    val incomePaise: Long,
    val expensePaise: Long,
) {
    val profitPaise: Long get() = incomePaise - expensePaise

    /** 0 when Vigha is 0 (CLAUDE.md). */
    val perVighaPaise: Long get() = LedgerMath.profitPerVigha(incomePaise, expensePaise, vigha)
    val hasVigha: Boolean get() = !vigha.isNaN() && vigha > 0.0
}

/** What one scorecard row displays. [valuePaise] null = "—" (no Vigha in per-Vigha mode). */
data class ScoreRowUi(
    val rank: Int,
    val score: FarmScore,
    val valuePaise: Long?,
    /** 0..1 width of the progress bar. */
    val barFraction: Float,
)

object FarmScoring {

    fun score(rows: List<FarmPnlRow>, cropByFarm: Map<Long, String>): List<FarmScore> = rows.map {
        FarmScore(it.farmId, it.name, cropByFarm[it.farmId], it.areaInVigha, it.incomePaise, it.expensePaise)
    }

    /**
     * Per-Vigha mode: best profit per Vigha first, farms without Vigha always last.
     * Total mode: best total profit first. Ties are broken by name.
     */
    fun rank(list: List<FarmScore>, byVigha: Boolean): List<FarmScore> = list.sortedWith(
        compareByDescending<FarmScore> {
            when {
                !byVigha -> it.profitPaise
                !it.hasVigha -> Long.MIN_VALUE
                else -> it.perVighaPaise
            }
        }.thenBy { it.name },
    )

    /** Bar width = max(0, value) / max(1, maxPositiveValue). */
    fun barFraction(value: Long, maxPositive: Long): Float =
        if (value <= 0L) 0f else (value.toDouble() / maxOf(1L, maxPositive).toDouble()).toFloat().coerceIn(0f, 1f)

    fun display(list: List<FarmScore>, byVigha: Boolean): List<ScoreRowUi> {
        val ranked = rank(list, byVigha)
        val values = ranked.map { s -> if (byVigha) s.perVighaPaise.takeIf { s.hasVigha } else s.profitPaise }
        val maxPositive = values.filterNotNull().maxOrNull()?.coerceAtLeast(0L) ?: 0L
        return ranked.mapIndexed { i, s ->
            val v = values[i]
            ScoreRowUi(rank = i + 1, score = s, valuePaise = v, barFraction = v?.let { barFraction(it, maxPositive) } ?: 0f)
        }
    }

    /** Sum of the Vigha of every farm ("કુલ જમીન"). Non-positive areas count as 0. */
    fun totalVigha(list: List<FarmScore>): Double = list.sumOf { if (it.hasVigha) it.vigha else 0.0 }

    /** 12.0 → "12", 7.5 → "7.5". */
    fun vighaText(v: Double): String =
        if (v == Math.floor(v) && !v.isInfinite()) v.toLong().toString() else "%.2f".format(java.util.Locale.ROOT, v).trimEnd('0').trimEnd('.')
}
