package com.hobermac.musicapp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import com.hobermac.musicapp.R
import com.hobermac.musicapp.adapters.SwipeSongAdapter
import com.hobermac.musicapp.data.entities.Song
import com.hobermac.musicapp.exoplayer.isPlaying
import com.hobermac.musicapp.other.Status
import com.hobermac.musicapp.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter

    @Inject
    lateinit var glide: RequestManager

    private var curPlayingSong: Song? = null

    private var playbackState:PlaybackStateCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        subscribeToObsorve()
        vpSong.adapter = swipeSongAdapter

        vpSong.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback()
        {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                if(playbackState?.isPlaying == true)
                {
                    mainViewModel.playOrToggleSong(swipeSongAdapter.songs[position])
                }
                else
                {
                    curPlayingSong = swipeSongAdapter.songs[position]
                }
            }
        })

        ivPlayPause.setOnClickListener()
        {
            curPlayingSong?.let {
                mainViewModel.playOrToggleSong(it, true)
            }
        }

        swipeSongAdapter.setItemClickListener {
            navHostFragment.findNavController().navigate(
                R.id.globalActionToSongFragment
            )
        }

        navHostFragment.findNavController().addOnDestinationChangedListener{_, destination, _ ->
            when(destination.id)
            {
                R.id.songFragment -> hideBottomBar()
                R.id.homeFragment -> showBottomBar()
                else -> showBottomBar()
            }
        }
    }

    private fun hideBottomBar()
    {
        ivCurSongImage.isVisible = false
        vpSong.isVisible = false
        ivPlayPause.isVisible = false
    }

    private fun showBottomBar()
    {
        ivCurSongImage.isVisible = true
        vpSong.isVisible = true
        ivPlayPause.isVisible = true
    }

    private fun switchViewPagerToCurrentSong(song: Song)
    {
        val newItemIndex = swipeSongAdapter.songs.indexOf(song)

        if(newItemIndex != -1)
        {
            vpSong.currentItem = newItemIndex
            curPlayingSong = song
        }
    }

    private fun subscribeToObsorve()
    {
        mainViewModel.mediaItems.observe(this)
        {
            it?.let{ result ->
                when(result.status)
                {
                    Status.SUCCESS ->{
                        result.data?.let{song ->
                            swipeSongAdapter.songs = song

                            if(song.isNotEmpty())
                            {
                                glide.load((curPlayingSong ?: song[0]).imageURL).into(ivCurSongImage)
                            }
                            switchViewPagerToCurrentSong(curPlayingSong?: return@observe)
                        }
                    }
                    Status.ERROR -> Unit
                    Status.LOADING -> Unit
                }
            }
        }

        mainViewModel.curPlayingSong.observe(this)
        {
            if(it == null) return@observe

            curPlayingSong = it.toSong()
            glide.load(curPlayingSong?.imageURL).into(ivCurSongImage)
            switchViewPagerToCurrentSong(curPlayingSong?: return@observe)
        }

        mainViewModel.playbackState.observe(this)
        {
            playbackState = it
            ivPlayPause.setImageResource(
                if(playbackState?.isPlaying == true)
                {
                    R.drawable.ic_pause
                }
                else
                {
                    R.drawable.ic_play
                }
            )
        }

        mainViewModel.isConnected.observe(this)
        {
            it?.getContentIfNotHandled()?.let { result ->
                when(result.status)
                {
                    Status.ERROR -> Snackbar.make(
                        rootLayout,
                        result.message ?: "An unknown error occurred",
                        Snackbar.LENGTH_LONG).show()
                    else -> Unit
                }
            }
        }

        mainViewModel.networkError.observe(this)
        {
            it?.getContentIfNotHandled()?.let { result ->
                when(result.status)
                {
                    Status.ERROR -> Snackbar.make(
                        rootLayout,
                        result.message ?: "An unknown error occurred",
                        Snackbar.LENGTH_LONG).show()
                    else -> Unit
                }
            }
        }
    }
}