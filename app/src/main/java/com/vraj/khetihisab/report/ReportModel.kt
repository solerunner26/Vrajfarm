package com.vraj.khetihisab.report

/**
 * Renderer-independent report content. [ReportContentBuilder] produces it from plain data (JVM-testable);
 * [GujaratiPdfGenerator] lays it out on A4 pages with StaticLayout.
 */
data class ReportDoc(
    val type: ReportType,
    val title: String,
    /** e.g. "01-04-2026 થી 31-03-2027", or "" for reports without a range. */
    val rangeLabel: String,
    val blocks: List<ReportBlock>,
)

sealed interface ReportBlock {
    /** Section heading (kept together with the first rows that follow it). */
    data class Heading(val text: String) : ReportBlock

    /** Free text, wrapped with StaticLayout (may span many lines). */
    data class Paragraph(val text: String, val muted: Boolean = false) : ReportBlock

    data class Table(
        val columns: List<Column>,
        val rows: List<Row>,
        /** Optional bold total row(s), drawn after the body (never split from the last body row). */
        val totals: List<Row> = emptyList(),
        /** Shown in place of the body when [rows] is empty. */
        val emptyText: String = ReportLabels.NO_ENTRIES,
    ) : ReportBlock {
        init {
            (rows + totals).forEach { r ->
                require(r.cells.size == columns.size) { "row has ${r.cells.size} cells, table has ${columns.size} columns" }
            }
        }
    }

    data class Spacer(val heightPt: Float = 10f) : ReportBlock
}

enum class Align { START, END, CENTER }

data class Column(
    val title: String,
    /** Relative width; widths are distributed proportionally over the content width. */
    val weight: Float,
    val align: Align = Align.START,
    /** Image column: the cell text is a file path (absolute or relative to filesDir) drawn as a thumbnail. */
    val isImage: Boolean = false,
)

enum class RowStyle { NORMAL, TOTAL, HIGHLIGHT }

data class Row(
    val cells: List<String>,
    val style: RowStyle = RowStyle.NORMAL,
)
