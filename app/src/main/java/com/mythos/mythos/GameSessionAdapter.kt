package com.mythos.mythos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class GameSessionAdapter(
    private var sessions: MutableList<GameSession>,
    private val onItemClick: (GameSession) -> Unit,
    private val onDeleteClick: (GameSession) -> Unit
) : RecyclerView.Adapter<GameSessionAdapter.SessionViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    inner class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val gameName: TextView = itemView.findViewById(R.id.tvGameName)
        private val gameSummary: TextView = itemView.findViewById(R.id.tvGameSummary)
        private val lastUpdated: TextView = itemView.findViewById(R.id.tvLastUpdated)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteGame)

        fun bind(session: GameSession) {
            gameName.text = session.metadata.gameName
            gameSummary.text = session.metadata.summary
            lastUpdated.text = "Actualizado: ${dateFormat.format(Date(session.metadata.lastUpdated))}"

            itemView.setOnClickListener { onItemClick(session) }
            deleteButton.setOnClickListener { onDeleteClick(session) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(sessions[position])
    }

    override fun getItemCount(): Int = sessions.size

    fun updateSessions(newSessions: List<GameSession>) {
        sessions.clear()
        sessions.addAll(newSessions)
        notifyDataSetChanged()
    }
}
