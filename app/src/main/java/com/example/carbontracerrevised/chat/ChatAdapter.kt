package com.example.carbontracerrevised.chat

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracerrevised.MainActivity
import com.example.carbontracerrevised.R

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        // Access the object associated with the item
        val view: View = if (viewType == 1) {
            LayoutInflater.from(parent.context).inflate(R.layout.chat_msg_gemini, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.chat_msg_self, parent, false)

        }
         return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int {
        return messages.size
    }
    override fun getItemViewType(position: Int): Int {
        // Return the view type based on the boolean attribute of the object
        return if (messages[position].fromGemini) {
            1
        } else {
            0
        }
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)

        fun bind(message: ChatMessage) {
            messageTextView.layoutParams as ConstraintLayout.LayoutParams
            messageTextView.text = MainActivity.makeWordsBold(message.message)
        }


        private fun removeAsterisks(spannable: SpannableString): SpannableString {
            val spannableBuilder = SpannableStringBuilder(spannable)

            var index = 0
            while (index < spannableBuilder.length) {
                if (spannableBuilder[index] == '*') {
                    spannableBuilder.delete(index, index + 1)
                } else {
                    index++
                }
            }

            return SpannableString.valueOf(spannableBuilder)

        }
    }
}