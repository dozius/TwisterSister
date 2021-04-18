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
package io.github.dozius;

import java.util.List;
import java.util.Set;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.Bank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.ControllerExtension;

import io.github.dozius.settings.AbstractDeviceSetting;
import io.github.dozius.settings.UserColorSettings;
import io.github.dozius.settings.SpecificDeviceSettings;
import io.github.dozius.twister.Twister;
import io.github.dozius.twister.TwisterColors;
import io.github.dozius.twister.TwisterKnob;
import io.github.dozius.util.CursorNormalizedValue;
import io.github.dozius.util.OnOffColorSupplier;

public class TwisterSisterExtension extends ControllerExtension
{
  public MidiIn midiIn;
  public MidiOut midiOut;
  public HardwareSurface hardwareSurface;
  public Twister twister;
  public CursorTrack cursorTrack;

  private DocumentState documentState;
  private SpecificDeviceSettings specificDeviceSettings;
  private OnOffColorSupplier deviceColorSupplier;
  private OnOffColorSupplier devicePageColorSupplier;
  private OnOffColorSupplier deviceSpecific1ColorSupplier;
  private OnOffColorSupplier deviceSpecific2ColorSupplier;

  protected TwisterSisterExtension(final TwisterSisterExtensionDefinition definition,
                                   final ControllerHost host)
  {
    super(definition, host);
  }

  @Override
  public void init()
  {
    final ControllerHost host = getHost();

    midiIn = host.getMidiInPort(0);
    midiOut = host.getMidiOutPort(0);
    hardwareSurface = host.createHardwareSurface();
    twister = new Twister(this);
    documentState = host.getDocumentState();
    specificDeviceSettings = new SpecificDeviceSettings(getSpecificDeviceSettingsPath());
    cursorTrack = host.createCursorTrack(1, 0);
    deviceColorSupplier = new OnOffColorSupplier();
    devicePageColorSupplier = new OnOffColorSupplier();
    deviceSpecific1ColorSupplier = new OnOffColorSupplier();
    deviceSpecific2ColorSupplier = new OnOffColorSupplier();

    loadPreferences();
    setupTrackBank();
    setupUserBanks();

    twister.setActiveBank(0);
  }

  @Override
  public void exit()
  {
    if (twister != null) {
      twister.lightsOff();
    }
  }

  @Override
  public void flush()
  {
    if (hardwareSurface != null) {
      hardwareSurface.updateHardware();
    }
  }

  /**
   * Gets the path to the specific device settings config file in a cross platform manner.
   *
   * @return Path to "SpecificDeviceSettings.toml".
   */
  private String getSpecificDeviceSettingsPath()
  {
    final String file = "SpecificDeviceSettings.toml";

    switch (getHost().getPlatformType())
    {
      case WINDOWS:
        final String userProfile = System.getenv("USERPROFILE").replace("\\", "/");
        return userProfile + "/Documents/Bitwig Studio/Extensions/" + file;

      case MAC:
        return System.getProperty("user.home") + "/Documents/Bitwig Studio/Extensions/" + file;

      case LINUX:
        return System.getProperty("user.home") + "/Bitwig Studio/Extensions/" + file;

      default:
        throw new IllegalArgumentException("Unknown Platform");
    }
  }

  /** Loads the extension preferences and sets up observers to allow for interactive updates. */
  private void loadPreferences()
  {
    final Preferences preferences = getHost().getPreferences();

    // Enable/disable notification popups on bank change
    final SettableBooleanValue popupEnabled = preferences.getBooleanSetting("Show Bank Popup",
                                                                            "Options", false);
    twister.setPopupEnabled(popupEnabled.get());
    popupEnabled.addValueObserver(twister::setPopupEnabled);

    // Set the color used for the row of controls related to devices, i.e. second row
    final SettableRangedValue deviceRowColor = preferences.getNumberSetting("Device Row Color",
                                                                            "Options", 0, 125, 1,
                                                                            null, 115);
    setDeviceRowColor(TwisterColors.ALL.get((int) deviceRowColor.getRaw()));
    deviceRowColor.addValueObserver(125,
                                    (value) -> setDeviceRowColor(TwisterColors.ALL.get(value)));

    // Sets the fine sensitivity factor for all knobs
    final SettableRangedValue globalFineSensitivity = preferences.getNumberSetting("Global Fine Sensitivity",
                                                                                   "Options", 0.01,
                                                                                   1.00, 0.01, null,
                                                                                   0.25);
    setGlobalFineSensitivity(globalFineSensitivity.get());
    globalFineSensitivity.addValueObserver(this::setGlobalFineSensitivity);
  }

