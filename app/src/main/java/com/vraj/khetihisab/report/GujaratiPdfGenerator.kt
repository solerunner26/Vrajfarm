package com.vraj.khetihisab.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.AppDatabase
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.data.repo.LedgerMath
import com.vraj.khetihisab.panchang.GujaratiPanchangHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Seven A4 Gujarati PDF reports (doc 15 Part A) on [PdfDocument].
 *
 * Every multi-character Gujarati run goes through [StaticLayout] (HarfBuzz shaping), so conjuncts
 * (ક્ષ, જ્ઞ, શ્ર, દ્ધ, ત્ર) render correctly — `Canvas.drawText` is never used for text.
 *
 * Layout is two-pass: content is measured and paginated first (see [TablePagination]), then each page is
 * drawn with its header and the "પાનું N / M" footer, so the total page count is known.
 */
/** Owner details printed in the letterhead of every report (owner request 2026-11-05). */
data class Letterhead(val ownerName: String, val address: String, val phone: String?) {
    companion object {
        val DEFAULT = Letterhead("ગિરવાનસિંહ વાળા", "મુ. પો. થોરાળા, તા. મહુવા, જિ. ભાવનગર", null)
    }
}

class GujaratiPdfGenerator(
    context: Context,
    private val dataSource: ReportDataSource,
    private val now: () -> ZonedDateTime = { ZonedDateTime.now(LedgerMath.IST) },
    /** Output folder; default `filesDir/reports` (FileProvider path "reports"). */
    private val outputDir: File = File(context.applicationContext.filesDir, REPORTS_DIR),
    private val letterhead: suspend () -> Letterhead = { Letterhead.DEFAULT },
) {
    private val appContext: Context = context.applicationContext

    /** Convenience constructor reading Room (letterhead from the OWNER profile). */
    constructor(context: Context, db: AppDatabase) : this(
        context,
        RoomReportDataSource(db),
        letterhead = {
            val o = db.profileDao().byRoleNow(com.vraj.khetihisab.data.entity.ProfileRole.OWNER)
            Letterhead(
                ownerName = o?.name?.takeIf { it.isNotBlank() } ?: Letterhead.DEFAULT.ownerName,
                address = o?.address?.takeIf { it.isNotBlank() } ?: Letterhead.DEFAULT.address,
                phone = o?.phone?.takeIf { it.isNotBlank() },
            )
        },
    )

    data class Result(val file: File, val pageCount: Int)

    /** Generates the report off the main thread and returns the PDF file. */
    suspend fun generate(type: ReportType, range: DateRange): File = generateWithInfo(type, range).file

    suspend fun generateWithInfo(type: ReportType, range: DateRange): Result = withContext(Dispatchers.Default) {
        val at = now()
        val doc = dataSource.load(type, range, at.toLocalDate())
        val file = File(outputDir, fileName(type, at))
        val pages = render(doc, file, at, runCatching { letterhead() }.getOrDefault(Letterhead.DEFAULT))
        pruneOld(outputDir)
        Result(file, pages)
    }

    /** Renders an already-built [doc] into [out]. Returns the page count. Blocking; call off the main thread. */
    fun render(doc: ReportDoc, out: File, at: ZonedDateTime = now(), head: Letterhead = Letterhead.DEFAULT): Int {
        out.parentFile?.mkdirs()
        val r = Renderer(appContext, doc, at, head)
        val pdf = PdfDocument()
        try {
            val pageCount = r.drawAll(pdf)
            val tmp = File(out.parentFile, out.name + ".part")
            FileOutputStream(tmp).use { pdf.writeTo(it) }
            if (out.exists()) out.delete()
            if (!tmp.renameTo(out)) {
                tmp.copyTo(out, overwrite = true)
                tmp.delete()
            }
            return pageCount
        } finally {
            pdf.close()
            r.recycle()
        }
    }

    companion object {
        const val REPORTS_DIR = "reports"
        const val PAGE_W = 595
        const val PAGE_H = 842
        const val MARGIN = 36f
        private const val KEEP_FILES = 60
        private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm", Locale.ROOT)

        fun fileName(type: ReportType, at: ZonedDateTime): String = "${type.slug}_${at.format(STAMP)}.pdf"

        /** Keeps the newest [KEEP_FILES] PDFs so `filesDir/reports` stays bounded. */
        internal fun pruneOld(dir: File, keep: Int = KEEP_FILES) {
            val pdfs = dir.listFiles { f -> f.isFile && f.name.endsWith(".pdf") }?.sortedByDescending { it.lastModified() } ?: return
            pdfs.drop(keep).forEach { it.delete() }
        }
    }
}

