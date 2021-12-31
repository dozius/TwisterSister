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

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.MidiOut;

import io.github.dozius.TwisterSisterExtension;
import io.github.dozius.util.MathUtil;

/** Twister light base class. */
public abstract class TwisterLight
{
  /** The available animation states of the light. */
  public static enum AnimationState
  {
   OFF("Off"), // animation off
   STROBE_8_1("Strobe 8/1"), //
   STROBE_4_1("Strobe 4/1"), //
   STROBE_2_1("Strobe 2/1"), //
   STROBE_1_1("Strobe 1/1"), //
   STROBE_1_2("Strobe 1/2"), //
   STROBE_1_4("Strobe 1/4"), //
   STROBE_1_8("Strobe 1/8"), //
   STROBE_1_16("Strobe 1/16"), //
   PULSE_8_1("Pulse 8/1"), //
   PULSE_4_1("Pulse 4/1"), //
   PULSE_2_1("Pulse 2/1"), //
   PULSE_1_1("Pulse 1/1"), //
   PULSE_1_2("Pulse 1/2"), //
   PULSE_1_4("Pulse 1/4"), //
   PULSE_1_8("Pulse 1/8"), //
   PULSE_1_16("Pulse 1/16"), //
   RAINBOW("Rainbow");

    private static final Map<String, AnimationState> optionMap = new HashMap<String, AnimationState>();

    private String optionString;

    static {
      for (AnimationState state : values()) {
        optionMap.put(state.getOptionString(), state);
      }
    }

    /**
     * Constructs an AnimationState
     *
     * @param optionString String that will be used in Bitwig option menus.
     */
    AnimationState(String optionString)
    {
      this.optionString = optionString;
    }

    /**
     * @return The animation state's option string.
     */
    public String getOptionString()
    {
      return optionString;
    }

    /**
     * Gets the enum constant with the specific option string.
     *
     * @param  optionString             The animation state's option string.
     *
     * @return                          The enum constant with the specific option string.
     *
     * @throws IllegalArgumentException If the option string is invalid.
     */
    public static AnimationState valueOfOptionString(String optionString)
    {
      final AnimationState state = optionMap.get(optionString);

      if (state != null) {
        return state;
      }

      throw new IllegalArgumentException("Unknown AnimationState option");
    }

    /**
     * @return An array of all the option strings for this enum.
     */
    public static String[] optionStrings()
    {
      return Arrays.stream(values()).map(AnimationState::getOptionString).toArray(String[]::new);
    }
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
