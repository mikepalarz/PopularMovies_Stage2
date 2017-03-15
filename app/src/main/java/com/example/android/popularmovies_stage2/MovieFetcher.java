/*
The following code is the property and sole work of Mike Palarz, a student at Udacity
 */

package com.example.android.popularmovies_stage2;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.android.popularmovies_stage2.data.MovieContract;
import com.example.android.popularmovies_stage2.fragments.SettingsFragment;
import com.facebook.stetho.Stetho;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class MovieFetcher {
    private static final String TAG = "MovieFetcher";

    public static int mMethodFlag;
    public static int mPopularPageNumber;
    public static int mTopRatedPageNumber;

    //Constants used for building the base URL
    private static final String MOVIEDB_BASE_URL = "https://api.themoviedb.org/3/movie";
    private static final String METHOD_MOVIE_POPULAR = "popular";
    private static final String METHOD_MOVIE_TOP_RATED = "top_rated";

    //Parameters to be used with the query
    private static final String PARAM_API_KEY = "api_key";
    private static final String PARAM_LANGUAGE = "language";
    private static final String PARAM_PAGE = "page";
    private static final String PARAM_APPEND_T0_RESPONSE = "append_to_response";

    //Parameter values to be used when building the URL
    private static final String API_KEY = "";
    private static final String LANGUAGE = "en-US";
    private static final String APPEND_VIDEOS = "videos";

    public static int getMethodFlag(){
        return mMethodFlag;
    }

    public static void setMethodFlag(int newMethodFlag){
        mMethodFlag = newMethodFlag;
    }

    public static int getPopularPageNumber(){
        return mPopularPageNumber;
    }

    public static void setPopularPageNumber(int newPageNumber){
        mPopularPageNumber = newPageNumber;
    }

    public static int getTopRatedPageNumber(){
        return mTopRatedPageNumber;
    }

    public static void setTopRatedPageNumber(int newPageNumber){
        mTopRatedPageNumber = newPageNumber;
    }


    public static URL buildURL(int methodFlag, int pageNumber){
        String queryMethod;

        switch (methodFlag){
            case 0:
                queryMethod = METHOD_MOVIE_POPULAR;
                break;

            case 1:
                queryMethod = METHOD_MOVIE_TOP_RATED;
                break;

            case 2:
                throw new UnsupportedOperationException("Can't build a URL for favorite movies");

            default:
                queryMethod = METHOD_MOVIE_POPULAR;
                break;
        }


        //A Uri is first built according to TheMovieDB API's documentation
        Uri builtUri = Uri.parse(MOVIEDB_BASE_URL).buildUpon()
                .appendPath(queryMethod)
                .appendQueryParameter(PARAM_API_KEY, API_KEY)
                .appendQueryParameter(PARAM_LANGUAGE, LANGUAGE)
                .appendQueryParameter(PARAM_PAGE, Integer.toString(pageNumber))
                .build();

        URL url = null;

        try{
            //The Uri is then converted into a URL if possible
            url = new URL(builtUri.toString());
            Log.i(TAG, "The resulting built URL: " + url.toString());
        }
        catch (MalformedURLException mue){
            mue.printStackTrace();
        }

        return url;
    }

    public static URL buildURLForDetails(int movieID){

        //A Uri is first built according to TheMovieDB API's documentation
        Uri builtUri = Uri.parse(MOVIEDB_BASE_URL).buildUpon()
                .appendPath(Integer.toString(movieID))
                .appendQueryParameter(PARAM_API_KEY, API_KEY)
                .appendQueryParameter(PARAM_LANGUAGE, LANGUAGE)
                .appendQueryParameter(PARAM_APPEND_T0_RESPONSE, APPEND_VIDEOS)
                .build();

        URL url = null;

        try{
            //The Uri is then converted into a URL if possible
            url = new URL(builtUri.toString());
            Log.i(TAG, "The resulting built URL: " + url.toString());
        }
        catch (MalformedURLException mue){
            mue.printStackTrace();
        }

        return url;
    }


    //This method returns the data that is provided by a URL. For the purposes of this project, the
    //URL that is expected to be used with this method is the one which is generated by buildURL().
    //The String which is returned is the full HTTP response. In the case of this project, that
    //String is an array of JSON data.
    public static String getHTTPResponse(URL url) throws IOException{
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try{
            InputStream in = connection.getInputStream();
            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            if(scanner.hasNext())
                return scanner.next();
            else
                return null;
        }
        finally {
            connection.disconnect();
        }
    }

    //This method parses through JSON data, which is expected to be provided by a String generated
    //by getHTTPResponse(). The String is parsed through, Movie objects are iteratively created and
    //appended to an array of ContentValues.
    private static ContentValues[] parseMovies(String httpResponse, int methodFlag) throws JSONException{
        JSONObject jsonRoot = new JSONObject(httpResponse);
        //All of the movies are contained within an array. The array in this case may be referred to
        //as "results"
        JSONArray jsonResults = jsonRoot.getJSONArray("results");

        ContentValues[] movies = new ContentValues[jsonResults.length()];

        //The array is then iterated through and a Movie object is created for each entry within
        //jsonResults. The Movie is then appended to movies.
        for (int i = 0; i < jsonResults.length(); i++){
            JSONObject jsonMovie = jsonResults.getJSONObject(i);

            //TODO: At some point, make String constants for the keys within the JSON
            String title = jsonMovie.getString("title");
            int ID = jsonMovie.getInt("id");
            String posterPath = jsonMovie.getString("poster_path");
            String overview = jsonMovie.getString("overview");
            String releaseDate = jsonMovie.getString("release_date");
            int voteCount = jsonMovie.getInt("vote_count");

            //getDouble() is being used in this case because a double is essentially a float, only
            //with a higher precision (more decimal values); therefore, the correct value should be
            //obtained if the returned double is cast into a float
            float voteAverage = (float) jsonMovie.getDouble("vote_average");

            String backdropPath = jsonMovie.getString("backdrop_path");

            ContentValues movieCV = new ContentValues();
            movieCV.put(MovieContract.MovieTable.COLUMN_TITLE, title);
            movieCV.put(MovieContract.MovieTable.COLUMN_MOVIE_ID, ID);
            movieCV.put(MovieContract.MovieTable.COLUMN_POSTER_PATH, posterPath);
            movieCV.put(MovieContract.MovieTable.COLUMN_OVERVIEW, overview);
            movieCV.put(MovieContract.MovieTable.COLUMN_RELEASE_DATE, releaseDate);
            movieCV.put(MovieContract.MovieTable.COLUMN_VOTE_COUNT, voteCount);
            movieCV.put(MovieContract.MovieTable.COLUMN_VOTE_AVERAGE, voteAverage);
            movieCV.put(MovieContract.MovieTable.COLUMN_BACKDROP_PATH, backdropPath);

            switch (methodFlag){
                case 0:
                    movieCV.put(MovieContract.MovieTable.COLUMN_SORTED_BY, Movie.SORTED_BY_POPULARITY);
                    break;
                case 1:
                    movieCV.put(MovieContract.MovieTable.COLUMN_SORTED_BY, Movie.SORTED_BY_TOP_RATED);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown value for the sorted by column");
            }

            movieCV.put(MovieContract.MovieTable.COLUMN_RUNTIME, 0);
            movieCV.put(MovieContract.MovieTable.COLUMN_FAVORITE, false);

            movies[i] = movieCV;

            //TODO: Make sure to update this later on so that the actual runtime value is included
            Movie movie = new Movie(title, ID, posterPath, overview, releaseDate, voteCount,
                    voteAverage, backdropPath, 0, false);

            //These log messages are kept for possible debugging purposes
            Log.i(TAG, "\nThe current movie has been added to the array: \n");
            Log.i(TAG, movie.toString());

        }
        return movies;
    }

    private static ContentValues parseMovieDetails(String httpResponse) throws JSONException{
        JSONObject jsonResponse = new JSONObject(httpResponse);
        ContentValues updatedValues = new ContentValues();

        int runtime = jsonResponse.getInt("runtime");

        JSONArray jsonVideos = jsonResponse.getJSONArray("videos");
        JSONArray jsonVideoResults = jsonVideos.getJSONArray(0);

        JSONObject firstTrailer = jsonVideoResults.getJSONObject(0);
        JSONObject secondTrailer = jsonVideoResults.getJSONObject(1);

        String firstTrailerKey = firstTrailer.getString("key");
        String firstTrailerName = firstTrailer.getString("name");

        String secondTrailerKey = secondTrailer.getString("key");
        String secondTrailerName = secondTrailer.getString("name");

        updatedValues.put(MovieContract.MovieTable.COLUMN_RUNTIME, runtime);
        updatedValues.put(MovieContract.MovieTable.COLUMN_FIRST_TRAILER_KEY, firstTrailerKey);
        updatedValues.put(MovieContract.MovieTable.COLUMN_FIRST_TRAILER_NAME, firstTrailerName);
        updatedValues.put(MovieContract.MovieTable.COLUMN_SECOND_TRAILER_KEY, secondTrailerKey);
        updatedValues.put(MovieContract.MovieTable.COLUMN_SECOND_TRAILER_NAME, secondTrailerName);

        return updatedValues;
    }

    public static ContentValues[] fetchMovies(){

        ContentValues[] movies;
        ArrayList<ContentValues> moviesList = new ArrayList<>();
        int pageNumber;
        int[] pageNumbers = {mPopularPageNumber, mTopRatedPageNumber};
        int max;

        for (int methodFlag = 0; methodFlag < 2; methodFlag++){
            pageNumber = pageNumbers[methodFlag];

            if (pageNumber == 0){
                pageNumber = 1;
            }

            max = pageNumber + 5;

            try {
                while(pageNumber < max) {
                    String httpResponse = getHTTPResponse(buildURL(methodFlag, pageNumber));
                    moviesList.addAll(Arrays.asList(parseMovies(httpResponse, methodFlag)));
                    pageNumber++;
                }
            }

            catch(IOException ioe){
                movies = null;
                ioe.printStackTrace();
            }

            catch(JSONException je){
                movies = null;
                je.printStackTrace();
            }

            if (methodFlag == 0){
                setPopularPageNumber(pageNumber);
            }
            else{
                setTopRatedPageNumber(pageNumber);
            }
        }

        movies = moviesList.toArray(new ContentValues[0]);

        return movies;
    }


    //A helper method, which essentially combines buildURL(), getHTTPResponse(), and parseMovies()
    //all within one method.
    public static ContentValues[] fetchMovies(Context context){

        ContentValues[] movies;
        ArrayList<ContentValues> moviesList = new ArrayList<>();
        int pageNumber;

        String methodFlagKey = context.getString(R.string.list_preference_sorting_options_key);
        int methodFlag = SettingsFragment.getPreferenceValue(context, methodFlagKey);

        switch (methodFlag){
            case 0:
                pageNumber = mPopularPageNumber;
                break;

            case 1:
                pageNumber = mTopRatedPageNumber;
                break;

            case 2:
                throw new UnsupportedOperationException("Can't fetch data with favorite movies");

            default:
                throw new UnsupportedOperationException("Unknown value for method flag");
        }


        /*
        * The first time the app is ever loaded (upon installation), the values of both page number
        * variables will be the default value of an int, which is 0. This will cause the app to
        * attempt to query TheMovieDB w/ a 0 page number, which will return an empty result because
        * no such page number exists. To get around this issue, we check for this and set the
        * pageNumber variable to 1.
        */

        if (pageNumber == 0){
            pageNumber = 1;
        }

        int max = pageNumber + 5;

        try {
            while(pageNumber < max) {
                String httpResponse = getHTTPResponse(buildURL(methodFlag, pageNumber));
                moviesList.addAll(Arrays.asList(parseMovies(httpResponse, methodFlag)));
                pageNumber++;
            }
        }

        catch(IOException ioe){
            movies = null;
            ioe.printStackTrace();
        }

        catch(JSONException je){
            movies = null;
            je.printStackTrace();
        }

        updatePageNumber(context, pageNumber);

        movies = moviesList.toArray(new ContentValues[0]);

        return movies;
    }

    //TODO: If you don't end up ever using this method, remove it
    private static void updatePageNumber(Context context, int newPageNumber){

        String methodFlagKey = context.getString(R.string.list_preference_sorting_options_key);
        int methodFlag = SettingsFragment.getPreferenceValue(context, methodFlagKey);

        switch (methodFlag){
            case 0:
                setPopularPageNumber(newPageNumber);
                break;

            case 1:
                setTopRatedPageNumber(newPageNumber);
                break;

            case 2:
                throw new UnsupportedOperationException("Cannot update page number for favorites");

            default:
                throw new UnsupportedOperationException("Unknown value for the page number");
        }
    }

}