/** One render pass over a [ReportDoc]. Not thread-safe; create per document. */
private class Renderer(private val ctx: Context, private val doc: ReportDoc, private val at: ZonedDateTime, private val head: Letterhead) {
    private val pageW = GujaratiPdfGenerator.PAGE_W.toFloat()
    private val pageH = GujaratiPdfGenerator.PAGE_H.toFloat()
    private val m = GujaratiPdfGenerator.MARGIN
    private val contentW = pageW - 2 * m
    private val contentWInt = contentW.toInt()

    private val regular: Typeface = runCatching { ResourcesCompat.getFont(ctx, R.font.noto_sans_gujarati) }.getOrNull() ?: Typeface.DEFAULT
    private val bold: Typeface = Typeface.create(regular, 700, false)

    private fun paint(size: Float, color: Int, tf: Typeface = regular) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = tf
        textSize = size
        this.color = color
    }

    private val pBody = paint(9.5f, INK)
    private val pBodyBold = paint(9.5f, INK, bold)
    private val pHead = paint(9f, BRAND_DARK, bold)
    private val pHeading = paint(12.5f, BRAND, bold)
    private val pPara = paint(10f, INK)
    private val pMuted = paint(9.5f, MUTED)
    private val pTitle = paint(18f, BRAND, bold)
    private val pLhName = paint(21f, BRAND, bold)
    private val pLhOwner = paint(11.5f, INK, bold)
    private val pLhLine = paint(10f, INK)
    private val pDate = paint(10.5f, INK)
    private val pRange = paint(10f, MUTED)
    private val pBandName = paint(12f, Color.WHITE, bold)
    private val pBandMeta = paint(8.5f, Color.WHITE)
    private val pFooter = paint(8.5f, MUTED)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.6f; color = GRID }
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val bitmaps = mutableMapOf<String, Bitmap?>()
    private val logo: Bitmap? = runCatching {
        ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher)?.toBitmap(LOGO_BIG_PX, LOGO_BIG_PX, Bitmap.Config.ARGB_8888)
    }.getOrNull()

    // Letterhead measurements (same on every page): logo square on the left, text centred on the right,
    // a rule, the date on the right, then the bold title.
    private val logoSide = 86f
    private val textX = m + logoSide + 18f
    private val textW = (pageW - m - textX).toInt()
    private val lhLines: List<StaticLayout> = listOfNotNull(
        layout(ReportLabels.APP_NAME, pLhName, textW, Align.CENTER),
        layout(head.ownerName, pLhOwner, textW, Align.CENTER),
        layout(head.address, pLhLine, textW, Align.CENTER),
        head.phone?.let { layout(ReportLabels.LH_PHONE.format(it), pLhLine, textW, Align.CENTER) },
    )
    private val bandH = maxOf(logoSide, lhLines.sumOf { it.height }.toFloat() + 6f)
    private val dateLayout = layout(ReportLabels.LH_DATE.format(dateText()), pDate, contentWInt, Align.END)
    private val titleLayout = layout(doc.title, pTitle, contentWInt, Align.CENTER)
    private val rangeLayout = doc.rangeLabel.takeIf { it.isNotBlank() }?.let { layout(it, pRange, contentWInt, Align.CENTER) }
    private val contentTop = m + bandH + 10f + 12f + dateLayout.height + 10f + titleLayout.height + (rangeLayout?.height ?: 0) + 12f
    private val contentBottom = pageH - m - 22f

    private val pages: MutableList<MutableList<(Canvas) -> Unit>> = mutableListOf()
    private var y = contentTop

    fun drawAll(pdf: PdfDocument): Int {
        newPage()
        for (b in doc.blocks) place(b)
        val total = pages.size
        pages.forEachIndexed { i, ops ->
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(GujaratiPdfGenerator.PAGE_W, GujaratiPdfGenerator.PAGE_H, i + 1).create())
            val c = page.canvas
            drawHeader(c)
            ops.forEach { it(c) }
            drawFooter(c, i + 1, total)
            pdf.finishPage(page)
        }
        return total
    }

    fun recycle() {
        bitmaps.values.forEach { it?.recycle() }
        logo?.recycle()
    }

    // ------------------------------------------------------------------ flow
    private fun newPage() {
        pages.add(mutableListOf())
        y = contentTop
    }

    private fun emit(op: (Canvas) -> Unit) {
        pages.last().add(op)
    }

    private fun ensure(h: Float) {
        if (y + h > contentBottom && y > contentTop) newPage()
    }

    private fun place(b: ReportBlock) {
        when (b) {
            is ReportBlock.Spacer -> y = min(y + b.heightPt, contentBottom)
            is ReportBlock.Heading -> {
                val l = layout(b.text, pHeading, contentWInt)
                ensure(l.height + 6f + MIN_KEEP_WITH_HEADING) // keep with the next rows
                y += 6f
                val top = y
                emit { c -> c.drawLayout(l, m, top) }
                y += l.height + 4f
            }
            is ReportBlock.Paragraph -> placeParagraph(b)
            is ReportBlock.Table -> placeTable(b)
        }
    }

    private fun placeParagraph(b: ReportBlock.Paragraph) {
        val l = layout(b.text, if (b.muted) pMuted else pPara, contentWInt)
        // Split long paragraphs line by line across pages.
        var line = 0
        while (line < l.lineCount) {
            ensure((l.getLineBottom(line) - l.getLineTop(line)).toFloat())
            val firstTop = l.getLineTop(line)
            var last = line
            while (last + 1 < l.lineCount && y + (l.getLineBottom(last + 1) - firstTop) <= contentBottom) last++
            val clipTop = firstTop
            val clipBottom = l.getLineBottom(last)
            val drawY = y
            emit { c ->
                c.save()
                c.translate(m, drawY - clipTop)
                c.clipRect(0f, clipTop.toFloat(), contentW, clipBottom.toFloat())
                l.draw(c)
                c.restore()
            }
            y += (clipBottom - clipTop).toFloat()
            line = last + 1
        }
        y += 4f
    }

    private fun placeTable(t: ReportBlock.Table) {
        val totalWeight = t.columns.sumOf { it.weight.toDouble() }.toFloat()
        val widths = t.columns.map { contentW * it.weight / totalWeight }
        val xs = widths.runningFold(m) { acc, w -> acc + w }

        fun cellLayouts(row: List<String>, header: Boolean, boldRow: Boolean): List<StaticLayout?> =
            row.mapIndexed { i, text ->
                val col = t.columns[i]
                if (col.isImage && !header) null
                else layout(text, if (header) pHead else if (boldRow) pBodyBold else pBody, (widths[i] - 2 * PAD_X).toInt().coerceAtLeast(8), col.align)
            }

        fun rowHeight(ls: List<StaticLayout?>, hasImage: Boolean): Float {
            val textH = ls.maxOf { it?.height ?: 0 }.toFloat()
            return max(textH, if (hasImage) THUMB else 0f) + 2 * PAD_Y
        }

        val header = cellLayouts(t.columns.map { it.title }, header = true, boldRow = false)
        val headerH = rowHeight(header, false)
        val allRows = t.rows + t.totals
        val measured = allRows.map { r -> cellLayouts(r.cells, false, r.style == RowStyle.TOTAL) }
        val hasImageCol = t.columns.any { it.isImage }
        val heights = measured.map { rowHeight(it, hasImageCol) }

        if (allRows.isEmpty()) {
            ensure(headerH + 24f)
            drawHeaderRow(header, headerH, xs, widths, t.columns)
            y += headerH
            placeParagraph(ReportBlock.Paragraph(t.emptyText, muted = true))
            return
        }

        val slices = TablePagination.plan(headerH, heights, y, contentTop, contentBottom, keepLast = t.totals.size)
        var currentOffset = 0
        for (s in slices) {
            while (currentOffset < s.pageOffset) {
                newPage()
                currentOffset++
            }
            y = s.headerY
            drawHeaderRow(header, headerH, xs, widths, t.columns)
            y += headerH
            for (i in s.rows) {
                val r = allRows[i]
                val h = heights[i]
                val top = y
                val bg = when {
                    r.style == RowStyle.TOTAL -> TOTAL_BG
                    r.style == RowStyle.HIGHLIGHT -> HIGHLIGHT_BG
                    i % 2 == 1 -> ZEBRA
                    else -> Color.TRANSPARENT
                }
                val cells = measured[i]
                val cols = t.columns
                val texts = r.cells
                val isTotal = r.style == RowStyle.TOTAL
                emit { c ->
                    if (bg != Color.TRANSPARENT) {
                        fill.color = bg
                        c.drawRect(m, top, m + contentW, top + h, fill)
                    }
                    cells.forEachIndexed { ci, l ->
                        if (cols[ci].isImage) {
                            drawThumb(c, texts[ci], xs[ci] + (widths[ci] - THUMB) / 2f, top + PAD_Y)
                        } else if (l != null) {
                            c.drawLayout(l, xs[ci] + PAD_X, top + PAD_Y)
                        }
                    }
                    if (isTotal) c.drawLine(m, top, m + contentW, top, line)
                    c.drawLine(m, top + h, m + contentW, top + h, line)
                }
                y += h
            }
        }
        y += 4f
    }

    private fun drawHeaderRow(header: List<StaticLayout?>, h: Float, xs: List<Float>, widths: List<Float>, cols: List<Column>) {
        val top = y
        emit { c ->
            fill.color = HEADER_BG
            c.drawRect(m, top, m + contentW, top + h, fill)
            header.forEachIndexed { i, l -> l?.let { c.drawLayout(it, xs[i] + PAD_X, top + PAD_Y) } }
            c.drawLine(m, top + h, m + contentW, top + h, line)
        }
    }

    // ------------------------------------------------------------------ header / footer
    private fun drawHeader(c: Canvas) {
        // Logo in a square on the left.
        val box = RectF(m, m, m + logoSide, m + logoSide)
        fill.color = BAND_SOFT
        c.drawRoundRect(box, 10f, 10f, fill)
        line.color = BRAND
        line.strokeWidth = 1.2f
        c.drawRoundRect(box, 10f, 10f, line)
        logo?.let { c.drawBitmap(it, null, RectF(box.left + 5f, box.top + 5f, box.right - 5f, box.bottom - 5f), imagePaint) }

        // Name, owner, address, phone — centred in the right block, vertically centred on the logo.
        var ty = m + (bandH - lhLines.sumOf { it.height }) / 2f
        lhLines.forEach { l -> c.drawLayout(l, textX, ty); ty += l.height }

        // Rule under the letterhead.
        val ruleY = m + bandH + 10f
        line.strokeWidth = 1.6f
        c.drawLine(m, ruleY, pageW - m, ruleY, line)

        // Date on the right, then the bold title.
        var y2 = ruleY + 12f
        c.drawLayout(dateLayout, m, y2)
        y2 += dateLayout.height + 10f
        c.drawLayout(titleLayout, m, y2)
        y2 += titleLayout.height
        rangeLayout?.let { c.drawLayout(it, m, y2) }
        line.strokeWidth = 0.6f
        line.color = GRID
        c.drawLine(m, contentTop - 5f, pageW - m, contentTop - 5f, line)
    }

    private fun dateText(): String {
        val d = at.format(DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT))
        val tithi = runCatching { GujaratiPanchangHelper.shortLabel(at.toLocalDate()) }.getOrNull()
        return if (tithi.isNullOrBlank()) d else "$d · $tithi"
    }

    private fun drawFooter(c: Canvas, n: Int, total: Int) {
        val fy = pageH - m - 14f
        c.drawLine(m, fy - 4f, pageW - m, fy - 4f, line)
        c.drawLayout(layout(ReportLabels.FOOTER_OWNER, pFooter, 300), m, fy)
        val pw = 150
        c.drawLayout(layout(ReportLabels.PAGE.format(n, total), pFooter, pw, Align.END), pageW - m - pw, fy)
    }

    private fun generatedLine(): String {
        val stamp = at.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.ROOT))
        val tithi = runCatching { GujaratiPanchangHelper.shortLabel(at.toLocalDate()) }.getOrNull()
        return ReportLabels.GENERATED.format(if (tithi.isNullOrBlank()) stamp else "$stamp · $tithi")
    }

    // ------------------------------------------------------------------ primitives
    private fun layout(text: CharSequence, p: TextPaint, width: Int, align: Align = Align.START): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, width.coerceAtLeast(1))
            .setAlignment(
                when (align) {
                    Align.START -> Layout.Alignment.ALIGN_NORMAL
                    Align.END -> Layout.Alignment.ALIGN_OPPOSITE
                    Align.CENTER -> Layout.Alignment.ALIGN_CENTER
                },
            )
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .setIncludePad(true)
            .build()

    private fun Canvas.drawLayout(l: StaticLayout, x: Float, y: Float) {
        save()
        translate(x, y)
        l.draw(this)
        restore()
    }

    private fun drawThumb(c: Canvas, path: String, x: Float, y: Float) {
        val bmp = if (path.isBlank()) null else bitmaps.getOrPut(path) { decodeThumb(path) }
        val dst = RectF(x, y, x + THUMB, y + THUMB)
        if (bmp == null) {
            fill.color = ZEBRA
            c.drawRoundRect(dst, 4f, 4f, fill)
            c.drawRoundRect(dst, 4f, 4f, line)
            return
        }
        // Center-crop to a square.
        val side = min(bmp.width, bmp.height)
        val src = Rect((bmp.width - side) / 2, (bmp.height - side) / 2, (bmp.width + side) / 2, (bmp.height + side) / 2)
        c.drawBitmap(bmp, src, dst, imagePaint)
        c.drawRoundRect(dst, 4f, 4f, line)
    }

    private fun decodeThumb(path: String): Bitmap? = runCatching {
        val f = File(path).takeIf { it.isAbsolute && it.exists() } ?: File(ctx.filesDir, path).takeIf { it.exists() } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= THUMB_PX) sample *= 2
        BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()

    companion object {
        private const val LOGO_BIG_PX = 360
        private const val PAD_X = 4f
        private const val PAD_Y = 3.5f
        /** Thumbnail box in pt (decoded at ≥ [THUMB_PX] px for print sharpness). */
        private const val THUMB = 32f
        private const val THUMB_PX = 96
        private const val LOGO_PX = 64
        private const val MIN_KEEP_WITH_HEADING = 44f

        // Print palette: flat brand green (no gradients in PDFs, doc 15).
        private val BAND = Color.parseColor("#1F5C45")
        private val BAND_SOFT = Color.parseColor("#EEF5F1")
        private val BRAND = Color.parseColor("#1F5C45")
        private val BRAND_DARK = Color.parseColor("#174A37")
        private val INK = Color.parseColor("#1B1F1D")
        private val MUTED = Color.parseColor("#5E6B63")
        private val GRID = Color.parseColor("#D5DDD8")
        private val ZEBRA = Color.parseColor("#F5F7F4")
        private val HEADER_BG = Color.parseColor("#E3EDE6")
        private val TOTAL_BG = Color.parseColor("#E8F1EC")
        /** Overdue / attention rows: M3 error-container tone. */
        private val HIGHLIGHT_BG = Color.parseColor("#F9DEDC")
    }
}
