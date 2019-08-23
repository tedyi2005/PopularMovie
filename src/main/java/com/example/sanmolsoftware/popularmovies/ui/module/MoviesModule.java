

package com.example.sanmolsoftware.popularmovies.ui.module;

import com.example.sanmolsoftware.popularmovies.ApplicationModule;
import com.example.sanmolsoftware.popularmovies.ui.fragment.FavoredMoviesFragment;
import com.example.sanmolsoftware.popularmovies.ui.fragment.MovieFragment;
import com.example.sanmolsoftware.popularmovies.ui.fragment.SortedMoviesFragment;

import dagger.Module;

@Module(
        injects = {
                SortedMoviesFragment.class,
                FavoredMoviesFragment.class,
                MovieFragment.class
        },
        addsTo = ApplicationModule.class
)
public final class MoviesModule {}
