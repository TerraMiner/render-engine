package ua.terra.renderengine.util

open class Point<N : Number>(
    open var x: N,
    open var y: N
) {
    open val clone get() = Point(x, y)
}