  /**
   * Sets the RGB light color for the row of device related controls on the track bank.
   *
   * @param color Color to set the RGB lights.
   */
  private void setDeviceRowColor(Color color)
  {
    deviceColorSupplier.setOnColor(color);
    devicePageColorSupplier.setOnColor(color);
    deviceSpecific1ColorSupplier.setOnColor(color);
    deviceSpecific2ColorSupplier.setOnColor(color);
  }

  /**
   * Sets the fine sensitivity factor for all controls
   *
   * @param factor Sensitivity factor to use.
   */
  private void setGlobalFineSensitivity(double factor)
  {
    for (Twister.Bank bank : twister.banks) {
      for (TwisterKnob knob : bank.knobs) {
        knob.setFineSensitivity(factor);
      }
    }
  }

  /** Sets up all the hardware for the track bank. */
  private void setupTrackBank()
  {
    addTrackKnobs();
    addDeviceKnobs();
  }

  /** Sets up all the hardware for all 3 user banks. */
  private void setupUserBanks()
  {
    final UserColorSettings colorSettings = new UserColorSettings(documentState);

    for (int bank = 1; bank < twister.banks.length; ++bank) {
      final TwisterKnob[] knobs = twister.banks[bank].knobs;
      final int settingsBank = bank - 1;

      for (int idx = 0; idx < knobs.length; ++idx) {
        final TwisterKnob knob = knobs[idx];
        final SettableRangedValue colorSetting = colorSettings.getSetting(settingsBank, idx);

        // Ring light follows whatever target is currently bound to the knob
        knob.ringLight().observeValue(knob.targetValue());

        // Change color setting with shift encoder
        knob.setShiftBinding(UserColorSettings.createTarget(getHost(), colorSetting));

        // Send changes from color setting to RGB light
        colorSetting.addValueObserver(126, knob.rgbLight()::setRawValue);

        // Reset color on double press
        knob.button().addDoubleClickedObserver(() -> colorSetting.set(0.0));
      }
    }
  }

  /** Sets up all the track related knobs. Track select, volume, pan, etc. */
  private void addTrackKnobs()
  {
    final TrackBank trackBank = cursorTrack.createSiblingsTrackBank(1, 0, 0, true, true);
    final TwisterKnob[] knobs = twister.banks[0].knobs;
    final SendBank sendBank = cursorTrack.sendBank();
    final Send send = sendBank.getItemAt(0);

    cursorTrack.color().markInterested();
    sendBank.canScrollForwards().markInterested();
    sendBank.itemCount().markInterested();
    send.sendChannelColor().markInterested();

    final TwisterKnob selectionKnob = knobs[0];
    selectionKnob.setBinding(cursorTrack);
    selectionKnob.ringLight().observeValue(new CursorNormalizedValue(cursorTrack, trackBank));
    selectionKnob.rgbLight().setColorSupplier(cursorTrack.color());
    selectionKnob.button().addClickedObserver(cursorTrack.mute()::toggle);
    selectionKnob.button().addLongPressedObserver(cursorTrack.arm()::toggle);

    final TwisterKnob volumeKnob = knobs[1];
    volumeKnob.setBinding(cursorTrack.volume());
    volumeKnob.ringLight().observeValue(cursorTrack.volume().value());
    volumeKnob.rgbLight().setColorSupplier(cursorTrack.color());
    volumeKnob.button().addLongPressedObserver(cursorTrack.solo()::toggle);
    volumeKnob.button().addClickedObserver(volumeKnob::toggleSensitivity);
    volumeKnob.button().addDoubleClickedObserver(cursorTrack.volume()::reset);

    final TwisterKnob panKnob = knobs[2];
    panKnob.setBinding(cursorTrack.pan());
    panKnob.ringLight().observeValue(cursorTrack.pan().value());
    panKnob.rgbLight().setColorSupplier(cursorTrack.color());
    panKnob.button().addClickedObserver(panKnob::toggleSensitivity);
    panKnob.button().addDoubleClickedObserver(cursorTrack.pan()::reset);

    final TwisterKnob sendKnob = knobs[3];
    sendKnob.setBinding(send);
    sendKnob.ringLight().observeValue(send.value());
    sendKnob.rgbLight().setColorSupplier(send.sendChannelColor());
    sendKnob.button().addClickedObserver(() -> circularScrollForward(sendBank));
  }

