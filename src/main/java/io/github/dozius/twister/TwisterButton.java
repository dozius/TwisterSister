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

import java.util.HashSet;
import java.util.Set;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;

import io.github.dozius.TwisterSisterExtension;

/**
 * A button on the Twister. Can be either a knob button or side button.
 *
 * Provides a clicked, double clicked and long press action that can be observed. An optional
 * "shift" button can be set for alternative actions when this button is held.
 */
public class TwisterButton
{
  private static final long DOUBLE_CLICK_DURATION = 300 * 1000000; // ms
  private static final int LONG_PRESS_DURATION = 250; // ms
  private static final int PRESSED_VALUE = 127;
  private static final int RELEASED_VALUE = 0;

  private final Set<Runnable> clickedObservers = new HashSet<>();
  private final Set<Runnable> doubleClickedObservers = new HashSet<>();
  private final Set<Runnable> longPressedObservers = new HashSet<>();
  private final Set<Runnable> shiftClickedObservers = new HashSet<>();
  private final Set<Runnable> shiftDoubleClickedObservers = new HashSet<>();
  private final Set<Runnable> shiftLongPressedObservers = new HashSet<>();
  private final Timer longPressTimer;
  private final HardwareButton button;

  private long lastReleasedTime;
  private TwisterButton shiftButton;

