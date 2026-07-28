package org.ironmaple.simulation.motorsims;

import static org.wpilib.units.Units.*;

import org.wpilib.units.measure.*;
import org.wpilib.simulation.DCMotorSim;
import org.wpilib.math.system.Models;

/**
 *
 *
 * <h1>{@link org.wpilib.wpilibj.simulation.DCMotorSim} with a bit of extra spice.</h1>
 *
 * <p>This class extends the functionality of the original {@link org.wpilib.wpilibj.simulation.DCMotorSim} and
 * models the following aspects in addition:
 *
 * <ul>
 *   <li>Motor Controller Closed Loops.
 *   <li>Smart current limiting.
 *   <li>Friction force on the rotor.
 * </ul>
 */
public class MapleMotorSim {
    private final SimMotorConfigs configs;

    private final DCMotorSim motorSim;

    private SimulatedMotorController controller;

    /**
     *
     *
     * <h2>Constructs a Brushless Motor Simulation Instance.</h2>
     *
     * @param configs the configuration for this motor
     */
    public MapleMotorSim(SimMotorConfigs configs) {
        this.configs = configs;
        this.controller = (mechanismAngle, mechanismVelocity, encoderAngle, encoderVelocity) -> Volts.of(0);
        this.motorSim = new DCMotorSim(
                Models.singleJointedArmFromPhysicalConstants(
                        configs.motor, 
                        configs.loadMOI.in(KilogramSquareMeters), 
                        configs.gearing
                ),
                configs.motor
        ); // I would probably create the two-state factory manually if this doesn't work
        SimulatedBattery.addMotor(this);
    }

    /**
     *
     *
     * <h2>Updates the simulation.</h2>
     *
     * <p>This is equivalent to{@link org.wpilib.wpilibj.simulation.DCMotorSim#update(double)}.
     */
    private Torque externalTorque = NewtonMeters.zero();

    /**
     *
     *
     * <h2>Updates the simulation.</h2>
     *
     * <p>This is equivalent to{@link org.wpilib.wpilibj.simulation.DCMotorSim#update(double)}.
     */
    public void update(Time dt) {
        double currentPositionRad = motorSim.getAngularPosition(); 
        double currentVelocityRadPerSec = motorSim.getAngularVelocity();

        // Calculate the scaled mechanism velocities using standard primitive * operators
        double mechanismVelocityRadPerSec = currentVelocityRadPerSec * configs.gearing;

        // 2. Wrap the doubles back into unit objects for your controller
        var appliedVoltage = controller.updateControlSignal(
                Radians.of(currentPositionRad),
                RadiansPerSecond.of(currentVelocityRadPerSec),
                Radians.of(0.0), // Fill with your actual target angle if you have one
                RadiansPerSecond.of(mechanismVelocityRadPerSec) 
        );

        appliedVoltage = SimulatedBattery.clamp(appliedVoltage);

        // 3. Keep voltage alterations primitive-safe
        double voltageVolts = appliedVoltage.in(Volts);
        double frictionVolts = configs.getFrictionVoltage().in(Volts);

        if (Math.abs(voltageVolts) < frictionVolts) {
            appliedVoltage = Volts.zero();
        } else if (voltageVolts > 0.0) {
            appliedVoltage = Volts.of(voltageVolts - frictionVolts);
        } else if (voltageVolts < 0.0) {
            appliedVoltage = Volts.of(voltageVolts + frictionVolts);
        }

        motorSim.setInputVoltage(appliedVoltage.in(Volts));
        motorSim.update(dt.in(Seconds));

        // Apply external torque (manual integration)
        // alpha = torque / inertia
        // w = w + alpha * dt
        if (externalTorque.magnitude() > 1e-6) {
            double torqueNm = externalTorque.in(NewtonMeters);
            double gearing = configs.gearing;

            // We apply torque to the LOAD.
            // alpha_load = T_load / J_load
            double alphaLoad = torqueNm / configs.loadMOI.in(KilogramSquareMeters);
            double deltaOmegaLoad = alphaLoad * dt.in(Seconds);
            double deltaOmegaMotor = deltaOmegaLoad * gearing;

            // Note: If motorSim uses Units (implied by linter), we must check the setter
            // too.
            // But the linter says Setter takes double.
            // And Getter returns AngularVelocity.
            // So we convert Getter to double, add delta, pass to Setter.
            motorSim.setAngularVelocity(motorSim.getAngularVelocity() + deltaOmegaMotor);
            motorSim.setAngle(motorSim.getAngularPosition() + deltaOmegaMotor * dt.in(Seconds));
        }

        double currentPos = motorSim.getAngularPosition();
        double reverseLimit = configs.reverseHardwareLimit.in(Radians);
        double forwardLimit = configs.forwardHardwareLimit.in(Radians);

        if (currentPos <= reverseLimit) {
            motorSim.setAngle(reverseLimit);
            motorSim.setAngularVelocity(0);
        } else if (currentPos >= forwardLimit) {
            motorSim.setAngle(forwardLimit);
            motorSim.setAngularVelocity(0);
        }
    }

