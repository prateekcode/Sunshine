package com.androidmonk.sunshine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import com.androidmonk.sunshine.data.WeatherContract;
import com.androidmonk.sunshine.sync.SunshineSyncUtils;
import com.androidmonk.sunshine.utilities.NetworkUtils;
import com.androidmonk.sunshine.utilities.OpenWeatherJsonUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> ,
        ForecastAdapter.ForecastAdapterOnClickHandler {

    private TextView mErrorTextMsg;
    private ProgressBar mLoadingContent;
    private RecyclerView mRecyclerView;
    private ForecastAdapter mForecastAdapter;
    private int mPosition = RecyclerView.NO_POSITION;



    /*
     * The columns of data that we are interested in displaying within our MainActivity's list of
     * weather data.
     */
    public static final String[] MAIN_FORECAST_PROJECTION = {
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
    };

    /*
     * We store the indices of the values in the array of Strings above to more quickly be able to
     * access the data from our query. If the order of the Strings above changes, these indices
     * must be adjusted to match the order of the Strings.
     */
    public static final int INDEX_WEATHER_DATE = 0;
    public static final int INDEX_WEATHER_MAX_TEMP = 1;
    public static final int INDEX_WEATHER_MIN_TEMP = 2;
    public static final int INDEX_WEATHER_CONDITION_ID = 3;


    private static final int FORECAST_LOADER_ID = 44;



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


        LinearLayoutManager layoutManager = new
                LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        mForecastAdapter = new ForecastAdapter(this, this);

        mRecyclerView.setAdapter(mForecastAdapter);

        showLoading();

        Bundle bundle = null;
        getSupportLoaderManager().initLoader(FORECAST_LOADER_ID, bundle, this);

        SunshineSyncUtils.startImmediateSync(this);

    }

    private void openPreferredLocationInMap(){
        double[] coords = SunshinePreferences.getLocationCoordinates(this);
        String posLat = Double.toString(coords[0]);
        String posLong = Double.toString(coords[1]);
        Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
        }
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



    private void showLoading(){
        mRecyclerView.setVisibility(View.INVISIBLE);
        mLoadingContent.setVisibility(View.VISIBLE);
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
    public void onClick(long weatherForDay) {
        Intent detailIntent = new Intent(getApplicationContext(), DetailActivity.class);
        //detailIntent.setData(uriForDateClicked);
        detailIntent.putExtra(Intent.EXTRA_TEXT, weatherForDay);
        startActivity(detailIntent);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, @Nullable Bundle bundle) {
        switch (loaderId){
            case FORECAST_LOADER_ID:
                /* URI for all rows of weather data in our weather table */
                Uri forecastQueryUri = WeatherContract.WeatherEntry.CONTENT_URI;
                /* Sort order: Ascending by date */
                String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
                /*
                 * A SELECTION in SQL declares which rows you'd like to return. In our case, we
                 * want all weather data from today onwards that is stored in our weather table.
                 * We created a handy method to do that in our WeatherEntry class.
                 */
                String selection = WeatherContract.WeatherEntry.getSqlSelectForTodayOnwards();

                return new CursorLoader(this,
                        forecastQueryUri,
                        MAIN_FORECAST_PROJECTION,
                        selection,
                        null,
                        sortOrder);

            default:
                throw new RuntimeException("Loader Not Implemented: " + loaderId);

        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        mForecastAdapter.swapCursor(data);
        if(mPosition == RecyclerView.NO_POSITION) {
            mPosition =0; }
        mRecyclerView.smoothScrollToPosition(mPosition);

        if (data.getCount() != 0){
            showWeatherDataView();
        }

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.forecast, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemThatHasSelected = item.getItemId();

        if (itemThatHasSelected == R.id.open_map){
            openPreferredLocationInMap();
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


}
