# 07 · 8 Farms P&L and Profit-per-Vigha Scorecard

**Owning agent:** compose-screens · **Canvas:** 04 8 Farms P&L + Vigha Scorecard · **Depends on:** 03

## Purpose
Show farming profit per farm and in total for a chosen season, strictly from `section = FARM`. Rank the 8 farms by **net profit per Vigha**, so the owner can see which land and crop performs best.

## User stories
- I pick Kharif 2026 and see income, expense and net for all farms together, plus the total land.
- I see the farms ranked by profit per Vigha, and can switch the ranking to total profit.
- I tap a farm to see its entries and its active crop.
- A farm with no Vigha set is not ranked wrongly. It says "વીઘા સેટ કરો".

## Screens
**FarmsScreen** (artboard 04):
1. `VkTopHeader("૮ ખેતર હિસાબ", onBack)`, with a trailing PDF `IconButton` (`ic_vk_report`) that generates report #2 or #3 (doc 15).
2. Period chips (`VkChip`, single select): **ખરીફ YYYY** (Jun–Oct), **રવી YYYY** (Nov–Mar), **ઉનાળુ YYYY** (Mar–Jun), **આખું વર્ષ** (Apr–Mar). The default is the season containing today.

   ⚠️ Rabi crosses the calendar year, so "રવી 2026" means 01-11-2026 → 31-03-2027.
3. Summary `VkCard(tone = Primary)` with a 2×2 stat grid:
   - ખેતી આવક
   - ખેતી ખર્ચ
   - ચોખ્ખો નફો (26sp, primary or error)
   - કુલ જમીન (sum of Vigha)

   Caption: "અંગત ખર્ચ આ હિસાબમાં ગણાતો નથી."
4. `VkSectionTitle("સ્કોરકાર્ડ")` with an inline `VkSegmented` of **નફો / વીઘા** and **કુલ નફો**.
5. A list of 8 `VkCard` rows, one per farm. Each row has:
   - rank medallion (36dp circle; ranks 1–3 in primaryContainer with a primary numeral, others in surfaceContainer)
   - farm name (17sp/800)
   - "{crop} · {vigha} વીઘા" or "વીઘા સેટ કરો"
   - value on the right (20sp/800, primary, or error if negative) with the caption "પ્રતિ વીઘા" or "કુલ નફો"
   - a 8dp `LinearProgressIndicator`-style bar (primary on surfaceContainer), with its width proportional to the maximum value

   Tapping a row opens FarmDetailScreen.

**FarmDetailScreen**:
- header with the farm name and the crop badge
- the same 3 stats for this farm
- an `ActiveCycle` card: crop, season, start date, with a "પાક બદલો" button that closes the current cycle and starts a new one
- the entry list for the period (`VkListRow`) with bill chips
- a FAB-style `VkPrimaryButton("+ આ ખેતર માટે એન્ટ્રી")` that opens Entry pre-filled with this farm

## Data
- `TxnDao.farmPnl(from, to)` and `farmTotals(from, to)` (doc 03).
- `MasterDao.activeCycleFor(farmId)` for the crop label.
- Period → date range: `SeasonRange.of(season, year): ClosedRange<LocalDate>`.

## Business logic
```kotlin
data class FarmScore(val farmId: Long, val name: String, val crop: String?, val vigha: Double,
                     val profitPaise: Long, val perVighaPaise: Long)
fun score(rows: List<FarmPnlRow>): List<FarmScore> = rows.map {
  val profit = it.incomePaise - it.expensePaise
  FarmScore(it.farmId, it.name, crop(it.farmId), it.areaInVigha, profit,
            if (it.areaInVigha <= 0.0) 0L else (profit / it.areaInVigha).roundToLong()) }
fun rank(list: List<FarmScore>, byVigha: Boolean) =
  list.sortedWith(compareByDescending<FarmScore> { if (byVigha && it.vigha <= 0) Long.MIN_VALUE else if (byVigha) it.perVighaPaise else it.profitPaise }
                  .thenBy { it.name })
// Farms with vigha 0 always sort last in "per vigha" mode and display "—".
```
Bar width = `max(0, value) / max(1, maxPositiveValue)`.

## Edge cases
- If every farm has a loss, all bars are 0 and the values show in error.
- A farm can have income but no Vigha. It is still shown and counted in the totals, but not ranked per Vigha.
- A deleted crop cycle still keeps its entries, which show under the farm.

## Gujarati strings
| Key | Text |
|---|---|
| `farms_title` | ૮ ખેતર હિસાબ |
| `kharif` | ખરીફ |
| `rabi` | રવી |
| `summer` | ઉનાળુ |
| `full_year` | આખું વર્ષ |
| `farm_income` | ખેતી આવક |
| `farm_expense` | ખેતી ખર્ચ |
| `net_profit` | ચોખ્ખો નફો |
| `total_land` | કુલ જમીન |
| `vigha` | વીઘા |
| `personal_excluded` | અંગત ખર્ચ આ હિસાબમાં ગણાતો નથી. |
| `scorecard` | સ્કોરકાર્ડ |
| `per_vigha` | નફો / વીઘા |
| `total_profit` | કુલ નફો |
| `per_vigha_caption` | પ્રતિ વીઘા |
| `change_crop` | પાક બદલો |
| `entry_for_farm` | + આ ખેતર માટે એન્ટ્રી |

## Acceptance criteria
- [ ] The totals equal the sum of the farm rows, and they exclude PERSONAL, FAMILY and GENERAL.
- [ ] Vigha 0 never crashes, ranks last and shows "—".
- [ ] Changing the period chip updates the stats in < 100 ms (Flow re-query).
- [ ] The Rabi range crosses the year boundary correctly.

## Tests
- `FarmScoreTest`: per-Vigha division and rounding; vigha 0 → 0 and last; ranking ties broken by name.
- `SeasonRangeTest`: Rabi 2026 = 2026-11-01..2027-03-31.
- DAO isolation test (doc 03).
