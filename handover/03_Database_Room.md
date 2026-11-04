# 03 · Database (Room) — 13 entities

**Owning agent:** room-database · **Depends on:** 02 (project exists) · **Wave 1**

## Purpose
This module is the single local source of truth for all money, farms, people, assets, reminders and Panchang preferences. It runs offline on the device. Farm P&L is strictly isolated from personal money.

> **Entity count is now 13.**
> - 9 original: Farm, Family, CropMaster, CategoryMaster, FarmCropCycle, Transaction, ShoppingItem, Asset, Reminder.
> - 2 from New_Features: PersonProfile, AssetLoan.
> - 2 for Panchang alerts (doc 10): PanchangAlertPref, CustomFestival.

## Conventions
- Money is stored as **`Long` paise**. Never use `Double` for money. Format it in the UI (`₹48,800`).
- Dates use `LocalDate` stored as `Long` epochDay, and instants use `Long` epochMillis, via `@TypeConverters`.
- Enums are stored as `String` (`EnumConverters`).
- Every table has `createdAt` and `updatedAt` (epochMillis).
- Soft delete via `isDeleted: Boolean` on Transaction, ShoppingItem, Asset and Reminder. Every query filters `isDeleted = 0`.
- Database: `AppDatabase` "vraj_kheti.db", `version = 1`, `exportSchema = true` (`room.schemaLocation = $projectDir/schemas`).

## Entities (`data/entity/`)
```kotlin
enum class AccountSection { FARM, FAMILY, GENERAL, PERSONAL }
enum class TxnType { INCOME, EXPENSE, ADVANCE }        // ADVANCE = ઉપાડ (Family)
enum class PayStatus { PAID, PENDING_PAYABLE, PENDING_RECEIVABLE } // દેવાના / લેવાના બાકી
enum class ShopTab { FARM, PERSONAL }
enum class Priority { HIGH, NORMAL }
enum class ProfileRole { OWNER, MANAGER_1, MANAGER_2 }
enum class PanchangAlertType { BIJ, AGIYARAS, POONAM, AMAS, FESTIVAL }

@Entity(tableName = "farm") data class FarmEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String /*ખેતર 1*/, val areaInVigha: Double,
  val managerRole: ProfileRole? = null, val sortOrder: Int, val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "family") data class FamilyEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0, val code: String /*A|B*/, val displayName: String,
  val whatsappPhone: String?, val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "crop_master", indices = [Index(value = ["name"], unique = true)])
data class CropMasterEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val isActive: Boolean = true)

@Entity(tableName = "category_master")
data class CategoryMasterEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String,
  val section: AccountSection, val type: TxnType, val isPhysicalTool: Boolean = false, val isActive: Boolean = true)

@Entity(tableName = "farm_crop_cycle",
  foreignKeys = [ForeignKey(FarmEntity::class, ["id"], ["farmId"], onDelete = CASCADE),
                 ForeignKey(CropMasterEntity::class, ["id"], ["cropId"], onDelete = RESTRICT)],
  indices = [Index("farmId"), Index("cropId")])
data class FarmCropCycleEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val farmId: Long, val cropId: Long,
  val season: String /*KHARIF|RABI|SUMMER*/, val year: Int, val startDate: LocalDate, val endDate: LocalDate?, val isActive: Boolean)

@Entity(tableName = "txn",
  foreignKeys = [ForeignKey(FarmEntity::class, ["id"], ["farmId"], onDelete = SET_NULL),
                 ForeignKey(FamilyEntity::class, ["id"], ["familyId"], onDelete = SET_NULL),
                 ForeignKey(FarmCropCycleEntity::class, ["id"], ["cycleId"], onDelete = SET_NULL),
                 ForeignKey(CategoryMasterEntity::class, ["id"], ["categoryId"], onDelete = SET_NULL)],
  indices = [Index("date"), Index("section"), Index("farmId"), Index("familyId"), Index("payStatus")])
data class TransactionEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0,
  val date: LocalDate, val tithiLabel: String /*cached "આસો વદ નોમ"*/, val section: AccountSection, val type: TxnType,
  val amountPaise: Long, val payStatus: PayStatus = PayStatus.PAID, val counterparty: String? = null,
  val farmId: Long? = null, val cycleId: Long? = null, val familyId: Long? = null, val categoryId: Long? = null,
  val note: String? = null, val voiceNote: String? = null, val billPhotoPath: String? = null,
  val mann: Double? = null, val extraKg: Double? = null, val ratePerMannPaise: Long? = null,
  val isDeleted: Boolean = false, val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "shopping_item", indices = [Index("tab"), Index("isBought")])
data class ShoppingItemEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val tab: ShopTab, val name: String,
  val forDate: LocalDate, val farmId: Long?, val estPricePaise: Long?, val isPhysicalTool: Boolean = false,
  val isBought: Boolean = false, val boughtAt: Long? = null, val convertedAssetId: Long? = null,
  val isDeleted: Boolean = false, val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "asset")
data class AssetEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val iconKey: String /*asset|lend|utility*/,
  val purchaseDate: LocalDate?, val pricePaise: Long?, val photoPath: String?, val note: String?,
  val isDeleted: Boolean = false, val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "asset_loan",
  foreignKeys = [ForeignKey(AssetEntity::class, ["id"], ["assetId"], onDelete = CASCADE)],
  indices = [Index("assetId"), Index("returnedAt"), Index("expectedReturnDate")])
data class AssetLoanEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val assetId: Long,
  val borrowerName: String, val borrowerPhone: String?, val borrowerPhotoPath: String?,
  val handedOverDate: LocalDate, val handedOverTithi: String, val expectedReturnDate: LocalDate,
  val returnedAt: Long? = null, val notifyOnDue: Boolean = true, val lastWhatsAppReminderAt: Long? = null,
  val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "reminder", indices = [Index("isDone"), Index("dueAt")])
data class ReminderEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val note: String?,
  val imagePath: String?, val priority: Priority, val dueAt: Long, val isDone: Boolean = false, val doneAt: Long? = null,
  val sourceType: String? = null /*LOAN|PANCHANG|MANUAL*/, val sourceId: Long? = null,
  val isDeleted: Boolean = false, val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "person_profile", indices = [Index(value = ["role"], unique = true)])
data class PersonProfileEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val role: ProfileRole,
  val name: String, val phone: String?, val address: String?, val photoPath: String?, val updatedAt: Long)

@Entity(tableName = "panchang_alert_pref")
data class PanchangAlertPrefEntity(@PrimaryKey val type: PanchangAlertType, val enabled: Boolean = true,
  val notifyEveningBefore: Boolean = true, val showDialogOnDay: Boolean = true,
  val doText: String /*user-editable guidance*/, val avoidText: String, val updatedAt: Long)

@Entity(tableName = "custom_festival")
data class CustomFestivalEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val nameGu: String,
  val monthIndex: Int? /*0..11 amanta*/, val paksha: String?, val tithiInPaksha: Int?,
  val fixedMonthDay: String? /*"01-14" for solar dates*/, val note: String?, val enabled: Boolean = true)
```
Panchang-related values that live outside the database, in `SharedPreferences`: `panchang_evening_hour` (default 19), `panchang_show_guidance` (default true), `panchang_dialog_dismissed_<epochDay>`.

