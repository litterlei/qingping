package cn.qingping.app;

public final class Rules {
  public static final long WINDOW_MS = 9000;

  public static boolean isSkip(String s) {
    if (s == null) return false;
    String t = s.trim().replaceAll("\\s+", "");
    return t.matches("(?i)(跳过|跳过广告|skip|skipad)([（(]?\\d{1,2}[sS秒]?[）)]?)?")
        || t.matches("(?i)[（(]?\\d{1,2}[sS秒]?[）)]?(跳过|跳过广告|skip)");
  }

  public static boolean safeBounds(int l, int t, int r, int b, int w, int h) {
    return w > 0
        && h > 0
        && l >= 0
        && t >= 0
        && r <= w
        && b <= h
        && r > l
        && b > t
        && (r - l) < w * 0.48
        && (b - t) < h * 0.16
        && t < h * 0.4
        && r > w * 0.5;
  }

  public static boolean withinWindow(long now, long start) {
    return now >= start && now - start <= WINDOW_MS;
  }

  private Rules() {}
}
