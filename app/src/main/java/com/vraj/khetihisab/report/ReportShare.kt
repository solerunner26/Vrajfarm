package com.vraj.khetihisab.report

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Open / share a generated PDF through the app FileProvider (authority `${applicationId}.files`,
 * path "reports/" in res/xml/file_paths.xml).
 */
object ReportShare {
    const val MIME_PDF = "application/pdf"

    fun authority(context: Context): String = "${context.packageName}.files"

    fun uriFor(context: Context, file: File): Uri = FileProvider.getUriForFile(context, authority(context), file)

    /** ACTION_VIEW intent for a PDF viewer. */
    fun viewIntent(context: Context, file: File): Intent {
        val uri = uriFor(context, file)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, MIME_PDF)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** ACTION_SEND chooser (print, WhatsApp, email …). The user picks the target; nothing is sent automatically. */
    fun shareIntent(context: Context, file: File, chooserTitle: CharSequence? = null): Intent {
        val uri = uriFor(context, file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(MIME_PDF)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(file.name, uri)
        return Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    /** Opens the PDF; returns false when no PDF viewer is installed (caller can fall back to [share]). */
    fun open(context: Context, file: File): Boolean = try {
        context.startActivity(viewIntent(context, file))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    fun share(context: Context, file: File, chooserTitle: CharSequence? = null) {
        context.startActivity(shareIntent(context, file, chooserTitle))
    }
}