Farm → manager assignment is stored on `FarmEntity.managerRole`, so profiles need no CSV column. This replaces `assignedFarmIds` from New_Features.

## DAOs (`data/dao/`) — all reads return `Flow`
```kotlin
@Dao interface TxnDao {
  @Insert suspend fun insert(t: TransactionEntity): Long
  @Update suspend fun update(t: TransactionEntity)
  @Query("UPDATE txn SET isDeleted=1, updatedAt=:now WHERE id=:id") suspend fun softDelete(id: Long, now: Long)
  @Query("SELECT * FROM txn WHERE isDeleted=0 ORDER BY date DESC, createdAt DESC LIMIT :limit")
  fun recent(limit: Int = 20): Flow<List<TransactionEntity>>

  // FARM P&L — the only P&L source. PERSONAL is excluded by design.
  @Query("""SELECT f.id farmId, f.name, f.areaInVigha,
      COALESCE(SUM(CASE WHEN t.type='INCOME'  THEN t.amountPaise END),0) incomePaise,
      COALESCE(SUM(CASE WHEN t.type='EXPENSE' THEN t.amountPaise END),0) expensePaise
    FROM farm f LEFT JOIN txn t ON t.farmId=f.id AND t.isDeleted=0 AND t.section='FARM'
      AND t.date BETWEEN :from AND :to
    GROUP BY f.id ORDER BY f.sortOrder""")
  fun farmPnl(from: LocalDate, to: LocalDate): Flow<List<FarmPnlRow>>

  @Query("""SELECT COALESCE(SUM(CASE WHEN type='INCOME' THEN amountPaise END),0) income,
      COALESCE(SUM(CASE WHEN type='EXPENSE' THEN amountPaise END),0) expense
    FROM txn WHERE isDeleted=0 AND section='FARM' AND date BETWEEN :from AND :to""")
  fun farmTotals(from: LocalDate, to: LocalDate): Flow<Totals>

  @Query("SELECT COALESCE(SUM(amountPaise),0) FROM txn WHERE isDeleted=0 AND section='GENERAL' AND type='EXPENSE' AND date BETWEEN :from AND :to")
  fun generalExpense(from: LocalDate, to: LocalDate): Flow<Long>

  @Query("""SELECT COALESCE(SUM(CASE WHEN type='INCOME' THEN amountPaise END),0) income,
      COALESCE(SUM(CASE WHEN type='EXPENSE' THEN amountPaise END),0) expense
    FROM txn WHERE isDeleted=0 AND section='PERSONAL' AND date BETWEEN :from AND :to""")
  fun personalTotals(from: LocalDate, to: LocalDate): Flow<Totals>

  @Query("""SELECT c.name, SUM(t.amountPaise) total FROM txn t JOIN category_master c ON c.id=t.categoryId
    WHERE t.isDeleted=0 AND t.section='PERSONAL' AND t.type='EXPENSE' AND t.date BETWEEN :from AND :to
    GROUP BY c.id ORDER BY total DESC""")
  fun personalByCategory(from: LocalDate, to: LocalDate): Flow<List<CategoryTotal>>

  @Query("""SELECT COALESCE(SUM(amountPaise),0) FROM txn WHERE isDeleted=0 AND section='FAMILY' AND type='ADVANCE'
    AND familyId=:familyId AND date BETWEEN :from AND :to""")
  fun familyAdvanceTotal(familyId: Long, from: LocalDate, to: LocalDate): Flow<Long>

  @Query("SELECT * FROM txn WHERE isDeleted=0 AND section='FAMILY' AND familyId=:familyId AND date BETWEEN :from AND :to ORDER BY date DESC")
  fun familyEntries(familyId: Long, from: LocalDate, to: LocalDate): Flow<List<TransactionEntity>>

  @Query("""SELECT COALESCE(SUM(CASE WHEN payStatus='PENDING_PAYABLE' THEN amountPaise END),0) payable,
      COALESCE(SUM(CASE WHEN payStatus='PENDING_RECEIVABLE' THEN amountPaise END),0) receivable,
      COUNT(CASE WHEN payStatus!='PAID' THEN 1 END) count
    FROM txn WHERE isDeleted=0 AND section!='PERSONAL'""")
  fun pendingUdhaar(): Flow<UdhaarSummary>

  @Query("SELECT * FROM txn WHERE isDeleted=0 AND payStatus!='PAID' ORDER BY date") fun pendingList(): Flow<List<TransactionEntity>>
  @Query("UPDATE txn SET payStatus='PAID', updatedAt=:now WHERE id=:id") suspend fun settle(id: Long, now: Long)
}
// The Home udhaar card covers farm, family and general money only. Personal udhaar shows inside doc 09.

@Dao interface ShoppingDao {
  @Query("SELECT * FROM shopping_item WHERE isDeleted=0 AND isBought=0 AND tab=:tab ORDER BY forDate DESC, id DESC")
  fun active(tab: ShopTab): Flow<List<ShoppingItemEntity>>
  @Query("SELECT COUNT(*) FROM shopping_item WHERE isDeleted=0 AND isBought=0") fun activeCount(): Flow<Int>
  @Query("UPDATE shopping_item SET isBought=:bought, boughtAt=:at, updatedAt=:at WHERE id=:id") suspend fun setBought(id: Long, bought: Boolean, at: Long?)
  @Insert suspend fun insert(i: ShoppingItemEntity): Long
  @Query("UPDATE shopping_item SET convertedAssetId=:assetId WHERE id=:id") suspend fun markConverted(id: Long, assetId: Long)
}

@Dao interface AssetDao {
  @Insert suspend fun insertAsset(a: AssetEntity): Long
  @Query("""SELECT a.*, (SELECT COUNT(*) FROM asset_loan l WHERE l.assetId=a.id AND l.returnedAt IS NULL) > 0 AS isLent
    FROM asset a WHERE a.isDeleted=0 ORDER BY a.name""") fun inventory(): Flow<List<AssetWithStatus>>
  @Insert suspend fun insertLoan(l: AssetLoanEntity): Long
  @Query("SELECT * FROM asset_loan WHERE returnedAt IS NULL ORDER BY expectedReturnDate") fun activeLoans(): Flow<List<LoanWithAsset>>  // @Transaction + @Relation
  @Query("SELECT * FROM asset_loan WHERE returnedAt IS NULL AND expectedReturnDate < :today ORDER BY expectedReturnDate") fun overdueLoans(today: LocalDate): Flow<List<LoanWithAsset>>
  @Query("SELECT * FROM asset_loan WHERE returnedAt IS NULL AND expectedReturnDate < :today") suspend fun overdueLoansNow(today: LocalDate): List<LoanWithAsset>
  @Query("UPDATE asset_loan SET returnedAt=:at, updatedAt=:at WHERE id=:id") suspend fun markReturned(id: Long, at: Long)
  @Query("UPDATE asset_loan SET lastWhatsAppReminderAt=:at WHERE id=:id") suspend fun markReminded(id: Long, at: Long)
  // History search: name, asset or date "23-11-2025". The repo converts a dd-MM-yyyy query to epochDay.
  @Query("""SELECT l.* FROM asset_loan l JOIN asset a ON a.id=l.assetId
    WHERE l.borrowerName LIKE '%'||:q||'%' OR a.name LIKE '%'||:q||'%' OR l.handedOverDate=:epochDay
    ORDER BY l.handedOverDate DESC""") fun searchHistory(q: String, epochDay: Long?): Flow<List<LoanWithAsset>>
}

@Dao interface ReminderDao {
  @Query("SELECT * FROM reminder WHERE isDeleted=0 AND isDone=0 ORDER BY priority='HIGH' DESC, dueAt") fun active(): Flow<List<ReminderEntity>>
  @Query("SELECT * FROM reminder WHERE isDeleted=0 AND isDone=1 ORDER BY doneAt DESC") fun archived(): Flow<List<ReminderEntity>>
  @Query("SELECT * FROM reminder WHERE isDeleted=0 AND isDone=0 AND priority='HIGH' ORDER BY dueAt LIMIT :n") fun urgent(n: Int = 3): Flow<List<ReminderEntity>>
  @Query("UPDATE reminder SET isDone=1, doneAt=:at, updatedAt=:at WHERE id=:id") suspend fun markDone(id: Long, at: Long)
  @Insert suspend fun insert(r: ReminderEntity): Long
}

@Dao interface MasterDao { /* CRUD + Flow lists for farm, family, crop_master, category_master, farm_crop_cycle; activeCycleFor(farmId) */ }
@Dao interface ProfileDao {
  @Query("SELECT * FROM person_profile ORDER BY role") fun all(): Flow<List<PersonProfileEntity>>
  @Upsert suspend fun upsert(p: PersonProfileEntity)
}
@Dao interface PanchangDao {
  @Query("SELECT * FROM panchang_alert_pref") fun prefs(): Flow<List<PanchangAlertPrefEntity>>
  @Query("SELECT * FROM panchang_alert_pref") suspend fun prefsNow(): List<PanchangAlertPrefEntity>
  @Upsert suspend fun upsertPref(p: PanchangAlertPrefEntity)
  @Query("SELECT * FROM custom_festival WHERE enabled=1") suspend fun customFestivals(): List<CustomFestivalEntity>
  @Upsert suspend fun upsertFestival(f: CustomFestivalEntity)
}
```
Helper rows (`FarmPnlRow`, `Totals`, `UdhaarSummary`, `CategoryTotal`, `AssetWithStatus`, `LoanWithAsset` with `@Embedded loan` + `@Relation asset`) live in `data/model/`.

