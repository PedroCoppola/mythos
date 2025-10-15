package com.mythos.mythos

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val context = holder.itemView.context

        // Detenemos cualquier animación que pudiera estar corriendo en esta vista reciclada
        (holder.itemView.tag as? ValueAnimator)?.cancel()
        holder.itemView.tag = null

        if (message.isLoading) {
            // --- ESTILO PARA EL MENSAJE DE "CARGANDO" ---
            holder.messageText.text = ". . ."
            holder.messageText.setTextColor(Color.GRAY)
            holder.messageText.typeface = Typeface.DEFAULT
            holder.messageText.alpha = 0.7f

        } else if (message.sender == Sender.USER) {
            // --- ESTILO PARA EL USUARIO ---
            holder.messageText.text = "> ${message.text}"
            holder.messageText.setTextColor(Color.parseColor("#eeba2b"))
            holder.messageText.typeface = Typeface.DEFAULT_BOLD
            holder.messageText.alpha = 1.0f

        } else { // Sender.MODEL
            // --- ESTILO PARA LA HISTORIA (MODELO) ---
            holder.messageText.setTextColor(Color.WHITE)
            holder.messageText.alpha = 1.0f
            try {
                // He puesto 'notoserif' que es la fuente que usas en el login. ¡Cámbiala si es otra!
                val fantasyFont = ResourcesCompat.getFont(context, R.font.notoserif)
                holder.messageText.typeface = fantasyFont ?: Typeface.SERIF
            } catch (e: Exception) {
                holder.messageText.typeface = Typeface.SERIF
            }

            // ----- ¡¡¡ LA LÓGICA CLAVE Y CORREGIDA ESTÁ AQUÍ !!! -----
            val isLastMessage = (position == messages.size - 1)

            if (isLastMessage) {
                // SI ES EL ÚLTIMO MENSAJE DEL MODELO: Animamos la escritura.
                holder.messageText.text = "" // Empezamos vacío para animar

                val textToAnimate = message.text
                val duration = (textToAnimate.length * 40).toLong().coerceIn(300, 4000)
                val animator = ValueAnimator.ofInt(0, textToAnimate.length)
                animator.duration = duration

                animator.addUpdateListener { animation ->
                    // Solo actualiza el texto si la vista no ha sido reciclada para otro mensaje
                    if (holder.adapterPosition == position) {
                        val animatedValue = animation.animatedValue as Int
                        holder.messageText.text = textToAnimate.substring(0, animatedValue)
                    } else {
                        // Si la vista se recicló, cancelamos la animación.
                        animation.cancel()
                    }
                }
                // Guardamos el animator en el tag para poder cancelarlo si es necesario.
                holder.itemView.tag = animator
                animator.start()

            } else {
                // SI ES UN MENSAJE ANTIGUO DEL MODELO: Mostramos el texto completo al instante.
                holder.messageText.text = message.text
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateLastMessage(message: ChatMessage) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            messages[lastIndex] = message
            notifyItemChanged(lastIndex)
        }
    }

    override fun onViewRecycled(holder: MessageViewHolder) {
        super.onViewRecycled(holder)
        (holder.itemView.tag as? ValueAnimator)?.cancel()
    }
}