    /**
     *
     *
     * <h2>Sets an External Torque on the Receiver.</h2>
     *
     * <p>Simulates an external load (gravity, contact, etc.) acting on the mechanism. The torque is applied for the
     * duration of the next update cycle.
     *
     * @param torque the torque to apply (positive = same direction as motor positive)
     */
    public void setExternalTorque(Torque torque) {
        this.externalTorque = torque;
    }

    public <T extends SimulatedMotorController> T useMotorController(T motorController) {
        this.controller = motorController;
        return motorController;
    }

    public SimulatedMotorController.GenericMotorController useSimpleDCMotorController() {
        return useMotorController(new SimulatedMotorController.GenericMotorController(configs.motor));
    }

    /**
     *
     *
     * <h2>Obtains the <strong>final</strong> position of the mechanism.</h2>
     *
     * <p>This is equivalent to {@link org.wpilib.wpilibj.simulation.DCMotorSim#getAngularPosition()}.
     *
     * @return the angular position of the mechanism, continuous
     */
    public Angle getAngularPosition() {
        return Radians.of(motorSim.getAngularPosition());
    }

    /**
     *
     *
     * <h2>Obtains the angular position measured by the relative encoder of the motor.</h2>
     *
     * @return the angular position measured by the encoder, continuous
     */
    public Angle getEncoderPosition() {
        return getAngularPosition().times(configs.gearing);
    }

    /**
     *
     *
     * <h2>Obtains the <strong>final</strong> velocity of the mechanism.</h2>
     *
     * <p>This is equivalent to {@link org.wpilib.wpilibj.simulation.DCMotorSim#getAngularVelocity()}.
     *
     * @return the final angular velocity of the mechanism
     */
    public AngularVelocity getVelocity() {
        return RadiansPerSecond.of(motorSim.getAngularVelocity());
    }

    /**
     *
     *
     * <h2>Obtains the angular velocity measured by the relative encoder of the motor.</h2>
     *
     * @return the angular velocity measured by the encoder
     */
    public AngularVelocity getEncoderVelocity() {
        return getVelocity().times(configs.gearing);
    }

    /**
     *
     *
     * <h2>Obtains the applied voltage by the motor controller.</h2>
     *
     * <p>The applied voltage is calculated by the motor controller in the previous call to {@link #update(Time)}
     *
     * <p>The motor controller specified by {@link #useMotorController(SimulatedMotorController)} is used to calculate
     * the applied voltage.
     *
     * <p>The applied voltage is also restricted for current limit and battery voltage.
     *
     * @return the applied voltage
     */
    public Voltage getAppliedVoltage() {
        return Volts.of(motorSim.getInputVoltage());
    }

    /**
     *
     *
     * <h2>Obtains the <strong>stator</strong> current.</h2>
     *
     * <p>This is equivalent to {@link DCMotorSim#getCurrentDrawAmps()}
     *
     * @return the stator current of the motor
     */
    public Current getStatorCurrent() {
        return Amps.of(motorSim.getCurrentDraw());
    }

    /**
     *
     *
     * <h2>Obtains the <strong>supply</strong> current.</h2>
     *
     * <p>The supply current is different from the stator current, as described <a href=
     * 'https://www.chiefdelphi.com/t/current-limiting-talonfx-values/374780/10'>here</a>.
     *
     * @return the supply current of the motor
     */
    public Current getSupplyCurrent() {
        // Supply Power = Stator Power (Conservation of Energy)
        // Hence,
        // Battery Voltage x Supply Current = Applied Voltage x Stator Current
        // Supply Current = Stator Current * Applied Voltage / Battery Voltage
        return getStatorCurrent().times(getAppliedVoltage().div(SimulatedBattery.getBatteryVoltage()));
    }

    /**
     *
     *
     * <h2>Obtains the configuration of the motor.</h2>
     *
     * <p>You can modify the configuration of this motor by:
     *
     * <pre>
     * <code>
     *     mapleMotorSim.getConfigs()
     *          .with...(...)
     *          .with...(...);
     * </code>
     * </pre>
     *
     * @return the configuration of the motor
     */
    public SimMotorConfigs getConfigs() {
        return this.configs;
    }
}
