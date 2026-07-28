package org.ironmaple.utils;

import static org.wpilib.units.Units.Inches;
import static org.wpilib.units.Units.Meters;

import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Translation2d;
import org.wpilib.math.geometry.Translation3d;
import org.wpilib.driverstation.Alliance;
import org.wpilib.driverstation.MatchState;

import java.util.Optional;

public class FieldMirroringUtils {
    public static final double FIELD_WIDTH = Inches.of(651).in(Meters);
    public static final double FIELD_HEIGHT = Inches.of(315).in(Meters);

    public static Rotation2d toCurrentAllianceRotation(Rotation2d rotationAtBlueSide) {
        return isSidePresentedAsRed() ? flip(rotationAtBlueSide) : rotationAtBlueSide;
    }

    public static Rotation2d flip(Rotation2d rotation) {
        return rotation.plus(Rotation2d.k180deg);
    }

    public static Translation2d toCurrentAllianceTranslation(Translation2d translationAtBlueSide) {
        return isSidePresentedAsRed() ? flip(translationAtBlueSide) : translationAtBlueSide;
    }

    public static Translation2d flip(Translation2d translation) {
        return new Translation2d(FIELD_WIDTH - translation.getX(), FIELD_HEIGHT - translation.getY());
    }

    public static Pose3d flip(Pose3d toFlip) {
        var flipped = flip(toFlip.getTranslation().toTranslation2d());
        return new Pose3d(flipped.getX(), flipped.getY(), toFlip.getZ(), toFlip.getRotation());
    }

    public static Translation3d toCurrentAllianceTranslation(Translation3d translation3dAtBlueSide) {
        final Translation2d translation3dAtCurrentAlliance =
                toCurrentAllianceTranslation(translation3dAtBlueSide.toTranslation2d());
        if (isSidePresentedAsRed())
            return new Translation3d(
                    translation3dAtCurrentAlliance.getX(),
                    translation3dAtCurrentAlliance.getY(),
                    translation3dAtBlueSide.getZ());
        return translation3dAtBlueSide;
    }

    public static Pose2d toCurrentAlliancePose(Pose2d poseAtBlueSide) {
        return new Pose2d(
                toCurrentAllianceTranslation(poseAtBlueSide.getTranslation()),
                toCurrentAllianceRotation(poseAtBlueSide.getRotation()));
    }

    public static boolean isSidePresentedAsRed() {
        final Optional<Alliance> alliance = MatchState.getAlliance();
        return alliance.isPresent() && alliance.get().equals(Alliance.RED);
    }

    public static Rotation2d getCurrentAllianceDriverStationFacing() {
        return toCurrentAllianceRotation(Rotation2d.kZero);
    }
}
