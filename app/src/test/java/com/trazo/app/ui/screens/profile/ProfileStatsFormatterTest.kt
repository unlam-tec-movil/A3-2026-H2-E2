package com.trazo.app.ui.screens.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileStatsFormatterTest {
    @Test
    fun formatDistanceKm_zero_hasNoDecimals() {
        assertEquals("0", ProfileStatsFormatter.formatDistanceKm(0.0))
    }

    @Test
    fun formatDistanceKm_wholeNumber_hasNoDecimals() {
        assertEquals("8", ProfileStatsFormatter.formatDistanceKm(8.0))
    }

    @Test
    fun formatDistanceKm_fraction_hasOneDecimalWithComma() {
        assertEquals("8,5", ProfileStatsFormatter.formatDistanceKm(8.5))
    }

    @Test
    fun formatDistanceKm_roundsToOneDecimal() {
        assertEquals("8,3", ProfileStatsFormatter.formatDistanceKm(8.27))
        assertEquals("9", ProfileStatsFormatter.formatDistanceKm(8.96))
    }

    @Test
    fun formatTotalTime_zero() {
        assertEquals("00:00:00", ProfileStatsFormatter.formatTotalTime(0))
    }

    @Test
    fun formatTotalTime_minutesOnly() {
        assertEquals("00:55:00", ProfileStatsFormatter.formatTotalTime(3_300))
    }

    @Test
    fun formatTotalTime_hoursMinutesSeconds() {
        assertEquals("01:01:01", ProfileStatsFormatter.formatTotalTime(3_661))
    }

    @Test
    fun formatTotalTime_moreThanTenHours() {
        assertEquals("12:34:56", ProfileStatsFormatter.formatTotalTime(45_296))
    }

    @Test
    fun formatTotalTime_moreThanNinetyNineHours_keepsAllHourDigits() {
        assertEquals("100:00:00", ProfileStatsFormatter.formatTotalTime(360_000))
    }
}
