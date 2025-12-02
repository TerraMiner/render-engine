package ua.terra.renderengine.window

import ua.terra.renderengine.util.Timer

open class Metrics(tickRate: Int) {
    var tpsLag = MeasurementField()
    var videoLag = MeasurementField()
    var pollLag = MeasurementField()
    var sortTime = MeasurementField()
    var engineRenderTime = MeasurementField()

    var framesPerSecond = IncBufferedField()
    var ticksPerSecond = IncBufferedField()
    var totalCommandCount: Int = 0
    var drawCalls: Int = 0

    val timer: Timer = Timer(tickRate)

    fun getStats(): String {
        return "FPS: $framesPerSecond\n" +
            "TPS: $ticksPerSecond\n" +
            "Commands: $totalCommandCount\n" +
            "DrawCalls: $drawCalls\n" +
            "Sort: ${sortTime}ms\n" +
            "Render: ${engineRenderTime}ms\n" +
            "TPS Lag: ${tpsLag}ms\n" +
            "Video Lag: ${videoLag}ms\n" +
            "Poll Lag: ${pollLag}ms"
    }

    class MeasurementField() {
        private var field: Double = .0
        fun measure(block: () -> Unit) {
            val start = System.nanoTime()
            block()
            field = (System.nanoTime() - start) / 1_000_000.0
        }
        fun getValue() = field
        override fun toString() = getValue().toString()
    }

    class IncBufferedField() {
        private var incremental = 0
        private var field: Int = 0
        fun increase() {
            incremental++
        }
        fun flush() {
            field = incremental
            incremental = 0
        }
        fun getValue() = field
        override fun toString() = getValue().toString()
    }
}