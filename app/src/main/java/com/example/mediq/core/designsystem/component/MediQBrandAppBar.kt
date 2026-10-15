package com.example.mediq.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQOnBrand
import com.example.mediq.core.designsystem.theme.MediQOnBrandMuted

/**
 * The green bar at the top of every signed-in screen: a title, an optional
 * supporting line under it, an optional back arrow, and an optional trailing
 * action.
 *
 * A plain [TopAppBar] with a `String` title cannot carry the second line the
 * figures draw (a count or a specialty under the title), which is why this
 * exists rather than each screen re-deriving it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediQBrandAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MediQOnBrandMuted,
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MediQGreen,
            titleContentColor = MediQOnBrand,
            navigationIconContentColor = MediQOnBrand,
            actionIconContentColor = MediQOnBrand,
        ),
    )
}
