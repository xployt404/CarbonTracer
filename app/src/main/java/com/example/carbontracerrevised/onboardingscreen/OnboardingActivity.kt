package com.example.carbontracerrevised.onboardingscreen


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.carbontracerrevised.R
import com.example.carbontracerrevised.ViewPagerAdapter

class OnboardingActivity : AppCompatActivity() {
    lateinit var viewPager: ViewPager2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

       viewPager = findViewById(R.id.onboarding_pager)
        val fragments = listOf(WelcomeFragment(), TutorialFragment(), ApiKeyFragment())
        val adapter = ViewPagerAdapter(this, fragments)
        viewPager.adapter = adapter
    }



}
