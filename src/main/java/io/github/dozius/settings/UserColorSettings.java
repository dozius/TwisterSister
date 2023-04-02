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

import java.util.List;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.Setting;

import io.github.dozius.twister.Twister;
import io.github.dozius.util.MathUtil;

/**
 * Color settings for the individual RGB lights in each user mappable bank.
 *
 * These settings can be accessed in the I/O panel. There is also a helper function to create
 * bindable targets for a setting so that it can be controlled via hardware.
 */
public class UserColorSettings
{
  private static final int NUM_USER_BANKS = 2;
  private static final int NUM_KNOBS_PER_BANK = Twister.Bank.NUM_KNOBS;

  private final List<String> options = List.of("Hide", "3", "4");
  private final SettableRangedValue[][] settings = new SettableRangedValue[NUM_USER_BANKS][NUM_KNOBS_PER_BANK];
  private final SettableEnumValue selector;

  /**
   * Creates all the settings from the document state.
   *
   * @param documentState The document state of the host.
   */
  public UserColorSettings(DocumentState documentState)
  {
    // Setup the bank selector
    final String[] strings = options.toArray(String[]::new);
    selector = documentState.getEnumSetting("Bank", "Colors", strings, options.get(0));
    selector.addValueObserver((value) -> showBank(options.indexOf(value) - 1));

    // Create all the individual settings
    for (int bank = 0; bank < settings.length; ++bank) {
      final SettableRangedValue[] settingsBank = settings[bank];

      for (int idx = 0; idx < settingsBank.length; ++idx) {
        final String label = String.format("Color %02d%" + (bank + 1) + "s", idx + 1, " ");
        final SettableRangedValue colorSetting = documentState.getNumberSetting(label, "Colors", 0,
                                                                                125, 1, null, 0);
        settingsBank[idx] = colorSetting;
        ((Setting) colorSetting).hide();
      }
    }
  }

  /**
   * Gets a specific setting.
   *
   * @param  colorBankIndex Bank index of the desired setting.
   * @param  knobIndex      Knob index of the desired setting.
   *
   * @return                The setting for the given bank and index.
   */
  public SettableRangedValue getSetting(int colorBankIndex, int knobIndex)
  {
    return settings[colorBankIndex][knobIndex];
  }

  /**
   * Creates a target to a color setting that is able to be bound to hardware.
   *
   * Despite being a SettableRangedValue, the settings are not compatible targets and this proxy
   * target must be created instead.
   *
   * @param  host    The controller host.
   * @param  setting The setting to create a target for.
   *
   * @return         A bindable target to the setting.
   */
  public static RelativeHardwarControlBindable createTarget(ControllerHost host,
                                                            SettableRangedValue setting)
  {
    return host.createRelativeHardwareControlAdjustmentTarget((value) -> {
      final double adjustedValue = MathUtil.clamp(setting.get() + value, 0.0, 1.0);
      setting.set(adjustedValue);
    });
  }

  /** Hides all the settings from the UI panel. */
  private void hideAll()
  {
    for (final SettableRangedValue[] settingsBank : settings) {
      for (final SettableRangedValue colorSetting : settingsBank) {
        ((Setting) colorSetting).hide();
      }
    }
  }

  /** Handles bank visibility */
  private void showBank(int index)
  {
    hideAll();

    if (index < 0) {
      return;
    }

    for (final SettableRangedValue colorSetting : settings[index]) {
      ((Setting) colorSetting).show();
    }
  }
}
