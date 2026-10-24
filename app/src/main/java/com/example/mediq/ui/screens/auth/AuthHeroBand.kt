package com.example.mediq.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.mediq.R
import com.example.mediq.ui.theme.MediQSize

/**
 * The photo band at the top of the sign-in screen, with the MediQ mark
 * overhanging into the form below it.
 *
 * The photo is decoration and carries no description; the mark is what says
 * the brand. The overhang is half the mark, so the caller leaves
 * `MediQSize.authLogo / 2` above its first line of text.
 */
@Composable
internal fun AuthHeroBand(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(id = R.drawable.hospital_image),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(MediQSize.authHero),
            contentScale = ContentScale.Crop
        )

        Image(
            painter = painterResource(id = R.drawable.mediq_logo),
            contentDescription = "MediQ",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = MediQSize.authLogo / 2)
                .size(MediQSize.authLogo)
        )
    }
}
