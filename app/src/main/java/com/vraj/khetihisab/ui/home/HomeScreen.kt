package com.vraj.khetihisab.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.model.LoanWithAsset
import com.vraj.khetihisab.data.model.Totals
import com.vraj.khetihisab.panchang.SpecialDay
import com.vraj.khetihisab.ui.components.VkEyeToggle
import com.vraj.khetihisab.ui.components.VK_MASK
import com.vraj.khetihisab.ui.components.VkHeroTile
import com.vraj.khetihisab.ui.components.VkMaskedRupees
import com.vraj.khetihisab.ui.components.VkSubTile
import com.vraj.khetihisab.ui.components.VkTile
import com.vraj.khetihisab.ui.components.VkTileOrnament
import com.vraj.khetihisab.ui.components.VkTileRow
import com.vraj.khetihisab.ui.components.VkBadge
import com.vraj.khetihisab.ui.components.VkBottomDock
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkHeaderChip
import com.vraj.khetihisab.ui.components.VkHeaderIconButton
import com.vraj.khetihisab.ui.components.VkListRow
import com.vraj.khetihisab.ui.components.VkPhotoSlot
import com.vraj.khetihisab.ui.components.VkRowDivider
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkStatus
import com.vraj.khetihisab.ui.components.VkStatusBadge
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTone
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.entry.BillViewer
import com.vraj.khetihisab.ui.farms.periodLabel
import com.vraj.khetihisab.ui.farms.SeasonRange
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Stateful Home (doc 05). Navigation is a lambda; the NavController stays in [homeGraph]. */
@Composable
fun HomeRoute(onOpen: (String) -> Unit) {
    val vm = screenViewModel { deps, _ -> HomeViewModel(deps) }
    val state by vm.state.collectAsStateWithLifecycle()
    val deps = rememberScreensDeps()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    HomeScreen(
        state = state,
        onOpen = onOpen,
        onSettle = vm::settle,
        onLoanWhatsApp = { loan ->
            scope.launch { ScreensShare.loanReminder(ctx, deps, loan, state.ownerName, state.ownerAddress) }
        },
    )
}

private enum class HomeSheet { NONE, UDHAAR, BUSINESS, ANIMAL }

