package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Search

/**
 * Compact search field with the same 44.dp height, icon inset, and glyph size
 * everywhere it is used in the app.
 */
@Composable
fun AnrealSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    glass: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    AnrealTextField(
        value = value,
        onValueChange = onValueChange,
        label = placeholder,
        placeholder = placeholder,
        modifier = modifier,
        glass = glass,
        showLabel = false,
        minHeight = AnrealSpacing.search,
        textStyle = MaterialTheme.typography.bodyMedium,
        leadingIcon = {
            Icon(
                imageVector = MaterialSymbols.Rounded.Search,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = glassMutedTextColor(),
            )
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}
