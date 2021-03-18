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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.ActionCategory;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;

/** Extension development utilities. */
public class DevUtil
{
  /**
   * Dumps all the available Bitwig actions to a text file.
   *
   * @param  host        The extension controller host.
   * @param  outputFile  The path of the output file.
   *
   * @throws IOException
   */
  public static void dumpBitwigActions(ControllerHost host, Path outputFile) throws IOException
  {
    final Application app = host.createApplication();
    final List<String> output = new ArrayList<>();

    for (ActionCategory actionCat : app.getActionCategories()) {
      output.add(actionCat.getName());
      for (Action action : actionCat.getActions()) {
        output.add("--- ID: " + action.getId() + " Name: " + action.getName());
      }
    }

    Files.write(outputFile, output, StandardCharsets.UTF_8);
  }
}
