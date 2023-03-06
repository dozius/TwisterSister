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
import com.bitwig.extension.api.Host;
import com.bitwig.extension.controller.api.Bank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorNavigationMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursor;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.ControllerExtension;

import io.github.dozius.settings.AbstractDeviceSetting;
import io.github.dozius.settings.UserColorSettings;
import io.github.dozius.settings.SpecificDeviceSettings;
import io.github.dozius.twister.Twister;
import io.github.dozius.twister.TwisterButton;
import io.github.dozius.twister.TwisterColors;
import io.github.dozius.twister.TwisterKnob;
import io.github.dozius.twister.TwisterLight;
import io.github.dozius.twister.TwisterLight.AnimationState;
import io.github.dozius.util.CursorNormalizedValue;
import io.github.dozius.util.Logger;
import io.github.dozius.util.OnOffColorSupplier;
import io.github.dozius.util.TrackGroupNavigator;

public class TwisterSisterExtension extends ControllerExtension
{
  public MidiIn midiIn;
  public MidiOut midiOut;
  public HardwareSurface hardwareSurface;
  public Twister twister;
  public CursorTrack cursorTrack;
  public CursorTrack cursorFourTrack;

  public TrackBank trackBank;
  public TrackBank trackBankFour;

  private DocumentState documentState;
  private SpecificDeviceSettings specificDeviceSettings;
  private OnOffColorSupplier deviceColorSupplier;
  private OnOffColorSupplier devicePageColorSupplier;
  private OnOffColorSupplier deviceSpecific1ColorSupplier;
  private OnOffColorSupplier deviceSpecific2ColorSupplier;
  private SettableEnumValue pinnedAnimationValue;

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
    cursorTrack = host.createCursorTrack("0", "one", 4, 1, true);
    // cursorFourTrack = host.createCursorTrack("1", "multi", 3, 1, true);

    trackBank = cursorTrack.createSiblingsTrackBank(1, 4, 1, true, false);
    trackBankFour = cursorTrack.createSiblingsTrackBank(8, 3, 1, true, false);

    deviceColorSupplier = new OnOffColorSupplier();
    devicePageColorSupplier = new OnOffColorSupplier();
    deviceSpecific1ColorSupplier = new OnOffColorSupplier();
    deviceSpecific2ColorSupplier = new OnOffColorSupplier();

    loadPreferences();

    setupTrackBank();
    setupFourTrackBank();
    setupUserBanks();
    setupBankButtons();

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
    final SettableBooleanValue ext1 = preferences.getBooleanSetting("Extender 1st",
                                                                            "Options", false);
    twister.setExtender1(ext1.get());
    ext1.addValueObserver(twister::setExtender1);

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

    // Sets the indicator animation for pinned tracks/devices
    final String[] pinnedAnimationOptions = AnimationState.optionStrings();
    pinnedAnimationValue = preferences.getEnumSetting("Pinned Indication", "Options",
                                                      pinnedAnimationOptions,
                                                      AnimationState.STROBE_1_1.getOptionString());
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

  /** Sets up the right side buttons for bank changes */
  private void setupBankButtons()
  {
    for (int bank = 0; bank < twister.banks.length; ++bank) {
      TwisterButton[] buttons;
      if(twister.ext1) {
        buttons = twister.banks[bank].rightSideButtons;
      } else {
        buttons = twister.banks[bank].leftSideButtons;
      }

      buttons[0].addClickedObserver(twister::previousBank);
      buttons[1].addClickedObserver(twister::nextBank);
      buttons[2].addClickedObserver(() -> twister.setActiveBank(0));
    }
  }

  /** Sets up all the hardware for the track bank. */
  private void setupTrackBank()
  {
    addTrackKnobs();
    addDeviceKnobs();
  }

  private void setupFourTrackBank()
  {
    addFourTrackKnobs();
  }

