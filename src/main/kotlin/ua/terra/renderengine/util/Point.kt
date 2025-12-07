package ua.terra.renderengine.util

/**
 * Represents a 2D point with x and y coordinates of a numeric type.
 * @param N The numeric type for the coordinates
 * @property x The x-coordinate
 * @property y The y-coordinate
 */
open class Point<N : Number>(
    open var x: N,
    open var y: N
) {
    /** Creates a copy of this point */
    open val clone get() = Point(x, y)
}