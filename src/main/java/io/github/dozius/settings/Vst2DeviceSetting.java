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
 * VST2 specific device settings.
 *
 * Contains an ID as well as any parameter IDs that were set during construction.
 */
public class Vst2DeviceSetting extends AbstractDeviceSetting<Integer, Integer>
{
  private Vst2DeviceSetting(Integer id, Map<String, Integer> params)
  {
    super(id, params);
  }

  /**
   * Constructs a Vst2DeviceSetting object from a TOML table.
   *
   * @param  table The TOML table from which to create the object.
   *
   * @return       A new Vst2DeviceSetting. Throws on errors.
   */
  public static Vst2DeviceSetting fromToml(TomlTable table)
  {
    final Integer id = Math.toIntExact(table.getLong("id"));

    // All VST2 IDs seem to be positive integers
    if (id < 0) {
      throw new IllegalArgumentException("VST2 device ID must not be negative");
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

    return new Vst2DeviceSetting(id, params);
  }

  @Override
  public Parameter createParameter(Device device, String key)
  {
    return device.createSpecificVst2Device(id).createParameter(params.get(key));
  }
}
