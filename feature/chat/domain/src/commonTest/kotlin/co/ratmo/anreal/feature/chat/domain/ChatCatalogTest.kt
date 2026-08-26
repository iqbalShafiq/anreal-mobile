package co.ratmo.anreal.feature.chat.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import kotlin.test.Test

class ChatCatalogTest {

    @Test
    fun preserves_a_supported_model_and_effort() {
        val resolution = reconcileCatalogSelection(
            catalog = catalog(
                model(
                    id = "m1",
                    reasoningEfforts = listOf("low", "high"),
                ),
            ),
            requestedModelId = "m1",
            requestedReasoningEffort = "high",
        )

        assertThat(resolution).isEqualTo(
            CatalogSelectionResolution(
                selectedModelId = "m1",
                selectedReasoningEffort = "high",
                modelUnavailable = false,
                unavailableModelId = null,
                unavailableModelLabel = null,
            ),
        )
    }

    @Test
    fun selects_the_first_server_model_when_no_model_is_persisted() {
        val resolution = reconcileCatalogSelection(
            catalog = catalog(
                model(id = "first", reasoningEfforts = listOf("low")),
                model(id = "second", reasoningEfforts = listOf("high")),
            ),
            requestedModelId = null,
            requestedReasoningEffort = null,
        )

        assertThat(resolution.selectedModelId).isEqualTo("first")
        assertThat(resolution.selectedReasoningEffort).isNull()
        assertThat(resolution.modelUnavailable).isFalse()
    }

    @Test
    fun reports_a_removed_model_and_clears_both_selections() {
        val resolution = reconcileCatalogSelection(
            catalog = catalog(model(id = "current", reasoningEfforts = listOf("low"))),
            requestedModelId = "removed",
            requestedReasoningEffort = "high",
        )

        assertThat(resolution).isEqualTo(
            CatalogSelectionResolution(
                selectedModelId = null,
                selectedReasoningEffort = null,
                modelUnavailable = true,
                unavailableModelId = "removed",
                unavailableModelLabel = "removed",
            ),
        )
    }

    @Test
    fun downgrades_xhigh_to_the_greatest_supported_lower_effort() {
        val resolution = reconcileCatalogSelection(
            catalog = catalog(
                model(id = "m1", reasoningEfforts = listOf("low", "high")),
                efforts = listOf("low", "high", "xhigh"),
            ),
            requestedModelId = "m1",
            requestedReasoningEffort = "xhigh",
        )

        assertThat(resolution.selectedReasoningEffort).isEqualTo("high")
    }

    @Test
    fun clears_an_effort_when_no_lower_option_exists() {
        val resolution = reconcileCatalogSelection(
            catalog = catalog(model(id = "m1", reasoningEfforts = listOf("high"))),
            requestedModelId = "m1",
            requestedReasoningEffort = "low",
        )

        assertThat(resolution.selectedReasoningEffort).isNull()
    }

    @Test
    fun treats_a_null_effort_as_no_effort() {
        val resolution = reconcileCatalogSelection(
            catalog = catalog(model(id = "m1", reasoningEfforts = listOf("low"))),
            requestedModelId = "m1",
            requestedReasoningEffort = null,
        )

        assertThat(resolution.selectedReasoningEffort).isNull()
    }

    @Test
    fun treats_none_as_no_effort() {
        val resolution = reconcileCatalogSelection(
            catalog = catalog(model(id = "m1", reasoningEfforts = listOf("none", "low"))),
            requestedModelId = "m1",
            requestedReasoningEffort = "none",
        )

        assertThat(resolution.selectedReasoningEffort).isNull()
    }

    @Test
    fun chooses_the_greatest_lower_unknown_effort_by_catalog_order() {
        val resolution = reconcileCatalogSelection(
            catalog = catalog(
                model(id = "m1", reasoningEfforts = listOf("legacy-low", "legacy-mid")),
                efforts = listOf("legacy-low", "legacy-mid", "legacy-high"),
            ),
            requestedModelId = "m1",
            requestedReasoningEffort = "legacy-high",
        )

        assertThat(resolution.selectedReasoningEffort).isEqualTo("legacy-mid")
    }

    @Test
    fun unknown_efforts_do_not_outrank_known_efforts() {
        val resolution = reconcileCatalogSelection(
            catalog = catalog(
                model(id = "m1", reasoningEfforts = listOf("low", "legacy-high")),
                efforts = listOf("low", "legacy-high", "xhigh"),
            ),
            requestedModelId = "m1",
            requestedReasoningEffort = "xhigh",
        )

        assertThat(resolution.selectedReasoningEffort).isEqualTo("low")
    }

    private fun catalog(
        vararg models: ChatModel,
        efforts: List<String> = listOf("low", "medium", "high", "xhigh"),
    ): ModelCatalog = ModelCatalog(
        models = models.toList(),
        efforts = efforts.map { key -> ReasoningEffort(key = key, label = key) },
    )

    private fun model(id: String, reasoningEfforts: List<String>): ChatModel = ChatModel(
        id = id,
        label = id,
        reasoningEfforts = reasoningEfforts,
    )
}
