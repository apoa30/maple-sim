package org.ironmaple.simulation.seasonspecific.rebuilt2026;

import static org.wpilib.units.Units.Inches;
import static org.wpilib.units.Units.Meters;

import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Translation2d;
import org.wpilib.math.kinematics.ChassisVelocities;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.Distance;
import org.wpilib.units.measure.LinearVelocity;
import org.ironmaple.simulation.gamepieces.GamePieceProjectile;

/**
 *
 *
 * <h1>Represents an FUEL launched into the air.</h1>
 *
 * <p>This class models a {@link RebuiltFuelOnField} launched into the air.
 *
 * <p>The simulation will determine if the FUEL hits its target—the HUB.
 *
 * <p>The user can specify a callback using {@link #setHitNetCallBack(Runnable)}, which will be triggered when the FUEL
 * hits the HUB.
 */
public class RebuiltFuelOnFly extends GamePieceProjectile {

    public RebuiltFuelOnFly(
            Translation2d robotPosition,
            Translation2d shooterPositionOnRobot,
            ChassisVelocities ChassisVelocities,
            Rotation2d shooterFacing,
            Distance initialHeight,
            LinearVelocity launchingSpeed,
            Angle shooterAngle) {
        super(
                RebuiltFuelOnField.REBUILT_FUEL_INFO,
                robotPosition,
                shooterPositionOnRobot,
                ChassisVelocities,
                shooterFacing,
                initialHeight,
                launchingSpeed,
                shooterAngle);

        super.withTouchGroundHeight(Inches.of(3).in(Meters));
        super.enableBecomesGamePieceOnFieldAfterTouchGround();
    }
}