**Profit per Vigha**, in the repository rather than SQL:
```kotlin
fun profitPerVigha(row: FarmPnlRow): Long = if (row.areaInVigha <= 0.0) 0L
    else ((row.incomePaise - row.expensePaise) / row.areaInVigha).roundToLong()
```

## Seeding (`RoomDatabase.Callback.onCreate`, one transaction)
- Farms ખેતર 1 … ખેતર 8 with areaInVigha `0.0`, which the user sets in Settings. Farms 1–4 get `MANAGER_1` and farms 5–8 get `MANAGER_2`.
- Family A ("પરિવાર A") and Family B ("પરિવાર B") with phone null.
- Profiles:
  - OWNER: name "ગિરવાનસિંહ વાળા", address "મુ. પો. થોરાળા, તા. મહુવા, જિ. ભાવનગર", phone null.
  - MANAGER_1 and MANAGER_2: name "" (blank), to be filled by the user.
- Crops: કપાસ, મગફળી, ડુંગળી, ઘઉં, બાજરી, જીરું, તલ, ચણા, એરંડા, જુવાર.
- Categories:
  - FARM EXPENSE: બિયારણ, ખાતર, દવા, મજૂરી, ડીઝલ, ભાડું, સાધન (isPhysicalTool=true), પાણી
  - FARM INCOME: પાક વેચાણ
  - GENERAL EXPENSE: લાઇટ બિલ, પાણી વ્યવસ્થા, CCTV, રિપેરિંગ
  - PERSONAL EXPENSE: કરિયાણું, શિક્ષણ, દવા, તહેવાર, કપડાં, અન્ય
  - PERSONAL INCOME: ભાડાની આવક, અન્ય આવક
  - FAMILY ADVANCE: રોકડ ઉપાડ
