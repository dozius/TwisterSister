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

/**
 * Generic math utilities.
 */
public class MathUtil
{
  // clamp - note we don't use generics here to avoid boxing/unboxing

  /**
   * Clamps the value to be between min and max.
   *
   * @param  val Value to clamp.
   * @param  min Clamp min.
   * @param  max Clamp max.
   *
   * @return     The clamped value.
   */
  public static float clamp(float val, float min, float max)
  {
    return Math.max(min, Math.min(max, val));
  }

  /**
   * Clamps the value to be between min and max.
   *
   * @param  val Value to clamp.
   * @param  min Clamp min.
   * @param  max Clamp max.
   *
   * @return     The clamped value.
   */
  public static int clamp(int val, int min, int max)
  {
    return Math.max(min, Math.min(max, val));
  }

  /**
   * Clamps the value to be between min and max.
   *
   * @param  val Value to clamp.
   * @param  min Clamp min.
   * @param  max Clamp max.
   *
   * @return     The clamped value.
   */
  public static double clamp(double val, double min, double max)
  {
    return Math.max(min, Math.min(max, val));
  }
}
