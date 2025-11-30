package ua.terra.renderengine.window

import ua.terra.renderengine.util.Timer

class Metrics(tickRate: Int) {
    var tpsLag = .0
    var videoLag = .0
    var pollLag = .0
    var framesPerSecond: Int = 0
    var ticksPerSecond: Int = 0

    val timer: Timer = Timer(tickRate)
}