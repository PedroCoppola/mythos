package com.mythos.mythos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderTextView: TextView = itemView.findViewById(R.id.message_sender)
        val messageTextView: TextView = itemView.findViewById(R.id.message_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.senderTextView.text = message.sender.name // USER or MODEL
        if (message.isLoading) {
            holder.messageTextView.text = "Typing..." // Or some other loading indicator
        } else {
            holder.messageTextView.text = message.text
        }

        // Basic differentiation (you can make this much more sophisticated)
        if (message.sender == Sender.USER) {
            holder.senderTextView.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            holder.messageTextView.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            // You might want to change background colors too
        } else {
            holder.senderTextView.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            holder.messageTextView.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateLastMessage(message: ChatMessage) {
        if (messages.isNotEmpty()) {
            messages[messages.size - 1] = message
            notifyItemChanged(messages.size - 1)
        }
    }
}
