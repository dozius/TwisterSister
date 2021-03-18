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
package io.github.dozius.twister;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.bitwig.extension.api.Color;

/**
 * Colors from the Twister source code.
 *
 * Index corresponds to MIDI values 0 to 127.
 *
 * Note that MIDI values 0 and 127 are special values and won't set the device to the color in the
 * list. Value 0 resets the color override and changes back to the "off color" that was set in the
 * MIDI Fighter Utility. Value 127 sets the color to the "on color" that was set in the MIDI Fighter
 * Utility.
 *
 * The manual states that 126 should be an override color, but this not the case due to a bug in the
 * Twister software. https://github.com/DJ-TechTools/Midi_Fighter_Twister_Open_Source/issues/9
 */
public class TwisterColors
{
  // All 128 colors from the Twister source code
  public static final List<Color> ALL = Collections.unmodifiableList(colorList());

  // Colors settable on the Twister that match the Bitwig device remote parameter colors
  public static final List<Color> BITWIG_PARAMETERS = Collections.unmodifiableList(parameterColorList());

  /** Generates the list of all Twister colors. */
  private static List<Color> colorList()
  {
    return Arrays.asList(Color.fromRGB255(0, 0, 0), // 0
                         Color.fromRGB255(0, 0, 255), // 1 - Blue
                         Color.fromRGB255(0, 21, 255), // 2 - Blue (Green Rising)
                         Color.fromRGB255(0, 34, 255), //
                         Color.fromRGB255(0, 46, 255), //
                         Color.fromRGB255(0, 59, 255), //
                         Color.fromRGB255(0, 68, 255), //
                         Color.fromRGB255(0, 80, 255), //
                         Color.fromRGB255(0, 93, 255), //
                         Color.fromRGB255(0, 106, 255), //
                         Color.fromRGB255(0, 119, 255), //
                         Color.fromRGB255(0, 127, 255), //
                         Color.fromRGB255(0, 140, 255), //
                         Color.fromRGB255(0, 153, 255), //
                         Color.fromRGB255(0, 165, 255), //
                         Color.fromRGB255(0, 178, 255), //
                         Color.fromRGB255(0, 191, 255), //
                         Color.fromRGB255(0, 199, 255), //
                         Color.fromRGB255(0, 212, 255), //
                         Color.fromRGB255(0, 225, 255), //
                         Color.fromRGB255(0, 238, 255), //
                         Color.fromRGB255(0, 250, 255), // 21 - End of Blue's Reign
                         Color.fromRGB255(0, 255, 250), // 22 - Green (Blue Fading)
                         Color.fromRGB255(0, 255, 237), //
                         Color.fromRGB255(0, 255, 225), //
                         Color.fromRGB255(0, 255, 212), //
                         Color.fromRGB255(0, 255, 199), //
                         Color.fromRGB255(0, 255, 191), //
                         Color.fromRGB255(0, 255, 178), //
                         Color.fromRGB255(0, 255, 165), //
                         Color.fromRGB255(0, 255, 153), //
                         Color.fromRGB255(0, 255, 140), //
                         Color.fromRGB255(0, 255, 127), //
                         Color.fromRGB255(0, 255, 119), //
                         Color.fromRGB255(0, 255, 106), //
                         Color.fromRGB255(0, 255, 93), //
                         Color.fromRGB255(0, 255, 80), //
                         Color.fromRGB255(0, 255, 67), //
                         Color.fromRGB255(0, 255, 59), //
                         Color.fromRGB255(0, 255, 46), //
                         Color.fromRGB255(0, 255, 33), //
                         Color.fromRGB255(0, 255, 21), //
                         Color.fromRGB255(0, 255, 8), //
                         Color.fromRGB255(0, 255, 0), // 43 - Green
                         Color.fromRGB255(12, 255, 0), // 44 - Green/Red Rising
                         Color.fromRGB255(25, 255, 0), //
                         Color.fromRGB255(38, 255, 0), //
                         Color.fromRGB255(51, 255, 0), //
                         Color.fromRGB255(63, 255, 0), //
                         Color.fromRGB255(72, 255, 0), //
                         Color.fromRGB255(84, 255, 0), //
                         Color.fromRGB255(97, 255, 0), //
                         Color.fromRGB255(110, 255, 0), //
                         Color.fromRGB255(123, 255, 0), //
                         Color.fromRGB255(131, 255, 0), //
                         Color.fromRGB255(144, 255, 0), //
                         Color.fromRGB255(157, 255, 0), //
                         Color.fromRGB255(170, 255, 0), //
                         Color.fromRGB255(182, 255, 0), //
                         Color.fromRGB255(191, 255, 0), //
                         Color.fromRGB255(203, 255, 0), //
                         Color.fromRGB255(216, 255, 0), //
                         Color.fromRGB255(229, 255, 0), //
                         Color.fromRGB255(242, 255, 0), //
                         Color.fromRGB255(255, 255, 0), // 64 - Green + Red (Yellow)
                         Color.fromRGB255(255, 246, 0), // 65 - Red, Green Fading
                         Color.fromRGB255(255, 233, 0), //
                         Color.fromRGB255(255, 220, 0), //
                         Color.fromRGB255(255, 208, 0), //
                         Color.fromRGB255(255, 195, 0), //
                         Color.fromRGB255(255, 187, 0), //
                         Color.fromRGB255(255, 174, 0), //
                         Color.fromRGB255(255, 161, 0), //
                         Color.fromRGB255(255, 148, 0), //
                         Color.fromRGB255(255, 135, 0), //
                         Color.fromRGB255(255, 127, 0), //
                         Color.fromRGB255(255, 114, 0), //
                         Color.fromRGB255(255, 102, 0), //
                         Color.fromRGB255(255, 89, 0), //
                         Color.fromRGB255(255, 76, 0), //
                         Color.fromRGB255(255, 63, 0), //
                         Color.fromRGB255(255, 55, 0), //
                         Color.fromRGB255(255, 42, 0), //
                         Color.fromRGB255(255, 29, 0), //
                         Color.fromRGB255(255, 16, 0), //
                         Color.fromRGB255(255, 4, 0), // 85 - End Red/Green Fading
                         Color.fromRGB255(255, 0, 4), // 86 - Red/ Blue Rising
                         Color.fromRGB255(255, 0, 16), //
                         Color.fromRGB255(255, 0, 29), //
                         Color.fromRGB255(255, 0, 42), //
                         Color.fromRGB255(255, 0, 55), //
                         Color.fromRGB255(255, 0, 63), //
                         Color.fromRGB255(255, 0, 76), //
                         Color.fromRGB255(255, 0, 89), //
                         Color.fromRGB255(255, 0, 102), //
                         Color.fromRGB255(255, 0, 114), //
                         Color.fromRGB255(255, 0, 127), //
                         Color.fromRGB255(255, 0, 135), //
                         Color.fromRGB255(255, 0, 148), //
                         Color.fromRGB255(255, 0, 161), //
                         Color.fromRGB255(255, 0, 174), //
                         Color.fromRGB255(255, 0, 186), //
                         Color.fromRGB255(255, 0, 195), //
                         Color.fromRGB255(255, 0, 208), //
                         Color.fromRGB255(255, 0, 221), //
                         Color.fromRGB255(255, 0, 233), //
                         Color.fromRGB255(255, 0, 246), //
                         Color.fromRGB255(255, 0, 255), // 107 - Blue + Red
                         Color.fromRGB255(242, 0, 255), // 108 - Blue/ Red Fading
                         Color.fromRGB255(229, 0, 255), //
                         Color.fromRGB255(216, 0, 255), //
                         Color.fromRGB255(204, 0, 255), //
                         Color.fromRGB255(191, 0, 255), //
                         Color.fromRGB255(182, 0, 255), //
                         Color.fromRGB255(169, 0, 255), //
                         Color.fromRGB255(157, 0, 255), //
                         Color.fromRGB255(144, 0, 255), //
                         Color.fromRGB255(131, 0, 255), //
                         Color.fromRGB255(123, 0, 255), //
                         Color.fromRGB255(110, 0, 255), //
                         Color.fromRGB255(97, 0, 255), //
                         Color.fromRGB255(85, 0, 255), //
                         Color.fromRGB255(72, 0, 255), //
                         Color.fromRGB255(63, 0, 255), //
                         Color.fromRGB255(50, 0, 255), //
                         Color.fromRGB255(38, 0, 255), //
                         Color.fromRGB255(25, 0, 255), // 126 - Blue-ish
                         Color.fromRGB255(240, 240, 225) // 127 - White ?
    );
  }

  /** Generates the list of Twister colors that match Bitwig parameters. */
  private static List<Color> parameterColorList()
  {
    // Tweaked by eye for closest match to Bitwig parameter colors
    return Arrays.asList(ALL.get(86), // RGB(244, 27, 62)
                         ALL.get(70), // RGB(255, 127, 23)
                         ALL.get(64), // RGB(252, 235, 35)
                         ALL.get(51), // RGB(91, 197, 21)
                         ALL.get(37), // RGB(101, 206, 146)
                         ALL.get(14), // RGB(92, 168, 238)
                         ALL.get(111), // RGB(195, 110, 255)
                         ALL.get(97)); // RGB(255, 84, 176)
  }
}
