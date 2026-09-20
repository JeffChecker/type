package dev.patrickgold.florisboard.ime.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickAction
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData

private data class ToolbarStyleItem(
    val title: String,
    val key: TextKeyData,
)

private val toolbarStyles = listOf(
    ToolbarStyleItem("Freundlich", TextKeyData.AI_FRIENDLY),
    ToolbarStyleItem("Professionell", TextKeyData.AI_PROFESSIONAL),
    ToolbarStyleItem("Geschäftlich", TextKeyData.AI_BUSINESS),
    ToolbarStyleItem("Stilvoll", TextKeyData.AI_ELEGANT),
    ToolbarStyleItem("Locker", TextKeyData.AI_CASUAL),
    ToolbarStyleItem("Humorvoll", TextKeyData.AI_HUMOROUS),
    ToolbarStyleItem("Sarkastisch", TextKeyData.AI_IRONIC),
    ToolbarStyleItem("Flirtend", TextKeyData.AI_FLIRTY),
    ToolbarStyleItem("Verführerisch / zweideutig", TextKeyData.AI_SUGGESTIVE),
    ToolbarStyleItem("Persönlich", TextKeyData.AI_PERSONAL),
    ToolbarStyleItem("Direkt", TextKeyData.AI_DIRECT),
    ToolbarStyleItem("Kurz", TextKeyData.AI_SHORT),
    ToolbarStyleItem("Einfache Sprache", TextKeyData.AI_SIMPLE),
    ToolbarStyleItem("Du-Form", TextKeyData.AI_DU),
    ToolbarStyleItem("Sie-Form", TextKeyData.AI_SIE),
)

@Composable
fun AiToolbar() {
    val context = LocalContext.current
    var styleMenuExpanded by remember { mutableStateOf(false) }

    fun trigger(key: TextKeyData) {
        val action = QuickAction.InsertKey(key)
        action.onPointerDown(context)
        action.onPointerUp(context)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color(0xFF111827))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AiToolbarButton(
            text = "KI ✓ Korrigieren",
            accent = true,
            onClick = { trigger(TextKeyData.AI_CORRECT) },
        )

        Box {
            AiToolbarButton(
                text = "Stil ▾",
                onClick = { styleMenuExpanded = true },
            )
            DropdownMenu(
                expanded = styleMenuExpanded,
                onDismissRequest = { styleMenuExpanded = false },
            ) {
                toolbarStyles.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.title) },
                        onClick = {
                            styleMenuExpanded = false
                            trigger(item.key)
                        },
                    )
                }
            }
        }

        AiToolbarButton(
            text = "Prompt+",
            onClick = { trigger(TextKeyData.AI_PROMPT) },
        )

        AiToolbarButton(
            text = "Mit Google übersetzen",
            onClick = { trigger(TextKeyData.AI_TRANSLATE) },
        )

        AiToolbarButton(
            text = "⚙ KI",
            onClick = { trigger(TextKeyData.AI_SETTINGS) },
        )
    }
}

@Composable
private fun AiToolbarButton(
    text: String,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val background = if (accent) Color(0xFF6750FF) else Color(0xFF25304A)
    val foreground = Color.White

    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = background,
            contentColor = foreground,
        ),
        modifier = Modifier.height(40.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
    ) {
        Text(
            text = text,
            color = foreground,
            fontSize = 13.sp,
            fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}
