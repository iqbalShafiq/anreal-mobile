package co.ratmo.anreal.feature.chat.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.feature.chat.domain.ChatModel
import co.ratmo.anreal.feature.chat.domain.ReasoningEffort
import co.ratmo.anreal.feature.chat.presentation.component.modelAndReasoningLabel
import kotlin.test.Test

class ModelAndReasoningLabelTest {

    @Test
    fun concatenatesEffortWhenReasoningIsNotNone() {
        val label = modelAndReasoningLabel(
            ChatState(
                models = listOf(ChatModel(id = "luna", label = "GPT Luna 5.6")),
                selectedModelId = "luna",
                reasoningEfforts = listOf(ReasoningEffort(key = "xhigh", label = "Xhigh")),
                selectedReasoning = "xhigh",
            ),
        )
        assertThat(label).isEqualTo("GPT Luna 5.6 Xhigh")
    }

    @Test
    fun usesModelLabelOnlyWhenReasoningIsNone() {
        val label = modelAndReasoningLabel(
            ChatState(
                models = listOf(ChatModel(id = "luna", label = "GPT Luna 5.6")),
                selectedModelId = "luna",
                selectedReasoning = null,
            ),
        )
        assertThat(label).isEqualTo("GPT Luna 5.6")
    }

    @Test
    fun fallsBackToModelCopyWhenCatalogIsEmpty() {
        val label = modelAndReasoningLabel(ChatState())
        assertThat(label).isEqualTo(AnrealCopy.get(AnrealCopy.LABEL_MODEL))
    }
}
