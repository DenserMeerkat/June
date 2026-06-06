package com.denser.june.core.domain.model.enums

import com.denser.june.core.R

enum class MapStyleProvider(val displayNameRes: Int) {
    MAPTILER(R.string.maptiler),
    STADIA(R.string.stadia),
    CARTO(R.string.carto),
    MAPBOX(R.string.mapbox)
}
