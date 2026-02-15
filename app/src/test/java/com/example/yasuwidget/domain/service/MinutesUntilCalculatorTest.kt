package com.example.yasuwidget.domain.service

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/**
 * MinutesUntilCalculator のユニットテスト
 */
class MinutesUntilCalculatorTest {

    @Test
    fun `発車まで3分の場合は3を返す`() {
        val result = MinutesUntilCalculator.calculate(
            LocalTime.of(7, 12), LocalTime.of(7, 15)
        )
        assertEquals(3, result)
    }

    @Test
    fun `発車まで0分の場合は0を返す`() {
        val result = MinutesUntilCalculator.calculate(
            LocalTime.of(7, 15), LocalTime.of(7, 15)
        )
        assertEquals(0, result)
    }

    @Test
    fun `発車まで60分の場合は60を返す`() {
        val result = MinutesUntilCalculator.calculate(
            LocalTime.of(7, 0), LocalTime.of(8, 0)
        )
        assertEquals(60, result)
    }

    @Test
    fun `発車まで90分の場合は90を返す`() {
        val result = MinutesUntilCalculator.calculate(
            LocalTime.of(7, 0), LocalTime.of(8, 30)
        )
        assertEquals(90, result)
    }

    @Test
    fun `秒の端数は切り捨てられる`() {
        val result = MinutesUntilCalculator.calculate(
            LocalTime.of(7, 12, 30), LocalTime.of(7, 15, 0)
        )
        assertEquals(2, result)
    }

    @Test
    fun `formatTextは正しい形式で表示する`() {
        assertEquals("3分後", MinutesUntilCalculator.formatText(3))
    }

    @Test
    fun `formatTextで0分後を表示する`() {
        assertEquals("0分後", MinutesUntilCalculator.formatText(0))
    }

    @Test
    fun `formatTextで120分後を表示する`() {
        assertEquals("120分後", MinutesUntilCalculator.formatText(120))
    }
}
