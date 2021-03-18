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

import com.bitwig.extension.api.util.midi.ShortMidiMessage;

/** Contains basic Twister hardware MIDI information. */
public class MidiInfo
{
  public final int channel;
  public final int cc;
  public final int statusByte;

  public MidiInfo(int channel, int cc)
  {
    this.channel = channel;
    this.cc = cc;
    this.statusByte = ShortMidiMessage.CONTROL_CHANGE + channel;
  }
}