- PanchangAlertPref: all 5 types enabled, with the default guidance texts from doc 10.

## Migrations policy
Ship `version = 1`. Every schema change after release must:
1. bump the version,
2. add an explicit `Migration(n, n+1)`,
3. add a `MigrationTestHelper` test.

**`fallbackToDestructiveMigration()` is forbidden**, because this is the owner's ledger. Before any migration runs, `BackupScheduler` (doc 15) takes an on-device copy of the DB file.

## Animal husbandry section (added 2026-11-04)
- **Enum:** `AccountSection` is now `FARM, ANIMAL, FAMILY, GENERAL, PERSONAL`. `ANIMAL` = પશુપાલન (cows, buffaloes). It is stored by name, so there is **no schema change and no migration**.
- **Seeded categories:**
  - ANIMAL/EXPENSE: ઘાસચારો, ખાણ-દાણ, પશુ દવા / ડૉક્ટર, પશુ ખરીદી, અન્ય પશુ ખર્ચ
  - ANIMAL/INCOME: દૂધ વેચાણ, પશુ વેચાણ
  - That makes 29 categories in total.
- **New queries (TxnDao / LedgerRepository):**
  - `animalTotals`, `animalByCategory`
  - `businessTotals(from, to)`: income = INCOME rows, expense = EXPENSE + ADVANCE rows, `section != 'PERSONAL'`
  - `businessBySection(from, to)`: the same totals, grouped by section
  - Pure twins: `LedgerMath.countsInBusinessPnl(section)` and `LedgerMath.businessTotals(entries)`
