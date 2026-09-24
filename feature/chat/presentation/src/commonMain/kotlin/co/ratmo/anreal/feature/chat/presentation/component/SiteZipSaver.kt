package co.ratmo.anreal.feature.chat.presentation.component

import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.CancellationException

sealed interface SiteZipSaveResult {
    data object Saved : SiteZipSaveResult
    data object Cancelled : SiteZipSaveResult
    data class Failed(val message: UiText) : SiteZipSaveResult
}

fun interface SiteZipSaver {
    suspend fun save(filename: String, bytes: ByteArray): SiteZipSaveResult
}

/**
 * Saves site bytes through the platform save dialog (FileKit `openFileSaver`, real on
 * Android and iOS). Cancellation is silent; failures surface as [UiText].
 */
class FileKitSiteZipSaver : SiteZipSaver {
    override suspend fun save(filename: String, bytes: ByteArray): SiteZipSaveResult {
        return try {
            val target = FileKit.openFileSaver(
                suggestedName = filename.substringBeforeLast("."),
                defaultExtension = filename.substringAfterLast(".", "").ifBlank { null },
            ) ?: return SiteZipSaveResult.Cancelled
            target.write(bytes)
            SiteZipSaveResult.Saved
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            SiteZipSaveResult.Failed(UiText.StringResource(AnrealCopy.SITE_DOWNLOAD_FAILED))
        }
    }
}
