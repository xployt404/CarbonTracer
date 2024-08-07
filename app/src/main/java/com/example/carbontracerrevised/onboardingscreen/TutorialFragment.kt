package com.example.carbontracerrevised.onboardingscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.carbontracerrevised.R
import com.google.android.material.button.MaterialButton

class TutorialFragment : Fragment(){
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val tutorialFragment = inflater.inflate(R.layout.tutorial_fragment, container, false)

        tutorialFragment.findViewById<MaterialButton>(R.id.continue_button).setOnClickListener {
            requireActivity().finish()
        }
        return tutorialFragment
    }
}