  /** Sets up all the device related knobs. Selection, remote control page, etc. */
  private void addDeviceKnobs()
  {
    final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
    final CursorRemoteControlsPage remoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8);
    final DeviceBank deviceBank = cursorDevice.createSiblingsDeviceBank(1);
    final TwisterKnob[] knobs = twister.banks[0].knobs;

    cursorDevice.exists().addValueObserver(deviceColorSupplier::setOn);
    cursorDevice.exists().addValueObserver(devicePageColorSupplier::setOn);
    remoteControlsPage.pageCount()
                      .addValueObserver((count) -> devicePageColorSupplier.setOn(count > 0));

    final TwisterKnob deviceKnob = knobs[4];
    deviceKnob.setBinding(cursorDevice);
    deviceKnob.ringLight().observeValue(new CursorNormalizedValue(cursorDevice, deviceBank));
    deviceKnob.rgbLight().setColorSupplier(deviceColorSupplier);
    deviceKnob.button().addClickedObserver(cursorDevice.isEnabled()::toggle);
    deviceKnob.button().addLongPressedObserver(cursorDevice.isExpanded()::toggle);

    final TwisterKnob pageKnob = knobs[5];
    pageKnob.setBinding(remoteControlsPage);
    pageKnob.ringLight().observeValue(new CursorNormalizedValue(remoteControlsPage));
    pageKnob.rgbLight().setColorSupplier(devicePageColorSupplier);
    pageKnob.button().addClickedObserver(cursorDevice.isWindowOpen()::toggle);
    pageKnob.button().addLongPressedObserver(cursorDevice.isRemoteControlsSectionVisible()::toggle);

    final TwisterKnob specificDeviceKnob1 = knobs[6];
    bindLongPressedToBrowseBeforeDevice(specificDeviceKnob1, cursorDevice);
    setupSpecificDeviceKnob("knob1", specificDeviceKnob1, cursorDevice,
                            deviceSpecific1ColorSupplier);

    final TwisterKnob specificDeviceKnob2 = knobs[7];
    bindLongPressedToBrowseAfterDevice(specificDeviceKnob2, cursorDevice);
    setupSpecificDeviceKnob("knob2", specificDeviceKnob2, cursorDevice,
                            deviceSpecific2ColorSupplier);

    final int firstKnobIndex = 8;

