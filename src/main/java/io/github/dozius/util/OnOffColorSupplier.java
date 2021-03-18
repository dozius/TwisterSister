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

import java.util.function.Supplier;

import com.bitwig.extension.api.Color;

/** A color supplier that can toggle between two color states, an "on" color and "off" color. */
public class OnOffColorSupplier implements Supplier<Color>
{
  private Color onColor;
  private Color offColor;
  private boolean isOn;

  /**
   * Constructs a new OnOffColorSupplier.
   *
   * @param onColor  The color to use for the "on" state.
   * @param offColor The color to use for the "off" state.
   */
  public OnOffColorSupplier(Color onColor, Color offColor)
  {
    this.onColor = onColor;
    this.offColor = offColor;
  }

  /**
   * Constructs a new OnOffColorSupplier with the "off" state set to black.
   *
   * @param onColor The color to use for the "on" state.
   */
  public OnOffColorSupplier(Color onColor)
  {
    this(onColor, Color.blackColor());
  }

  /** Constructs a new OnOffColorSupplier with the "on" and "off" state set to black. */
  public OnOffColorSupplier()
  {
    this(Color.blackColor(), Color.blackColor());
  }

  /**
   * Sets the color for the "on" state.
   *
   * @param onColor The desired color.
   */
  public void setOnColor(Color onColor)
  {
    this.onColor = onColor;
  }

  /**
   * Sets the color for the "off" state.
   *
   * @param offColor The desired color.
   */
  public void setOffColor(Color offColor)
  {
    this.offColor = offColor;
  }

  /**
   * Sets the state.
   *
   * @param on "on" if true, "off" if false.
   */
  public void setOn(boolean on)
  {
    this.isOn = on;
  }

  /** @return The current state color. */
  @Override
  public Color get()
  {
    return isOn ? onColor : offColor;
  }
}
