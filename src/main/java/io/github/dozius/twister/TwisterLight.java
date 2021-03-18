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

import java.util.EnumMap;

import com.bitwig.extension.controller.api.MidiOut;

import io.github.dozius.TwisterSisterExtension;
import io.github.dozius.util.MathUtil;

/** Twister light base class. */
public abstract class TwisterLight
{
  /** The available animation states of the light. */
  public static enum AnimationState
  {
   OFF, // animation off
   STROBE_8_1, // 8/1
   STROBE_4_1, // 4/1
   STROBE_2_1, // 2/1
   STROBE_1_1, // 1/1
   STROBE_1_2, // 1/2
   STROBE_1_4, // 1/4
   STROBE_1_8, // 1/8
   STROBE_1_16, // 1/16
   PULSE_8_1, // 8/1
   PULSE_4_1, // 4/1
   PULSE_2_1, // 2/1
   PULSE_1_1, // 1/1
   PULSE_1_2, // 1/2
   PULSE_1_4, // 1/4
   PULSE_1_8, // 1/8
   PULSE_1_16, // 1/16
   RAINBOW;
  }

  protected final MidiOut midiOut;

  private final MidiInfo midiInfo;
  private final EnumMap<AnimationState, Integer> animationMap;
  private final int brightnessStartValue;

  /**
   * Creates a new TwisterLight.
   *
   * @param extension            The parent Bitwig extension.
   * @param animationMidiInfo    MIDI information for animations.
   * @param animationStartValue  Animation range starting value.
   * @param brightnessStartValue Brightness range starting value.
   */
  public TwisterLight(TwisterSisterExtension extension, MidiInfo animationMidiInfo,
                      int animationStartValue, int brightnessStartValue)
  {
    this.midiOut = extension.midiOut;
    this.midiInfo = animationMidiInfo;
    this.animationMap = createAnimationMap(animationStartValue);
    this.brightnessStartValue = brightnessStartValue;
  }

  /**
   * Sets the desired animation state.
   *
   * @param state Animation state.
   */
  public void setAnimationState(AnimationState state)
  {
    assert animationMap.containsKey(state) : "Invalid state";
    sendAnimation(animationMap.get(state));
  }

  /**
   * Overrides the brightness setting with a new brightness value.
   *
   * @param brightness The desired brightness.
   */
  public void overrideBrightness(double brightness)
  {
    final double value = MathUtil.clamp(brightness, 0.0, 1.0);
    sendAnimation((int) ((value * 30.0) + brightnessStartValue));
  }

  /** Resets the brightness override. */
  public void resetBrightness()
  {
    sendAnimation(0);
  }

  /** Turns the light off and resets all animations. */
  public abstract void lightOff();

  /**
   * Generates the animation value map based on the start value.
   *
   * The ring light and RGB light have different starting MIDI values for the animations. By
   * providing the starting offset value, the sub class will create the correct value map for
   * itself.
   *
   * @param  startValue Starting value of the animation range.
   *
   * @return            A populated animation value map.
   */
  private static EnumMap<AnimationState, Integer> createAnimationMap(int startValue)
  {
    EnumMap<AnimationState, Integer> map = new EnumMap<>(AnimationState.class);

    map.put(AnimationState.OFF, 0);
    map.put(AnimationState.RAINBOW, 127);

    int i = startValue;
    for (AnimationState state : AnimationState.values()) {
      if (state == AnimationState.OFF || state == AnimationState.RAINBOW) {
        continue;
      }

      map.put(state, i);
      ++i;
    }

    return map;
  }

  /**
   * Sends the raw animation MIDI value to the device.
   *
   * @param value The raw MIDI value.
   */
  private void sendAnimation(int value)
  {
    midiOut.sendMidi(midiInfo.statusByte, midiInfo.cc, value);
  }
}
