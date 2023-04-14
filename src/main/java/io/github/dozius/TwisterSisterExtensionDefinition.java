/*
 * Copyright 2021-2023 Dan Smith
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
package io.github.dozius;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class TwisterSisterExtensionDefinition extends ControllerExtensionDefinition
{
  private static final UUID DRIVER_ID = UUID.fromString("b4c9dbb6-c8b8-4f79-8c3a-cd43650fab44");

  public TwisterSisterExtensionDefinition()
  {
  }

  @Override
  public String getName()
  {
    return "Twister Sister";
  }

  @Override
  public String getAuthor()
  {
    return "Dan Smith";
  }

  @Override
  public String getVersion()
  {
    return "2.0.1";
  }

  @Override
  public UUID getId()
  {
    return DRIVER_ID;
  }

  @Override
  public String getHardwareVendor()
  {
    return "DJ TechTools";
  }

  @Override
  public String getHardwareModel()
  {
    return "MIDI Fighter Twister";
  }

  @Override
  public int getRequiredAPIVersion()
  {
    return 15;
  }

  @Override
  public int getNumMidiInPorts()
  {
    return 1;
  }

  @Override
  public int getNumMidiOutPorts()
  {
    return 1;
  }

  @Override
  public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                             final PlatformType platformType)
  {
    switch (platformType)
    {
      case WINDOWS:
      case MAC:
        list.add(new String[] {"Midi Fighter Twister"}, new String[] {"Midi Fighter Twister"});
        break;

      case LINUX:
        list.add(new String[] {"Midi Fighter Twister MIDI 1"},
                 new String[] {"Midi Fighter Twister MIDI 1"});
        break;
    }
  }

  @Override
  public TwisterSisterExtension createInstance(final ControllerHost host)
  {
    return new TwisterSisterExtension(this, host);
  }

  @Override
  public String getHelpFilePath()
  {
    return "https://github.com/dozius/TwisterSister/tree/main/docs";
  }
}
