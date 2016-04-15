package cn.edu.uoh.cs.weatherforecast;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String tag = "MainActivity";

    private CityList cityList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // 恢复保存的城市名称
        reloadCityList();
        // 刷新天气
        refreshWeatherInfo();
    }

    /**
     * 刷新天气情况
     */
    private void refreshWeatherInfo() {
        // 城市名称
        String cityName = cityList.getCurrentCityName();
        Log.i(tag, "refresh weather: " + cityName);
        // 修改工具栏标题
        if (cityName == null) {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setTitle("请添加城市");
            return;
        }
        // 调用异步任务类，访问网络，获取数据
        NetTask task = new NetTask();
        task.execute(cityName);
    }

    /**
     * 重新加载城市名列表
     */
    private void reloadCityList() {
        if (cityList == null) {
            cityList = CityList.load(this);
        } else {
            cityList.reload(this);
        }
    }

    private void showForecastList(String json) {
        // 把json字符串转换为WeatherInfo对象
        Gson gson = new Gson();
        WeatherInfo weatherInfo = gson.fromJson(json, WeatherInfo.class);

        // 判断是否正确的获取的数据，如果不是，显示错误信息，并返回
        if (!weatherInfo.getDesc().equals("OK")) {
            showError("获取天气信息出错。");
        }
        // 修改工具栏标题
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        String cityName = weatherInfo.getData().getCity();
        if (cityName != null) {
            toolbar.setTitle(cityName);
        } else {
            toolbar.setTitle("请添加城市");
            return;
        }
        // 把WeatherInfo对象转换为List<Map<String, Object>>对象
        List<Map<String, Object>> dataList = WeatherInfo2SimpleAdapterData.convert(weatherInfo);

        // 创建SimpleAdapter对象
        SimpleAdapter adapter = new SimpleAdapter(this, dataList,
                R.layout.list_item,
                new String[]{"date", "temperature", "weather", "wind"},
                new int[]{R.id.txtDate, R.id.txtTemperature, R.id.txtWeather, R.id.txtWind}
        );

        // 获取ListView并设置它的Adapter
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);
    }

    /**
     * 显示错误信息提示框
     *
     * @param msg 显示的错误消息
     */
    public void showError(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("出错了")
                .setMessage(msg)
                .setPositiveButton("确定",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                            }
                        }).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        // 刷新天气情况
        if (id == R.id.action_refresh) {
            refreshWeatherInfo();
            return true;
        }
        // 设置城市列表
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, CityListActivity.class);
            startActivityForResult(intent, 1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 重CityListActivity返回后执行此方法：
     * 重新获取城市列表
     * 刷新天气情况
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 重新获取城市列表
        reloadCityList();
        // 刷新天气情况
        refreshWeatherInfo();
    }

    // 定义内部类（异步任务），用于通过网络获取数据（Android不能在主线程中访问网络）
    class NetTask extends AsyncTask<String, Void, String> {
        IOException exception = null;

        @Override
        protected String doInBackground(String... params) {
            try {
                // 获取调用execute方法时传入的参数：城市名
                String cityName = params[0];
                Log.i(tag, "Get weather： " + cityName);
                // 获取天气信息，得到的是Json字符串
                String json = HttpTools.getWeatherInfo(cityName);
                Log.i(tag, json);
                return json;
            } catch (IOException e) {
                // 如果有异常，记录
                exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (exception == null) {
                // 显示获取的信息
                // TextView txtInfo = (TextView) findViewById(R.id.txtInfo);
                // txtInfo.setText(result);
                showForecastList(result);
            } else {
                // 如果有异常，显示错误消息
                showError("获取天气信息失败：" + exception.getMessage());
            }
        }
    }
}
