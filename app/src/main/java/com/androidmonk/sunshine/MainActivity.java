package com.androidmonk.sunshine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class MainActivity extends AppCompatActivity implements
        ForecastAdapter.ForecastAdapterOnClickHandler, LoaderManager.LoaderCallbacks<String[]> , SharedPreferences.OnSharedPreferenceChangeListener {

    private TextView mErrorTextMsg;
    private ProgressBar mLoadingContent;
    private RecyclerView mRecyclerView;
    private ForecastAdapter mForecastAdapter;

    private static final int FORECAST_LOADER_ID = 0;

    private static final String TAG = MainActivity.class.getSimpleName();

    private static boolean PREFERENCES_HAVE_BEEN_UPDATED = false;

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

        int loaderId = FORECAST_LOADER_ID;
        LoaderManager.LoaderCallbacks<String[]> callbacks = MainActivity.this;
        Bundle bundle = null;
        getSupportLoaderManager().initLoader(loaderId, bundle, callbacks);


        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        //loadWeatherData();

    }

    private void loadWeatherData(){
        showWeatherDataView();
        String location = SunshinePreferences.getPreferredWeatherLocation(this);
        //new FetchWeatherTask().execute(location);

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
    protected void onStart() {
        super.onStart();
        if (PREFERENCES_HAVE_BEEN_UPDATED){
            Log.d(TAG, "onStart: preferences were updated");
            getSupportLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
            PREFERENCES_HAVE_BEEN_UPDATED = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
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

    @NonNull
    @Override
    public Loader<String[]> onCreateLoader(int id, @Nullable Bundle args) {
        return new AsyncTaskLoader<String[]>(this) {

            String[] mWeatherData = null;

            @Override
            protected void onStartLoading() {
                if (mWeatherData != null){
                    deliverResult(mWeatherData);
                }else {
                    mLoadingContent.setVisibility(View.VISIBLE);
                    forceLoad();
                }
            }

            @Nullable
            @Override
            public String[] loadInBackground() {

                String locationQuery = SunshinePreferences.getPreferredWeatherLocation(MainActivity.this);
                URL weatherRequestUrl = NetworkUtils.buildUrl(locationQuery);
                try {
                    String jsonWeatherResponse = NetworkUtils.getResponseFromHttpUrl(weatherRequestUrl);
                    String[] simpleJsonWeatherData = OpenWeatherJsonUtils.getSimpleWeatherStringsFromJson(MainActivity.this, jsonWeatherResponse);
                    return simpleJsonWeatherData;
                }catch (Exception e){
                    e.printStackTrace();
                    return null;
                }
            }

            public void deliverResult(String[] data){
                mWeatherData = data;
                super.deliverResult(data);
            }
        };
    }

    @Override
    public void onLoadFinished(@NonNull Loader<String[]> loader, String[] data) {
        mLoadingContent.setVisibility(View.INVISIBLE);
        mForecastAdapter.setmWeatherData(data);
        if (null==data){
            showErrorMessage();
        }else {
            showWeatherDataView();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String[]> loader) {

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.forecast, menu);
        return true;
    }

    private void invalidateData(){
        mForecastAdapter.setmWeatherData(null);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemThatHasSelected = item.getItemId();
        if (itemThatHasSelected == R.id.refresh_weather){
            invalidateData();
            getSupportLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
            //mForecastAdapter.setmWeatherData(null);
            //loadWeatherData();
            return true;
        }

        if (itemThatHasSelected == R.id.open_map){
            openMap();
            return true;
        }

        if (itemThatHasSelected == R.id.action_setting){
            Intent intent = new Intent(this, SettingActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void openMap(){
        String addressString = SunshinePreferences.getPreferredWeatherLocation(this);
        Uri geoLocation = Uri.parse("geo:0,0?q=" + addressString);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager())!=null){
            startActivity(intent);
        }else {
            Log.d(TAG, "Couldn't call" + geoLocation.toString() + ", no receiving app installed");
        }

    }

    public void showMap(Uri uri){
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
        if (mapIntent.resolveActivity(getPackageManager())!=null){
            startActivity(mapIntent);
        }else {
            Log.d(TAG, "Couldn't call" );
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        PREFERENCES_HAVE_BEEN_UPDATED = true;
    }
}
