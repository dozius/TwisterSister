/*
 * Copyright 2021 Dan Smith
 *
 * This file is part of Twister Sister.
 *
 * Twister Sister is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Twister Sister is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Twister
 * Sister. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package io.github.dozius.twister;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.DoubleValue;
import com.bitwig.extension.controller.api.HardwareBindable;
import com.bitwig.extension.controller.api.RelativeHardwareControlBinding;

import io.github.dozius.TwisterSisterExtension;

/** A twister knob, including encoder, shift encoder, button and lights. */
public class TwisterKnob
{
  private static final int FULL_ROTATION = 127;

  private final RelativeHardwareKnob knob;
  private final TwisterButton button;
  private final TwisterRGBLight rgbLight;
  private final TwisterRingLight ringLight;
  private final RelativeHardwareKnob shiftKnob;
  private final TwisterRingLight shiftRingLight;

  private boolean isFineSensitivity = false;
  private double fineSensitivity = 0.25;
  private double sensitivity = 1.0;

  /**
   * Creates a new TwisterKnob.
   *
   * @param extension The parent Bitwig extension.
   * @param midiInfo  MIDI info for the knob.
   */
  public TwisterKnob(TwisterSisterExtension extension, KnobMidiInfo midiInfo)
  {
    this(extension, midiInfo, Color.nullColor());
  }

  /**
   * Creates a new TwisterKnob and sets the RGB light color.
   *
   * @param extension The parent Bitwig extension.
   * @param midiInfo  MIDI info for the knob.
   * @param color     Desired light color.
   */
  public TwisterKnob(TwisterSisterExtension extension, KnobMidiInfo midiInfo, Color color)
  {
    final int cc = midiInfo.encoder.cc;
    final int channel = midiInfo.encoder.channel;

    knob = extension.hardwareSurface.createRelativeHardwareKnob("Knob " + cc);
    knob.setAdjustValueMatcher(extension.midiIn.createRelativeBinOffsetCCValueMatcher(channel, cc,
                                                                                      FULL_ROTATION));

    button = new TwisterButton(extension, midiInfo.button, "Knob");
    rgbLight = new TwisterRGBLight(extension, midiInfo.rgbLight, color);
    ringLight = new TwisterRingLight(extension, midiInfo.ringLight);

    final int shiftChannel = Twister.MidiChannel.SHIFT;

    shiftKnob = extension.hardwareSurface.createRelativeHardwareKnob("Shift Knob " + cc);
    shiftKnob.setAdjustValueMatcher(extension.midiIn.createRelativeBinOffsetCCValueMatcher(shiftChannel,
                                                                                           cc,
                                                                                           FULL_ROTATION));
    shiftRingLight = new TwisterRingLight(extension,
                                          new LightMidiInfo(shiftChannel,
                                                            midiInfo.ringLight.animation.channel,
                                                            cc));
  }

  /** The associated button for this knob. */
  public TwisterButton button()
  {
    return button;
  }

  /** The associated RGB light for this knob. */
  public TwisterRGBLight rgbLight()
  {
    return rgbLight;
  }

  /** The associated ring light for this knob. */
  public TwisterRingLight ringLight()
  {
    return ringLight;
  }

  /** The associated shift ring light for this knob. */
  public TwisterRingLight shiftRingLight()
  {
    return shiftRingLight;
  }

  /** The value of the target that this knob has been bound to (0-1). */
  public DoubleValue targetValue()
  {
    return knob.targetValue();
  }

  /** The value of the target that this shift knob has been bound to (0-1). */
  public DoubleValue shiftTargetValue()
  {
    return shiftKnob.targetValue();
  }

  /**
   * Sets the regular sensitivity factor for the knob. If regular sensitivity is active it is
   * applied immediately.
   *
   * @param factor The sensitivity factor to apply.
   */
  public void setSensitivity(double factor)
  {
    sensitivity = factor;

    if (!isFineSensitivity) {
      knob.setSensitivity(sensitivity);
    }
  }

  /**
   * Sets the fine sensitivity factor for the knob. If fine sensitivity is active it is applied
   * immediately.
   *
   * @param factor The sensitivity factor to apply.
   */
  public void setFineSensitivity(double factor)
  {
    fineSensitivity = factor;

    if (isFineSensitivity) {
      knob.setSensitivity(fineSensitivity);
    }
  }

  /** Toggles between regular and fine sensitivity. */
  public void toggleSensitivity()
  {
    isFineSensitivity = !isFineSensitivity;
    knob.setSensitivity(isFineSensitivity ? fineSensitivity : sensitivity);
  }

  /**
   * Binds the knob to the supplied target.
   *
   * @param  target Target to bind.
   *
   * @return        The created binding.
   */
  public RelativeHardwareControlBinding setBinding(HardwareBindable target)
  {
    return knob.setBinding(target);
  }

  /**
   * Binds the shift knob to the supplied target.
   *
   * @param  target Target to bind.
   *
   * @return        The created binding.
   */
  public RelativeHardwareControlBinding setShiftBinding(HardwareBindable target)
  {
    return shiftKnob.setBinding(target);
  }

  /** Resets all animations and turns off all lights. */
  public void lightsOff()
  {
    ringLight.lightOff();
    shiftRingLight.lightOff();
    rgbLight.lightOff();
  }
}
