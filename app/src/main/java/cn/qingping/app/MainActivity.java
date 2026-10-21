package cn.qingping.app;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  private LinearLayout content;
  private int page = 0;
  private TextView statusView;
  private final Handler statusHandler = new Handler(Looper.getMainLooper());
  private final Runnable refreshStatus =
      new Runnable() {
        @Override
        public void run() {
          updateStatus();
          statusHandler.postDelayed(this, 1000);
        }
      };

  private void updateStatus() {
    if (statusView != null)
      statusView.setText(
          SkipService.connected
              ? (Store.active(this) ? "● 服务已连接 · 自动跳过已开启" : "● 服务已连接 · 已暂停")
              : "○ 服务未连接");
  }

  @Override
  public void onPause() {
    statusHandler.removeCallbacks(refreshStatus);
    super.onPause();
  }

  private final String[] names = {"百度地图", "百度贴吧", "淘宝", "京东", "美团", "懂球帝"};
  private final String[] packages = {
    "com.baidu.BaiduMap",
    "com.baidu.tieba",
    "com.taobao.taobao",
    "com.jingdong.app.mall",
    "com.sankuai.meituan",
    "com.dongqiudi.news"
  };

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    if (b != null) page = b.getInt("page");
  }

  @Override
  public void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putInt("page", page);
  }

  @Override
  public void onResume() {
    super.onResume();
    render();
    statusHandler.post(refreshStatus);
  }

  private int dp(int n) {
    return (int) (n * getResources().getDisplayMetrics().density);
  }

  private TextView text(String s, int size) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(size);
    t.setTextColor(0xff173e38);
    t.setPadding(0, dp(9), 0, dp(9));
    content.addView(t);
    return t;
  }

  private void button(String title, Runnable r) {
    Button b = new Button(this);
    b.setText(title);
    b.setAllCaps(false);
    content.addView(b, new LinearLayout.LayoutParams(-1, -2));
    b.setOnClickListener(v -> r.run());
  }

  private void render() {
    statusView = null;
    LinearLayout base = new LinearLayout(this);
    base.setOrientation(LinearLayout.VERTICAL);
    base.setBackgroundColor(0xfff4f8f6);
    base.setPadding(dp(20), dp(12), dp(20), dp(12));
    base.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
          v.setPadding(dp(20), bars.top + dp(12), dp(20), bars.bottom + dp(12));
          return insets;
        });
    setContentView(base);
    WindowInsetsController controller = getWindow().getInsetsController();
    if (controller != null)
      controller.setSystemBarsAppearance(
          WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
              | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
          WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
              | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
    TextView title = new TextView(this);
    title.setText("清屏");
    title.setTextSize(32);
    title.setTextColor(0xff107c72);
    base.addView(title);
    LinearLayout nav = new LinearLayout(this);
    base.addView(nav);
    String[] tabs = {"首页", "应用", "统计", "设置"};
    for (int i = 0; i < 4; i++) {
      final int p = i;
      Button b = new Button(this);
      b.setText(tabs[i]);
      b.setTextSize(13);
      nav.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
      b.setOnClickListener(
          v -> {
            page = p;
            render();
          });
    }
    ScrollView scroll = new ScrollView(this);
    base.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    scroll.addView(content);
    if (page == 0) home();
    else if (page == 1) apps();
    else if (page == 2) stats();
    else settings();
  }

  private void home() {
    text("少一点打扰，多一点清爽", 23);
    statusView = text("", 20);
    updateStatus();
    text("启动应用后的 9 秒内，识别右上方明确的跳过控件。每次进入应用最多执行一次点击。只处理你主动开启的应用。", 16);
    Switch active = new Switch(this);
    active.setText("启用自动跳过");
    active.setChecked(Store.active(this));
    content.addView(active);
    active.setOnCheckedChangeListener(
        (v, on) -> {
          Store.prefs(this).edit().putBoolean("active", on).apply();
          render();
        });
    button(
        "开启无障碍服务",
        () ->
            new AlertDialog.Builder(this)
                .setTitle("启用前请了解")
                .setMessage(
                    "清屏需要读取界面控件并代你点击跳过。仅本地处理，不联网、不保存屏幕文字或截图。\n\n请在系统无障碍设置中选择清屏并手动启用；你可以随时暂停。")
                .setNegativeButton("取消", null)
                .setPositiveButton(
                    "前往设置", (d, w) -> open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .show());
    button(
        "选择需要处理的应用",
        () -> {
          page = 1;
          render();
        });
    button(
        "运行本机演示",
        () -> {
          Store.prefs(this).edit().putBoolean("app:" + getPackageName(), true).apply();
          startActivity(new Intent(this, DemoActivity.class));
        });
    text("0.1.0 测试版\n已为小米 15 / HyperOS 3 / Android 16 准备设置指引。通用规则不保证覆盖所有广告。暂不阻断跨应用跳转或摇一摇广告。", 14);
  }

  private boolean installed(String p) {
    try {
      getPackageManager().getApplicationInfo(p, 0);
      return true;
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  private void row(String name, String p) {
    boolean present = installed(p);
    Switch s = new Switch(this);
    s.setText(present ? name : getString(R.string.app_unavailable, name));
    s.setPadding(0, dp(12), 0, dp(12));
    s.setChecked(Store.enabled(this, p));
    s.setEnabled(present);
    content.addView(s);
    s.setOnCheckedChangeListener(
        (v, on) -> Store.prefs(this).edit().putBoolean("app:" + p, on).apply());
  }

  private void apps() {
    text("优先应用", 22);
    text("以下应用使用通用规则，尚不代表对应版本广告适配通过。默认关闭，请按需开启。", 14);
    HashSet<String> seen = new HashSet<>();
    for (int i = 0; i < names.length; i++) {
      row(names[i], packages[i]);
      seen.add(packages[i]);
    }
    text("其他应用", 22);
    List<ResolveInfo> list =
        getPackageManager()
            .queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
    list.sort(
        (a, b) ->
            a.loadLabel(getPackageManager())
                .toString()
                .compareTo(b.loadLabel(getPackageManager()).toString()));
    for (ResolveInfo r : list) {
      String p = r.activityInfo.packageName;
      if (seen.add(p)) row(r.loadLabel(getPackageManager()).toString(), p);
    }
  }

  private String label(String p) {
    try {
      return getPackageManager()
          .getApplicationLabel(getPackageManager().getApplicationInfo(p, 0))
          .toString();
    } catch (Exception e) {
      return p;
    }
  }

  private void stats() {
    text("本机动作统计", 22);
    text("“点击接受”表示系统接受了点击请求；“控件消失”表示随后在同一应用中未再发现跳过控件，不等于广告成功拦截或跳转被阻止。", 14);
    TreeSet<String> apps = new TreeSet<>();
    for (String k : Store.prefs(this).getAll().keySet())
      if (k.startsWith("attempt:")) apps.add(k.substring(8));
    if (apps.isEmpty()) text("还没有自动点击记录", 18);
    for (String p : apps)
      text(
          label(p)
              + "\n点击接受 "
              + Store.prefs(this).getInt("attempt:" + p, 0)
              + " 次  ·  控件消失 "
              + Store.prefs(this).getInt("observed:" + p, 0)
              + " 次",
          17);
    text("最近记录", 20);
    TextView log = text(Store.prefs(this).getString("log", "暂无记录"), 13);
    log.setTextIsSelectable(true);
    button(
        "清空统计",
        () ->
            new AlertDialog.Builder(this)
                .setTitle("清空本机统计？")
                .setMessage("应用开关和权限不受影响。此操作不可撤销。")
                .setNegativeButton("取消", null)
                .setPositiveButton(
                    "清空",
                    (d, w) -> {
                      var e = Store.prefs(this).edit();
                      for (String k : Store.prefs(this).getAll().keySet())
                        if (k.startsWith("attempt:")
                            || k.startsWith("observed:")
                            || k.equals("log")) e.remove(k);
                      e.apply();
                      render();
                    })
                .show());
  }

  private void settings() {
    text("小米 15 设置指引", 22);
    text(
        "适配目标：3.0.305.0.WOCCNXM.C11 / Android 16\n当前设备："
            + Build.MANUFACTURER
            + " "
            + Build.MODEL
            + " / Android "
            + Build.VERSION.RELEASE,
        14);
    text("1 · 无障碍\n在系统设置中开启“清屏 · 开屏跳过”。安装包若触发受限设置，请在信任本应用后，根据系统提示手动授权。", 16);
    button("打开无障碍设置", () -> open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    text("2 · 自启动与电池\n在设置中搜索“自启动”，允许清屏自启动；在应用电池设置中按需选择无限制。菜单名称以当前系统为准。", 16);
    button(
        "打开应用信息",
        () ->
            open(
                new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()))));
    button(
        "打开电池优化列表", () -> open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
    text("3 · 后台稳定性\n可在最近任务中锁定清屏。重启、清理后台后返回首页确认“服务已连接”。强行停止后不能保证恢复，需要重新打开并检查无障碍服务。", 16);
    text(
        "隐私与边界\n"
            + "本应用无联网权限；不收集账号、截图、输入内容。记录仅含应用包名、时间及动作结果。\n\n"
            + "不提供全局广告网络过滤、强制保活、跨应用跳转阻断。不开启 Root、Shizuku 或悬浮窗权限。",
        15);
  }

  private void open(Intent i) {
    try {
      startActivity(i);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, "当前系统没有此入口，请在系统设置中搜索相关项目", Toast.LENGTH_LONG).show();
    }
  }
}
