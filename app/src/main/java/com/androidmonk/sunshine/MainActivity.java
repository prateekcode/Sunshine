package com.androidmonk.sunshine;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidmonk.sunshine.Adapter.ForecastAdapter;
import com.androidmonk.sunshine.data.SunshinePreferences;
import com.androidmonk.sunshine.utilities.NetworkUtils;
import com.androidmonk.sunshine.utilities.OpenWeatherJsonUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements ForecastAdapter.ForecastAdapterOnClickHandler {

    private TextView mErrorTextMsg;
    private ProgressBar mLoadingContent;
    private RecyclerView mRecyclerView;
    private ForecastAdapter mForecastAdapter;

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //mWeatherTextView = (TextView) findViewById(R.id.tv_weather_data);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_forecast);
        mErrorTextMsg = (TextView) findViewById(R.id.error_text_view);
        mLoadingContent = (ProgressBar) findViewById(R.id.pb_content_loading);


        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        mForecastAdapter = new ForecastAdapter(this);
        mRecyclerView.setAdapter(mForecastAdapter);

        loadWeatherData();

    }

    private void loadWeatherData(){
        showWeatherDataView();
        String location = SunshinePreferences.getPreferredWeatherLocation(this);
        new FetchWeatherTask().execute(location);

    }

    private void showWeatherDataView(){
        mErrorTextMsg.setVisibility(View.INVISIBLE);
       // mWeatherTextView.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showErrorMessage(){
        mErrorTextMsg.setVisibility(View.VISIBLE);
        //mWeatherTextView.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onClick(String weatherForDay) {
        Context context = this;
        Toast.makeText(context, weatherForDay, Toast.LENGTH_SHORT)
                .show();
        Intent detailIntent = new Intent(getApplicationContext(), DetailActivity.class);
        detailIntent.putExtra(Intent.EXTRA_TEXT, weatherForDay);
        startActivity(detailIntent);
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
                mForecastAdapter.setmWeatherData(weatherData);
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
            mForecastAdapter.setmWeatherData(null);
            loadWeatherData();
            return true;
        }

        if (itemThatHasSelected == R.id.open_map){
            openMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void openMap(){
        String addressString = "1600 Amphitheatre Pakway, CA";
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("geo")
                .path("0,0")
                .query(addressString);
        Uri addressUri = builder.build();
        showMap(addressUri);
    }

    public void showMap(Uri uri){
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
        if (mapIntent.resolveActivity(getPackageManager())!=null){
            startActivity(mapIntent);
        }else {
            Log.d(TAG, "Couldn't call" );
        }
    }

}
