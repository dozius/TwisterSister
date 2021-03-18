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
package io.github.dozius.util;

import com.bitwig.extension.controller.api.ControllerHost;

/** Global logger intended for debugging. */
public final class Logger
{
  private static ControllerHost host;

  private Logger()
  {
  }

  /**
   * Initialize the Logger. Must be called before any logging can happen.
   *
   * @param controllerHost The extension controller host.
   */
  public static void init(ControllerHost controllerHost)
  {
    host = controllerHost;
  }

  /**
   * Logs a message to the Bitwig console with a timestamp.
   *
   * @param message The message to log.
   */
  public static void logTimeStamped(String message)
  {
    log("[" + java.time.LocalTime.now().toString() + "] " + message);
  }

  /**
   * Logs a message to the Bitwig console.
   *
   * @param message The message to log.
   */
  public static void log(String message)
  {
    if (host != null) {
      host.println(message);
    }
  }
}
