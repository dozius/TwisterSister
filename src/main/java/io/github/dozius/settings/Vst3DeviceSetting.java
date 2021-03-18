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

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.Parameter;

import org.tomlj.TomlTable;

/**
 * VST3 specific device settings.
 *
 * Contains an ID as well as any parameter IDs that were set during construction.
 */
public class Vst3DeviceSetting extends AbstractDeviceSetting<String, Integer>
{
  private Vst3DeviceSetting(String id, Map<String, Integer> params)
  {
    super(id, params);
  }

  /**
   * Constructs a Vst3DeviceSetting object from a TOML table.
   *
   * @param  table The TOML table from which to create the object.
   *
   * @return       A new Vst3DeviceSetting. Throws on errors.
   */
  public static Vst3DeviceSetting fromToml(TomlTable table)
  {
    final String id = table.getString("id");

    // All VST3 IDs seem to be a 32 character hex strings
    if (id.length() != 32) {
      throw new IllegalArgumentException("VST3 device ID must be 32 characters long");
    }

    final Map<String, Integer> params = new HashMap<>();
    final TomlTable tomlParams = table.getTable("params");

    for (String key : tomlParams.keySet()) {
      final Integer paramId = Math.toIntExact(tomlParams.getLong(key));

      if (paramId < 0) {
        throw new IllegalArgumentException("Parameter ID must not be negative");
      }

      params.put(key, paramId);
    }

    return new Vst3DeviceSetting(id, params);
  }

  @Override
  public Parameter createParameter(Device device, String key)
  {
    return device.createSpecificVst3Device(id).createParameter(params.get(key));
  }
}
