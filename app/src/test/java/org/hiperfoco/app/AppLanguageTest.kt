package org.hiperfoco.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppLanguageTest {
    @Test
    fun usesPortugueseByDefaultAndRestoresLanguageInNewContexts() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.selected(context))
        assertEquals("Criar universo", AppLanguage.context(context).getString(R.string.create_universe))

        AppLanguage.ENGLISH.save(context)
        assertEquals("Create universe", AppLanguage.context(context).getString(R.string.create_universe))

        AppLanguage.SPANISH.save(context)
        val recreated = AppLanguage.context(context)
        assertEquals(AppLanguage.SPANISH, AppLanguage.selected(recreated))
        assertEquals("¿Eliminar el subtema «Linux»?", recreated.getString(R.string.delete_topic_prompt, "Linux"))
    }
}
