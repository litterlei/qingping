package cn.qingping.app;

import android.content.*;
import java.text.SimpleDateFormat;
import java.util.*;

final class Store {
  static android.content.SharedPreferences prefs(Context c) {
    return c.getSharedPreferences("qingping", 0);
  }

  static boolean active(Context c) {
    return prefs(c).getBoolean("active", true);
  }

  static boolean enabled(Context c, String p) {
    return prefs(c).getBoolean("app:" + p, false);
  }

  static void record(Context c, String p, String kind) {
    var s = prefs(c);
    String k = kind + ":" + p;
    String date = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
    String log =
        date
            + "  "
            + p
            + "  "
            + (kind.equals("attempt") ? "点击动作已接受" : "跳过控件已消失")
            + "\n"
            + s.getString("log", "");
    if (log.length() > 12000) log = log.substring(0, 12000);
    s.edit().putInt(k, s.getInt(k, 0) + 1).putString("log", log).apply();
  }
}
