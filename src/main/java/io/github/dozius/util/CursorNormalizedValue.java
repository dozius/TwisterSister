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

import java.util.HashSet;
import java.util.Set;

import com.bitwig.extension.callback.DoubleValueChangedCallback;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.TrackBank;

/**
 * Wraps a cursor and bank in order to provide a normalized 0-1 value for the cursor position.
 *
 * Sends -1.0 if there are no items in the bank.
 */
public class CursorNormalizedValue
{
  private final Set<DoubleValueChangedCallback> observers = new HashSet<>();
  private int cursorIndex = -1;
  private int cursorCount = -1;

  /**
   * Creates a wrapper for a track cursor.
   *
   * @param cursorTrack The track cursor to wrap.
   * @param trackBank   The bank for the cursor.
   */
  public CursorNormalizedValue(CursorTrack cursorTrack, TrackBank trackBank)
  {
    trackBank.channelCount().markInterested();
    trackBank.channelCount().addValueObserver(this::setCursorCount);

    cursorTrack.position().markInterested();
    cursorTrack.position().addValueObserver(this::setCursorIndex);
  }

  /**
   * Creates a wrapper for a device cursor.
   *
   * @param cursorDevice The device cursor to wrap.
   * @param deviceBank   The bank for the cursor.
   */
  public CursorNormalizedValue(CursorDevice cursorDevice, DeviceBank deviceBank)
  {
    deviceBank.itemCount().markInterested();
    deviceBank.itemCount().addValueObserver(this::setCursorCount);

    cursorDevice.position().markInterested();
    cursorDevice.position().addValueObserver(this::setCursorIndex);
  }

  /**
   * Creates a wrapper for remote controls pages.
   *
   * @param cursorRemoteControlsPage The remote control pages to wrap.
   */
  public CursorNormalizedValue(CursorRemoteControlsPage cursorRemoteControlsPage)
  {
    cursorRemoteControlsPage.pageCount().markInterested();
    cursorRemoteControlsPage.pageCount().addValueObserver(this::setCursorCount);

    cursorRemoteControlsPage.selectedPageIndex().markInterested();
    cursorRemoteControlsPage.selectedPageIndex().addValueObserver(this::setCursorIndex);
  }

  /**
   * Adds and observer for the wrapped value.
   *
   * @param  callback The observer callback.
   *
   * @return          True if this set did not already contain the specified element.
   */
  public Boolean addValueObserver(DoubleValueChangedCallback callback)
  {
    return observers.add(callback);
  }

  /** Handles when the cursor index changes. */
  private void setCursorIndex(int index)
  {
    cursorIndex = index;
    updateCursorFeedback();
  }

  /** Handles when the bank item count changes. */
  private void setCursorCount(int count)
  {
    cursorCount = count;
    updateCursorFeedback();
  }

  /** Generates the normalized value and notifies observers. */
  private void updateCursorFeedback()
  {
    if (cursorCount < 1 || cursorIndex < 0) {
      notifyObservers(-1);
      return;
    }

    if (cursorCount < 2) {
      notifyObservers(0);
      return;
    }

    final double normalized = cursorIndex / (cursorCount - 1.0);
    notifyObservers(MathUtil.clamp(normalized, 0.0, 1.0));
  }

  private void notifyObservers(double value)
  {
    for (DoubleValueChangedCallback observer : observers) {
      observer.valueChanged(value);
    }
  }
}
