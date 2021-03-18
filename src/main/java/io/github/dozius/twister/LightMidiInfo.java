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

/** Contains MIDI information for a Twister light. */
public class LightMidiInfo
{
  public final MidiInfo light;
  public final MidiInfo animation;

  public LightMidiInfo(int channel, int animationChannel, int cc)
  {
    this.light = new MidiInfo(channel, cc);
    this.animation = new MidiInfo(animationChannel, cc);
  }
}