/** Stateless Home, used by the UI tests with a fixed [HomeUiState]. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpen: (String) -> Unit,
    onSettle: (Long) -> Unit,
    onLoanWhatsApp: (LoanWithAsset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheet by rememberSaveable { mutableStateOf(HomeSheet.NONE) }
    var billPath by remember { mutableStateOf<String?>(null) }
    // Eye toggle: null = follow the Settings switch; a tap overrides it until Home is left.
    var reveal by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val shown = reveal ?: state.showNumbers
    var actionsFor by remember { mutableStateOf<LedgerRowUi?>(null) }
    val pad = Modifier.padding(horizontal = 16.dp)

    // Dock sits BELOW the list (not over it), so no tile or row is ever hidden under the buttons.
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().testTag("home_list"),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") { HomeHeader(state, onOpen) }
            item(key = "panchang") { UpcomingCard(state.today, state.upcoming, { onOpen(HomeRoutes.PANCHANG) }, pad) }

            if (state.hasAlerts) {
                item(key = "attention") { VkSectionTitle(stringResource(R.string.scr_home_attention), pad) }
                if (state.showUdhaar) {
                    item(key = "udhaar") { UdhaarCard(state, shown, { sheet = HomeSheet.UDHAAR }, pad) }
                }
                state.overdue.forEach { card ->
                    item(key = "loan_${card.loan.loan.id}") { OverdueLoanCard(card, onLoanWhatsApp, { onOpen(HomeRoutes.ASSETS) }, pad) }
                }
                if (state.overdueMore > 0) {
                    item(key = "loan_more") {
                        VkTextButton(stringResource(R.string.scr_more_n, state.overdueMore), { onOpen(HomeRoutes.ASSETS) }, pad)
                    }
                }
                state.urgent.forEach { u ->
                    item(key = "rem_${u.reminder.id}") { UrgentReminderCard(u, state.today, { onOpen(HomeRoutes.REMINDERS) }, pad) }
                }
            }

            item(key = "hisab") {
                HisabHero(
                    state = state,
                    shown = shown,
                    onToggle = { reveal = !shown },
                    onBusiness = { sheet = HomeSheet.BUSINESS },
                    onFarms = { onOpen(HomeRoutes.FARMS) },
                    onAnimal = { sheet = HomeSheet.ANIMAL },
                    onPersonal = { onOpen(HomeRoutes.PERSONAL) },
                    modifier = pad,
                )
            }
            item(key = "grid") { SectionTiles(state, shown, onOpen, pad) }

            item(key = "recent_title") {
                VkSectionTitle(
                    stringResource(R.string.scr_recent),
                    pad,
                    action = stringResource(R.string.scr_see_all),
                    onAction = { onOpen(HomeRoutes.FARMS) },
                )
            }
            item(key = "recent") { RecentCard(state.recent, shown, { billPath = it }, { actionsFor = it }, pad) }
        }

        VkBottomDock(
            onExpense = { onOpen(HomeRoutes.entry(HomeRoutes.KIND_EXPENSE)) },
            onIncome = { onOpen(HomeRoutes.entry(HomeRoutes.KIND_INCOME)) },
        )
    }

    when (sheet) {
        HomeSheet.UDHAAR -> PendingUdhaarSheet(state.pending, onSettle, { billPath = it }, { sheet = HomeSheet.NONE })
        HomeSheet.BUSINESS -> BusinessBreakdownSheet(state.business, { sheet = HomeSheet.NONE })
        HomeSheet.ANIMAL -> AnimalSheet(state.today, state.animal, state.animalCategories, { sheet = HomeSheet.NONE }, onMilk = { onOpen(HomeRoutes.MILK) })
        HomeSheet.NONE -> Unit
    }
    billPath?.let { BillViewer(it) { billPath = null } }
    actionsFor?.let { r -> EntryActionsSheet(r, onEdit = { onOpen(HomeRoutes.editEntry(r.id)) }, onDismiss = { actionsFor = null }) }
}

// ---------------------------------------------------------------- header + panchang

@Composable
private fun HomeHeader(state: HomeUiState, onOpen: (String) -> Unit) {
    val x = LocalVkExtra.current
    val greeting = ScreensFormat.greetingName(state.ownerName)
    VkTopHeader(
        title = stringResource(R.string.app_name),
        subtitle = if (greeting.isEmpty()) null else stringResource(R.string.scr_home_greeting, greeting),
        hero = true,
        actions = {
            PersonAvatar(
                photoPath = state.ownerPhoto,
                // No letter: the owner's photo, or the 3D profile icon (owner request 2026-11-05).
                initial = "",
                size = 44.dp,
                onClick = { onOpen(HomeRoutes.PROFILES) },
                contentDescription = stringResource(R.string.scr_cd_profile),
            )
            VkHeaderIconButton(VkIcon.SETTINGS, stringResource(R.string.hm_cd_settings)) { onOpen(HomeRoutes.SETTINGS) }
        },
    ) {
        val day = state.panchang
        // Tap today's date → Panchang calendar (owner request 2026-11-05).
        Text(
            if (day != null) stringResource(R.string.hm_date_weekday, ScreensFormat.longDate(state.today), day.weekdayGu) else ScreensFormat.longDate(state.today),
            color = x.onGradient,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { onOpen(HomeRoutes.PANCHANG) }
                .testTag("home_date")
                .wrapContentHeight(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (day != null) {
                Text(
                    stringResource(R.string.hm_tithi_vs, day.tithiGu, day.vsLabel),
                    color = x.onGradient,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            BackupPill(state.backup) { onOpen(HomeRoutes.BACKUP_SETTINGS) }
        }
    }
}

@Composable
private fun BackupPill(status: BackupUiStatus, onOpenSettings: () -> Unit) {
    val label = stringResource(
        when (status) {
            BackupUiStatus.OK -> R.string.scr_backup_ok
            BackupUiStatus.PENDING -> R.string.scr_backup_pending
            BackupUiStatus.FAILED -> R.string.scr_backup_failed
        },
    )
    val cs = MaterialTheme.colorScheme
    // Not taken today (or failed) → red pill with a flag, so it cannot be missed (owner 2026-11-05).
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onOpenSettings)
            .testTag("home_backup"),
        contentAlignment = Alignment.Center,
    ) {
        if (status == BackupUiStatus.OK) {
            VkHeaderChip(label)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.vraj.khetihisab.ui.components.VkAlertFlag(size = 22.dp)
                Spacer(Modifier.width(6.dp))
                VkBadge(label, cs.error, cs.onError)
            }
        }
    }
}

@Composable
internal fun relativeDayLabel(date: LocalDate, today: LocalDate): String {
    val dm = ScreensFormat.dm(date)
    return when (java.time.temporal.ChronoUnit.DAYS.between(today, date)) {
        0L -> stringResource(R.string.hm_rel_date, stringResource(R.string.scr_day_today), dm)
        1L -> stringResource(R.string.hm_rel_date, stringResource(R.string.scr_day_tomorrow), dm)
        2L -> stringResource(R.string.hm_rel_date, stringResource(R.string.scr_day_after), dm)
        else -> dm
    }
}

@Composable
private fun UpcomingCard(today: LocalDate, upcoming: List<SpecialDay>, onClick: () -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    VkCard(modifier.fillMaxWidth(), tone = VkTone.Panchang, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VkIconImage(VkIcon.PANCHANG, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.scr_home_upcoming), style = MaterialTheme.typography.titleMedium, color = cs.onTertiaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.hm_open_calendar), style = MaterialTheme.typography.bodySmall, color = cs.tertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (upcoming.isEmpty()) {
            Text(stringResource(R.string.hm_no_upcoming), style = MaterialTheme.typography.bodyLarge, color = cs.onTertiaryContainer)
        } else {
            upcoming.forEach { sp ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sp.titleGu,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = cs.onTertiaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    VkBadge(relativeDayLabel(sp.date, today), cs.surfaceContainerLowest, cs.tertiary)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- alerts

@Composable
private fun UdhaarCard(state: HomeUiState, shown: Boolean, onSettle: () -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    VkCard(modifier.fillMaxWidth().testTag("home_udhaar"), tone = VkTone.Error, onClick = onSettle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VkIconImage(VkIcon.BILL, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.scr_udhaar_title),
                style = MaterialTheme.typography.titleLarge,
                color = cs.onErrorContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            VkBadge(stringResource(R.string.scr_udhaar_count, state.udhaar.count), cs.error, cs.onError)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(stringResource(R.string.scr_udhaar_payable), if (shown) ScreensFormat.signedRupees(state.udhaar.payable) else VK_MASK, Modifier.weight(1f), cs.onErrorContainer, big = true)
            StatTile(stringResource(R.string.scr_udhaar_receivable), if (shown) ScreensFormat.signedRupees(state.udhaar.receivable) else VK_MASK, Modifier.weight(1f), cs.onErrorContainer, big = true)
        }
        VkTonalButton(stringResource(R.string.scr_udhaar_settle), onSettle, Modifier.fillMaxWidth())
    }
}

@Composable
private fun OverdueLoanCard(card: LoanCardUi, onWhatsApp: (LoanWithAsset) -> Unit, onOpen: () -> Unit, modifier: Modifier) {
    val loan = card.loan
    VkCard(modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VkPhotoSlot(
                path = loan.loan.borrowerPhotoPath,
                placeholderText = loan.loan.borrowerName,
                onCapture = onOpen,
                size = 56.dp,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.scr_loan_not_returned, loan.asset.name), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.hm_loan_with, loan.loan.borrowerName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                VkStatusBadge(VkStatus.Overdue(card.daysLate))
            }
            Spacer(Modifier.width(8.dp))
            val cd = stringResource(R.string.hm_loan_wa_cd)
            FilledTonalIconButton(
                onClick = { onWhatsApp(loan) },
                modifier = Modifier.size(60.dp).semantics { contentDescription = cd },
            ) { VkIconImage(VkIcon.MESSAGE, size = 32.dp) }
        }
    }
}

@Composable
private fun UrgentReminderCard(u: UrgentUi, today: LocalDate, onClick: () -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    VkCard(modifier.fillMaxWidth(), onClick = onClick, contentPadding = 0.dp) {
        VkListRow(
            title = u.reminder.title,
            meta = relativeDayLabel(u.dueDate, today).let { if (it == ScreensFormat.dm(u.dueDate)) ScreensFormat.dmy(u.dueDate) else it },
            leadingIcon = VkIcon.REMINDER,
            trailing = { VkBadge(stringResource(R.string.hm_reminder_high), cs.primary, cs.onPrimary) },
        )
    }
}

// ---------------------------------------------------------------- account cards

@Composable
private fun CardTitleRow(icon: VkIcon, title: String, chip: String? = null) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        VkIconImage(icon, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (chip != null) {
            Spacer(Modifier.width(8.dp))
            VkBadge(chip, cs.primaryContainer, cs.onPrimaryContainer)
        }
    }
}

/** આવક / ખર્ચ / ચોખ્ખો નફો in three columns; the net is primary, or error when negative. */
@Composable
internal fun ThreeStats(t: Totals) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(stringResource(R.string.scr_income), ScreensFormat.signedRupees(t.income), Modifier.weight(1f))
        StatTile(stringResource(R.string.scr_expense), ScreensFormat.signedRupees(t.expense), Modifier.weight(1f))
        StatTile(
            stringResource(R.string.scr_net_profit),
            ScreensFormat.signedRupees(t.net),
            Modifier.weight(1f),
            valueColor = if (t.net < 0) cs.error else cs.primary,
        )
    }
}

