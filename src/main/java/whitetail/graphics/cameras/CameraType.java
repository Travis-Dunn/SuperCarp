package whitetail.graphics.cameras;

public enum CameraType {
    NULL,
    /*
        Escape Velocity camera. Stays in x/z plane, rotation is fixed at zero in
        all axes. Defined by its follow target position, angle, and distance
        from target. Zoom is the only player-controllable degree of freedom.
        Uses perspective projection.
    */
    FOLLOW,
    MENU,
    COUNT
}
