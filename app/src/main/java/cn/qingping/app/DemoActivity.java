package cn.qingping.app;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class DemoActivity extends Activity {
  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    SkipService.beginDemo();
    FrameLayout root = new FrameLayout(this);
    root.setBackgroundColor(0xffe5f2ee);
    setContentView(root);
    TextView label = new TextView(this);
    label.setText("清屏 · 本机演示\n\n此页模拟开屏广告\n启用清屏自身后可测试自动跳过\n也可手动点击右上角");
    label.setTextSize(22);
    label.setGravity(Gravity.CENTER);
    root.addView(label, new FrameLayout.LayoutParams(-1, -1));
    Button skip = new Button(this);
    skip.setText(
        getIntent().getStringExtra("label") == null ? "跳过" : getIntent().getStringExtra("label"));
    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(240, 140, Gravity.TOP | Gravity.END);
    lp.topMargin = 110;
    lp.setMarginEnd(24);
    root.addView(skip, lp);
    skip.setOnClickListener(
        v -> {
          root.removeView(skip);
          label.setText("演示页已关闭\n\n请返回清屏查看动作记录。\n手动点击不会计入自动点击统计。");
        });
  }
}
