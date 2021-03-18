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

/**
 * Contains MIDI information for a Twister knob. This include the encoder, button and both lights.
 */
public class KnobMidiInfo
{
  public final MidiInfo encoder;
  public final MidiInfo button;
  public final LightMidiInfo rgbLight;
  public final LightMidiInfo ringLight;

  public KnobMidiInfo(MidiInfo encoder, MidiInfo button, LightMidiInfo rgbLight,
                      LightMidiInfo ringLight)
  {
    this.encoder = encoder;
    this.button = button;
    this.rgbLight = rgbLight;
    this.ringLight = ringLight;
  }
}
