package cn.qingping.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class SmokeInstrumentation extends Instrumentation {
  private int checks;

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    start();
  }

  private void check(boolean value, String name) {
    checks++;
    if (!value) throw new AssertionError(name);
  }

  private Activity launch(Class<?> cls, String label) {
    Intent i = new Intent(getTargetContext(), cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (label != null) i.putExtra("label", label);
    Activity a = startActivitySync(i);
    waitForIdleSync();
    return a;
  }

  private int count() {
    return Store.prefs(getTargetContext()).getInt("attempt:cn.qingping.app", 0);
  }

  private void close(Activity a) {
    runOnMainSync(a::finish);
    waitForIdleSync();
  }

  private void configure(boolean active, boolean enabled) {
    Store.prefs(getTargetContext())
        .edit()
        .clear()
        .putBoolean("active", active)
        .putBoolean("app:cn.qingping.app", enabled)
        .commit();
  }

  private Button findButton(View v, String label) {
    if (v instanceof Button && label.contentEquals(((Button) v).getText())) return (Button) v;
    if (v instanceof android.view.ViewGroup) {
      var g = (android.view.ViewGroup) v;
      for (int i = 0; i < g.getChildCount(); i++) {
        Button b = findButton(g.getChildAt(i), label);
        if (b != null) return b;
      }
    }
    return null;
  }

  @Override
  public void onStart() {
    Bundle result = new Bundle();
    try {
      // Instrumentation force-stops the target process: reconnect only in this test device.
      UiAutomation automation =
          getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
      for (String command :
          new String[] {
            "settings put secure enabled_accessibility_services ''",
            "settings put secure enabled_accessibility_services cn.qingping.app/.SkipService",
            "settings put secure accessibility_enabled 1"
          }) {
        try (ParcelFileDescriptor fd = automation.executeShellCommand(command);
            java.io.FileInputStream stream = new java.io.FileInputStream(fd.getFileDescriptor())) {
          while (stream.read() != -1) {}
        }
      }
      configure(true, true);
      Activity home = launch(MainActivity.class, null);
      for (String label : new String[] {"应用", "统计", "设置", "首页"})
        runOnMainSync(
            () -> {
              Button b = findButton(home.getWindow().getDecorView(), label);
              check(b != null, "navigation " + label);
              b.performClick();
            });
      long deadline = SystemClock.elapsedRealtime() + 6000;
      while (!SkipService.connected && SystemClock.elapsedRealtime() < deadline)
        SystemClock.sleep(100);
      check(SkipService.connected, "accessibility service connected");
      Activity demo = launch(DemoActivity.class, null);
      SystemClock.sleep(2300);
      check(count() == 1, "service auto click exactly once");
      check(
          Store.prefs(getTargetContext()).getInt("observed:cn.qingping.app", 0) == 1,
          "disappearance recorded");
      SystemClock.sleep(1000);
      check(count() == 1, "no duplicate");
      close(demo);
      configure(true, true);
      demo = launch(DemoActivity.class, "跳过登录");
      SystemClock.sleep(1500);
      check(count() == 0, "login never skipped");
      close(demo);
      configure(false, true);
      demo = launch(DemoActivity.class, null);
      SystemClock.sleep(1500);
      check(count() == 0, "pause prevents click");
      close(demo);
      configure(true, false);
      demo = launch(DemoActivity.class, null);
      SystemClock.sleep(1500);
      check(count() == 0, "disabled app prevents click");
      close(demo);
      configure(true, false);
      Store.prefs(getTargetContext()).edit().putBoolean("app:cn.qingping.fixture", true).commit();
      getTargetContext()
          .startActivity(
              new Intent()
                  .setClassName("cn.qingping.fixture", "cn.qingping.fixture.AdActivity")
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      long fixtureDeadline = SystemClock.elapsedRealtime() + 6000;
      while (Store.prefs(getTargetContext()).getInt("observed:cn.qingping.fixture", 0) == 0
          && SystemClock.elapsedRealtime() < fixtureDeadline) SystemClock.sleep(100);
      check(
          Store.prefs(getTargetContext()).getInt("attempt:cn.qingping.fixture", 0) == 1,
          "external app clicked once");
      check(
          Store.prefs(getTargetContext()).getInt("observed:cn.qingping.fixture", 0) == 1,
          "external app disappearance recorded");
      automation.performGlobalAction(
          android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
      configure(true, false);
      close(home);
      result.putString("stream", "PASS: " + checks + " Android smoke checks\n");
      finish(Activity.RESULT_OK, result);
    } catch (Throwable e) {
      result.putString("stream", "FAIL: " + android.util.Log.getStackTraceString(e));
      finish(Activity.RESULT_CANCELED, result);
    }
  }
}
