package co.ratmo.anreal.core.data.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import kotlin.test.Test

class TinkTokenCipherTest {

    @Test
    fun encrypt_round_trips_and_is_not_plaintext() {
        AeadConfig.register()
        val handle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
        val aead = handle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        val cipher = TinkTokenCipher(aead)

        val encrypted = cipher.encrypt("abc.session")
        assertThat(encrypted).isNotEqualTo("abc.session")
        assertThat(cipher.decrypt(encrypted)).isEqualTo("abc.session")
    }
}
