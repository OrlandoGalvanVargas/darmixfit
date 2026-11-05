package com.darmix.darmixfit.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ReactorColors(
    val energy: Color,
    val bezelRim: Color,
    val bezelChamfer: Color,
    val bezelFace: Color,
    val housingVoid: Color,
    val tubeDark: Color,
    val channelMid: Color,
    val flowLight: Color,
    val metal: List<Color>,
    val brushLight: Color,
    val bevel: Color,
    val bevelHighlight: Color,
    val windowRim: Color,
    val housingEdge: Color,
    val lip: Color,
    val wellEdge: Color,
    val well: List<Color>,
    val baseHalo: Color,
    val baseGlow: Color,
    val band: Color,
    val slot: Color,
    val slotLit: Color,
    val slotOutline: Color,
    val ringEdge: Color,
    val lens: List<Color>,
    val waveBack: Color,
    val waveBackCrest: Color,
    val waveFront: Color,
    val waveCrestGlow: Color,
    val centerDark: Color,
    val full: List<Color>,
    val specTint: Color,
    val lensRimDark: Color,
    val glassRing: Color,
    val glassTab: Color,
    val rimOuterLight: Color,
    val rimGray: Color,
    val rimLight: Color,
    val liquidRim: List<Color>,
    val number: Color
) {
    fun map(transform: (Color) -> Color): ReactorColors = ReactorColors(
        energy = transform(energy),
        bezelRim = transform(bezelRim),
        bezelChamfer = transform(bezelChamfer),
        bezelFace = transform(bezelFace),
        housingVoid = transform(housingVoid),
        tubeDark = transform(tubeDark),
        channelMid = transform(channelMid),
        flowLight = transform(flowLight),
        metal = metal.map(transform),
        brushLight = transform(brushLight),
        bevel = transform(bevel),
        bevelHighlight = transform(bevelHighlight),
        windowRim = transform(windowRim),
        housingEdge = transform(housingEdge),
        lip = transform(lip),
        wellEdge = transform(wellEdge),
        well = well.map(transform),
        baseHalo = transform(baseHalo),
        baseGlow = transform(baseGlow),
        band = transform(band),
        slot = transform(slot),
        slotLit = transform(slotLit),
        slotOutline = transform(slotOutline),
        ringEdge = transform(ringEdge),
        lens = lens.map(transform),
        waveBack = transform(waveBack),
        waveBackCrest = transform(waveBackCrest),
        waveFront = transform(waveFront),
        waveCrestGlow = transform(waveCrestGlow),
        centerDark = transform(centerDark),
        full = full.map(transform),
        specTint = transform(specTint),
        lensRimDark = transform(lensRimDark),
        glassRing = transform(glassRing),
        glassTab = transform(glassTab),
        rimOuterLight = transform(rimOuterLight),
        rimGray = transform(rimGray),
        rimLight = transform(rimLight),
        liquidRim = liquidRim.map(transform),
        number = transform(number)
    )

    companion object {
        val Volt = ReactorColors(
            energy = DarmixPalette.Volt,
            bezelRim = Color(0xFF6A7461),
            bezelChamfer = Color(0xFF3A4134),
            bezelFace = Color(0xFF22271E),
            housingVoid = Color(0xFF020301),
            tubeDark = Color(0xFF0A0E08),
            channelMid = Color(0xFF7FB51A),
            flowLight = Color(0xFFF2FFD0),
            metal = listOf(
                Color(0xFF40473C),
                Color(0xFF2C3229),
                Color(0xFF363D32),
                Color(0xFF2A3027),
                Color(0xFF1E221B)
            ),
            brushLight = Color(0xFFC9D1BF),
            bevel = Color(0xFFA7B09B),
            bevelHighlight = Color(0xFFE4EADC),
            windowRim = Color(0xFF050604),
            housingEdge = Color(0xFF5A6450),
            lip = Color(0xFF4A5242),
            wellEdge = Color(0xFF0B0E09),
            well = listOf(Color(0xFF0A0C08), Color(0xFF1A1F16), Color(0xFF2A3024)),
            baseHalo = Color(0xFF4E7A00),
            baseGlow = Color(0xFF2F4510),
            band = Color(0xFF3C4437),
            slot = Color(0xFF3E5A0C),
            slotLit = Color(0xFFDBFF7A),
            slotOutline = Color(0xFF1F2D07),
            ringEdge = Color(0xFF0B0D09),
            lens = listOf(Color(0xFF1C330A), Color(0xFF101C07), Color(0xFF050803)),
            waveBack = Color(0xFF86BC18),
            waveBackCrest = Color(0xFFDBFF7A),
            waveFront = Color(0xFFBDF23A),
            waveCrestGlow = Color(0xFFF2FFD0),
            centerDark = Color(0xFF1F3304),
            full = listOf(Color(0xFF4E7A00), Color(0xFF6E9E10), Color(0xFFA6DB2A), Color(0xFFDBFF7A)),
            specTint = Color(0xFFE6FBB8),
            lensRimDark = Color(0xFF050703),
            glassRing = Color(0xFFD9E0D3),
            glassTab = Color(0xFFE9EDE4),
            rimOuterLight = Color(0xFFA9B1A3),
            rimGray = Color(0xFF7E8679),
            rimLight = Color(0xFFE9EDE4),
            liquidRim = listOf(Color(0xFF2C3720), Color(0xFF313E22), Color(0xFF374626), Color(0xFF3E502A)),
            number = Color(0xFFF4F6F1)
        )

        private val Cache: Map<AccentPalette, ReactorColors> = AccentPalette.entries.associateWith { accent ->
            if (accent == AccentPalette.VOLT) Volt else Volt.map { it.toAccent(accent) }
        }

        fun of(accent: AccentPalette): ReactorColors = Cache.getValue(accent)
    }
}

val LocalReactorColors = staticCompositionLocalOf { ReactorColors.Volt }
