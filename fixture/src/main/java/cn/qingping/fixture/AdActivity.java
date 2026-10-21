package cn.qingping.fixture;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

public class AdActivity extends Activity {
  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    FrameLayout root = new FrameLayout(this);
    setContentView(root);
    TextView label = new TextView(this);
    label.setText("独立应用广告模拟");
    label.setGravity(Gravity.CENTER);
    root.addView(label, new FrameLayout.LayoutParams(-1, -1));
    Button skip = new Button(this);
    skip.setText("跳过");
    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(240, 140, Gravity.TOP | Gravity.END);
    lp.topMargin = 110;
    lp.setMarginEnd(24);
    root.addView(skip, lp);
    skip.setOnClickListener(
        v -> {
          root.removeView(skip);
          label.setText("广告已跳过");
        });
  }
}
