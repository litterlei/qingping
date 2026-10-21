package cn.qingping.app;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.*;
import android.view.accessibility.*;
import java.util.*;

public class SkipService extends AccessibilityService {
  static volatile boolean connected;
  private static SkipService running;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private String foreground = "";
  private long started;
  private boolean acted;
  private int generation;

  @Override
  public void onServiceConnected() {
    connected = true;
    running = this;
  }

  @Override
  public void onDestroy() {
    connected = false;
    running = null;
    handler.removeCallbacksAndMessages(null);
    super.onDestroy();
  }

  @Override
  public void onInterrupt() {
    handler.removeCallbacksAndMessages(null);
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent e) {
    if (!Store.active(this) || e.getPackageName() == null) return;
    String p = e.getPackageName().toString();
    long now = SystemClock.elapsedRealtime();
    if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !p.equals(foreground))
      startSession(p);
    if (!p.equals(foreground)
        || acted
        || !Store.enabled(this, p)
        || !Rules.withinWindow(now, started)) return;
    AccessibilityNodeInfo root = getRootInActiveWindow();
    if (root == null || root.getPackageName() == null || !p.contentEquals(root.getPackageName()))
      return;
    AccessibilityNodeInfo target = find(root);
    if (target == null) return;
    if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
      acted = true;
      Store.record(this, p, "attempt");
      int g = generation;
      handler.postDelayed(
          () -> {
            if (g != generation || !Store.active(this) || !Store.enabled(this, p)) return;
            AccessibilityNodeInfo next = getRootInActiveWindow();
            if (next != null
                && next.getPackageName() != null
                && p.contentEquals(next.getPackageName())
                && find(next) == null) Store.record(this, p, "observed");
          },
          700);
    }
  }

  // The in-app demo starts before content events arrive, unlike external package changes.
  static void beginDemo() {
    if (running != null) running.startSession(running.getPackageName());
  }

  private void startSession(String packageName) {
    foreground = packageName;
    started = SystemClock.elapsedRealtime();
    acted = false;
    generation++;
  }

  private AccessibilityNodeInfo find(AccessibilityNodeInfo root) {
    Rect screen = new Rect();
    root.getBoundsInScreen(screen);
    ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<>();
    q.add(root);
    int visited = 0;
    while (!q.isEmpty() && visited++ < 250) {
      AccessibilityNodeInfo node = q.removeFirst();
      if (node.isVisibleToUser()
          && (Rules.isSkip(text(node.getText()))
              || Rules.isSkip(text(node.getContentDescription())))) {
        AccessibilityNodeInfo button = node;
        for (int i = 0; i < 3 && button != null; i++, button = button.getParent()) {
          Rect b = new Rect();
          button.getBoundsInScreen(b);
          if (button.isClickable()
              && button.isEnabled()
              && Rules.safeBounds(
                  b.left - screen.left,
                  b.top - screen.top,
                  b.right - screen.left,
                  b.bottom - screen.top,
                  screen.width(),
                  screen.height())) return button;
        }
      }
      for (int i = 0; i < node.getChildCount() && q.size() < 250; i++) {
        var child = node.getChild(i);
        if (child != null) q.add(child);
      }
    }
    return null;
  }

  private String text(CharSequence s) {
    return s == null ? null : s.toString();
  }
}
