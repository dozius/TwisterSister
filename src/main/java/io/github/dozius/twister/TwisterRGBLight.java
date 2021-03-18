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

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;

import io.github.dozius.TwisterSisterExtension;

/** The RGB light on a twister knob. */
public class TwisterRGBLight extends TwisterLight
{
  private static final int ANIMATION_START_VALUE = 1;
  private static final int BRIGHTNESS_START_VALUE = 17;

  private final MultiStateHardwareLight light;
  private final MidiInfo midiInfo;

  /**
   * Creates a new TwisterRGBLight.
   *
   * @param extension     The parent Bitwig extension.
   * @param lightMidiInfo MIDI information for the light.
   */
  public TwisterRGBLight(TwisterSisterExtension extension, LightMidiInfo lightMidiInfo)
  {
    this(extension, lightMidiInfo, Color.nullColor());
  }

  /**
   * Creates a new TwisterRGBLight and sets the color.
   *
   * @param extension     The parent Bitwig extension.
   * @param lightMidiInfo MIDI information for the light.
   * @param color         Color to set the light.
   */
  public TwisterRGBLight(TwisterSisterExtension extension, LightMidiInfo lightMidiInfo, Color color)
  {
    super(extension, lightMidiInfo.animation, ANIMATION_START_VALUE, BRIGHTNESS_START_VALUE);

    midiInfo = lightMidiInfo.light;

    light = extension.hardwareSurface.createMultiStateHardwareLight("RGB Light " + midiInfo.cc);

    light.setColorToStateFunction(col -> new LightState(col));
    light.state().onUpdateHardware(new LightStateSender(midiOut, midiInfo));
    light.setColor(color);
  }

  /**
   * Sets the color of the light.
   *
   * @param color Desired color.
   */
  public void setColor(Color color)
  {
    light.setColor(color);
  }

  /**
   * Sets the color of the light using a raw MIDI value.
   *
   * @param value Desired color as a MIDI value.
   */
  public void setRawValue(int value)
  {
    light.setColor(TwisterColors.ALL.get(value));
  }

  /**
   * Sets the color supplier for the light.
   *
   * @param colorSupplier Color supplier for the light.
   */
  public void setColorSupplier(Supplier<Color> colorSupplier)
  {
    light.setColorSupplier(colorSupplier);
  }

  @Override
  public void lightOff()
  {
    setAnimationState(AnimationState.OFF);

    light.setColor(Color.blackColor());

    // Force MIDI to be sent immediately
    midiOut.sendMidi(midiInfo.statusByte, midiInfo.cc, 0);
  }

  /** Handler of internal light state. */
  private class LightState extends InternalHardwareLightState
  {
    private int colorIndex = 0;
    private Color color = Color.nullColor();

    /**
     * Creates a LightState with the desired color.
     *
     * @param color Desired color.
     */
    public LightState(Color color)
    {
      colorToState(color);
    }

    @Override
    public HardwareLightVisualState getVisualState()
    {
      return HardwareLightVisualState.createForColor(color);
    }

    @Override
    public boolean equals(final Object obj)
    {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      return colorIndex == ((LightState) obj).getColorIndex();
    }

    /**
     * Converts a color to the the nearest representable color of the twister.
     *
     * @param color Desired color.
     */
    public void colorToState(Color color)
    {
      // Find if an exact match exists and use this MIDI value if it does
      int existingIndex = TwisterColors.ALL.indexOf(color);

      if (existingIndex >= 0) {
        colorIndex = existingIndex;
        this.color = color;
        return;
      }

      // No exact match found, proceed with approximation
      final int r = color.getRed255();
      final int g = color.getGreen255();
      final int b = color.getBlue255();

      // hue = 0, saturation = 1, brightness = 2
      final float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
      final float hue = hsb[0];
      final float saturation = hsb[1];

      /*
       * 0 turns off the color override and returns to the inactive color set via sysex. Both 126 &
       * 127 enable override but set the color to the active color set via sysex. Seems that the
       * inclusion of 126 for this behaviour is a bug.
       *
       * ref: process_sw_rgb_update() in encoders.c
       */
      if (saturation > 0.0) {
        final double baseSaturation = 2.0 / 3.0; // RGB 0000FF
        colorIndex = Math.min(Math.floorMod((int) (125 * (baseSaturation - hue) + 1), 126), 125);
        this.color = color;
      }
      else {
        colorIndex = 0; // Desaturated colors turn off LED
        this.color = Color.blackColor();
      }
    }

    /** @return Twister color index of the current color state. */
    public int getColorIndex()
    {
      return colorIndex;
    }
  }

  /** Consumer that sends the light state to the twister. */
  private class LightStateSender implements Consumer<LightState>
  {
    private final MidiOut midiOut;
    private final MidiInfo midiInfo;

    /**
     * Creates a LightStateSender.
     *
     * @param midiOut  MIDI output port to use.
     * @param midiInfo MIDI info for the light.
     */
    protected LightStateSender(final MidiOut midiOut, final MidiInfo midiInfo)
    {
      super();
      this.midiOut = midiOut;
      this.midiInfo = midiInfo;
    }

    @Override
    public void accept(final LightState state)
    {
      if (state == null) {
        return;
      }

      midiOut.sendMidi(midiInfo.statusByte, midiInfo.cc, state.getColorIndex());
    }
  }
}
