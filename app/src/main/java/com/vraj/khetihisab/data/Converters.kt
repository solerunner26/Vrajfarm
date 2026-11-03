package com.vraj.khetihisab.data

import androidx.room.TypeConverter
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.PanchangAlertType
import com.vraj.khetihisab.data.entity.PayStatus
import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.data.entity.ProfileRole
import com.vraj.khetihisab.data.entity.ShopTab
import com.vraj.khetihisab.data.entity.TxnType
import java.time.LocalDate

/** LocalDate <-> epochDay (Long). Instants are stored directly as epochMillis Long. */
class DateConverters {
    @TypeConverter fun localDateToEpochDay(d: LocalDate?): Long? = d?.toEpochDay()
    @TypeConverter fun epochDayToLocalDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)
}

/** Enums are stored as their `name` (TEXT), so raw SQL can compare with e.g. section='FARM'. */
class EnumConverters {
    @TypeConverter fun fromSection(v: AccountSection?): String? = v?.name
    @TypeConverter fun toSection(v: String?): AccountSection? = v?.let(AccountSection::valueOf)

    @TypeConverter fun fromTxnType(v: TxnType?): String? = v?.name
    @TypeConverter fun toTxnType(v: String?): TxnType? = v?.let(TxnType::valueOf)

    @TypeConverter fun fromPayStatus(v: PayStatus?): String? = v?.name
    @TypeConverter fun toPayStatus(v: String?): PayStatus? = v?.let(PayStatus::valueOf)

    @TypeConverter fun fromShopTab(v: ShopTab?): String? = v?.name
    @TypeConverter fun toShopTab(v: String?): ShopTab? = v?.let(ShopTab::valueOf)

    @TypeConverter fun fromPriority(v: Priority?): String? = v?.name
    @TypeConverter fun toPriority(v: String?): Priority? = v?.let(Priority::valueOf)

    @TypeConverter fun fromRole(v: ProfileRole?): String? = v?.name
    @TypeConverter fun toRole(v: String?): ProfileRole? = v?.let(ProfileRole::valueOf)

    @TypeConverter fun fromAlertType(v: PanchangAlertType?): String? = v?.name
    @TypeConverter fun toAlertType(v: String?): PanchangAlertType? = v?.let(PanchangAlertType::valueOf)
}
