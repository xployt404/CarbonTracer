package com.example.carbontracerrevised.onboardingscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.carbontracerrevised.R


class WelcomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val welcomeFragment = inflater.inflate(R.layout.welcome_fragment, container, false)
        welcomeFragment.findViewById<Button>(R.id.continue_button).setOnClickListener {
            (activity as OnboardingActivity).viewPager.currentItem = 1
        }
        return welcomeFragment
    }
}
