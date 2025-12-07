package ua.terra.renderengine.window

import ua.terra.renderengine.util.Timer

/**
 * Performance metrics tracker for the render engine.
 * Collects and reports FPS, TPS, and various timing measurements.
 * @property tickRate Target ticks per second
 */
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

    /**
     * Returns formatted statistics string with all metrics.
     */
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
}

