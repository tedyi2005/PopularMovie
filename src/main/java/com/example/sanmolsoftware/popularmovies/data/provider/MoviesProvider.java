
package com.example.sanmolsoftware.popularmovies.data.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import com.example.sanmolsoftware.popularmovies.utils.SelectionBuilder;

import java.util.Arrays;

import timber.log.Timber;

import static com.example.sanmolsoftware.popularmovies.data.provider.MoviesContract.CONTENT_AUTHORITY;


public final class MoviesProvider extends ContentProvider {
    private static final String TAG = MoviesProvider.class.getSimpleName();

    private SQLiteOpenHelper mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int GENRES = 100;

    private static final int MOVIES = 200;
    private static final int MOVIES_ID = 201;
    private static final int MOVIES_ID_GENRES = 202;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = CONTENT_AUTHORITY;

        matcher.addURI(authority, "genres", GENRES);

        matcher.addURI(authority, "movies", MOVIES);
        matcher.addURI(authority, "movies/*", MOVIES_ID);
        matcher.addURI(authority, "movies/*/genres", MOVIES_ID_GENRES);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new MoviesDatabase(getContext());
        return true;
    }

    private void deleteDatabase() {
        mOpenHelper.close();
        Context context = getContext();
        MoviesDatabase.deleteDatabase(context);
        mOpenHelper = new MoviesDatabase(getContext());
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case GENRES:
                return MoviesContract.Genres.CONTENT_TYPE;
            case MOVIES:
                return MoviesContract.Movies.CONTENT_TYPE;
            case MOVIES_ID:
                return MoviesContract.Movies.CONTENT_ITEM_TYPE;
            case MOVIES_ID_GENRES:
                return MoviesContract.Genres.CONTENT_TYPE;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        final int match = sUriMatcher.match(uri);

        Timber.tag(TAG).v("uri=" + uri + " match=" + match + " proj=" + Arrays.toString(projection) +
                " selection=" + selection + " args=" + Arrays.toString(selectionArgs) + ")");

        final SelectionBuilder builder = buildExpandedSelection(uri, match);

        boolean distinct = !TextUtils.isEmpty(uri.getQueryParameter(MoviesContract.QUERY_PARAMETER_DISTINCT));

        Cursor cursor = builder
                .where(selection, selectionArgs)
                .query(db, distinct, projection, sortOrder, null);

        Context context = getContext();
        if (null != context) {
            cursor.setNotificationUri(context.getContentResolver(), uri);
        }
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Timber.tag(TAG).v("insert(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case GENRES: {
                db.insertOrThrow(MoviesDatabase.Tables.GENRES, null, values);
                notifyChange(uri);
                return MoviesContract.Genres.buildGenreUri(values.getAsString(MoviesContract.Genres.GENRE_ID));
            }
            case MOVIES: {
                db.insertOrThrow(MoviesDatabase.Tables.MOVIES, null, values);
                notifyChange(uri);
                return MoviesContract.Movies.buildMovieUri(values.getAsString(MoviesContract.Movies.MOVIE_ID));
            }
            case MOVIES_ID_GENRES: {
                db.insertOrThrow(MoviesDatabase.Tables.MOVIES_GENRES, null, values);
                notifyChange(uri);
                return MoviesContract.Genres.buildGenreUri(values.getAsString(MoviesContract.Genres.GENRE_ID));
            }
            default: {
                throw new UnsupportedOperationException("Unknown insert uri: " + uri);
            }
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Timber.tag(TAG).v("update(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        notifyChange(uri);
        return retVal;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Timber.tag(TAG).v("delete(uri=" + uri + ")");
        if (uri.equals(MoviesContract.BASE_CONTENT_URI)) {
            deleteDatabase();
            notifyChange(uri);
            return 1;
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).delete(db);
        notifyChange(uri);
        return retVal;
    }

    private void notifyChange(Uri uri) {
        getContext().getContentResolver().notifyChange(uri, null);
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case GENRES: {
                return builder.table(MoviesDatabase.Tables.GENRES);
            }
            case MOVIES: {
                return builder.table(MoviesDatabase.Tables.MOVIES);
            }
            case MOVIES_ID: {
                final String movieId = MoviesContract.Movies.getMovieId(uri);
                return builder.table(MoviesDatabase.Tables.MOVIES)
                        .where(MoviesContract.Movies.MOVIE_ID + "=?", movieId);
            }
            case MOVIES_ID_GENRES: {
                final String movieId = MoviesContract.Movies.getMovieId(uri);
                return builder.table(MoviesDatabase.Tables.MOVIES_GENRES)
                        .where(MoviesContract.Movies.MOVIE_ID + "=?", movieId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri for " + match + ": " + uri);
            }
        }
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case GENRES: {
                return builder.table(MoviesDatabase.Tables.GENRES);
            }
            case MOVIES: {
                return builder.table(MoviesDatabase.Tables.MOVIES);
            }
            case MOVIES_ID: {
                final String movieId = MoviesContract.Movies.getMovieId(uri);
                return builder.table(MoviesDatabase.Tables.MOVIES_JOIN_GENRES)
                        .mapToTable(MoviesContract.Movies._ID, MoviesDatabase.Tables.MOVIES)
                        .mapToTable(MoviesContract.Movies.MOVIE_ID, MoviesDatabase.Tables.MOVIES)
                        .where(Qualified.MOVIES_MOVIE_ID + "=?", movieId);
            }
            case MOVIES_ID_GENRES: {
                final String movieId = MoviesContract.Movies.getMovieId(uri);
                return builder.table(MoviesDatabase.Tables.MOVIES_GENRES_JOIN_GENRES)
                        .mapToTable(MoviesContract.Genres._ID, MoviesDatabase.Tables.GENRES)
                        .mapToTable(MoviesContract.Genres.GENRE_ID, MoviesDatabase.Tables.GENRES)
                        .where(Qualified.MOVIES_GENRES_GENRE_ID + "=?", movieId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    private interface Qualified {
        String MOVIES_MOVIE_ID = MoviesDatabase.Tables.MOVIES + "." + MoviesContract.Movies.MOVIE_ID;
        String MOVIES_GENRES_GENRE_ID = MoviesDatabase.Tables.MOVIES_GENRES + "." + MoviesDatabase.MoviesGenres.MOVIE_ID;
    }
}
