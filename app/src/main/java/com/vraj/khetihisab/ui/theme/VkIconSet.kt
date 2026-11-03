// GENERATED from design/icons/*.svg — do not edit by hand.
// Regenerate with the svg2vk converter (see ui/theme/VkIcons.kt header).
package com.vraj.khetihisab.ui.theme

internal enum class VkInk { T, A, B, H }

internal class VkIconPart(
    val d: String,
    val fill: VkInk?,
    val stroke: VkInk? = null,
    val strokeWidth: Float = 0f,
    val alpha: Float = 1f,
    val roundCap: Boolean = false,
    val roundJoin: Boolean = false,
    val rotate: Float = 0f,
)

/** The 24 two-tone `ic_vk_*` icons (96×96 viewport). Draw with [VkIconImage]; never tint. */
enum class VkIcon(internal val resName: String, internal val parts: List<VkIconPart>) {
    ASSET(
        "ic_vk_asset",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M22 58V24h24l7 18h17a7 7 0 0 1 7 7v9z", VkInk.A),
            VkIconPart("M33,30H39A3,3 0 0 1 42,33V39A3,3 0 0 1 39,42H33A3,3 0 0 1 30,39V33A3,3 0 0 1 33,30Z", VkInk.H, alpha = 0.85f),
            VkIconPart("M17,64A17,17 0 1 0 51,64A17,17 0 1 0 17,64Z", VkInk.A),
            VkIconPart("M27,64A7,7 0 1 0 41,64A7,7 0 1 0 27,64Z", VkInk.H),
            VkIconPart("M59,70A11,11 0 1 0 81,70A11,11 0 1 0 59,70Z", VkInk.B),
            VkIconPart("M66,70A4,4 0 1 0 74,70A4,4 0 1 0 66,70Z", VkInk.H),
        ),
    ),
    BACKUP(
        "ic_vk_backup",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M30 74a16 16 0 0 1-3-31.7A22 22 0 0 1 70 38a18 18 0 0 1 1 36z", VkInk.A),
            VkIconPart("M36 55l9 9 17-17", null, stroke = VkInk.B, strokeWidth = 8f, roundCap = true, roundJoin = true),
        ),
    ),
    BILL(
        "ic_vk_bill",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M22 12h52v70l-8.7-6-8.6 6-8.7-6-8.7 6-8.6-6-8.7 6z", VkInk.A),
            VkIconPart("M35.5,26H60.5A3.5,3.5 0 0 1 64,29.5V29.5A3.5,3.5 0 0 1 60.5,33H35.5A3.5,3.5 0 0 1 32,29.5V29.5A3.5,3.5 0 0 1 35.5,26Z", VkInk.B),
            VkIconPart("M35.5,40H50.5A3.5,3.5 0 0 1 54,43.5V43.5A3.5,3.5 0 0 1 50.5,47H35.5A3.5,3.5 0 0 1 32,43.5V43.5A3.5,3.5 0 0 1 35.5,40Z", VkInk.B),
            VkIconPart("M35.5,54H56.5A3.5,3.5 0 0 1 60,57.5V57.5A3.5,3.5 0 0 1 56.5,61H35.5A3.5,3.5 0 0 1 32,57.5V57.5A3.5,3.5 0 0 1 35.5,54Z", VkInk.B, alpha = 0.8f),
        ),
    ),
    CALCULATOR(
        "ic_vk_calculator",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M34,12H62A14,14 0 0 1 76,26V70A14,14 0 0 1 62,84H34A14,14 0 0 1 20,70V26A14,14 0 0 1 34,12Z", VkInk.A),
            VkIconPart("M34,21H62A5,5 0 0 1 67,26V31A5,5 0 0 1 62,36H34A5,5 0 0 1 29,31V26A5,5 0 0 1 34,21Z", VkInk.H),
            VkIconPart("M34,44H40A5,5 0 0 1 45,49V53A5,5 0 0 1 40,58H34A5,5 0 0 1 29,53V49A5,5 0 0 1 34,44Z", VkInk.B),
            VkIconPart("M56,44H62A5,5 0 0 1 67,49V53A5,5 0 0 1 62,58H56A5,5 0 0 1 51,53V49A5,5 0 0 1 56,44Z", VkInk.B),
            VkIconPart("M34,63H40A5,5 0 0 1 45,68V71A5,5 0 0 1 40,76H34A5,5 0 0 1 29,71V68A5,5 0 0 1 34,63Z", VkInk.B),
            VkIconPart("M56,63H62A5,5 0 0 1 67,68V71A5,5 0 0 1 62,76H56A5,5 0 0 1 51,71V68A5,5 0 0 1 56,63Z", VkInk.H, alpha = 0.8f),
        ),
    ),
    CAMERA(
        "ic_vk_camera",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M34 30l5-9h18l5 9z", VkInk.A),
            VkIconPart("M26,29H70A14,14 0 0 1 84,43V65A14,14 0 0 1 70,79H26A14,14 0 0 1 12,65V43A14,14 0 0 1 26,29Z", VkInk.A),
            VkIconPart("M32,54A16,16 0 1 0 64,54A16,16 0 1 0 32,54Z", VkInk.B),
            VkIconPart("M41,54A7,7 0 1 0 55,54A7,7 0 1 0 41,54Z", VkInk.H),
            VkIconPart("M68.5,40A3.5,3.5 0 1 0 75.5,40A3.5,3.5 0 1 0 68.5,40Z", VkInk.H),
        ),
    ),
    DIYA(
        "ic_vk_diya",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M48 14c-8 10-10 17-10 22a10 10 0 0 0 20 0c0-5-2-12-10-22z", VkInk.B),
            VkIconPart("M12 54h72c0 15-16 26-36 26S12 69 12 54z", VkInk.A),
            VkIconPart("M43,78H53A3,3 0 0 1 56,81V81A3,3 0 0 1 53,84H43A3,3 0 0 1 40,81V81A3,3 0 0 1 43,78Z", VkInk.A),
        ),
    ),
    EXPENSE(
        "ic_vk_expense",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M16,42A26,26 0 1 0 68,42A26,26 0 1 0 16,42Z", VkInk.A),
            VkIconPart("M27,42A15,15 0 1 0 57,42A15,15 0 1 0 27,42Z", null, stroke = VkInk.H, strokeWidth = 4f, alpha = 0.7f),
            VkIconPart("M68 84 84 65H74V46H62v19H52z", VkInk.B),
        ),
    ),
    FAMILY(
        "ic_vk_family",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M26,32A11,11 0 1 0 48,32A11,11 0 1 0 26,32Z", VkInk.A),
            VkIconPart("M16 76c0-14 9-24 21-24s21 10 21 24z", VkInk.A),
            VkIconPart("M51,38A10,10 0 1 0 71,38A10,10 0 1 0 51,38Z", VkInk.B, alpha = 0.95f),
            VkIconPart("M42 78c0-13 8-21 19-21s19 8 19 21z", VkInk.B, alpha = 0.95f),
        ),
    ),
    FARM(
        "ic_vk_farm",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M10 70c14-8 30-12 38-12s24 4 38 12v8a8 8 0 0 1-8 8H18a8 8 0 0 1-8-8z", VkInk.A),
            VkIconPart("M20 76c9-4 19-6 28-6s19 2 28 6", null, stroke = VkInk.H, strokeWidth = 3f, alpha = 0.55f, roundCap = true),
            VkIconPart("M48 60V34", null, stroke = VkInk.A, strokeWidth = 5f, roundCap = true),
            VkIconPart("M48 48c-14 0-22-8-22-20 12 0 22 6 22 20z", VkInk.B),
            VkIconPart("M48 40c0-12 8-20 22-20 0 12-8 20-22 20z", VkInk.A),
        ),
    ),
    HELP(
        "ic_vk_help",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M16,48A32,32 0 1 0 80,48A32,32 0 1 0 16,48Z", VkInk.A),
            VkIconPart("M38 39a10 10 0 1 1 15 8.6c-3.5 2-5 4.4-5 8.4", null, stroke = VkInk.B, strokeWidth = 8f, roundCap = true),
            VkIconPart("M43,68A5,5 0 1 0 53,68A5,5 0 1 0 43,68Z", VkInk.B),
        ),
    ),
    INCOME(
        "ic_vk_income",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M16,54A26,26 0 1 0 68,54A26,26 0 1 0 16,54Z", VkInk.A),
            VkIconPart("M27,54A15,15 0 1 0 57,54A15,15 0 1 0 27,54Z", null, stroke = VkInk.H, strokeWidth = 4f, alpha = 0.7f),
            VkIconPart("M68 12 84 31H74v19H62V31H52z", VkInk.B),
        ),
    ),
    LEND(
        "ic_vk_lend",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M14 58l14-14c4-4 9-5 14-3l10 4H64a6 6 0 0 1 0 12H48", null, stroke = VkInk.A, strokeWidth = 8f, roundCap = true, roundJoin = true),
            VkIconPart("M14 74h34l22-12a6 6 0 0 1 8 8L56 84H14z", VkInk.A),
            VkIconPart("M54,26A12,12 0 1 0 78,26A12,12 0 1 0 54,26Z", VkInk.B),
            VkIconPart("M61 26l4 4 7-7", null, stroke = VkInk.H, strokeWidth = 4f, roundCap = true, roundJoin = true),
        ),
    ),
    LOCK(
        "ic_vk_lock",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M33 44V34a15 15 0 0 1 30 0v10", null, stroke = VkInk.B, strokeWidth = 8f, roundCap = true),
            VkIconPart("M33,42H63A13,13 0 0 1 76,55V71A13,13 0 0 1 63,84H33A13,13 0 0 1 20,71V55A13,13 0 0 1 33,42Z", VkInk.A),
            VkIconPart("M42,60A6,6 0 1 0 54,60A6,6 0 1 0 42,60Z", VkInk.H),
            VkIconPart("M48,62H48A3,3 0 0 1 51,65V70A3,3 0 0 1 48,73H48A3,3 0 0 1 45,70V65A3,3 0 0 1 48,62Z", VkInk.H),
        ),
    ),
    MESSAGE(
        "ic_vk_message",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M44 14c-17 0-30 11-30 26 0 8 4 14 9 19l-3 13 14-7c3 1 6 1 10 1 17 0 30-12 30-26S61 14 44 14z", VkInk.A),
            VkIconPart("M64 46c10 0 18 7 18 16 0 4-2 8-5 11l2 9-9-5c-2 .6-4 1-6 1-10 0-18-7-18-16s8-16 18-16z", VkInk.B, alpha = 0.95f),
        ),
    ),
    MIC(
        "ic_vk_mic",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M48,12H48A13,13 0 0 1 61,25V43A13,13 0 0 1 48,56H48A13,13 0 0 1 35,43V25A13,13 0 0 1 48,12Z", VkInk.A),
            VkIconPart("M24 44a24 24 0 0 0 48 0M48 68v12M36 82h24", null, stroke = VkInk.B, strokeWidth = 7f, roundCap = true),
        ),
    ),
    MOON(
        "ic_vk_moon",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M58 14a34 34 0 1 0 24 58A28 28 0 0 1 58 14z", VkInk.A),
            VkIconPart("M65,28A5,5 0 1 0 75,28A5,5 0 1 0 65,28Z", VkInk.B),
            VkIconPart("M76.5,44A3.5,3.5 0 1 0 83.5,44A3.5,3.5 0 1 0 76.5,44Z", VkInk.B),
        ),
    ),
    PANCHANG(
        "ic_vk_panchang",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M30,22H66A14,14 0 0 1 80,36V68A14,14 0 0 1 66,82H30A14,14 0 0 1 16,68V36A14,14 0 0 1 30,22Z", VkInk.A),
            VkIconPart("M33.5,14H33.5A3.5,3.5 0 0 1 37,17.5V26.5A3.5,3.5 0 0 1 33.5,30H33.5A3.5,3.5 0 0 1 30,26.5V17.5A3.5,3.5 0 0 1 33.5,14Z", VkInk.A),
            VkIconPart("M62.5,14H62.5A3.5,3.5 0 0 1 66,17.5V26.5A3.5,3.5 0 0 1 62.5,30H62.5A3.5,3.5 0 0 1 59,26.5V17.5A3.5,3.5 0 0 1 62.5,14Z", VkInk.A),
            VkIconPart("M54 38a15 15 0 1 0 8 26 12 12 0 1 1-8-26z", VkInk.B),
        ),
    ),
    PERSONAL(
        "ic_vk_personal",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M48 16 14 44h9v30a6 6 0 0 0 6 6h38a6 6 0 0 0 6-6V44h9z", VkInk.A),
            VkIconPart("M46,52H50A8,8 0 0 1 58,60V72A8,8 0 0 1 50,80H46A8,8 0 0 1 38,72V60A8,8 0 0 1 46,52Z", VkInk.B),
        ),
    ),
    PROFILE(
        "ic_vk_profile",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M14,48A34,34 0 1 0 82,48A34,34 0 1 0 14,48Z", VkInk.A),
            VkIconPart("M36,40A12,12 0 1 0 60,40A12,12 0 1 0 36,40Z", VkInk.B),
            VkIconPart("M26 74c3-12 12-18 22-18s19 6 22 18a34 34 0 0 1-44 0z", VkInk.B),
        ),
    ),
    REMINDER(
        "ic_vk_reminder",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M48 14a22 22 0 0 0-22 22v15l-7 11h58l-7-11V36a22 22 0 0 0-22-22z", VkInk.A),
            VkIconPart("M39,72A9,9 0 1 0 57,72A9,9 0 1 0 39,72Z", VkInk.B),
        ),
    ),
    REPORT(
        "ic_vk_report",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M27 12h29l18 18v50a6 6 0 0 1-6 6H27a6 6 0 0 1-6-6V18a6 6 0 0 1 6-6z", VkInk.A),
            VkIconPart("M56 12v18h18z", VkInk.H, alpha = 0.55f),
            VkIconPart("M34,46H62A3,3 0 0 1 65,49V49A3,3 0 0 1 62,52H34A3,3 0 0 1 31,49V49A3,3 0 0 1 34,46Z", VkInk.B),
            VkIconPart("M34,58H54A3,3 0 0 1 57,61V61A3,3 0 0 1 54,64H34A3,3 0 0 1 31,61V61A3,3 0 0 1 34,58Z", VkInk.B),
            VkIconPart("M34,70H46A3,3 0 0 1 49,73V73A3,3 0 0 1 46,76H34A3,3 0 0 1 31,73V73A3,3 0 0 1 34,70Z", VkInk.B),
        ),
    ),
    SETTINGS(
        "ic_vk_settings",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M46,12H50A4,4 0 0 1 54,16V28A4,4 0 0 1 50,32H46A4,4 0 0 1 42,28V16A4,4 0 0 1 46,12Z", VkInk.A),
            VkIconPart("M46,12H50A4,4 0 0 1 54,16V28A4,4 0 0 1 50,32H46A4,4 0 0 1 42,28V16A4,4 0 0 1 46,12Z", VkInk.A, rotate = 45f),
            VkIconPart("M46,12H50A4,4 0 0 1 54,16V28A4,4 0 0 1 50,32H46A4,4 0 0 1 42,28V16A4,4 0 0 1 46,12Z", VkInk.A, rotate = 90f),
            VkIconPart("M46,12H50A4,4 0 0 1 54,16V28A4,4 0 0 1 50,32H46A4,4 0 0 1 42,28V16A4,4 0 0 1 46,12Z", VkInk.A, rotate = 135f),
            VkIconPart("M46,12H50A4,4 0 0 1 54,16V28A4,4 0 0 1 50,32H46A4,4 0 0 1 42,28V16A4,4 0 0 1 46,12Z", VkInk.A, rotate = 180f),
            VkIconPart("M46,12H50A4,4 0 0 1 54,16V28A4,4 0 0 1 50,32H46A4,4 0 0 1 42,28V16A4,4 0 0 1 46,12Z", VkInk.A, rotate = 225f),
            VkIconPart("M46,12H50A4,4 0 0 1 54,16V28A4,4 0 0 1 50,32H46A4,4 0 0 1 42,28V16A4,4 0 0 1 46,12Z", VkInk.A, rotate = 270f),
            VkIconPart("M46,12H50A4,4 0 0 1 54,16V28A4,4 0 0 1 50,32H46A4,4 0 0 1 42,28V16A4,4 0 0 1 46,12Z", VkInk.A, rotate = 315f),
            VkIconPart("M24,48A24,24 0 1 0 72,48A24,24 0 1 0 24,48Z", VkInk.A),
            VkIconPart("M37,48A11,11 0 1 0 59,48A11,11 0 1 0 37,48Z", VkInk.B),
        ),
    ),
    SHOPPING(
        "ic_vk_shopping",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M36 36v-6a12 12 0 0 1 24 0v6", null, stroke = VkInk.B, strokeWidth = 7f, roundCap = true),
            VkIconPart("M22 36h52l-4 38a7 7 0 0 1-7 6H33a7 7 0 0 1-7-6z", VkInk.A),
            VkIconPart("M40,56A8,8 0 1 0 56,56A8,8 0 1 0 40,56Z", VkInk.B),
        ),
    ),
    UTILITY(
        "ic_vk_utility",
        listOf(
            VkIconPart("M28,0H68A28,28 0 0 1 96,28V68A28,28 0 0 1 68,96H28A28,28 0 0 1 0,68V28A28,28 0 0 1 28,0Z", VkInk.T),
            VkIconPart("M46 12 24 52h16l-6 32 30-42H48l8-30z", VkInk.A),
            VkIconPart("M68 44s-13 14-13 23a13 13 0 0 0 26 0c0-9-13-23-13-23z", VkInk.B, alpha = 0.95f),
        ),
    ),
}
