package co.ratmo.anreal.core.data.auth

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val KeysetName = "anreal_session_keyset"
private const val PrefFileName = "anreal_tink"
private const val MasterKeyUri = "android-keystore://anreal_session"
private val AssociatedData = "anreal.session_token".encodeToByteArray()

class TinkTokenCipher(
    private val aead: Aead,
) : TokenCipher {

    constructor(context: Context) : this(keystoreAead(context.applicationContext))

    @OptIn(ExperimentalEncodingApi::class)
    override fun encrypt(plaintext: String): String {
        return Base64.encode(aead.encrypt(plaintext.encodeToByteArray(), AssociatedData))
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun decrypt(ciphertext: String): String {
        return aead.decrypt(Base64.decode(ciphertext), AssociatedData).decodeToString()
    }
}

private fun keystoreAead(context: Context): Aead {
    AeadConfig.register()
    val handle = AndroidKeysetManager.Builder()
        .withSharedPref(context, KeysetName, PrefFileName)
        .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
        .withMasterKeyUri(MasterKeyUri)
        .build()
        .keysetHandle
    return handle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
}
