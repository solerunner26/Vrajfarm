package com.vraj.khetihisab.report

/**
 * Pure table pagination (doc 15 "TableRenderer"): rows are measured with StaticLayout first, then this
 * splits them over pages, repeating the header row on every page. JVM-tested without Android.
 */
object TablePagination {

    /**
     * One page's share of a table.
     * @property pageOffset 0 = the page the table starts on, 1 = the next page, …
     * @property headerY y of the repeated header row on that page.
     * @property rows indices into the row-height list.
     */
    data class Slice(val pageOffset: Int, val headerY: Float, val rows: IntRange)

    /**
     * @param headerH header row height
     * @param rowHeights every body + total row height, in draw order
     * @param startY cursor on the current page
     * @param top content top of a fresh page
     * @param bottom content bottom (exclusive limit) of every page
     * @param keepLast the last [keepLast] rows (totals) never start a page alone: they move together with
     *        the preceding body row when they would not fit.
     */
    fun plan(
        headerH: Float,
        rowHeights: List<Float>,
        startY: Float,
        top: Float,
        bottom: Float,
        keepLast: Int = 0,
    ): List<Slice> {
        require(bottom > top) { "bottom must be below top" }
        if (rowHeights.isEmpty()) {
            val offset = if (startY + headerH > bottom && startY > top) 1 else 0
            return listOf(Slice(offset, if (offset == 0) startY else top, IntRange.EMPTY))
        }
        val slices = mutableListOf<Slice>()
        var page = 0
        // The header must be followed by at least one row on the same page.
        var headerY = startY
        if (startY > top && startY + headerH + rowHeights[0] > bottom) {
            page = 1
            headerY = top
        }
        var y = headerY + headerH
        var first = 0
        val n = rowHeights.size
        val keepFrom = (n - keepLast).coerceAtLeast(1) // index of the first "kept" total row
        var i = 0
        while (i < n) {
            val h = rowHeights[i]
            // Totals group: needs room for every remaining total row; otherwise carry the previous body row over.
            val needed = if (keepLast > 0 && i == keepFrom - 1 && i + 1 < n) {
                (i until n).sumOf { rowHeights[it].toDouble() }.toFloat()
            } else h
            val fitsAll = y + needed <= bottom
            val fitsRow = y + h <= bottom
            if (!fitsRow || (!fitsAll && i > first && needed <= (bottom - top - headerH))) {
                if (i > first) {
                    slices += Slice(page, headerY, first until i)
                    page++
                    headerY = top
                    y = top + headerH
                    first = i
                    continue
                }
                // A single row taller than the free space at the top of a page: draw it anyway (clipped).
            }
            y += h
            i++
        }
        slices += Slice(page, headerY, first until n)
        return slices
    }

    /** Pages occupied, counting from the starting page (1 = fits where it starts). */
    fun pagesSpanned(slices: List<Slice>): Int = (slices.maxOfOrNull { it.pageOffset } ?: 0) + 1
}
