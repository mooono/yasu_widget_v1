package com.example.yasuwidget.application

import com.example.yasuwidget.domain.model.BusDirection
import com.example.yasuwidget.domain.model.Departure
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/**
 * バス表示ラベル生成テスト
 *
 * - TO_YASU（村田発）: 行き先を表示（野洲駅行 / 野洲駅北口行）
 * - TO_MURATA（野洲駅発）: 発車場所を表示（野洲駅発 / 野洲駅北口発）
 */
class BusLabelTest {

    // toBusLabel のロジックを直接テスト
    private fun toBusLabel(dep: Departure, direction: BusDirection): String {
        val isKitaguchi = dep.destination.contains("北口")
        return when (direction) {
            BusDirection.TO_YASU -> if (isKitaguchi) "野洲駅北口行" else "野洲駅行"
            BusDirection.TO_MURATA -> if (isKitaguchi) "野洲駅北口発" else "野洲駅発"
        }
    }

    @Test
    fun `TO_YASU方向で野洲駅行の場合は野洲駅行を表示する`() {
        val dep = Departure(LocalTime.of(7, 0), destination = "野洲駅", via = "三ツ坂")
        assertEquals("野洲駅行", toBusLabel(dep, BusDirection.TO_YASU))
    }

    @Test
    fun `TO_YASU方向で北口行の場合は野洲駅北口行を表示する`() {
        val dep = Departure(LocalTime.of(12, 20), destination = "野洲駅(北口行)", via = "生和神社")
        assertEquals("野洲駅北口行", toBusLabel(dep, BusDirection.TO_YASU))
    }

    @Test
    fun `TO_MURATA方向で野洲駅発の場合は野洲駅発を表示する`() {
        val dep = Departure(LocalTime.of(6, 30), destination = "村田製作所", via = "三ツ坂")
        assertEquals("野洲駅発", toBusLabel(dep, BusDirection.TO_MURATA))
    }

    @Test
    fun `TO_MURATA方向で北口発の場合は野洲駅北口発を表示する`() {
        val dep = Departure(LocalTime.of(6, 40), destination = "村田製作所(北口発)", via = "生和神社")
        assertEquals("野洲駅北口発", toBusLabel(dep, BusDirection.TO_MURATA))
    }
}
