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
import java.util.UUID;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.Bank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
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
import com.bitwig.extension.controller.api.SpecificBitwigDevice;
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
  public SpecificDeviceSettings specificDeviceSettings;
  private OnOffColorSupplier deviceColorSupplier;
  private OnOffColorSupplier devicePageColorSupplier;
  private OnOffColorSupplier[] deviceSpecificColorSuppliers = new OnOffColorSupplier[16];
  private SettableEnumValue pinnedAnimationValue;

  private DeviceMatcher eqDeviceMatcher;
  private DeviceBank eqFilterDeviceBank;
  public Device device;

  public final UUID EQ_PLUS_ID = java.util.UUID.fromString("e4815188-ba6f-4d14-bcfc-2dcb8f778ccb");

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
    cursorFourTrack = host.createCursorTrack("1", "multi", 3, 1, true);

    deviceColorSupplier = new OnOffColorSupplier();
    devicePageColorSupplier = new OnOffColorSupplier();
    for(int idx = 0; idx < 16; idx++) {
      deviceSpecificColorSuppliers[idx] = new OnOffColorSupplier();
    }

    loadPreferences();
    setupTrackBank();
    setupFourTrackBank();
    eqDeviceMatcher = host.createBitwigDeviceMatcher(EQ_PLUS_ID);
    int adds = 2;
    if(twister.eq) { setupEQKbobs(adds); adds += 1; }
    if(twister.spec) { setupSpecKnobs(adds); adds += 1; }
    if (adds < 4) { setupUserBanks(adds); }
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

    final SettableBooleanValue enableSpecBank = preferences.getBooleanSetting("Specific Device", "Options", false);
    twister.setSpecific(enableSpecBank.get());
    enableSpecBank.addValueObserver(twister::setSpecific);

    final SettableBooleanValue enableEqBank = preferences.getBooleanSetting("EQ Page", "Options", false);
    twister.setEq(enableEqBank.get());
    enableEqBank.addValueObserver(twister::setEq);
    final SettableBooleanValue dual = preferences.getBooleanSetting("Dual Twister Mode", "Options", false);
    twister.setDual(dual.get());
    dual.addValueObserver(twister::setDual);

    final SettableBooleanValue ext1 = preferences.getBooleanSetting("Extender", "Options", false);
    twister.setExtender1(ext1.get());
    ext1.addValueObserver(twister::setExtender1);

    final SettableBooleanValue popupEnabled = preferences.getBooleanSetting("Show Bank Popup", "Options", false);
    twister.setPopupEnabled(popupEnabled.get());
    popupEnabled.addValueObserver(twister::setPopupEnabled);

    // Set the color used for the row of controls related to devices, i.e. second row
    final SettableRangedValue deviceRowColor = preferences.getNumberSetting("Device Row Color", "Options", 0, 125, 1, null, 115);
    setDeviceRowColor(TwisterColors.ALL.get((int) deviceRowColor.getRaw()));
    deviceRowColor.addValueObserver(125,
                                    (value) -> setDeviceRowColor(TwisterColors.ALL.get(value)));

    // Sets the fine sensitivity factor for all knobs
    final SettableRangedValue globalFineSensitivity = preferences.getNumberSetting("Global Fine Sensitivity", "Options", 0.01, 1.00, 0.01, null, 0.25);
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

    for (int i = 0; i < deviceSpecificColorSuppliers.length; i++) {
      deviceSpecificColorSuppliers[i].setOnColor(color);
    }
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
    trackBank = cursorTrack.createSiblingsTrackBank(1, 4, 1, false, false);
    addTrackKnobs();
    addDeviceKnobs();
  }

  private void setupFourTrackBank()
  {
    trackBankFour = cursorFourTrack.createSiblingsTrackBank(twister.dual ? 8 : 4, 3, 1, false, false);
    addFourTrackKnobs();
  }

  /** Sets up all the hardware for all 3 user banks. */
  private void setupUserBanks(int firstBank)
  {
    final UserColorSettings colorSettings = new UserColorSettings(documentState);

    for (int bank = firstBank; bank < twister.banks.length; ++bank) {
      final TwisterKnob[] knobs = twister.banks[bank].knobs;
      final int settingsBank = bank - 2;

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

  private void setupSpecKnobs(int bankNumber) {
    final TwisterKnob[] knobs = twister.banks[bankNumber].knobs;
    final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
    setupUIFollowsCursor(cursorTrack);
    for (int idx = 0; idx < knobs.length; idx++) {
      cursorTrack.exists().addValueObserver(deviceSpecificColorSuppliers[idx]::setOn);
      // cursorDevice.exists().addValueObserver(deviceSpecificColorSuppliers[idx]::setOn);
      final TwisterKnob knob = knobs[idx];
      knob.ringLight().observeValue(knob.targetValue());
      setupSpecificDeviceKnob(String.format("knob%d", idx + 1), knob, cursorDevice, deviceSpecificColorSuppliers[idx]);
    }
  }

  private void setupEQKbobs(int bankNumber) {

    eqFilterDeviceBank = cursorTrack.createDeviceBank(1);
    eqFilterDeviceBank.setDeviceMatcher(eqDeviceMatcher);
    final Device device = eqFilterDeviceBank.getItemAt(0);
    // Track tb = trackBank.getItemAt(0);
    final TwisterKnob[] knobs = twister.banks[bankNumber].knobs;
    TwisterButton shiftButton;

    if ( twister.ext1 ) {
      shiftButton = twister.banks[bankNumber].rightSideButtons[2];
    } else {
      shiftButton = twister.banks[bankNumber].leftSideButtons[2];
    }

    shiftButton.isPressed().markInterested();
    setupUIFollowsCursor(cursorTrack);
    final SpecificBitwigDevice specificEQDevice = device.createSpecificBitwigDevice(EQ_PLUS_ID);
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 4; j++) {
        final Parameter enabledParam = specificEQDevice.createParameter(String.format("ENABLE%d", j + 1 + 4 * i));
        final Parameter freqParam = specificEQDevice.createParameter(String.format("FREQ%d", j + 1 + 4 * i));
        final Parameter gainParam = specificEQDevice.createParameter(String.format("GAIN%d", j + 1 + 4 * i));
        final Parameter resParam = specificEQDevice.createParameter(String.format("Q%d", j + 1 + 4 * i));
        final Parameter typeParam = specificEQDevice.createParameter(String.format("TYPE%d", j + 1 + 4 * i));

        final TwisterKnob freqKnob = knobs[j + (i * 8)];
        freqKnob.rgbLight().overrideBrightness(0);
        final TwisterKnob gainKnob = knobs[4 + j + (i * 8)];
        gainKnob.rgbLight().overrideBrightness(0);

        freqParam.markInterested();
        gainParam.markInterested();
        enabledParam.markInterested();
        resParam.markInterested();
        typeParam.markInterested();

        enabledParam.value().addValueObserver((value) -> {
          if (value > 0.0) {
            freqKnob.rgbLight().overrideBrightness(255);
          } else {
            freqKnob.rgbLight().overrideBrightness(0);
          }
        });

        typeParam.value().addValueObserver((value) -> {
          if (value > 0.0) {
            gainKnob.rgbLight().overrideBrightness(255);
            gainKnob.ringLight().overrideBrightness(255);
            if (enabledParam.getAsDouble() > 0.0) {
              freqKnob.rgbLight().overrideBrightness(255);
            } else {
              freqKnob.rgbLight().overrideBrightness(0);
            }
          } else {
            gainKnob.rgbLight().overrideBrightness(0);
            gainKnob.ringLight().overrideBrightness(0);
          }
        });
        freqParam.value().addValueObserver((value) -> {
          freqKnob.rgbLight().setRawValue((int) (80 - value * 64));
          gainKnob.rgbLight().setRawValue((int) (80 - value * 64));
        });

        freqKnob.setBinding(freqParam);
        gainKnob.setBinding(gainParam);

        freqKnob.setShiftBinding(resParam);

        gainKnob.setShiftBinding(typeParam);
        gainKnob.button().setDoubleClickedObserver(() -> {
          if (typeParam.getAsDouble() != 0) {
            typeParam.setImmediately(0);
          }
        });

        // freqKnob.button().setShiftButton(shiftButton);
        freqKnob.button().setDoubleClickedObserver(() -> {
          if (enabledParam.get() == 1) {
            enabledParam.setImmediately(0);
            freqKnob.rgbLight().overrideBrightness(0);
            freqKnob.ringLight().overrideBrightness(0);
          }
          else {
            enabledParam.setImmediately(1);
            freqKnob.rgbLight().overrideBrightness(255);
            freqKnob.ringLight().overrideBrightness(255);
          }
        });

        freqKnob.ringLight().observeValue(freqParam.value());
        gainKnob.ringLight().observeValue(gainParam.value());
        freqKnob.shiftRingLight().observeValue(resParam.value());
        gainKnob.shiftRingLight().observeValue(typeParam.value());
      }
    }
  }
  /** Sets up all the track related knobs. Track select, volume, pan, etc. */
  private void addTrackKnobs()
  {

    // final TrackBank trackBank = cursorTrack.createSiblingsTrackBank(1, 4, 1, true, false);
    Track tb = trackBank.getItemAt(0);

    final TwisterKnob[] knobs = twister.banks[0].knobs;
    TwisterButton shiftButton;
    if ( twister.ext1 ) {
      shiftButton = twister.banks[0].rightSideButtons[2];
    } else {
      shiftButton = twister.banks[0].leftSideButtons[2];
    }
 
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
    send1Knob.button().addClickedObserver(() -> sendBank.scrollBackwards());

    final TwisterKnob send2Knob = knobs[3];
    send2Knob.setBinding(send2);
    send2Knob.ringLight().observeValue(send2.value());
    send2Knob.rgbLight().setColorSupplier(send2.sendChannelColor());
    send2Knob.button().setShiftButton(shiftButton);
    send2Knob.button().addClickedObserver(() -> sendBank.scrollForwards());

    final TwisterKnob send3Knob = knobs[6];
    send3Knob.setBinding(send3);
    send3Knob.ringLight().observeValue(send3.value());
    send3Knob.rgbLight().setColorSupplier(send3.sendChannelColor());
    send3Knob.button().setShiftButton(shiftButton);
    // send3Knob.button().addClickedObserver(() -> circularScrollForward(sendBank));

    final TwisterKnob send4Knob = knobs[7];
    send4Knob.setBinding(send4);
    send4Knob.ringLight().observeValue(send4.value());
    send4Knob.rgbLight().setColorSupplier(send4.sendChannelColor());
    send4Knob.button().setShiftButton(shiftButton);
    // send4Knob.button().addClickedObserver(() -> circularScrollForward(sendBank));
  }

  /** Sets up all the track related knobs. Track select, volume, pan, etc. */
  private void addFourTrackKnobs()
  {
    final TwisterKnob[] knobs = twister.banks[1].knobs;
 
    setupUIFollowsCursor(cursorTrack);

    final TrackGroupNavigator trackGroupNavigator = new TrackGroupNavigator(cursorFourTrack);
    TwisterKnob nBank = knobs[3];
      nBank.button().addClickedObserver(() -> {
        for (int i = 0; i < (twister.dual ? 8 : 4); i++) cursorFourTrack.selectNext();
      });
      nBank.button().addLongPressedObserver(() -> trackGroupNavigator.navigateGroups(false));

    TwisterKnob pBank = knobs[2];
      pBank.button().addClickedObserver(() -> {
        for (int i = 0; i < (twister.dual ? 8 : 4); i++) cursorFourTrack.selectPrevious();
      });
      pBank.button().addLongPressedObserver(() -> trackGroupNavigator.navigateGroups(false));
 
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
        if(j == 1) {
          tb.arm().addValueObserver((armed) -> {
            sendKnob.rgbLight().setAnimationState(armed ? AnimationState.STROBE_1_1 : AnimationState.OFF);
          });
          sendKnob.button().addClickedObserver(() -> tb.arm().toggle());
        }
        if(j == 2) {
          tb.solo().addValueObserver((solo) -> {
            sendKnob.rgbLight().setAnimationState(solo ? AnimationState.PULSE_1_1 : AnimationState.OFF);
          });
          sendKnob.button().addClickedObserver(() -> tb.solo().toggle());
        }
      }

      final TwisterKnob volKnob = knobs[12 + i - offset];
        volKnob.setBinding(tb.volume());
        volKnob.ringLight().observeValue(tb.volume().value());
        volKnob.rgbLight().setColorSupplier(tb.color());
        tb.mute().addValueObserver((mute) -> {
          volKnob.rgbLight().setAnimationState(mute ? AnimationState.RAINBOW : AnimationState.OFF);
        });

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

  /**
   * Sets up a knob to control a specific device control parameter based on key name.
   *
   * @param controlKey    The controls key to apply to the knob.
   * @param knob          The knob to setup.
   * @param device        The device used to create the parameters.
   */
  private void setupSpecificDeviceKnob(String controlKey, TwisterKnob knob, Device device, OnOffColorSupplier colorSupplier)
  {
    knob.rgbLight().setColorSupplier(colorSupplier);
    knob.button().addClickedObserver(knob::toggleSensitivity);
    final Set<String> keys = specificDeviceSettings.controlMap().get(controlKey);

    if (keys == null) {
      knob.rgbLight().setRawValue(127);
      return;
    }

    for (String key : keys) {
      if(specificDeviceSettings.bitwigDevices() != null) {
        bindSpecificDevices(specificDeviceSettings.bitwigDevices(), key, device, knob, colorSupplier);
      }
      if(specificDeviceSettings.vst2Devices() != null) {
        bindSpecificDevices(specificDeviceSettings.vst2Devices(), key, device, knob, colorSupplier);
      }
      if(specificDeviceSettings.vst3Devices() != null) {
        bindSpecificDevices(specificDeviceSettings.vst3Devices(), key, device, knob, colorSupplier);
      }
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
   */
  private <IdType, SettingType extends AbstractDeviceSetting<IdType, ?>> void bindSpecificDevices(List<SettingType> settings,
                                                                                                  String key,
                                                                                                  Device device,
                                                                                                  TwisterKnob knob,
                                                                                                  OnOffColorSupplier colorSupplier)
  {
    for (final SettingType setting : settings) {
      if (setting.parameters().get(key) == null) { continue; }

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