  /**
   * Creates a new TwisterButton.
   *
   * @param extension The parent Bitwig extension.
   * @param midiInfo  MIDI info for the button.
   * @param idPrefix  A prefix string to use when creating the hardware object ID.
   */
  public TwisterButton(TwisterSisterExtension extension, MidiInfo midiInfo, String idPrefix)
  {
    assert !idPrefix.isEmpty() : "ID prefix is empty";

    final MidiIn midiIn = extension.midiIn;
    final ControllerHost host = extension.getHost();
    final int channel = midiInfo.channel;
    final int cc = midiInfo.cc;

    button = extension.hardwareSurface.createHardwareButton(idPrefix + " Button " + midiInfo.cc);

    button.pressedAction()
          .setActionMatcher(midiIn.createCCActionMatcher(channel, cc, PRESSED_VALUE));

    button.releasedAction()
          .setActionMatcher(midiIn.createCCActionMatcher(channel, cc, RELEASED_VALUE));

    button.pressedAction()
          .setBinding(host.createAction(this::handlePressed, () -> "Handle button pressed"));

    button.releasedAction()
          .setBinding(host.createAction(this::handleReleased, () -> "Handle button released"));

    longPressTimer = new Timer(LONG_PRESS_DURATION, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt)
      {
        notifyLongPressedObservers();
      }
    });

    longPressTimer.setRepeats(false);
  }

  /**
   * Sets the button that will be used for shift functionality.
   *
   * @param shiftButton The button to use for shift functionality.
   */
  public void setShiftButton(TwisterButton shiftButton)
  {
    this.shiftButton = shiftButton;
  }

  /**
   * Sets an observer of the double clicked action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observer.
   *
   * @param observer Observer to set to double click action.
   */
  public void setDoubleClickedObserver(Runnable observer)
  {
    doubleClickedObservers.clear();
    doubleClickedObservers.add(observer);
  }

  /**
   * Sets an observer of the shift double clicked action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observers.
   *
   * @param shiftObserver Observer to set to shift double click action.
   */
  public void setShiftDoubleClickedObserver(Runnable shiftObserver)
  {
    shiftDoubleClickedObservers.clear();
    shiftDoubleClickedObservers.add(shiftObserver);
  }

  /**
   * Adds an observer of the double clicked action.
   *
   * @param  observer Observer to add to double click action.
   *
   * @return          True if the observer was already added.
   */
  public boolean addDoubleClickedObserver(Runnable observer)
  {
    return doubleClickedObservers.add(observer);
  }

  /**
   * Adds an observer of the shift double clicked action.
   *
   * @param  shiftObserver Observer to add to shift double click action.
   *
   * @return               True if the observer was already added.
   */
  public boolean addShiftDoubleClickedObserver(Runnable shiftObserver)
  {
    return shiftDoubleClickedObservers.add(shiftObserver);
  }

  /** Clears all observers of the double clicked action. */
  public void clearDoubleClickedObservers()
  {
    doubleClickedObservers.clear();
  }

  /** Clears all observers of the shift double clicked action. */
  public void clearShiftDoubleClickedObservers()
  {
    shiftDoubleClickedObservers.clear();
  }

  /**
   * Sets an observer of the clicked action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observer.
   *
   * @param observer Observer to set to the clicked action.
   */
  public boolean setClickedObserver(Runnable observer)
  {
    clickedObservers.clear();
    return clickedObservers.add(observer);
  }

  /**
   * Sets an observer of the shift clicked action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observer.
   *
   * @param shiftObserver Observer to set to the shift clicked action.
   */
  public boolean setShiftClickedObserver(Runnable shiftObserver)
  {
    shiftClickedObservers.clear();
    return shiftClickedObservers.add(shiftObserver);
  }

  /**
   * Adds an observer of the clicked action.
   *
   * @param  observer Observer to add to the clicked action.
   *
   * @return          True if the observer was already added.
   */
  public boolean addClickedObserver(Runnable observer)
  {
    return clickedObservers.add(observer);
  }

  /**
   * Adds an observer of the shift clicked action.
   *
   * @param  shiftObserver Observer to add to the shift clicked action.
   *
   * @return               True if the observer was already added.
   */
  public boolean addShiftClickedObserver(Runnable shiftObserver)
  {
    return shiftClickedObservers.add(shiftObserver);
  }

  /** Clears all observers of the clicked action. */
  public void clearClickedObservers()
  {
    clickedObservers.clear();
  }

  /** Clears all observers of the shift clicked action. */
  public void clearShiftClickedObservers()
  {
    shiftClickedObservers.clear();
  }

  /**
   * Sets an observer of the long pressed action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observer.
   *
   * @param observer Observer to set to the long pressed action.
   */
  public boolean setLongPressedObserver(Runnable observer)
  {
    longPressedObservers.clear();
    return longPressedObservers.add(observer);
  }

  /**
   * Sets an observer of the shift long pressed action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observer.
   *
   * @param shiftObserver Observer to set to the long pressed action.
   */
  public boolean setShiftLongPressedObserver(Runnable shiftObserver)
  {
    shiftLongPressedObservers.clear();
    return shiftLongPressedObservers.add(shiftObserver);
  }

  /**
   * Adds an observer of the long pressed action.
   *
   * @param  observer Observer to add to the long pressed action.
   *
   * @return          True if the observer was already added.
   */
  public boolean addLongPressedObserver(Runnable observer)
  {
    return longPressedObservers.add(observer);
  }

  /**
   * Adds an observer of the shift long pressed action.
   *
   * @param  shiftObserver Observer to add to the shift long pressed action.
   *
   * @return               True if the observer was already added.
   */
  public boolean addShiftLongPressedObserver(Runnable shiftObserver)
  {
    return shiftLongPressedObservers.add(shiftObserver);
  }

  /** Clears all observers of the long pressed action. */
  public void clearLongPressedObserver()
  {
    longPressedObservers.clear();
  }

  /** Clears all observers of the shift long pressed action. */
  public void clearShiftLongPressedObserver()
  {
    shiftLongPressedObservers.clear();
  }

  /** Returns the buttons pressed state. */
  public BooleanValue isPressed()
  {
    return button.isPressed();
  }

  /** Internal handler of the hardware pressed action. */
  private void handlePressed()
  {
    longPressTimer.start();
  }

  /** Internal handler of the hardware released action. */
  private void handleReleased()
  {
    if (longPressTimer.isRunning()) {
      longPressTimer.stop();
      notifyClickedObservers();

      long now = System.nanoTime();

      if ((now - lastReleasedTime) < DOUBLE_CLICK_DURATION) {
        notifyDoubleClickedObservers();
      }

      lastReleasedTime = now;
    }
  }

  private boolean isShiftPressed()
  {
    if (shiftButton == null) {
      return false;
    }

    return shiftButton.isPressed().get();
  }

  private void notifyClickedObservers()
  {
    for (Runnable observer : isShiftPressed() ? shiftClickedObservers : clickedObservers) {
      observer.run();
    }
  }

  private void notifyDoubleClickedObservers()
  {
    for (Runnable observer : isShiftPressed() ? shiftDoubleClickedObservers
                                              : doubleClickedObservers) {
      observer.run();
    }
  }

  private void notifyLongPressedObservers()
  {
    for (Runnable observer : isShiftPressed() ? shiftLongPressedObservers : longPressedObservers) {
      observer.run();
    }
  }
}
