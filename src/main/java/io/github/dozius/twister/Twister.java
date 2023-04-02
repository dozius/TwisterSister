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

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;

import io.github.dozius.TwisterSisterExtension;

/**
 * Twister hardware.
 *
 * All the hardware available on the MIDI Fighter Twister is setup on construction and accessible
 * through this class.
 */
public class Twister
{
  /** Twister MIDI channels. Zero indexed. */
  public static class MidiChannel
  {
    public static final int ENCODER = 0;
    public static final int BUTTON = 1;
    public static final int RGB_ANIMATION = 2;
    public static final int SIDE_BUTTON = 3;
    public static final int SYSTEM = 3;
    public static final int SHIFT = 4;
    public static final int RING_ANIMATION = 5;
    public static final int SEQUENCER = 7;
  }

  /** A single bank of Twister hardware. */
  public class Bank
  {
    public static final int NUM_KNOBS = 16;
    public static final int NUM_LEFT_SIDE_BUTTONS = 3;
    public static final int NUM_RIGHT_SIDE_BUTTONS = 3;
    public static final int NUM_SIDE_BUTTONS = NUM_LEFT_SIDE_BUTTONS + NUM_RIGHT_SIDE_BUTTONS;

    public final TwisterKnob[] knobs = new TwisterKnob[NUM_KNOBS];
    public final TwisterButton[] leftSideButtons = new TwisterButton[NUM_LEFT_SIDE_BUTTONS];
    public final TwisterButton[] rightSideButtons = new TwisterButton[NUM_RIGHT_SIDE_BUTTONS];
  }

  public static final int NUM_BANKS = 4;

  public final Bank[] banks = new Bank[NUM_BANKS];

  private final MidiOut midiOut;
  private final HardwareSurface hardwareSurface;
  private final ControllerHost host;

  private boolean popupEnabled = false;
  public boolean ext1 = false;
  public boolean dual = false;
  public boolean eq = false;
  public boolean spec = false;
  private int activeBank = -1;

  /**
   * Creates a new Twister.
   *
   * @param extension The parent Bitwig extension.
   */
  public Twister(TwisterSisterExtension extension)
  {
    this.midiOut = extension.midiOut;
    this.hardwareSurface = extension.hardwareSurface;
    this.host = extension.getHost();

    createHardware(extension);
    createSequencerInput(extension.midiIn);
    createBankSwitchListener(extension.midiIn);
  }

  /**
   * Sets the active bank to the next available bank.
   *
   * If the currently active bank is the last bank then this does nothing.
   */
  public void nextBank()
  {
    int nextBank = activeBank + 1;

    if (nextBank >= NUM_BANKS) {
      return;
    }

    setActiveBank(nextBank);
  }

  /**
   * Sets the active bank to the previous available bank.
   *
   * If the currently active bank is the first bank then this does nothing.
   */
  public void previousBank()
  {
    int previousBank = activeBank - 1;

    if (previousBank < 0) {
      return;
    }

    setActiveBank(previousBank);
  }

  /**
   * Sets the active bank to the desired index.
   *
   * @param index Desired bank index.
   */
  public void setActiveBank(int index)
  {
    assert index > 0 && index < NUM_BANKS : "index is invalid";

    if (activeBank == index) {
      return; // already active, do nothing
    }

    activeBank = index;
    midiOut.sendMidi(ShortMidiMessage.CONTROL_CHANGE + MidiChannel.SYSTEM, activeBank, 127);
    showBankChangePopup(activeBank);
  }

  /**
   * Enables/disables any popup notifications related to Twister activity.
   *
   * @param enabled True to enable, false to disable.
   */
  public void setPopupEnabled(boolean enabled)
  {
    popupEnabled = enabled;
  }

