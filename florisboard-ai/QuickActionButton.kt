/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.ime.smartbar.quickaction

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.compose.tooltip.PlainTooltip
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.computeImageVector
import dev.patrickgold.florisboard.ime.keyboard.computeLabel
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggText

enum class QuickActionBarType {
    INTERACTIVE_BUTTON,
    INTERACTIVE_TILE,
    EDITOR_TILE;
}

private data class AiStyleMenuItem(val title: String, val key: TextKeyData)

private val AiStyleMenuItems = listOf(
    AiStyleMenuItem("Sarkastisch / ironisch", TextKeyData.AI_IRONIC),
    AiStyleMenuItem("Flirtend", TextKeyData.AI_FLIRTY),
    AiStyleMenuItem("Verführerisch / zweideutig", TextKeyData.AI_SUGGESTIVE),
    AiStyleMenuItem("Stilvoll / elegant", TextKeyData.AI_ELEGANT),
    AiStyleMenuItem("Geschäftlich", TextKeyData.AI_BUSINESS),
    AiStyleMenuItem("Professionell", TextKeyData.AI_PROFESSIONAL),
    AiStyleMenuItem("Freundlich", TextKeyData.AI_FRIENDLY),
    AiStyleMenuItem("Persönlich", TextKeyData.AI_PERSONAL),
    AiStyleMenuItem("Du-Form", TextKeyData.AI_DU),
    AiStyleMenuItem("Sie-Form", TextKeyData.AI_SIE),
    AiStyleMenuItem("Locker", TextKeyData.AI_CASUAL),
    AiStyleMenuItem("Humorvoll", TextKeyData.AI_HUMOROUS),
    AiStyleMenuItem("Direkt", TextKeyData.AI_DIRECT),
    AiStyleMenuItem("Kurz", TextKeyData.AI_SHORT),
    AiStyleMenuItem("Einfache Sprache", TextKeyData.AI_SIMPLE),
)

@Composable
fun QuickActionButton(
    action: QuickAction,
    evaluator: ComputingEvaluator,
    modifier: Modifier = Modifier,
    type: QuickActionBarType = QuickActionBarType.INTERACTIVE_BUTTON,
) {
    val context = LocalContext.current
    val inputFeedbackController = FlorisImeService.inputFeedbackController()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isEnabled = type == QuickActionBarType.EDITOR_TILE || evaluator.evaluateEnabled(action.keyData())
    val isAiStyleMenu = action is QuickAction.InsertKey && action.data.code == KeyCode.AI_STYLE_MENU
    var styleMenuExpanded by remember { mutableStateOf(false) }

    val elementName = when (type) {
        QuickActionBarType.INTERACTIVE_BUTTON -> FlorisImeUi.SmartbarActionKey
        QuickActionBarType.INTERACTIVE_TILE -> FlorisImeUi.SmartbarActionTile
        QuickActionBarType.EDITOR_TILE -> FlorisImeUi.SmartbarActionsEditorTile
    }.elementName
    val attributes = mapOf(FlorisImeUi.Attr.Code to action.keyData().code)
    val selector = when {
        isPressed -> SnyggSelector.PRESSED
        !isEnabled -> SnyggSelector.DISABLED
        else -> null
    }

    DisposableEffect(action, isEnabled) {
        onDispose {
            if (action is QuickAction.InsertKey && !isAiStyleMenu) {
                action.onPointerCancel(context)
            }
        }
    }

    PlainTooltip(action.computeTooltip(evaluator), enabled = type == QuickActionBarType.INTERACTIVE_BUTTON) {
        Box {
            SnyggBox(
                elementName = elementName,
                attributes = attributes,
                selector = selector,
                modifier = modifier,
                clickAndSemanticsModifier = Modifier
                    .aspectRatio(1f)
                    .indication(interactionSource, LocalIndication.current)
                    .pointerInput(action, isEnabled, isAiStyleMenu) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            if (isEnabled && type != QuickActionBarType.EDITOR_TILE) {
                                val press = PressInteraction.Press(down.position)
                                inputFeedbackController?.keyPress(TextKeyData.UNSPECIFIED)
                                interactionSource.tryEmit(press)
                                if (!isAiStyleMenu) action.onPointerDown(context)
                                val up = waitForUpOrCancellation()
                                if (up != null) {
                                    up.consume()
                                    interactionSource.tryEmit(PressInteraction.Release(press))
                                    if (isAiStyleMenu) {
                                        styleMenuExpanded = true
                                    } else {
                                        action.onPointerUp(context)
                                    }
                                } else {
                                    interactionSource.tryEmit(PressInteraction.Cancel(press))
                                    if (!isAiStyleMenu) action.onPointerCancel(context)
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (action) {
                        is QuickAction.InsertKey -> {
                            val (imageVector, label) = remember(action, evaluator) {
                                evaluator.computeImageVector(action.data) to evaluator.computeLabel(action.data)
                            }
                            if (imageVector != null) {
                                SnyggBox(
                                    elementName = "$elementName-icon",
                                    attributes = attributes,
                                    selector = selector,
                                ) {
                                    SnyggIcon(imageVector = imageVector)
                                }
                            } else if (label != null) {
                                SnyggText(
                                    elementName = "$elementName-text",
                                    attributes = attributes,
                                    selector = selector,
                                    text = label,
                                )
                            }
                        }
                        is QuickAction.InsertText -> {
                            SnyggText(
                                elementName = "$elementName-text",
                                attributes = attributes,
                                selector = selector,
                                text = action.data.firstOrNull().toString().ifBlank { "?" },
                            )
                        }
                    }

                    if (type != QuickActionBarType.INTERACTIVE_BUTTON) {
                        SnyggText(
                            elementName = "$elementName-text",
                            attributes = attributes,
                            selector = selector,
                            text = action.computeDisplayName(evaluator = evaluator),
                        )
                    }
                }
            }

            if (isAiStyleMenu && type != QuickActionBarType.EDITOR_TILE) {
                DropdownMenu(
                    expanded = styleMenuExpanded,
                    onDismissRequest = { styleMenuExpanded = false },
                ) {
                    AiStyleMenuItems.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.title) },
                            onClick = {
                                styleMenuExpanded = false
                                val styleAction = QuickAction.InsertKey(item.key)
                                styleAction.onPointerDown(context)
                                styleAction.onPointerUp(context)
                            },
                        )
                    }
                }
            }
        }
    }
}
