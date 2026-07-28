package org.ironmaple.simulation.seasonspecific.rebuilt2026;

import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Translation2d;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.Distance;
import org.wpilib.units.measure.LinearVelocity;
import org.ironmaple.simulation.Arena;

public interface Arena2026 extends Arena {
    boolean isActive(boolean isBlue);

    void addPieceWithVariance(
            Translation2d piecePose,
            Rotation2d yaw,
            Distance height,
            LinearVelocity speed,
            Angle pitch,
            double xVariance,
            double yVariance,
            double yawVariance,
            double speedVariance,
            double pitchVariance);

    void outpostDump(boolean isBlue);

    void outpostThrowForGoal(boolean isBlue);

    void outpostThrow(boolean isBlue, Rotation2d throwYaw, Angle throwPitch, LinearVelocity speed);
}
