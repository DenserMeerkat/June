package com.denser.june.presentation.utils

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.denser.june.core.domain.model.enums.MapStyleProvider
import com.materialkolor.dynamicColorScheme
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.compose.camera.CameraState

object MapAccentColorizer {

    fun applyThemeAccents(
        style: Style,
        colorScheme: ColorScheme,
        provider: MapStyleProvider,
        isDark: Boolean = false
    ) {
        val scheme = if ((colorScheme.surface.luminance() < 0.5f) != isDark) {
            dynamicColorScheme(primary = colorScheme.primary, isDark = isDark)
        } else {
            colorScheme
        }
        when (provider) {
            MapStyleProvider.CARTO -> applyCartoAccents(style, scheme, isDark)
            MapStyleProvider.STADIA -> applyStadiaAccents(style, scheme, isDark)
            else -> {}
        }
    }

    private fun applyCartoAccents(
        style: Style,
        colorScheme: ColorScheme,
        isDark: Boolean
    ) {
        if (isDark) {
            val landBg = colorScheme.surfaceContainerLowest.toArgb()
            val landcover = colorScheme.surfaceContainerLow.toArgb()
            val residential = colorScheme.surfaceContainer.toArgb()
            val buildings = colorScheme.surfaceContainerHigh.toArgb()
            val buildingOutline = colorScheme.outlineVariant.copy(alpha = 0.20f).toArgb()

            val water = colorScheme.surfaceVariant.toArgb()
            val parks = colorScheme.secondaryContainer.copy(alpha = 0.18f).toArgb()

            val highway = colorScheme.primary.copy(alpha = 0.40f).toArgb()
            val transit = colorScheme.secondary.copy(alpha = 0.35f).toArgb()

            setLayerBackground(style, "background", landBg)
            setLayerFill(style, "landcover", landcover)
            setLayerFill(style, "landuse_residential", residential)
            setLayerFill(style, "building-top", buildings)
            setLayerFillOutline(style, "building-top", buildingOutline)

            setLayerFill(style, "water", water)
            setLayerLine(style, "waterway", water)

            setLayerFill(style, "park_national_park", parks)
            setLayerFill(style, "park_nature_reserve", parks)
            setLayerFill(style, "landuse", parks)

            val cartoHighways = listOf(
                "road_mot_fill_noramp", "road_mot_fill_ramp",
                "road_trunk_fill_noramp", "road_trunk_fill_ramp",
                "bridge_mot_fill", "bridge_trunk_fill",
                "tunnel_mot_fill", "tunnel_trunk_fill"
            )
            cartoHighways.forEach { setLayerLine(style, it, highway) }

            val cartoTransit = listOf("rail", "rail_dash", "tunnel_rail", "tunnel_rail_dash")
            cartoTransit.forEach { setLayerLine(style, it, transit) }
        } else {
            val landBg = colorScheme.surfaceContainerLowest.toArgb()
            val landcover = colorScheme.surfaceContainerLow.toArgb()
            val residential = colorScheme.surfaceContainer.toArgb()
            val buildings = colorScheme.surfaceContainerHigh.toArgb()
            val buildingOutline = colorScheme.outlineVariant.copy(alpha = 0.20f).toArgb()

            val water = colorScheme.surfaceVariant.toArgb()
            val parks = colorScheme.secondaryContainer.copy(alpha = 0.18f).toArgb()

            val highway = colorScheme.primary.copy(alpha = 0.25f).toArgb()
            val transit = colorScheme.secondary.copy(alpha = 0.25f).toArgb()

            setLayerBackground(style, "background", landBg)
            setLayerFill(style, "landcover", landcover)
            setLayerFill(style, "landuse_residential", residential)
            setLayerFill(style, "building-top", buildings)
            setLayerFillOutline(style, "building-top", buildingOutline)

            setLayerFill(style, "water", water)
            setLayerLine(style, "waterway", water)

            setLayerFill(style, "park_national_park", parks)
            setLayerFill(style, "park_nature_reserve", parks)
            setLayerFill(style, "landuse", parks)

            val cartoHighways = listOf(
                "road_mot_fill_noramp", "road_mot_fill_ramp",
                "road_trunk_fill_noramp", "road_trunk_fill_ramp",
                "bridge_mot_fill", "bridge_trunk_fill",
                "tunnel_mot_fill", "tunnel_trunk_fill"
            )
            cartoHighways.forEach { setLayerLine(style, it, highway) }

            val cartoTransit = listOf("rail", "rail_dash", "tunnel_rail", "tunnel_rail_dash")
            cartoTransit.forEach { setLayerLine(style, it, transit) }
        }
    }

