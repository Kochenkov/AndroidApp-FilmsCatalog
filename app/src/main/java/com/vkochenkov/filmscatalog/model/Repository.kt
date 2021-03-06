package com.vkochenkov.filmscatalog.model

import android.app.Application
import com.vkochenkov.filmscatalog.App
import com.vkochenkov.filmscatalog.R
import com.vkochenkov.filmscatalog.model.api.ApiService
import com.vkochenkov.filmscatalog.model.api.ApiService.Companion.PAGES_SIZE
import com.vkochenkov.filmscatalog.model.api.ResponseFromApi
import com.vkochenkov.filmscatalog.model.db.Film
import com.vkochenkov.filmscatalog.model.db.FilmsDao
import io.reactivex.MaybeObserver
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class Repository {

    @Inject
    lateinit var dao: FilmsDao
    @Inject
    lateinit var api: ApiService
    @Inject
    lateinit var appContext: Application

    init {
        App.appComponent.inject(this)
    }

    fun getFilmsWithPagination(page: Int, callback: GetFilmsFromDatabaseCallback) {
        dao.getFilmsWithPagination(page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(maybeObserver(callback))
    }

    fun getFavourites(callback: GetFilmsFromDatabaseCallback) {
        dao.getFavourites()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(maybeObserver(callback))
    }

    fun likeFilm(name: String) {
        dao.setLikeFilm(name)
    }

    fun unlikeFilm(name: String) {
        dao.setUnlikeFilm(name)
    }

    fun saveFilmsToDb(films: List<Film>) {
        dao.insertAllFilms(films)
    }

    fun getFilmsFromApi(sincePage: Int, callback: GetFilmsFromApiCallback) {
        api.getAnimeListWithPages(PAGES_SIZE, sincePage)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ResponseFromApi> {
                override fun onSuccess(response: ResponseFromApi) {
                    val filmsListFromApi = ArrayList<Film>()
                    response.data.forEach {
                        filmsListFromApi.add(
                            Film(
                                it.attributes.serverName,
                                it.attributes.title,
                                it.attributes.description,
                                it.attributes.posterImage.original,
                                it.attributes.startDate,
                                it.attributes.ageRating,
                                it.attributes.episodeCount,
                                it.attributes.averageRating,
                                false,
                                0L
                            )
                        )
                    }
                    callback.onSuccess(filmsListFromApi)
                }

                override fun onError(t: Throwable) {
                    callback.onFailure(appContext.getString(R.string.api_error_connection_str))
                }

                override fun onSubscribe(d: Disposable) {}

            })
    }

    fun getFilm(name: String, callback: GetFilmFromDatabaseCallback) {
        dao.getFilm(name)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(maybeObserver(callback))
    }

    fun setNotificationFilm(name: String, date: Long) {
        dao.setNotificationFilm(name, date)
    }

    fun clearNotificationFilm(name: String) {
        dao.clearNotificationFilm(name)
    }

    interface GetFilmsFromApiCallback {
        fun onSuccess(films: List<Film>)
        fun onFailure(str: String)
    }

    interface GetFilmsFromDatabaseCallback {
        fun onSuccess(films: List<Film>)
    }

    interface GetFilmFromDatabaseCallback {
        fun onSuccess(film: Film)
    }

    private fun maybeObserver(callback: GetFilmsFromDatabaseCallback) =
        object : MaybeObserver<List<Film>> {
            override fun onSuccess(data: List<Film>) {
                callback.onSuccess(data)
            }
            override fun onError(t: Throwable) {}
            override fun onSubscribe(d: Disposable) {}
            override fun onComplete() {}
        }

    private fun maybeObserver(callback: GetFilmFromDatabaseCallback) =
        object : MaybeObserver<Film> {
            override fun onSuccess(data: Film) {
                callback.onSuccess(data)
            }
            override fun onError(t: Throwable) {}
            override fun onSubscribe(d: Disposable) {}
            override fun onComplete() {}
        }
}