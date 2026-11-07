package org.hiperfoco.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HiperFocoScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun searchesUniversesAndSubtopicsWithoutAccents() {
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("Buscar universos e subtemas")).fetchSemanticsNodes().isNotEmpty()
        }
        val search = compose.onNodeWithTag("busca-interesses")
        search.performTextReplacement("fosseis")
        compose.onNodeWithText("Dinossauros").assertIsDisplayed()
        compose.onNodeWithText("Fósseis e escavações").assertIsDisplayed()
        compose.onNodeWithText("Linux").assertDoesNotExist()

        search.performTextReplacement("PROGRAMACAO")
        compose.onNodeWithText("Programação").assertIsDisplayed()
        compose.onNodeWithText("Dinossauros").assertDoesNotExist()

        search.performTextReplacement("nenhuma correspondência")
        compose.onNodeWithText("Nenhum resultado encontrado.").assertIsDisplayed()
        compose.onNodeWithText("Limpar").performClick()
        compose.onNodeWithText("Astronomia").assertIsDisplayed()
    }

    @Test
    fun createsAndRenamesTopicThenKeepsItAfterRecreatingActivity() {
        val universe = "Teste ${UUID.randomUUID().toString().take(8)}"
        val openingLabel = "Abrir subtemas de $universe"
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("Criar universo")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Criar universo").performClick()
        saveName(universe)

        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("Novo universo")).fetchSemanticsNodes().isEmpty()
        }
        val openUniverse = hasContentDescription(openingLabel)
        compose.onNodeWithTag("lista-interesses").performScrollToNode(openUniverse)
        compose.onNode(openUniverse).performClick()
        compose.onNodeWithText("Criar subtema").performClick()
        saveName("Lua")
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("Lua")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Editar").performClick()
        saveName("Satélites")

        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("Satélites")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Satélites").assertIsDisplayed()

        compose.onNodeWithText("Voltar aos universos").performClick()
        val deleteUniverse = hasContentDescription("Excluir $universe")
        compose.onNodeWithTag("lista-interesses").performScrollToNode(deleteUniverse)
        compose.onNode(deleteUniverse).performClick()
        compose.onNode(hasText("Excluir") and hasAnyAncestor(isDialog())).performClick()
    }

    private fun saveName(name: String) {
        compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement(name)
        compose.onNodeWithText("Salvar").performClick()
    }
}
