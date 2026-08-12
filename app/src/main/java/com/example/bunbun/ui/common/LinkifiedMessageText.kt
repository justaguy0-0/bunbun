package com.example.bunbun.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.example.bunbun.R

private const val WEB_LINK_TAG = "bunbun-web-link"

@Composable
fun LinkifiedMessageText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val links = remember(text) { detectWebLinks(text) }
    if (links.isEmpty()) {
        Text(text = text, modifier = modifier, style = MaterialTheme.typography.bodyMedium, color = color)
        return
    }

    val context = LocalContext.current
    val errorMessage = stringResource(R.string.chat_link_open_error)
    val actionLabel = stringResource(R.string.chat_open_link_description)
    val linkColor = MaterialTheme.colorScheme.primary
    val annotatedText = remember(text, links, linkColor) {
        buildAnnotatedString {
            append(text)
            links.forEach { link ->
                addStyle(
                    SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    start = link.start,
                    end = link.end,
                )
                addStringAnnotation(WEB_LINK_TAG, link.normalizedUrl, link.start, link.end)
            }
        }
    }
    val openLink: (String) -> Unit = { url ->
        if (!openWebLink(context, url)) Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
    val accessibilityActions = links.map { link ->
        CustomAccessibilityAction(
            label = "$actionLabel ${link.text}",
            action = {
                openLink(link.normalizedUrl)
                true
            },
        )
    }

    @Suppress("DEPRECATION")
    ClickableText(
        text = annotatedText,
        modifier = modifier.semantics { customActions = accessibilityActions },
        style = MaterialTheme.typography.bodyMedium.copy(color = color),
        onClick = { offset ->
            annotatedText.getStringAnnotations(WEB_LINK_TAG, offset, offset)
                .firstOrNull()
                ?.let { openLink(it.item) }
        },
    )
}

private fun openWebLink(context: Context, candidate: String): Boolean {
    val url = normalizeWebUrl(candidate) ?: return false
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }
}
