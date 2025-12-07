package ua.terra.renderengine.camera

import ua.terra.renderengine.util.Point

/**
 * Interface for objects that the camera can follow.
 */
fun interface CameraTarget {
    /**
     * Returns the point in scene coordinates that the camera should focus on.
     */
    fun getCameraPoint(): Point<Float>
}


