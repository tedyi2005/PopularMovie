
package com.example.sanmolsoftware.popularmovies.data.repository;




import com.example.sanmolsoftware.popularmovies.data.model.Genre;

import java.util.Map;

import rx.Observable;


public interface GenresRepository {

    Observable<Map<Integer, Genre>> genres();

}
