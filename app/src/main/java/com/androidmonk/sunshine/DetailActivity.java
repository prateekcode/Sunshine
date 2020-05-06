package com.androidmonk.sunshine;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ShareCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class DetailActivity extends AppCompatActivity {

    private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
    private TextView mWeatherData;
    private String mForecast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        mWeatherData = (TextView) findViewById(R.id.tv_weather_display);

        Intent intentThatStarted = getIntent();

        if (intentThatStarted !=null){
            if (intentThatStarted.hasExtra(Intent.EXTRA_TEXT)){
                mForecast = intentThatStarted.getStringExtra(Intent.EXTRA_TEXT);
                mWeatherData.setText(mForecast);
            }
        }
    }


    private Intent createShareForecastIntent(){
        Intent shareIntent = ShareCompat.IntentBuilder
                .from(this)
                .setType("text/plain")
                .setText(mForecast + FORECAST_SHARE_HASHTAG)
                .getIntent();

        return shareIntent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail, menu);
        MenuItem menuItem = menu.findItem(R.id.action_share);
        menuItem.setIntent(createShareForecastIntent());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_setting){
            Intent startSetting = new Intent(this, SettingActivity.class);
            startActivity(startSetting);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
