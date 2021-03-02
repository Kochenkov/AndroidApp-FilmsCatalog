package com.vkochenkov.filmscatalog.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.vkochenkov.filmscatalog.App
import com.vkochenkov.filmscatalog.model.entities.Film
import com.vkochenkov.filmscatalog.model.Repository

class FilmsViewModel : ViewModel() {

    private var repository: Repository = App.instance!!.repository

    fun getFilms(): LiveData<List<Film>>
    {
        return repository.getFilmsFromDb()
    }

    fun likeFilm(name: String) {
        repository.likeFilm(name)
    }

    fun unlikeFilm(name: String) {
        repository.unlikeFilm(name)
    }
}