    private fun applyStadiaAccents(
        style: Style,
        colorScheme: ColorScheme,
        isDark: Boolean
    ) {
        if (isDark) {
            val landBg = colorScheme.surfaceContainerLowest.toArgb()
            val residential = colorScheme.surfaceContainerLow.toArgb()
            val buildings = colorScheme.surfaceContainer.toArgb()
            val buildingOutline = colorScheme.outlineVariant.copy(alpha = 0.20f).toArgb()

            val water = colorScheme.surfaceDim.toArgb()
            val parks = colorScheme.secondary.copy(alpha = 0.08f).toArgb()
            val highway = colorScheme.surfaceDim.toArgb()
            val transit = colorScheme.outlineVariant.copy(alpha = 0.35f).toArgb()

            setLayerBackground(style, "background", landBg)
            setLayerFill(style, "landuse_residential", residential)
            setLayerFill(style, "building", buildings)
            setLayerFillOutline(style, "building", buildingOutline)

            setLayerFill(style, "water", water)
            setLayerLine(style, "waterway", water)

            setLayerFill(style, "park_fill", parks)
            setLayerFill(style, "landcover_park", parks)
            setLayerFill(style, "landcover_wood", parks)

            val stadiaHighways = listOf(
                "highway_motorway_inner", "highway_major_inner",
                "tunnel_motorway_inner", "highway_motorway_bridge_inner"
            )
            stadiaHighways.forEach { setLayerLine(style, it, highway) }

            val stadiaTransit = listOf(
                "railway", "railway_dashline", "railway_service", "railway_service_dashline"
            )
            stadiaTransit.forEach { setLayerLine(style, it, transit) }
        } else {
            val landBg = colorScheme.surfaceContainerLowest.toArgb()
            val residential = colorScheme.surfaceContainerLow.toArgb()
            val buildings = colorScheme.surfaceContainer.toArgb()
            val buildingOutline = colorScheme.outlineVariant.copy(alpha = 0.20f).toArgb()

            val water = colorScheme.surfaceVariant.toArgb()
            val parks = colorScheme.secondary.copy(alpha = 0.10f).toArgb()
            val highway = colorScheme.primary.copy(alpha = 0.15f).toArgb()
            val transit = colorScheme.secondary.copy(alpha = 0.18f).toArgb()

            setLayerBackground(style, "background", landBg)
            setLayerFill(style, "landuse_residential", residential)
            setLayerFill(style, "building", buildings)
            setLayerFillOutline(style, "building", buildingOutline)

            setLayerFill(style, "water", water)
            setLayerLine(style, "waterway", water)

            setLayerFill(style, "park_fill", parks)
            setLayerFill(style, "landcover_park", parks)
            setLayerFill(style, "landcover_wood", parks)

            val stadiaHighways = listOf(
                "highway_motorway_inner", "highway_major_inner",
                "tunnel_motorway_inner", "highway_motorway_bridge_inner"
            )
            stadiaHighways.forEach { setLayerLine(style, it, highway) }

            val stadiaTransit = listOf(
                "railway", "railway_dashline", "railway_service", "railway_service_dashline"
            )
            stadiaTransit.forEach { setLayerLine(style, it, transit) }
        }
    }

    private fun setLayerBackground(style: Style, layerId: String, color: Int) {
        try {
            val layer = style.getLayer(layerId)
            if (layer is BackgroundLayer) {
                layer.setProperties(PropertyFactory.backgroundColor(color))
            }
        } catch (_: Throwable) {
        }
    }

    private fun setLayerFill(style: Style, layerId: String, color: Int) {
        try {
            val layer = style.getLayer(layerId)
            if (layer is FillLayer) {
                layer.setProperties(PropertyFactory.fillColor(color))
            }
        } catch (_: Throwable) {
        }
    }

    private fun setLayerFillOutline(style: Style, layerId: String, color: Int) {
        try {
            val layer = style.getLayer(layerId)
            if (layer is FillLayer) {
                layer.setProperties(PropertyFactory.fillOutlineColor(color))
            }
        } catch (_: Throwable) {
        }
    }

    private fun setLayerLine(style: Style, layerId: String, color: Int) {
        try {
            val layer = style.getLayer(layerId)
            if (layer is LineLayer) {
                layer.setProperties(PropertyFactory.lineColor(color))
            }
        } catch (_: Throwable) {
        }
    }

    fun CameraState.getMapLibreMap(): MapLibreMap? {
        return try {
            val field = this::class.java.declaredFields.firstOrNull {
                it.name == "mapState" || it.name.startsWith("mapState$")
            } ?: return null
            field.isAccessible = true
            val state = field.get(this) as? androidx.compose.runtime.State<*> ?: return null
            val adapter = state.value ?: return null
            val mapField = adapter::class.java.declaredFields.firstOrNull {
                it.type == MapLibreMap::class.java || it.name == "map"
            } ?: return null
            mapField.isAccessible = true
            mapField.get(adapter) as? MapLibreMap
        } catch (_: Throwable) {
            null
        }
    }
}
