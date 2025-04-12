package com.example.purrytify.ui.screens.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.purrytify.R
import com.example.purrytify.db.entity.Songs

class SongAdapter(
    private val songs: List<Songs>,
    private val onNavigate: (String) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songName: TextView = itemView.findViewById(R.id.songName)
        val songArtist: TextView = itemView.findViewById(R.id.songArtist)
        val songImageComposeView: ComposeView = itemView.findViewById(R.id.songImageComposeView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.song_item_layout, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        val context = holder.itemView.context

        holder.songName.text = song.name
        holder.songArtist.text = song.artist
        val artworkUri = song.artwork

        holder.songImageComposeView.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context).data(data = artworkUri).apply {
                        crossfade(true)
                    }.build()
                ),
                contentDescription = "Artist Artwork",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        holder.itemView.setOnClickListener {
            onNavigate("songDetails/${song.id}")
        }
    }

    override fun getItemCount(): Int {
        return songs.size
    }
}