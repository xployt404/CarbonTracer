package com.example.carbontracerrevised.onboardingscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.carbontracerrevised.ConfigFile
import com.example.carbontracerrevised.MainActivity
import com.example.carbontracerrevised.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ApiKeyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val apiKeyFragment = inflater.inflate(R.layout.api_key_fragment, container, false)
        val apiKeyEditText = apiKeyFragment.findViewById<EditText>(R.id.api_edit_text)
        val config = JSONObject()
        apiKeyFragment.findViewById<MaterialButton>(R.id.continue_button).setOnClickListener {
            config.put("apiKey", apiKeyEditText.text)
            config.put("flashMode", 2)
            lifecycleScope.launch {
                withContext(Dispatchers.IO){
                    ConfigFile.write(requireContext(), config.toString())
                }
                withContext(Dispatchers.Main){
                    requireActivity().finish()
                }
            }


        }
        return apiKeyFragment
    }
}

