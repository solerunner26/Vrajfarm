package com.vraj.khetihisab.ui.panchang

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vraj.khetihisab.R
import com.vraj.khetihisab.panchang.PanchangAlertType
import java.time.ZoneId

/** Resource-backed labels and the shape markers of the Panchang calendar. Colours come from MaterialTheme. */
object PanchangUi {
    val IST: ZoneId = ZoneId.of("Asia/Kolkata")
}

@Composable
fun panchangTypeLabel(t: PanchangAlertType): String = stringResource(
    when (t) {
        PanchangAlertType.FESTIVAL -> R.string.st_type_festival
        PanchangAlertType.AGIYARAS -> R.string.st_type_agiyaras
        PanchangAlertType.AMAS -> R.string.st_type_amas
        PanchangAlertType.POONAM -> R.string.st_type_poonam
        PanchangAlertType.BIJ -> R.string.st_type_bij
    },
)

@Composable
fun shortTithiLabel(t: ShortTithi?): String = when (t?.kind) {
    null -> ""
    ShortTithi.Kind.AMAS -> stringResource(R.string.st_type_amas)
    ShortTithi.Kind.POONAM -> stringResource(R.string.st_type_poonam)
    ShortTithi.Kind.SUD -> stringResource(R.string.st_cal_sud_short, t.n)
    ShortTithi.Kind.VAD -> stringResource(R.string.st_cal_vad_short, t.n)
}

@Composable
fun dayPeriodLabel(p: DayPeriod): String = stringResource(
    when (p) {
        DayPeriod.MORNING -> R.string.st_cal_morning
        DayPeriod.AFTERNOON -> R.string.st_cal_afternoon
        DayPeriod.EVENING -> R.string.st_cal_evening
        DayPeriod.NIGHT -> R.string.st_cal_night
    },
)

@Composable
fun markerLabel(m: DayMarker): String = stringResource(
    when (m) {
        DayMarker.FESTIVAL -> R.string.st_type_festival
        DayMarker.POONAM -> R.string.st_type_poonam
        DayMarker.AMAS -> R.string.st_type_amas
        DayMarker.AGIYARAS -> R.string.st_type_agiyaras
        DayMarker.BIJ -> R.string.st_type_bij
    },
)

/**
 * Marker glyph by shape (doc 10): Poonam filled circle, Amas hollow circle, Agiyaras "૧૧" badge,
 * Bij crescent, Festival 4-point star in tertiary (Panchang gold).
 */
@Composable
fun MarkerGlyph(marker: DayMarker, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    val cs = MaterialTheme.colorScheme
    when (marker) {
        DayMarker.AGIYARAS -> Box(
            modifier
                .clip(RoundedCornerShape(3.dp))
                .background(cs.primary)
                .padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.st_cal_agiyaras_badge),
                color = cs.onPrimary,
                style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = MaterialTheme.typography.labelSmall.fontFamily, lineHeight = 9.sp),
                maxLines = 1,
                softWrap = false,
            )
        }
        else -> Canvas(modifier.size(size)) {
            val r = this.size.minDimension / 2f
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            when (marker) {
                DayMarker.POONAM -> drawCircle(cs.primary, radius = r, center = c)
                DayMarker.AMAS -> drawCircle(cs.primary, radius = r - 1.dp.toPx(), center = c, style = Stroke(1.6.dp.toPx()))
                DayMarker.BIJ -> {
                    val outer = Path().apply { addOval(Rect(c, r)) }
                    val inner = Path().apply { addOval(Rect(Offset(c.x + r * 0.55f, c.y - r * 0.15f), r * 0.95f)) }
                    drawPath(Path.combine(PathOperation.Difference, outer, inner), cs.primary)
                }
                DayMarker.FESTIVAL -> {
                    val star = Path().apply {
                        moveTo(c.x, c.y - r)
                        quadraticTo(c.x + r * 0.18f, c.y - r * 0.18f, c.x + r, c.y)
                        quadraticTo(c.x + r * 0.18f, c.y + r * 0.18f, c.x, c.y + r)
                        quadraticTo(c.x - r * 0.18f, c.y + r * 0.18f, c.x - r, c.y)
                        quadraticTo(c.x - r * 0.18f, c.y - r * 0.18f, c.x, c.y - r)
                        close()
                    }
                    drawPath(star, cs.tertiary)
                }
                DayMarker.AGIYARAS -> Unit
            }
        }
    }
}
