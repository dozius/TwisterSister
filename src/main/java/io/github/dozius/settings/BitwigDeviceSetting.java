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
import java.util.UUID;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.Parameter;

import org.tomlj.TomlTable;

/**
 * Bitwig specific device settings.
 *
 * Contains an ID as well as any parameter IDs that were set during construction.
 */
public class BitwigDeviceSetting extends AbstractDeviceSetting<UUID, String>
{
  private BitwigDeviceSetting(UUID id, Map<String, String> params)
  {
    super(id, params);
  }

  /**
   * Constructs a BitwigDeviceSetting object from a TOML table.
   *
   * @param  table The TOML table from which to create the object.
   *
   * @return       A new BitwigDeviceSetting. Throws on errors.
   */
  public static BitwigDeviceSetting fromToml(TomlTable table)
  {
    final UUID id = UUID.fromString(table.getString("id"));

    final Map<String, String> params = new HashMap<>();
    final TomlTable tomlParams = table.getTable("params");

    for (String key : tomlParams.keySet()) {
      final String paramId = tomlParams.getString(key);

      if (paramId == null || paramId.isEmpty()) {
        throw new IllegalArgumentException("Parameter ID must not be null or empty");
      }

      params.put(key, paramId);
    }

    return new BitwigDeviceSetting(id, params);
  }

  @Override
  public Parameter createParameter(Device device, String key)
  {
    return device.createSpecificBitwigDevice(id).createParameter(params.get(key));
  }
}
