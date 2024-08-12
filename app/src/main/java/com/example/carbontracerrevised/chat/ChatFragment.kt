package com.example.carbontracerrevised.chat

import android.Manifest.permission.CAMERA
import android.Manifest.permission.INTERNET
import android.Manifest.permission.RECORD_AUDIO
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracerrevised.AudioRecorder
import com.example.carbontracerrevised.GeminiModel
import com.example.carbontracerrevised.MainActivity
import com.example.carbontracerrevised.R
import com.example.carbontracerrevised.SharedViewModel
import com.google.ai.client.generativeai.type.UnknownException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

class ChatFragment : Fragment() {
    private val model = GeminiModel()
    private lateinit var typingIndicatorLayout : CardView
    private var chatHistoryList : MutableList<ChatMessage> = mutableListOf()
    private lateinit var chatHistory: ChatHistory
    private lateinit var viewModel: SharedViewModel
    private var chatAdapter = ChatAdapter(this.chatHistoryList)
    private lateinit var recyclerView: RecyclerView
    private lateinit var recorder : AudioRecorder
    private lateinit var chatDbList : MutableList<ChatMessage>
    private lateinit var glowView: View
    private lateinit var sendButton: ImageButton

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val chatFragmentView = inflater.inflate(R.layout.chat_fragment, container, false)
        val messageTextEdit = chatFragmentView.findViewById<EditText>(R.id.messageEditText)
        recyclerView = chatFragmentView.findViewById(R.id.chatHistory)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.itemAnimator = null
        recorder = AudioRecorder()
        recyclerView.adapter = chatAdapter
        chatHistory = ChatHistory(requireContext())
        viewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        viewModel.sharedString.observe(viewLifecycleOwner) { data ->
            lifecycleScope.launch {
                try {
                    launch {
                        addMessage(
                            model.Tracer().evaluateFootprint(requireContext(), data),
                            isGemini = true
                        )
                        withContext(Dispatchers.Main){
                            stopTypingAnimation()
                        }
                    }
                    withContext(Dispatchers.Main){
                        scrollToBottom()
                        startTypingAnimation()
                        (activity as MainActivity).viewPager.currentItem = 0
                    }
                }catch (e: UnknownException){
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Unable to reach Gemini >_<", Toast.LENGTH_SHORT).show()
                    }
                }catch (e : Exception){
                    withContext(Dispatchers.Main){
                        Toast.makeText(requireContext(), "Response from the AI was inconclusive >_<", Toast.LENGTH_SHORT).show()
                    }
                }finally {
                    withContext(Dispatchers.Main){
                        stopTypingAnimation()
                    }
                }
            }
        }
        typingIndicatorLayout = chatFragmentView.findViewById(R.id.typingIndicatorCardView)
        val clearChatBtn = chatFragmentView.findViewById<ImageButton>(R.id.clear_chat_button)
        glowView = chatFragmentView.findViewById(R.id.glowView)
        clearChatBtn.setOnClickListener {
            lifecycleScope.launch {
                chatHistory.clearChartHistory()
                updateChatHistory()
            }
        }
        messageTextEdit.setOnClickListener {
            scrollToBottom()
        }
        messageTextEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed for this case
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not needed for this case
            }

            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                val containsLetter = input.any { it.isLetter() }

                if (containsLetter) {
                    chatFragmentView.findViewById<ImageButton>(R.id.sendButton).setImageResource(R.drawable.ic_send)
                } else {
                    chatFragmentView.findViewById<ImageButton>(R.id.sendButton).setImageResource(R.drawable.ic_mic)
                }
            }
        })


        sendButton = chatFragmentView.findViewById(R.id.sendButton)
        sendButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.i(TAG, "ACTION DOWN")
                    try {
                        if (messageTextEdit.text.isEmpty() && !model.generating){
                            startPulsatingGlow()
                            recorder.startRecording(requireContext(), lifecycleScope)
                        }else{
                            v.performClick()
                        }
                    }catch (e: Exception){
                        Log.e(TAG, e.message.toString())
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "Action Up")
                    if (recorder.isRecording){
                        Log.d(TAG, "Stopping Recording")
                        scrollToBottom()
                        stopPulsatingGlow()
                        recorder.stopRecording()
                        if (!model.generating){
                            sendAudio()
                        }
                        recorder.isRecording = false
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    Log.d(TAG, "Action was CANCEL")
                    recorder.stopRecording()
                    stopPulsatingGlow()
                    true
                }
                MotionEvent.ACTION_OUTSIDE -> {
                    Log.d(TAG, "Movement occurred outside bounds of current screen element")
                    true
                }
                else -> false
            }

        }
        sendButton.setOnClickListener {
            if (!model.generating and messageTextEdit.text.isNotEmpty()){
                sendText(messageTextEdit.text.toString())
                messageTextEdit.text = null
            }
        }
        (requireActivity() as MainActivity).checkAndRequestPermissions(REQUIRED_PERMISSIONS)

        return chatFragmentView
    }

    private fun sendAudio() {
        lifecycleScope.launch {
            startTypingAnimation()
            try {
                model.File().sendFile(requireContext(),
                        File(
                            requireContext().filesDir, "recording.ogg"
                        ).toUri(),
                        chatHistory
                    )
            }catch (e : Exception){
                addMessage("ERROR: ".plus(e.message.toString()), true)
            }finally {
                stopTypingAnimation()
            }
            updateChatHistory()
        }
    }

    private fun addMessage(msg : String, isGemini : Boolean){
        Log.d("MESSAGE", "adding message: $msg")
        lifecycleScope.launch {
            chatHistory.insertChatMessage(
                isGemini,
                msg,
                Instant.now().toEpochMilli().toString()
            )
            updateChatHistory()
        }
    }

    private fun scrollToBottom(){
        recyclerView.post {recyclerView.scrollToPosition(recyclerView.adapter?.itemCount?.minus(1)!!)}
    }

    private fun startTypingAnimation() {
        val dot1 = requireActivity().findViewById<View>(R.id.dot1)
        val dot2 = requireActivity().findViewById<View>(R.id.dot2)
        val dot3 = requireActivity().findViewById<View>(R.id.dot3)
        typingIndicatorLayout.visibility = View.VISIBLE

        val animator1 = ObjectAnimator.ofFloat(dot1, "translationY", 0f, -8f)
        val animator2 = ObjectAnimator.ofFloat(dot2, "translationY", 0f, -12f)
        animator2.startDelay = 200
        val animator3 = ObjectAnimator.ofFloat(dot3, "translationY", 0f, -18f)
        animator3.startDelay = 400
        val animator4 = ObjectAnimator.ofFloat(dot1, "translationY", -8f, 0f)
        animator4.startDelay = 500
        val animator5 = ObjectAnimator.ofFloat(dot2, "translationY", -12f, 0f)
        animator5.startDelay = 700
        val animator6 = ObjectAnimator.ofFloat(dot3, "translationY", -18f, 0f)
        animator6.startDelay = 900

        val upAnimatorSet = AnimatorSet()
        upAnimatorSet.duration = 500
        upAnimatorSet.interpolator = AccelerateInterpolator()
        upAnimatorSet.playTogether(animator1, animator2, animator3, animator4, animator5, animator6)
        upAnimatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                upAnimatorSet.start()
            }
        })
        upAnimatorSet.start()

    }

    override fun onResume() {
        super.onResume()

        if (model.generating){
            startTypingAnimation()
        }else{
            stopTypingAnimation()
        }
        lifecycleScope.launch{
            updateChatHistory()
        }

    }

    private suspend fun updateChatHistory(){

        chatHistoryList.lastIndex
        chatDbList = chatHistory.readChatHistory()
        chatHistoryList.clear()
        Log.i(TAG, chatHistoryList.count().toString())
        chatHistoryList.addAll(chatDbList)
        withContext(Dispatchers.Main){
            chatAdapter.notifyItemInserted(chatHistoryList.lastIndex)
            scrollToBottom()
        }
    }

    private fun sendText(msg: String) {
        model.generating = true
        addMessage(msg, false)
        startTypingAnimation()
        scrollToBottom()

        // Launch a coroutine to handle the network call
       lifecycleScope.launch {
            lateinit var response: String
            try {
                // Switch to IO context for the network call
                model.Chat(requireContext()).sendPrompt(msg)
                model.chatHistoryString += "output: $response\n\n"
            } catch (e: Exception) {
                response = "ERROR: ${e.message}"
            } finally {
                model.generating = false
                addMessage(response, true)
                stopTypingAnimation()
                Log.d("GEMINI RESPONSE", response)
            }
        }
    }
    private fun stopTypingAnimation(){
        typingIndicatorLayout.visibility = View.GONE
    }
    companion object{
        private const val TAG = "ChatFragment"
        private val REQUIRED_PERMISSIONS = arrayOf(RECORD_AUDIO, INTERNET, CAMERA)
    }

    override fun onStop() {
        super.onStop()
        recorder.stopRecording()
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder.stopRecording()
        model.generating = false
    }
    private fun startPulsatingGlow() {
        glowView.visibility = View.VISIBLE
        val scaleX = ObjectAnimator.ofFloat(glowView, "scaleX", 1f, 1.6f, 1f)
        val scaleY = ObjectAnimator.ofFloat(glowView, "scaleY", 1f, 1.6f, 1f)
        scaleX.duration = 1500
        scaleY.duration = 1500
        scaleX.repeatCount = ObjectAnimator.INFINITE
        scaleY.repeatCount = ObjectAnimator.INFINITE
        scaleX.repeatMode = ObjectAnimator.REVERSE
        scaleY.repeatMode = ObjectAnimator.REVERSE

        val animatorSet = AnimatorSet()
        animatorSet.play(scaleX).with(scaleY)
        animatorSet.start()
    }

    private fun stopPulsatingGlow() {
        glowView.visibility = View.INVISIBLE
        glowView.clearAnimation() // Stop any ongoing animations
    }
}