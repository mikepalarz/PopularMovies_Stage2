/*
The following code is the property and sole work of Mike Palarz, a student at Udacity
 */

package com.example.android.popularmovies_stage2;

import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import com.example.android.popularmovies_stage2.data.MovieContract;

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
import java.util.List;
import java.util.Scanner;

/*
 This class is used in order to make use of TheMovieDB API by generating API queries, parsing
 through the returned JSON data, creating Movie objects from the received JSON data, and then
  providing an ArrayList of Movies that can be used by the MovieSelection and MovieDetails classes.
 */

public class MovieFetcher {
    private static final String TAG = "MovieFetcher";

    public static int mMethodFlag;

    //Constants used for building the base URL
    private static final String MOVIEDB_BASE_URL = "https://api.themoviedb.org/3/movie";
    private static final String METHOD_MOVIE_POPULAR = "popular";
    private static final String METHOD_MOVIE_TOP_RATED = "top_rated";

    //Parameters to be used with the query
    private static final String PARAM_API_KEY = "api_key";
    private static final String PARAM_LANGUAGE = "language";
    private static final String PARAM_PAGE = "page";

    //Parameter values to be used when building the URL
    private static final String API_KEY = "";
    private static final String LANGUAGE = "en-US";

    //TODO: Get a better grasp on static methods and then look over your work here. Is it really
    //necessary to declare all of these methods as static? And more importantly, is it a good
    //practice to do so?

    public static int getMethodFlag(){
        return mMethodFlag;
    }

    public static void setMethodFlag(int newMethodFlag){
        mMethodFlag = newMethodFlag;
    }

    //This method is used in order to build the URL that will be used to query the TheMovieDB. The
    //methodFlag parameter specifies which of the two API methods will be used (either polling
    //movies by popularity or top rated). The pageNumber parameter specifies which page of data
    //should be obtained. The page number is used within MovieSelection in order to allow for
    //pagination/infinite scrolling.
    public static URL buildURL(int pageNumber){
        String queryMethod;
        switch (mMethodFlag){
            case 0:
                queryMethod = METHOD_MOVIE_POPULAR;
                break;
            case 1:
                queryMethod = METHOD_MOVIE_TOP_RATED;
                break;
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
    //appended to an ArrayList.
    private static ContentValues[] parseMovies(String httpResponse) throws JSONException{
        JSONObject jsonRoot = new JSONObject(httpResponse);
        //All of the movies are contained within an array. The array in this case may be referred to
        //as "results"
        JSONArray jsonResults = jsonRoot.getJSONArray("results");

        ContentValues[] movies = new ContentValues[jsonResults.length()];

        //The array is then iterated through and a Movie object is created for each entry within
        //jsonResults. The Movie is then appended to moviesList.
        for (int i = 0; i < jsonResults.length(); i++){
            JSONObject jsonMovie = jsonResults.getJSONObject(i);

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

            switch (mMethodFlag){
                case 0:
                    movieCV.put(MovieContract.MovieTable.COLUMN_SORTED_BY, Movie.SORTED_BY_POPULARITY);
                    break;
                case 1:
                    movieCV.put(MovieContract.MovieTable.COLUMN_SORTED_BY, Movie.SORTED_BY_TOP_RATED);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown value for the sorted by column");
            }

            movies[i] = movieCV;

            Movie movie = new Movie(title, ID, posterPath, overview, releaseDate, voteCount,
                    voteAverage, backdropPath);

            //These log messages are kept for possible debugging purposes
            Log.i(TAG, "\nThe current movie has been added to the array: \n");
            Log.i(TAG, movie.toString());

        }
        return movies;
    }

    //A helper method, which essentially combines buildURL(), getHTTPResponse(), and parseMovies()
    //all within one method.
    public static ContentValues[] fetchMovies(int pageNumber){
        ContentValues[] movies;
        try {
            String httpResponse = getHTTPResponse(buildURL(pageNumber));
            movies = parseMovies(httpResponse);
        }
        catch(IOException ioe){
            movies = null;
            ioe.printStackTrace();
        }
        catch(JSONException je){
            movies = null;
            je.printStackTrace();
        }
        return movies;
    }
}