/**
 * "હિસાબ" hero tile (owner layout 2026-11-04): કુલ નફો-નુકસાન, ખેતર, પશુપાલન, અંગત as sub-tiles.
 * Amounts start hidden as XXXXX.XX; the eye shows them (Home only, not the rest of the app).
 */
@Composable
private fun HisabHero(
    state: HomeUiState,
    shown: Boolean,
    onToggle: () -> Unit,
    onBusiness: () -> Unit,
    onFarms: () -> Unit,
    onAnimal: () -> Unit,
    onPersonal: () -> Unit,
    modifier: Modifier,
) {
    val cs = MaterialTheme.colorScheme
    @Composable
    fun money(paise: Long, tag: String) = VkMaskedRupees(
        paise,
        revealed = shown,
        color = if (paise < 0) cs.error else cs.primary,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp),
        testTag = tag,
    )
    VkHeroTile(
        icon = VkIcon.REPORT,
        title = stringResource(R.string.v2_hisab),
        subtitle = stringResource(if (shown) R.string.v2_hisab_sub_shown else R.string.v2_hisab_sub_hidden),
        modifier = modifier.fillMaxWidth().testTag("home_hisab"),
        trailing = {
            VkEyeToggle(
                revealed = shown,
                onToggle = onToggle,
                showLabel = stringResource(R.string.v2_eye_show),
                hideLabel = stringResource(R.string.v2_eye_hide),
            )
        },
    ) {
        VkTileRow {
            VkSubTile(VkIcon.REPORT, stringResource(R.string.v2_kul_nafo), onBusiness, Modifier.weight(1f).fillMaxHeight(), testTag = "home_business") {
                money(state.business.totals.net, "home_business_amount")
                Text(stringResource(R.string.hm_year_chip, SeasonRange.cropYearLabel(state.business.cropYear)), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            VkSubTile(VkIcon.FARM, stringResource(R.string.v2_khetar_hisab), onFarms, Modifier.weight(1f).fillMaxHeight(), testTag = "home_farms") {
                money(state.farm.net, "home_farms_amount")
                Text(periodLabel(state.farmPeriod), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        VkTileRow {
            VkSubTile(VkIcon.ANIMAL, stringResource(R.string.v2_pashupalan_hisab), onAnimal, Modifier.weight(1f).fillMaxHeight(), testTag = "home_animal") {
                money(state.animal.net, "home_animal_amount")
                Text(stringResource(R.string.v2_this_month), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
            }
            VkSubTile(VkIcon.PERSONAL, stringResource(R.string.v2_angat_hisab), onPersonal, Modifier.weight(1f).fillMaxHeight(), testTag = "home_personal_hisab") {
                money(state.personalNetMonth, "home_personal_amount")
                Text(stringResource(R.string.v2_this_month), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/** [ભાગિયા ઉપાડ] [અંગત / ઘર] / [સામાન્ય ખર્ચ] [ખરીદી, મિલકત] — the same tile as Settings. */
@Composable
private fun SectionTiles(state: HomeUiState, shown: Boolean, onOpen: (String) -> Unit, modifier: Modifier) {
    @Composable
    fun money(label: String, paise: Long): @Composable () -> Unit = {
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            VkMaskedRupees(paise, revealed = shown, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp))
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        VkTileRow {
            VkTile(
                VkIcon.FAMILY, stringResource(R.string.scr_sec_family), { onOpen(HomeRoutes.FAMILY) },
                Modifier.weight(1f).fillMaxHeight(),
                ornament = VkTileOrnament.FIELD, index = 1, testTag = "home_tile_family",
                value = money(stringResource(R.string.v2_this_month), state.familyMonth),
            )
            VkTile(
                VkIcon.PERSONAL, stringResource(R.string.scr_sec_personal), { onOpen(HomeRoutes.PERSONAL) },
                Modifier.weight(1f).fillMaxHeight(),
                ornament = VkTileOrnament.RANGOLI, index = 2, testTag = "home_tile_personal",
                value = money(stringResource(R.string.v3_expense_this_month), state.personalExpenseMonth),
            )
        }
        VkTileRow {
            VkTile(
                VkIcon.UTILITY, stringResource(R.string.scr_sec_general), { onOpen(HomeRoutes.GENERAL) },
                Modifier.weight(1f).fillMaxHeight(),
                ornament = VkTileOrnament.SUN, index = 3, testTag = "home_tile_general",
                value = money(stringResource(R.string.v2_this_month), state.generalMonth),
            )
            VkTile(
                VkIcon.SHOPPING, stringResource(R.string.scr_sec_shop), { onOpen(HomeRoutes.SHOPPING) },
                Modifier.weight(1f).fillMaxHeight(),
                subtitle = stringResource(R.string.scr_shop_meta, state.shoppingPending, state.assetsLent),
                ornament = VkTileOrnament.LEAF, index = 4, testTag = "home_tile_shop",
            )
        }
        VkTileRow {
            VkTile(
                VkIcon.ASSET, stringResource(R.string.v3_assets_tile), { onOpen(HomeRoutes.ASSETS) },
                Modifier.weight(1f).fillMaxHeight(),
                subtitle = stringResource(R.string.v3_assets_lent, state.assetsLent),
                ornament = VkTileOrnament.WAVE, index = 5, testTag = "home_tile_assets",
            )
            VkTile(
                VkIcon.ANIMAL, stringResource(R.string.v3_milk_title), { onOpen(HomeRoutes.MILK) },
                Modifier.weight(1f).fillMaxHeight(),
                subtitle = stringResource(R.string.v3_milk_sub),
                ornament = VkTileOrnament.MOON, index = 6, testTag = "home_tile_milk",
            )
        }
    }
}

@Composable
private fun RecentCard(rows: List<LedgerRowUi>, shown: Boolean, onViewBill: (String) -> Unit, onRow: (LedgerRowUi) -> Unit, modifier: Modifier) {
    VkCard(modifier.fillMaxWidth(), contentPadding = 0.dp) {
        if (rows.isEmpty()) {
            Text(
                stringResource(R.string.scr_no_entries),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        rows.forEachIndexed { i, r ->
            if (i > 0) VkRowDivider()
            TxnListRow(
                title = r.title,
                meta = r.meta,
                icon = r.icon,
                amountPaise = r.amountPaise,
                kind = r.kind,
                billPath = r.billPath,
                onViewBill = onViewBill,
                pendingBadge = r.pending,
                onClick = { onRow(r) },
                masked = !shown,
            )
        }
    }
}

/** Net text in primary / error, never shrinking (doc 01 §8.3). */
@Composable
internal fun NetText(paise: Long, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Text(
        ScreensFormat.signedRupees(paise),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = if (paise < 0) cs.error else cs.primary,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.wrapContentWidth(),
    )
}
