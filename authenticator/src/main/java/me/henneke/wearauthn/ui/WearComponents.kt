package me.henneke.wearauthn.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

@Composable
fun WearListScreen(
    title: String,
    modifier: Modifier = Modifier,
    state: TransformingLazyColumnState = rememberTransformingLazyColumnState(),
    content: TransformingLazyColumnScope.() -> Unit,
) {
    AppScaffold(modifier = modifier) {
        ScreenScaffold(scrollState = state) { contentPadding ->
            TransformingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = state,
                contentPadding = contentPadding,
            ) {
                item { ListHeader { Text(title) } }
                content()
            }
        }
    }
}

@Composable
fun WearSection(title: String) {
    ListSubHeader(label = { Text(title) })
}

@Composable
fun WearButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    @DrawableRes iconRes: Int? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(horizontal = 6.dp),
        secondaryLabel = secondaryLabel?.let { text -> { Text(text) } },
        icon = iconRes?.let { drawable ->
            {
                Icon(
                    painter = painterResource(drawable),
                    contentDescription = null,
                )
            }
        },
        enabled = enabled,
        colors = colors,
        label = { Text(label) },
    )
}

@Composable
fun WearBodyItem(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
    color: Color = Color.Unspecified,
    text: String,
) {
    Text(text = text, modifier = modifier.fillMaxWidth().padding(contentPadding), color = color)
}
