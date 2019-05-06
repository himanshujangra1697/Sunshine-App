package com.example.android.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private ArrayAdapter mForecastAdapter;

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if(id == R.id.action_refresh){
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("121003" +
                    ",in");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Once the root view for the fragment has been created, it's time
        //the list view with some dummy data

        //create some dummy data for the ListView, Here's a sample weekly
        //represented as "day, weather, high low"
        String[] forecastArray = {
                "Monday - Sunny - 88 / 63",
                "Tuesday - Foggy - 70 / 46",
                "Wednesday - Cloudy - 72 / 63",
                "Thursday - Rainy - 64 / 51",
                "Friday - Foggy - 70 / 46",
                "Saturday - TRAPPED IN WEATHER STATION - 76 / 68",
                "Sunday - Sunny - 75 / 70"
        };

        List<String> weekForecast = new ArrayList<String>(
                Arrays.asList(forecastArray));

        //Now that we have some dummy forecast dat, create an ArrayAdapter.
        //The ArrayAdapter will take the data from the source (like our dummy forecast)
        //use it to populate the ListView it's attached to.
        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(),                          //The current context (This fragment's parent activity)
                R.layout.list_item_forecast,            // ID of List Item Layout
                R.id.list_item_forecast_textview,       // ID of the Text View to populate
                weekForecast                            // Forecast data
        );

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get the reference to the ListView, and attach this adapter to
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        //Method used to wire up behaviour on a list item when it is clicked.
        // This will gonna start the "DetailedActivity".
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = (String) mForecastAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(intent);
            }
        });

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        /**
         * The date/time conversion is going to be moved outside the AsyncTask later,
         * so, convenience we're breaking it out into its own method now.
         * @param time
         * @return
         */
        private String getReadableDateString(long time){
            // Because the api returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            Date date = new Date(time * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
            return format.format(date).toString();
        }

        /**
         * Prepare the weather high/lows for presentation.
         * @param high
         * @param low
         * @return
         */
        private String formatHighLows(double high, double low){
            // For presentation, assume the user doesn't care about the tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);
            return  roundedHigh + "/" + roundedLow;
        }


        /**
         *
         * @param forecastJsonStr
         * @param numDays
         * @return
         * @throws JSONException
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
            throws JSONException{

            // These are the names of json objects that need to be extracted
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DATETIME = "dt";
            final String OWM_DESCRIPTION = "description";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++){
                //For now, using the format "Day, Description, high/low"
                String day;
                String description;
                String highAndLow;

                //Get Json object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                //The date/time is required as a long. We need to convert that
                //into something human-readable, since most people won't read "1400356800" as
                //"this saturday"
                long dateTime = dayForecast.getLong(OWM_DATETIME);
                day = getReadableDateString(dateTime);

                //The description is in a child array called "Weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                //Temperatures are in a child object called "temp". Try not to name Variables
                //"temp" when working with Temperature. It confuses Everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + "-" + description + "-" + highAndLow;
            }
            for (String s : resultStrs){
                Log.v(LOG_TAG, "Forecast Entry: " + s);
            }
            return resultStrs;

        }

        @Override
        protected String[] doInBackground(String ... params) {


            if(params.length == 0){
                return null;
            }

            //These two need to be declared outside the try/catch
            //so that they can be closed in finally block
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String forecastJsonStr = null;

            String format = "json";
            String units = "metric";
            int numDays = 7;
            String appid = "bd5e378503939ddaee76f12ad7a97608";

            try {

                final String FORECAST_BASE_URL =
                        "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String KEY_PARAM = "appid";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(KEY_PARAM, appid)
                        .build();

                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }

                forecastJsonStr = buffer.toString();
                Log.v(LOG_TAG, "Forecast String: " + forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            }
            finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try{
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e){
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result){
            if(result != null){
                mForecastAdapter.clear();
                for (String dayForecaststr : result){
                    mForecastAdapter.add(dayForecaststr);
                }
                // New data is back from server. Hooray!
            }
        }
    }
}