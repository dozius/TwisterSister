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
package io.github.dozius.settings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

/**
 * Represents data loaded from a specific devices settings file.
 *
 * This data can be used to programmatically setup specific devices using the specific device
 * portion of the extension API. Since the API provides no way to query device and param IDs, this
 * allows for more flexibility than a hardcoded approach to specific device setup.
 *
 * The TOML file is required to a have a specific structure.
 *
 * A table called "controls". This table can have any number of string array keys. These string
 * arrays are loaded into the controlMap. This table is optional.
 *
 * Three table arrays, "bitwig", "vst3" and "vst2". These represent all the device settings. These
 * arrays are optional.
 *
 * Each table requires an id key that is equal to the device ID taken from Bitwig. For Bitwig and
 * VST3 devices it is a string, for VST2 it is an integer.
 *
 * Each table has a sub table called "params". This table contains any number of keys that are equal
 * to parameter IDs taken from Bitwig. Bitwig device parameters are strings, VST3 and VST2 device
 * parameters are integers.
 *
 * Example:
 *
 * <pre>
 * [controls]
 * knob1 = ["mix"]
 * knob2 = ["output_gain", "decay_time"]
 *
 * [[bitwig]]
 * id = "b5b2b08e-730e-4192-be71-f572ceb5069b"
 * params.mix = "MIX"
 * params.output_gain = "LEVEL"
 *
 * [[vst3]]
 * id = "5653547665653376616C68616C6C6176"
 * params.mix = 48
 * params.decay_time = 50
 *
 * [[vst2]]
 * id = 1315513406
 * params.mix = 11
 * </pre>
 *
 * These table arrays end up as the bitwigDevices, vst3Devices and vst2Devices lists.
 */
public class SpecificDeviceSettings
{
  private final List<BitwigDeviceSetting> bitwigDevices;
  private final List<Vst3DeviceSetting> vst3Devices;
  private final List<Vst2DeviceSetting> vst2Devices;

  /**
   * Constructs a SpecificDeviceSettings object from the specified TOML file.
   *
   * @param settingsPath The path to the TOML file to parse.
   */
  public SpecificDeviceSettings(Path settingsPath)
  {
    try {
      final TomlParseResult toml = Toml.parse(settingsPath);

      if (toml.hasErrors()) {
        final ArrayList<String> errorMessages = new ArrayList<>();

        for (final TomlParseError error : toml.errors()) {
          errorMessages.add(error.toString());
        }

        throw new ParseException(String.join("\n", errorMessages), 0);
      }
      bitwigDevices = loadDevices(toml, "bitwig", BitwigDeviceSetting::fromToml);
      vst3Devices = loadDevices(toml, "vst3", Vst3DeviceSetting::fromToml);
      vst2Devices = loadDevices(toml, "vst2", Vst2DeviceSetting::fromToml);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Constructs a SpecificDeviceSettings object from the specified TOML file.
   *
   * @param settingsPath The path to the TOML file to parse as a string.
   */
  public SpecificDeviceSettings(String settingsPath)
  {
    this(Paths.get(settingsPath));
  }

  // /**
  //  * @return Map of controls with parameter keys.
  //  */
  // public Map<String, Set<String>> controlMap()
  // {
  //   return controlMap;
  // }

  /**
   * @return List of Bitwig devices.
   */
  public List<BitwigDeviceSetting> bitwigDevices()
  {
    return bitwigDevices;
  }

  /**
   * @return List of VST3 devices.
   */
  public List<Vst3DeviceSetting> vst3Devices()
  {
    return vst3Devices;
  }

  /**
   * @return List of VST2 devices.
   */
  public List<Vst2DeviceSetting> vst2Devices()
  {
    return vst2Devices;
  }

  /**
   * Loads all the devices of a specific type.
   *
   * @param  <IdType>      The type of the device ID.
   * @param  <SettingType> The settings type for the device.
   * @param  result        The parse result of the TOML config file.
   * @param  key           The key of the array of device tables.
   * @param  deviceBuilder A function that will construct the device setting from a TOML table.
   *
   * @return               A list of valid device settings.
   */
  private <IdType, SettingType extends AbstractDeviceSetting<IdType, ?>> List<SettingType> loadDevices(TomlParseResult result,
                                                                                                       String key,
                                                                                                       Function<TomlTable, SettingType> deviceBuilder)
  {
    final TomlArray devices = result.getArray(key);

    if (devices == null) {
      return null;
    }

    final Map<IdType, SettingType> settings = new HashMap<>();

    for (int idx = 0; idx < devices.size(); ++idx) {
      final SettingType device = deviceBuilder.apply(devices.getTable(idx));

      if (settings.containsKey(device.id())) {
        throw new RuntimeException("Duplicate " + key + " device ID found: " + device.id());
      }

      settings.put(device.id(), device);
    }

    return new ArrayList<>(settings.values());
  }
}
