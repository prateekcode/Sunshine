package com.androidmonk.sunshine;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androidmonk.sunshine.data.SunshinePreferences;
import com.androidmonk.sunshine.utilities.NetworkUtils;
import com.androidmonk.sunshine.utilities.OpenWeatherJsonUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private TextView mWeatherTextView, mErrorTextMsg;
    private ProgressBar mLoadingContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWeatherTextView = (TextView) findViewById(R.id.tv_weather_data);
        mErrorTextMsg = (TextView) findViewById(R.id.error_text_view);
        mLoadingContent = (ProgressBar) findViewById(R.id.pb_content_loading);
        loadWeatherData();

    }

    private void loadWeatherData(){
        showWeatherDataView();
        String location = SunshinePreferences.getPreferredWeatherLocation(this);
        new FetchWeatherTask().execute(location);

    }

    private void showWeatherDataView(){
        mErrorTextMsg.setVisibility(View.INVISIBLE);
        mWeatherTextView.setVisibility(View.VISIBLE);
    }

    private void showErrorMessage(){
        mErrorTextMsg.setVisibility(View.VISIBLE);
        mWeatherTextView.setVisibility(View.INVISIBLE);
    }



    public class FetchWeatherTask extends AsyncTask<String, URL, String[]>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingContent.setVisibility(View.VISIBLE);
        }

        @Override
        protected String[] doInBackground(String... params) {
            if (params.length == 0){
                return null;
            }

            String location = params[0];
            URL weatherRequestUrl = NetworkUtils.buildUrl(location);
            try {
                String jsonWeatherResponse = NetworkUtils.getResponseFromHttpUrl(weatherRequestUrl);
                String[] simpleJsonWeatherData = OpenWeatherJsonUtils.getSimpleWeatherStringsFromJson(MainActivity.this, jsonWeatherResponse);
                return simpleJsonWeatherData;
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] weatherData) {
            mLoadingContent.setVisibility(View.INVISIBLE);
            if (weatherData != null){
                showWeatherDataView();
                for (String weatherString : weatherData){
                    mWeatherTextView.append((weatherString) + "\n\n\n");
                }
            }else {
                showErrorMessage();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.forecast, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemThatHasSelected = item.getItemId();
        if (itemThatHasSelected == R.id.refresh_weather){
            loadWeatherData();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



}
