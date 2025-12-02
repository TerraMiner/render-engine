package ua.terra.renderengine.util

fun getIndex(x: Int, y: Int, maxX: Int, maxY: Int): Int {
    return x.coerceIn(0,maxX-1) + y.coerceIn(0,maxY-1) * maxX
}

fun getCoords(index: Int, maxX: Int, maxY: Int): Point<Int> {
    return Point((index % maxX).coerceIn(0, maxX - 1), (index / maxX).coerceIn(0, maxY - 1))
}