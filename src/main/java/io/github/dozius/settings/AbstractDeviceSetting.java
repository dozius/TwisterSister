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

import java.util.Map;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.Parameter;

/** Base class for specific device settings. */
public abstract class AbstractDeviceSetting<IdType, ParamType>
{
  protected final IdType id;
  protected final Map<String, ParamType> params;

  /**
   * Creates a new AbstractDeviceSetting.
   *
   * @param id     The ID of the device.
   * @param params The device parameters mapped to string keys.
   */
  protected AbstractDeviceSetting(IdType id, Map<String, ParamType> params)
  {
    this.id = id;
    this.params = params;
  }

  /**
   * @return The device ID.
   */
  public IdType id()
  {
    return id;
  }

  /**
   * @return Parameter IDs mapped to string keys.
   */
  public Map<String, ParamType> parameters()
  {
    return params;
  }

  /**
   * Creates a specific device parameter for the device based on the key.
   *
   * @param  device The device from which to create the parameter.
   * @param  key    The key of the parameter to create.
   *
   * @return        The parameter if it exists, otherwise null.
   */
  public abstract Parameter createParameter(Device device, String key);
}