    for (int i = 0; i < remoteControlsPage.getParameterCount(); ++i) {
      final Color color = TwisterColors.BITWIG_PARAMETERS.get(i);
      final OnOffColorSupplier colorSupplier = new OnOffColorSupplier(color);
      final TwisterKnob knob = knobs[firstKnobIndex + i];
      final RemoteControl control = remoteControlsPage.getParameter(i);

      control.setIndication(true);
      control.exists().addValueObserver(colorSupplier::setOn);
      knob.setBinding(control);
      knob.ringLight().observeValue(control.value());
      knob.rgbLight().setColorSupplier(colorSupplier);
      knob.button().addClickedObserver(knob::toggleSensitivity);
      knob.button().addDoubleClickedObserver(control::reset);
    }
  }

  /**
   * Helper function to scroll a bank forward in a circular fashion, i.e. wraps around to the
   * beginning if scrolling past the end.
   *
   * @param bank The bank to scroll.
   */
  private void circularScrollForward(Bank<?> bank)
  {
    if (bank.canScrollForwards().get()) {
      bank.scrollForwards();
    }
    else {
      bank.scrollBy(-(bank.itemCount().get() - 1));
    }
  }

  /**
   * Binds a knob buttons long pressed action to insert a device after the selected device or at the
   * start of the chain if no devices exist yet.
   *
   * @param knob         Knob to bind.
   * @param cursorDevice Device cursor to follow.
   */
  private void bindLongPressedToBrowseAfterDevice(TwisterKnob knob, CursorDevice cursorDevice)
  {
    cursorDevice.exists().addValueObserver((exists) -> {
      final Runnable browseAfter = cursorDevice.afterDeviceInsertionPoint()::browse;
      final Runnable browseStart = cursorDevice.deviceChain()
                                               .startOfDeviceChainInsertionPoint()::browse;

      knob.button().setLongPressedObserver(exists ? browseAfter : browseStart);
    });
  }

  /**
   * Binds a knob buttons long pressed action to insert a device before the selected device or at
   * the start of the chain if no devices exist yet.
   *
   * @param knob         Knob to bind.
   * @param cursorDevice Device cursor to follow.
   */
  private void bindLongPressedToBrowseBeforeDevice(TwisterKnob knob, CursorDevice cursorDevice)
  {
    cursorDevice.exists().addValueObserver((exists) -> {
      final Runnable browseBefore = cursorDevice.beforeDeviceInsertionPoint()::browse;
      final Runnable browseStart = cursorDevice.deviceChain()
                                               .startOfDeviceChainInsertionPoint()::browse;

      knob.button().setLongPressedObserver(exists ? browseBefore : browseStart);
    });
  }

  /**
   * Sets up a knob to control a specific device control parameter based on key name.
   *
   * @param controlKey    The controls key to apply to the knob.
   * @param knob          The knob to setup.
   * @param device        The device used to create the parameters.
   * @param colorSupplier The color supplier for the knob.
   */
  private void setupSpecificDeviceKnob(String controlKey, TwisterKnob knob, Device device,
                                       OnOffColorSupplier colorSupplier)
  {
    knob.rgbLight().setColorSupplier(colorSupplier);
    knob.button().addClickedObserver(knob::toggleSensitivity);

    final Set<String> keys = specificDeviceSettings.controlMap().get(controlKey);

    if (keys == null) {
      return;
    }

    for (String key : keys) {
      bindSpecificDevices(specificDeviceSettings.bitwigDevices(), key, device, knob, colorSupplier);
      bindSpecificDevices(specificDeviceSettings.vst3Devices(), key, device, knob, colorSupplier);
      bindSpecificDevices(specificDeviceSettings.vst2Devices(), key, device, knob, colorSupplier);
    }
  }

  /**
   * Binds all available parameters from all devices that match the key.
   *
   * @param <IdType>      The type of the device ID. This will be deduced from settings.
   * @param <SettingType> The type of device settings object. This will be deduced from settings.
   * @param settings      The list of device settings to search.
   * @param key           The parameter key to match against.
   * @param device        The device used to create parameters.
   * @param knob          The knob to bind parameters to.
   * @param colorSupplier The knobs color supplier.
   */
  private <IdType, SettingType extends AbstractDeviceSetting<IdType, ?>> void bindSpecificDevices(List<SettingType> settings,
                                                                                                  String key,
                                                                                                  Device device,
                                                                                                  TwisterKnob knob,
                                                                                                  OnOffColorSupplier colorSupplier)
  {
    for (final SettingType setting : settings) {
      if (setting.parameters().get(key) == null) {
        continue;
      }

      final Parameter param = setting.createParameter(device, key);

      knob.ringLight().observeValue(param.value());

      param.exists().markInterested();
      param.exists().addValueObserver((exists) -> {
        if (exists) {
          knob.setBinding(param);
          knob.button().setDoubleClickedObserver(param::reset);
        }

        colorSupplier.setOn(exists);
      });
    }
  }
}
