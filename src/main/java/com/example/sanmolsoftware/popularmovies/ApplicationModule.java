

package com.example.sanmolsoftware.popularmovies;

import android.app.Application;


import com.example.sanmolsoftware.popularmovies.data.DataModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        includes = DataModule.class,
        injects = {
                PopularMoviesApplication.class
        },
        library = true
)
public final class ApplicationModule {
    private final PopularMoviesApplication application;

    public ApplicationModule(PopularMoviesApplication application) {
        this.application = application;
    }

    @Provides @Singleton
    Application provideApplication() {
        return application;
    }

}

