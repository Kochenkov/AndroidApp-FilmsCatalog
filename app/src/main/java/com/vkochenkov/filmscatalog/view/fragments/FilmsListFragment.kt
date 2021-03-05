package com.vkochenkov.filmscatalog.view.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.vkochenkov.filmscatalog.R
import com.vkochenkov.filmscatalog.model.LocalDataStore
import com.vkochenkov.filmscatalog.model.db.Film
import com.vkochenkov.filmscatalog.view.MainActivity.Companion.FILM
import com.vkochenkov.filmscatalog.view.recycler.main.FilmItemClickListener
import com.vkochenkov.filmscatalog.view.recycler.main.FilmsAdapter
import com.vkochenkov.filmscatalog.viewmodel.FilmsViewModel


class FilmsListFragment : Fragment() {

    private val filmsViewModel by lazy {
        ViewModelProvider(this).get(FilmsViewModel::class.java)
    }

    private lateinit var filmsRecycler: RecyclerView
    private lateinit var mainToolbar: Toolbar

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_films_list, container, false)

        initFields(view)
        initRecyclerView(view)
        initRecyclerPagination()
        initSwipeToRefresh()
        initOnDataChangeObserver()
        initOnErrorObserver()

        filmsViewModel.getFilmsWithPaging(progressBar, true)

        return view
    }

    override fun onResume() {
        super.onResume()
        filmsRecycler.adapter?.notifyDataSetChanged()
        mainToolbar.setTitle(R.string.app_name)
    }

    private fun initFields(view: View) {
        filmsRecycler = view.findViewById(R.id.films_list)
        mainToolbar = (activity as AppCompatActivity).findViewById(R.id.main_toolbar)
        progressBar = view.findViewById(R.id.films_progress_bar)
        swipeRefresh = view.findViewById(R.id.films_swipe_refresh)
    }

    private fun initSwipeToRefresh() {
        swipeRefresh.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener {
            filmsViewModel.getFilmsWithPaging(swipeRefresh)
        })
    }

    private fun initRecyclerPagination() {
        filmsRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) { //check for scroll down
                    val visibleItemCount = filmsRecycler.layoutManager!!.getChildCount()
                    val totalItemCount = filmsRecycler.layoutManager!!.getItemCount()
                    val pastVisiblesItems =
                        (filmsRecycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

                    if (visibleItemCount + pastVisiblesItems >= totalItemCount) {
                        filmsViewModel.getFilmsWithPaging(progressBar)
                    }
                }
            }
        })
    }

    private fun initRecyclerView(view: View) {
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            filmsRecycler.layoutManager = LinearLayoutManager(view.context)
        } else {
            filmsRecycler.layoutManager = GridLayoutManager(view.context, 2)
        }

        filmsRecycler.adapter =
            FilmsAdapter(object : FilmItemClickListener {
                override fun detailsClickListener(film: Film) {

                    LocalDataStore.currentSelectedFilm = film
                    filmsRecycler.adapter?.notifyDataSetChanged()

                    openSelectedFilmFragment(film)
                }

                override fun likeClickListener(film: Film, position: Int) {
                    if (film.liked) {
                        film.liked = false
                        filmsViewModel.unlikeFilm(film.serverName)
                    } else {
                        film.liked = true
                        filmsViewModel.likeFilm(film.serverName)
                    }
                    filmsRecycler.adapter?.notifyItemChanged(position)
                }
            })
    }

    private fun initOnErrorObserver() {
        filmsViewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                val snackbar = Snackbar.make(this.requireView(), it, Snackbar.LENGTH_LONG)
                snackbar.setAction(context?.getString(R.string.retry_request_str)) { retryRequest() }
                val tv = snackbar.view.findViewById<View>(R.id.snackbar_text) as TextView
                tv.maxLines = 3
                snackbar.show()
            }
        })
    }

    private fun retryRequest() {
        filmsViewModel.getFilmsWithPaging(progressBar)
    }

    private fun initOnDataChangeObserver() {
        filmsViewModel.filmsLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                (filmsRecycler.adapter as FilmsAdapter).refreshDataList(it)
            }
        })
    }

    private fun openSelectedFilmFragment(film: Film) {
        val bundle = Bundle()
        bundle.putParcelable(FILM, film)

        val filmInfoFragment = FilmInfoFragment()
        filmInfoFragment.arguments = bundle

        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            .replace(R.id.fragments_container, filmInfoFragment)
            .addToBackStack("FilmInfoFragment")
            .commit()
    }
}