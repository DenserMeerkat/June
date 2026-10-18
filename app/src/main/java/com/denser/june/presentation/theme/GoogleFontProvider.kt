package com.denser.june.presentation.theme

import androidx.compose.ui.text.googlefonts.GoogleFont
import com.denser.june.R
import com.denser.june.core.domain.model.enums.FontCategory

data class GoogleFontMetadata(
    val name: String,
    val category: FontCategory
)

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val googleFontsMetadata = listOf(
    // Sans Serif
    GoogleFontMetadata("Inter", FontCategory.SANS_SERIF),
    GoogleFontMetadata("Outfit", FontCategory.SANS_SERIF),
    GoogleFontMetadata("Figtree", FontCategory.SANS_SERIF),
    GoogleFontMetadata("Open Sans", FontCategory.SANS_SERIF),
    GoogleFontMetadata("Montserrat", FontCategory.SANS_SERIF),
    GoogleFontMetadata("Raleway", FontCategory.SANS_SERIF),
    GoogleFontMetadata("Nunito", FontCategory.SANS_SERIF),

    // Serif
    GoogleFontMetadata("Playfair Display", FontCategory.SERIF),
    GoogleFontMetadata("Lora", FontCategory.SERIF),
    GoogleFontMetadata("Merriweather", FontCategory.SERIF),
    GoogleFontMetadata("EB Garamond", FontCategory.SERIF),
    GoogleFontMetadata("Cormorant Garamond", FontCategory.SERIF),
    GoogleFontMetadata("Bitter", FontCategory.SERIF),

    // Handwriting
    GoogleFontMetadata("Caveat", FontCategory.HANDWRITING),
    GoogleFontMetadata("Dancing Script", FontCategory.HANDWRITING),
    GoogleFontMetadata("Pacifico", FontCategory.HANDWRITING),
    GoogleFontMetadata("Indie Flower", FontCategory.HANDWRITING),
    GoogleFontMetadata("Satisfy", FontCategory.HANDWRITING),

    // Monospace & Display
    GoogleFontMetadata("JetBrains Mono", FontCategory.MONOSPACE),
    GoogleFontMetadata("Space Grotesk", FontCategory.MONOSPACE),
    GoogleFontMetadata("Roboto Mono", FontCategory.MONOSPACE),
    GoogleFontMetadata("Abril Fatface", FontCategory.DISPLAY),
    GoogleFontMetadata("Oswald", FontCategory.DISPLAY)
).sortedBy { it.name }

val googleFontsList = googleFontsMetadata.map { it.name }
