package io.github.dozius.util;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Track;

/**
 * Wraps a track cursor to provide group navigation.
 */
public class TrackGroupNavigator
{
  private final Track parentTrack;
  private CursorTrack cursorTrack;

  /**
   * Creates a TrackGroupNavigator.
   *
   * To navigate in and out of groups call navigateGroups().
   *
   * @param cursorTrack The TrackCursor to use for navigation.
   */
  public TrackGroupNavigator(CursorTrack cursorTrack)
  {
    this.cursorTrack = cursorTrack;
    parentTrack = cursorTrack.createParentTrack(0, 0);

    cursorTrack.isGroup().markInterested();
    cursorTrack.isGroupExpanded().markInterested();
    parentTrack.isGroupExpanded().markInterested();
  }

  /**
   * Navigates groups when called.
   *
   * If the current track is a group, then it will be unfolded and the first child track will be
   * selected.
   *
   * If the current track is not a group, then it will attempt to navigate up to the parent track if
   * one exists. If fold is true, then the parent group is automatically folded when navigating out
   * of the group.
   *
   * @param fold When true, a group is folded upon exiting the group. When false, the group will
   *             remain unfolded on exiting.
   */
  public void navigateGroups(boolean fold)
  {
    if (cursorTrack.isGroup().get()) {
      cursorTrack.isGroupExpanded().set(true);
      cursorTrack.selectFirstChild();
    }
    else {
      if (parentTrack == null) {
        return;
      }

      cursorTrack.selectParent();

      if (fold) {
        cursorTrack.isGroupExpanded().set(false);
      }
    }
  }
}