  /** Sets up all the hardware for all 3 user banks. */
  private void setupUserBanks()
  {
    final UserColorSettings colorSettings = new UserColorSettings(documentState);

    for (int bank = 2; bank < twister.banks.length; ++bank) {
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
    // final TrackBank trackBank = cursorTrack.createSiblingsTrackBank(1, 4, 1, true, false);
    Track tb = trackBank.getItemAt(0);

    final TwisterKnob[] knobs = twister.banks[0].knobs;
    final TwisterButton shiftButton = twister.banks[0].rightSideButtons[2];
    
    final SendBank sendBank = tb.sendBank();
    final Send send1 = sendBank.getItemAt(0);
    final Send send2 = sendBank.getItemAt(1);
    final Send send3 = sendBank.getItemAt(2);
    final Send send4 = sendBank.getItemAt(3);
    
    final TrackGroupNavigator trackGroupNavigator = new TrackGroupNavigator(cursorTrack);

    shiftButton.isPressed().markInterested();
    tb.color().markInterested();
    
    sendBank.canScrollForwards().markInterested();
    sendBank.itemCount().markInterested();
    send1.sendChannelColor().markInterested();
    send2.sendChannelColor().markInterested();
    send3.sendChannelColor().markInterested();
    send4.sendChannelColor().markInterested();

    setupUIFollowsCursor(cursorTrack);

    final TwisterKnob selectionKnob = knobs[0];
    selectionKnob.setBinding(cursorTrack);
    selectionKnob.ringLight().observeValue(new CursorNormalizedValue(cursorTrack, trackBank));
    // selectionKnob.ringLight().observeValue(new CursorNormalizedValue(cursorTrack, trackBank));
    selectionKnob.rgbLight().setColorSupplier(tb.color());
    selectionKnob.button().setShiftButton(shiftButton);
    selectionKnob.button().addClickedObserver(() -> trackGroupNavigator.navigateGroups(true));
    selectionKnob.button().addShiftClickedObserver(() -> trackGroupNavigator.navigateGroups(false));
    selectionKnob.button().addLongPressedObserver(cursorTrack.isPinned()::toggle);
    selectionKnob.button().addShiftLongPressedObserver(cursorTrack.arm()::toggle);
    bindPinnedInidcator(cursorTrack, selectionKnob.rgbLight());

    final TwisterKnob volumeKnob = knobs[1];
    volumeKnob.setBinding(tb.volume());
    volumeKnob.ringLight().observeValue(tb.volume().value());
    volumeKnob.rgbLight().setColorSupplier(tb.color());
    volumeKnob.button().setShiftButton(shiftButton);
    volumeKnob.button().addClickedObserver(volumeKnob::toggleSensitivity);
    volumeKnob.button().addDoubleClickedObserver(tb.volume()::reset);
    volumeKnob.button().addLongPressedObserver(tb.mute()::toggle);

    final TwisterKnob send1Knob = knobs[2];
    send1Knob.setBinding(send1);
    send1Knob.ringLight().observeValue(send1.value());
    send1Knob.rgbLight().setColorSupplier(send1.sendChannelColor());
    send1Knob.button().setShiftButton(shiftButton);
    send1Knob.button().addClickedObserver(() -> circularScrollForward(sendBank));

    final TwisterKnob send2Knob = knobs[3];
    send2Knob.setBinding(send2);
    send2Knob.ringLight().observeValue(send2.value());
    send2Knob.rgbLight().setColorSupplier(send2.sendChannelColor());
    send2Knob.button().setShiftButton(shiftButton);
    send2Knob.button().addClickedObserver(() -> circularScrollForward(sendBank));

    final TwisterKnob send3Knob = knobs[6];
    send3Knob.setBinding(send3);
    send3Knob.ringLight().observeValue(send3.value());
    send3Knob.rgbLight().setColorSupplier(send3.sendChannelColor());
    send3Knob.button().setShiftButton(shiftButton);
    send3Knob.button().addClickedObserver(() -> circularScrollForward(sendBank));

    final TwisterKnob send4Knob = knobs[7];
    send4Knob.setBinding(send4);
    send4Knob.ringLight().observeValue(send4.value());
    send4Knob.rgbLight().setColorSupplier(send4.sendChannelColor());
    send4Knob.button().setShiftButton(shiftButton);
    send4Knob.button().addClickedObserver(() -> circularScrollForward(sendBank));
  }

  /** Sets up all the track related knobs. Track select, volume, pan, etc. */
  private void addFourTrackKnobs()
  {
    // final TrackBank trackBank = cursorFourTrack.createSiblingsTrackBank(8, 4, 1, true, false);
    final TwisterKnob[] knobs = twister.banks[1].knobs;
    // cursorFourTrack.sel(8);;
    // trackBankFour.getCapacityOfBank();
    // cursorFourTrack.color().markInterested();
    // cursorFourTrack.color;
    
    setupUIFollowsCursor(cursorTrack);
    trackBankFour.setSizeOfBank(8);
    final TrackGroupNavigator trackGroupNavigator = new TrackGroupNavigator(cursorTrack);
    TwisterKnob nBank = knobs[15];
      nBank.button().addLongPressedObserver(() -> { 
        for (int i = 0; i < 8; i++) cursorTrack.selectNext();
      });
    TwisterKnob pBank = knobs[14];
      pBank.button().addLongPressedObserver(() -> {
        for (int i = 0; i < 8; i++) cursorTrack.selectPrevious();
      });
    TwisterKnob fwKnob = knobs[3];
      fwKnob.setShiftBinding(cursorTrack);
      fwKnob.shiftRingLight().observeValue(new CursorNormalizedValue(cursorTrack, trackBankFour));
      fwKnob.button().addDoubleClickedObserver(() -> trackGroupNavigator.navigateGroups(false));
    int offset = 0;
    if (twister.ext1) {
      offset = 4;
    }
    for (int i = offset; i < offset + 4; i++) {
      Track tb = trackBankFour.getItemAt(i);

      tb.color().markInterested();
      final SendBank sb = tb.sendBank();
      
      for (int j = 0; j < 3; j++) {
        Send send = sb.getItemAt(j);
        send.sendChannelColor().markInterested();
        TwisterKnob sendKnob = knobs[j * 4 + i - offset];
          sendKnob.setBinding(send);
          sendKnob.ringLight().observeValue(send.value());
          sendKnob.rgbLight().setColorSupplier(tb.color());
        if(j == 1) sendKnob.button().addClickedObserver(() -> tb.arm().toggle());
        if(j == 2) sendKnob.button().addClickedObserver(() -> tb.solo().toggle());
      }

      final TwisterKnob volKnob = knobs[12 + i - offset];
        volKnob.setBinding(tb.volume());
        volKnob.ringLight().observeValue(tb.volume().value());
        volKnob.rgbLight().setColorSupplier(tb.color());
        volKnob.button().addClickedObserver(() -> tb.mute().toggle());
        volKnob.setShiftBinding(tb.pan());
        volKnob.shiftRingLight().observeValue(tb.pan().value());
    }

  }

  /** Sets up all the device related knobs. Selection, remote control page, etc. */
  private void addDeviceKnobs()
  {
    final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
    final CursorRemoteControlsPage remoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8);
    final DeviceBank deviceBank = cursorDevice.createSiblingsDeviceBank(1);
    final TwisterKnob[] knobs = twister.banks[0].knobs;
    final TwisterButton shiftButton = twister.banks[0].rightSideButtons[2];

    shiftButton.isPressed().markInterested();

    cursorDevice.exists().addValueObserver(deviceColorSupplier::setOn);
    cursorDevice.exists().addValueObserver(devicePageColorSupplier::setOn);
    remoteControlsPage.pageCount()
                      .addValueObserver((count) -> devicePageColorSupplier.setOn(count > 0));

    final TwisterKnob deviceKnob = knobs[4];
    deviceKnob.setBinding(cursorDevice);
    deviceKnob.ringLight().observeValue(new CursorNormalizedValue(cursorDevice, deviceBank));
    deviceKnob.rgbLight().setColorSupplier(deviceColorSupplier);
    deviceKnob.button().setShiftButton(shiftButton);
    deviceKnob.button().addClickedObserver(cursorDevice.isEnabled()::toggle);
    deviceKnob.button().addShiftClickedObserver(cursorDevice.isExpanded()::toggle);
    deviceKnob.button().addLongPressedObserver(cursorDevice.isPinned()::toggle);
    bindPinnedInidcator(cursorDevice, deviceKnob.rgbLight());

    final TwisterKnob pageKnob = knobs[5];
    pageKnob.setBinding(remoteControlsPage);
    pageKnob.ringLight().observeValue(new CursorNormalizedValue(remoteControlsPage));
    pageKnob.rgbLight().setColorSupplier(devicePageColorSupplier);
    pageKnob.button().setShiftButton(shiftButton);
    pageKnob.button().addClickedObserver(cursorDevice.isWindowOpen()::toggle);
    pageKnob.button().addLongPressedObserver(cursorDevice.isRemoteControlsSectionVisible()::toggle);

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
      knob.button().setShiftButton(shiftButton);
      knob.button().addClickedObserver(knob::toggleSensitivity);
      knob.button().addDoubleClickedObserver(control::reset);
    }
  }

  /**
   * Makes the UI follow the track cursor selection.
   *
   * Only follows when the cursor is not pinned.
   *
   * @param cursorTrack The cursor to follow.
   */
  private void setupUIFollowsCursor(CursorTrack cursorTrack)
  {
    cursorTrack.isPinned().markInterested();

    cursorTrack.position().addValueObserver((position) -> {
      if (!cursorTrack.isPinned().get()) {
        cursorTrack.makeVisibleInArranger();
        cursorTrack.makeVisibleInMixer();
      }
    });
  }

  /**
   * Helper to bind a pinnable cursors pinned state to a light indicator.
   *
   * @param cursor The cursor whose pinned state will be observed.
   * @param light  The light that will blink when the state is pinned.
   */
  private void bindPinnedInidcator(PinnableCursor cursor, TwisterLight light)
  {
    cursor.isPinned().addValueObserver((isPinned) -> {
      light.setAnimationState(isPinned ? AnimationState.valueOfOptionString(pinnedAnimationValue.get())
                                       : TwisterLight.AnimationState.OFF);
    });

    pinnedAnimationValue.addValueObserver((animation) -> {
      light.setAnimationState(cursor.isPinned()
                                    .get() ? AnimationState.valueOfOptionString(animation)
                                           : TwisterLight.AnimationState.OFF);
    });
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
}