  public void setSpecific(boolean spec_status) {
    spec = spec_status;
  }
  public void setEq(boolean eq_status) {
    eq = eq_status;
  }
  public void setDual(boolean dual_status) {
    dual = dual_status;
  }
  public void setExtender1(boolean extender) {
    ext1 = extender;
  }
  /** Turns off all lights on the Twister. */
  public void lightsOff()
  {
    // Helps with "stuck" lights when quitting Bitwig
    hardwareSurface.updateHardware();

    for (Twister.Bank bank : banks) {
      for (TwisterKnob knob : bank.knobs) {
        knob.lightsOff();
      }
    }
  }

  /** Creates all the hardware for the Twister. */
  private void createHardware(TwisterSisterExtension extension)
  {
    final int sideButtonsFirstLeftCC = 8;
    final int sideButtonsFirstRightCC = sideButtonsFirstLeftCC + Twister.Bank.NUM_LEFT_SIDE_BUTTONS;

    for (int bank = 0; bank < banks.length; ++bank) {
      banks[bank] = new Bank();

      TwisterKnob[] hardwareKnobs = banks[bank].knobs;
      TwisterButton[] hardwareLSBs = banks[bank].leftSideButtons;
      TwisterButton[] hardwareRSBs = banks[bank].rightSideButtons;

      final int knobsBankOffset = Twister.Bank.NUM_KNOBS * bank;
      final int sideButtonsBankOffset = Twister.Bank.NUM_SIDE_BUTTONS * bank;

      for (int knob = 0; knob < hardwareKnobs.length; ++knob) {
        final int cc = knob + knobsBankOffset;
        final MidiInfo encoder = new MidiInfo(MidiChannel.ENCODER, cc);
        final MidiInfo button = new MidiInfo(MidiChannel.BUTTON, cc);
        final LightMidiInfo rgbLight = new LightMidiInfo(MidiChannel.BUTTON,
                                                         MidiChannel.RGB_ANIMATION, cc);
        final LightMidiInfo ringLight = new LightMidiInfo(MidiChannel.ENCODER,
                                                          MidiChannel.RING_ANIMATION, cc);
        final KnobMidiInfo knobInfo = new KnobMidiInfo(encoder, button, rgbLight, ringLight);

        hardwareKnobs[knob] = new TwisterKnob(extension, knobInfo);
      }

      for (int button = 0; button < hardwareLSBs.length; ++button) {
        final int cc = sideButtonsFirstLeftCC + button + sideButtonsBankOffset;
        final MidiInfo midiInfo = new MidiInfo(MidiChannel.SIDE_BUTTON, cc);

        hardwareLSBs[button] = new TwisterButton(extension, midiInfo, "Side");
      }

      for (int button = 0; button < hardwareRSBs.length; ++button) {
        final int cc = sideButtonsFirstRightCC + button + sideButtonsBankOffset;
        final MidiInfo midiInfo = new MidiInfo(MidiChannel.SIDE_BUTTON, cc);

        hardwareRSBs[button] = new TwisterButton(extension, midiInfo, "Side");
      }
    }
  }

  /**
   * Create a listener for bank switch messages.
   *
   * Updates the internal active bank index and shows a popup notification if popups are enabled.
   */
  private void createBankSwitchListener(MidiIn midiIn)
  {
    midiIn.setMidiCallback((status, data1, data2) -> {
      // Filter everything except bank change messages
      if (status != 0xB3 || data1 > 3 || data2 != 0x7F) {
        return;
      }

      if (activeBank != data1) {
        activeBank = data1;
        showBankChangePopup(data1);
      }
    });
  }

  /** Creates a note input for the sequencer channel on the Twister. */
  private void createSequencerInput(MidiIn midiIn)
  {
    midiIn.createNoteInput("Sequencer", "?7????");
  }

  /** Shows a bank change popup notification if notifications are enabled. */
  private void showBankChangePopup(int index)
  {
    if (popupEnabled) {
      host.showPopupNotification("Twister Bank " + (index + 1));
    }
  }
}
