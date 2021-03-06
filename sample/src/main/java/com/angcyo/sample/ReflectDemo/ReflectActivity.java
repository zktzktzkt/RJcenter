package com.angcyo.sample.ReflectDemo;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.angcyo.sample.R;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

@SuppressWarnings("deprecation")
public class ReflectActivity extends AppCompatActivity {

    /**
     * 通过反射获取所有静态字段
     */
    public static String getAllStaticField(Class<?> target) {
        StringBuilder builder = new StringBuilder();
        try {
            Field[] declaredFields = target.getDeclaredFields();

            for (Field field : declaredFields) {
                int modifiers = field.getModifiers();//字段的修饰符
                if (Modifier.isStatic(modifiers)) {//判断是否是静态类型,
                    if (Modifier.isPrivate(modifiers)) {//是private修饰
                        field.setAccessible(true);//设置可访问权限为 public
                    }

                    Class<?> declaringClass = field.getDeclaringClass();//所在的类
                    Class<?> type = field.getType();//类型
                    builder.append(field.getName());//字段名
                    builder.append(" <font color='#ff0000'>--></font> ");
                    builder.append(type.getName());//
                    builder.append(" <font color='#ff0000'>--></font> ");
                    builder.append(field.get(null));//字段值
                    builder.append("<br>");
                }
            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    public static void binning(Object object,ArrayList<String> jsons, ArrayList<Field> targets) throws IllegalAccessException {
        if (jsons.size() < 1 || targets.size() < 1) {
            return;
        }
        String json = jsons.remove(0);
        for (int i = 0; i < targets.size(); i++) {
            Field field = targets.get(i);
            if (json.equals(field.getName())) {
                field.set(object, json);
                targets.remove(i);
                break;
            }
        }
        binning(object, jsons, targets);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reflect);
        initView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void initView() {
        TextView textView = (TextView) findViewById(R.id.text);
        textView.setText(Html.fromHtml(getAllStaticField(Build.class) + getAllStaticField(DemoClass.class)));
    }

    public static class DemoClass {
        public static final int a = 2;
        public static final long b = 3;
        public static String c = "234";
        private static String e = "23eeeeee4";
        private static String e2 = "2---23eeeeee4";
        private static String e3 = "3--323eeeeee4";
        private static String[] e3a = new String[]{"asdf", "asdf"};
        private static int[] ins = new int[]{1, 2, 3};
        private static long[] easdf3 = new long[]{1241421, 1251125, 152};
        private final String g = "final g";
        public String d = "dddd234";
        private String f = "fffffffff23eeeeee4";
    }

}
