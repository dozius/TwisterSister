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

import com.bitwig.extension.controller.api.DoubleValue;
import com.bitwig.extension.controller.api.SettableRangedValue;

import io.github.dozius.TwisterSisterExtension;
import io.github.dozius.util.CursorNormalizedValue;
import io.github.dozius.util.MathUtil;

/** The ring light on a Twister knob. */
public class TwisterRingLight extends TwisterLight
{
  private static final int ANIMATION_START_VALUE = 49;
  private static final int BRIGHTNESS_START_VALUE = 65;

  private final MidiInfo midiInfo;

  /**
   * Creates a new TwisterRingLight.
   *
   * @param extension     The parent bitwig extension.
   * @param lightMidiInfo Midi information for the light.
   */
  public TwisterRingLight(TwisterSisterExtension extension, LightMidiInfo lightMidiInfo)
  {
    super(extension, lightMidiInfo.animation, ANIMATION_START_VALUE, BRIGHTNESS_START_VALUE);
    this.midiInfo = lightMidiInfo.light;
  }

  /**
   * Sets the ring value using raw MIDI data.
   *
   * @param value The desired ring value in MIDI.
   */
  public void setRawValue(int value)
  {
    midiOut.sendMidi(midiInfo.statusByte, midiInfo.cc, MathUtil.clamp(value, 0, 127));
  }

  /**
   * Sets the ring value using a normalized 0-1 range.
   *
   * @param value The desired ring value in normalized range.
   */
  public void setValue(double value)
  {
    setRawValue((int) (value * 127.0));
  }

  /**
   * A special normalized 0-1 range value for ring lights being used as cursors in "dot" mode.
   *
   * Intended to be used with the value from a CursorNormalizedValue wrapper.
   *
   * Values in the 0-1 range will show the dot, even when at 0. Negative values hide the dot.
   *
   * @param value The cursor value to set.
   */
  public void setCursorValue(double value)
  {
    if (value < 0) {
      setRawValue(0);
      return;
    }

    setRawValue((int) ((value * 126.0) + 1.0));
  }

  /**
   * Makes this light an observer of the passed in value.
   *
   * @param value The value to observe.
   */
  public void observeValue(SettableRangedValue value)
  {
    value.markInterested();
    value.addValueObserver(128, this::setRawValue);
  }

  /**
   * Makes this light an observer of the passed in value.
   *
   * @param value The value to observe.
   */
  public void observeValue(DoubleValue value)
  {
    value.markInterested();
    value.addValueObserver(this::setValue);
  }

  /**
   * Makes this light an observer of the passed in wrapper.
   *
   * @param wrapper The wrapper to observe.
   */
  public void observeValue(CursorNormalizedValue wrapper)
  {
    wrapper.addValueObserver(this::setCursorValue);
  }

  @Override
  public void lightOff()
  {
    setAnimationState(AnimationState.OFF);
    setRawValue(0);
  }
}
