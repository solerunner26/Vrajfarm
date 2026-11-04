package com.vraj.khetihisab.ui.entry

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.home.rememberScreensDeps

/**
 * Gujarati speech-to-text through the system recognizer (RecognizerIntent, gu-IN). The app itself
 * needs no RECORD_AUDIO: the recognizer activity owns the microphone. Offline needs Google's gu-IN pack.
 *
 * @return a launcher; calling it opens the recognizer, or calls [onUnavailable] when no app handles it.
 */
@Composable
fun rememberGujaratiVoiceInput(onText: (String) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val deps = rememberScreensDeps()
    val prompt = stringResource(R.string.scr_voice_prompt)
    val textCb by rememberUpdatedState(onText)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(textCb)
        }
    }
    return {
        val intent = voiceIntent(prompt)
        if (intent.resolveActivity(context.packageManager) == null) {
            onUnavailable()
        } else {
            deps.lockManager.suppressNextLock = true
            runCatching { launcher.launch(intent) }.onFailure {
                deps.lockManager.suppressNextLock = false
                onUnavailable()
            }
        }
    }
}

fun voiceIntent(prompt: String): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "gu-IN")
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "gu-IN")
    putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
}
