import cn.qingping.app.Rules;

public class RulesTest {
  static int n;

  static void check(boolean x, String label) {
    n++;
    if (!x) throw new AssertionError(label);
  }

  public static void main(String[] args) {
    for (String s : new String[] {"跳过", "跳过 5 秒", "5s 跳过", "跳过(3)", "Skip ad", "跳过广告"})
      check(Rules.isSkip(s), s);
    for (String s :
        new String[] {"跳过登录", "跳过此步骤", "不跳过", "点击跳过并安装", "skip tutorial", "跳过并同意", "", "跳过999秒"})
      check(!Rules.isSkip(s), s);
    check(!Rules.isSkip(null), "null");
    check(Rules.safeBounds(850, 80, 1030, 170, 1080, 2400), "top right");
    check(!Rules.safeBounds(0, 0, 1080, 2400, 1080, 2400), "whole screen");
    check(!Rules.safeBounds(800, 1800, 1030, 1900, 1080, 2400), "bottom");
    check(!Rules.safeBounds(-5, 10, 900, 80, 1080, 2400), "offscreen");
    check(Rules.withinWindow(9000, 0), "deadline");
    check(!Rules.withinWindow(9001, 0), "expired");
    check(!Rules.withinWindow(1, 2), "clock");
    System.out.println("PASS: " + n + " safety checks");
  }
}
