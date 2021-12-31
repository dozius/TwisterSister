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
 * Provides a clicked, double clicked and long press action that can be observed.
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
  private final Timer longPressTimer;
  private final HardwareButton button;

  private long lastReleasedTime;

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
   * Sets an observer of the double clicked action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observer.
   *
   * @param observer Observer to add.
   */
  public void setDoubleClickedObserver(Runnable observer)
  {
    doubleClickedObservers.clear();
    doubleClickedObservers.add(observer);
  }

  /**
   * Adds an observer of the double clicked action.
   *
   * @param  observer Observer to add.
   *
   * @return          True if the observer was already added.
   */
  public boolean addDoubleClickedObserver(Runnable observer)
  {
    return doubleClickedObservers.add(observer);
  }

  /** Clears all observers of the double clicked action. */
  public void clearDoubleClickedObservers()
  {
    doubleClickedObservers.clear();
  }

  /**
   * Sets an observer of the clicked action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observer.
   *
   * @param observer Observer to add.
   */
  public boolean setClickedObserver(Runnable observer)
  {
    clickedObservers.clear();
    return clickedObservers.add(observer);
  }

  /**
   * Adds an observer of the clicked action.
   *
   * @param  observer Observer to add.
   *
   * @return          True if the observer was already added.
   */
  public boolean addClickedObserver(Runnable observer)
  {
    return clickedObservers.add(observer);
  }

  /** Clears all observers of the clicked action. */
  public void clearClickedObservers()
  {
    clickedObservers.clear();
  }

  /**
   * Sets an observer of the long pressed action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observer.
   *
   * @param observer Observer to add.
   */
  public boolean setLongPressedObserver(Runnable observer)
  {
    longPressedObservers.clear();
    return longPressedObservers.add(observer);
  }

  /**
   * Adds an observer of the long pressed action.
   *
   * @param  observer Observer to add.
   *
   * @return          True if the observer was already added.
   */
  public boolean addLongPressedObserver(Runnable observer)
  {
    return longPressedObservers.add(observer);
  }

  /** Clears all observers of the long pressed action. */
  public void clearLongPressedObserver()
  {
    longPressedObservers.clear();
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

  private void notifyClickedObservers()
  {
    for (Runnable observer : clickedObservers) {
      observer.run();
    }
  }

  private void notifyDoubleClickedObservers()
  {
    for (Runnable observer : doubleClickedObservers) {
      observer.run();
    }
  }

  private void notifyLongPressedObservers()
  {
    for (Runnable observer : longPressedObservers) {
      observer.run();
    }
  }
}