- **Tests:** `BusinessPnlTest` (JVM) and `AppDaoTest.businessPnlCountsAnimalHusbandryButNotPersonal`.
- **Existing dev installs** seeded before this change won't have the ANIMAL categories, because seeding only runs on create. Clear the app data once. The app is not in production yet.

## Isolation rules (tested)
0. **Business P&L = everything except PERSONAL:** FARM + ANIMAL + GENERAL expenses + FAMILY advances (farm managers' salary). PERSONAL is never part of any P&L.
1. Farm P&L and farm totals filter `section='FARM'` only. PERSONAL, ANIMAL, FAMILY and GENERAL rows never change the per-farm profit or the Vigha scorecard. Those sections appear in the business P&L instead.
2. GENERAL expenses appear on their own card. They are not allocated to farms.
3. Personal shopping items (`tab=PERSONAL`) are never counted in farm reports.
4. Family A and Family B totals filter by `familyId`.
5. The Home udhaar card excludes PERSONAL.

## Edge cases
- Deleting a farm needs a confirm dialog, then cascades its cycles. Its transactions keep `farmId = null` and appear under "ખેતર કાઢી નાખેલ" in reports.
- A loan's asset is soft-deleted → hide it from the inventory but keep it in history.
- Two loans can be active on the same asset only if the user confirms ("પહેલેથી ઉછીનું આપેલ છે").

## Acceptance criteria
- [ ] All 13 entities compile, and the schema JSON is exported.
- [ ] The seeding produces exactly the data above, once.
- [ ] Every query listed is exposed through a repository in `data/repo/`.
- [ ] Money is stored only as `Long` paise.

## Tests (`androidTest`, `Room.inMemoryDatabaseBuilder`)
- `AppDaoTest.personalExcludedFromFarmPnl`: insert FARM ₹1,000 income and PERSONAL ₹500 expense → farm profit is 1,000.
- `AppDaoTest.familyIsolation`: an A advance of ₹5,000 does not change B.
- `AppDaoTest.pendingUdhaarExcludesPersonal`
- `AssetLoanDaoTest.overdueExcludesReturned` and `searchHistoryByDate`: tractor lent 23-11-2025 is found by the query "23-11-2025" and by "ટ્રેક્ટર".
- `ProfileDaoTest.uniqueRoles`: upserting OWNER twice keeps 1 row.
- `RepoTest.profitPerVighaZero`: area 0 → 0.
