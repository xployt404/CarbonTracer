package com.example.carbontracerrevised

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.text.Html
import android.text.Spanned
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.carbontracerrevised.camera.CameraFragment
import com.example.carbontracerrevised.chat.ChatFragment
import com.example.carbontracerrevised.onboardingscreen.OnboardingActivity
import com.example.carbontracerrevised.statistics.StatisticsFragment
import com.example.carbontracerrevised.tracer.CONSUMER_PRODUCTS
import com.example.carbontracerrevised.tracer.ELECTRONICS
import com.example.carbontracerrevised.tracer.GROCERIES
import com.example.carbontracerrevised.tracer.MISC
import com.example.carbontracerrevised.tracer.TRANSPORT
import com.example.carbontracerrevised.tracer.Traceable
import com.example.carbontracerrevised.tracer.TraceableAdapter
import com.example.carbontracerrevised.tracer.TraceableList
import com.example.carbontracerrevised.tracer.TracerFragment
import com.google.ai.client.generativeai.type.UnknownException
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class MainActivity : AppCompatActivity() {
    companion object{
        const val CAMERA = 1
        fun makeWordsBold(input: String): Spanned {
            // Create a SpannableString from the input
            // Find the start and end indices of the text enclosed in double asterisks
            val regex = "\\*\\*(.*?)\\*\\*".toRegex()
            val formattedText = regex.replace(input.replace("\n", "<br>")) { matchResult ->
                "<b>${matchResult.groupValues[1]}</b>"

            }



            return Html.fromHtml(formattedText,Html.FROM_HTML_MODE_LEGACY)
        }

    }

    private var traceableList : MutableList<Traceable> = mutableListOf()
    lateinit var traceableListObject: TraceableList
    private lateinit var traceableAdapter : TraceableAdapter
    private var lastPage = CAMERA
    lateinit var viewPager: ViewPager2
    private lateinit var requestMultiplePermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var tabLayout: TabLayout

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        //TODO: Remove
        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .penaltyDeath()
                .penaltyLog()
                .detectAll()
                .build()
        )
        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
        // Initialize the permission request launcher
        requestMultiplePermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionsResult(permissions)
        }
        traceableListObject = TraceableList.getInstance(this)
        viewPager = findViewById(R.id.pager)
        traceableAdapter = TraceableAdapter(this, lifecycleScope, traceableList)
        val tracerFragment = TracerFragment.newInstance(traceableAdapter)
        val fragments = listOf(ChatFragment(), CameraFragment(), tracerFragment, StatisticsFragment.newInstance(traceableAdapter))
        val adapter = ViewPagerAdapter(this@MainActivity, fragments)
        viewPager.adapter = adapter


        tabLayout = findViewById(R.id.tabLayout)
        val rootView = findViewById<View>(android.R.id.content)
        val rect = Rect()
        rootView.getWindowVisibleDisplayFrame(rect)
        val height = rect.bottom
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            rootView.getWindowVisibleDisplayFrame(rect)
            if (rect.bottom< height){
                tabLayout.visibility = View.GONE
            }else{
                tabLayout.visibility = View.VISIBLE
            }
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // This method will be invoked when a tab is selected
                viewPager.currentItem = tab.position
                // React to tab selection
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // This method will be invoked when a tab is unselected
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // This method will be invoked when a tab is reselected
            }
        })

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tabLayout.getTabAt(position)?.select()
                lastPage = position
            }
        })
        lifecycleScope.launch {
            if (!configFileExists()){
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
            }
        }
    }
    suspend fun updateListFromDatabase() {
        withContext(Dispatchers.Default) {
            traceableAdapter.traceableList.clear()
            traceableAdapter.traceableList.addAll(traceableListObject.readTracer())
        }

        // Update UI on the main thread
        withContext(Dispatchers.Main) {
            traceableAdapter.notifyDataSetChanged()
        }
    }

    override fun onStart() {
        super.onStart()
        viewPager.currentItem = 1
    }
    fun showPopupWindow(anchorView: View, fullResponse :String) {
        // Inflate the popup_layout.xml
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.full_response_pop_up, null)


        // Create the PopupWindow
        val popupWindow = PopupWindow(popupView,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT)

        popupView.findViewById<TextView>(R.id.popupText).text = makeWordsBold(fullResponse)
        // Set up the close button in the popup
        val closeButton: ImageButton = popupView.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            popupWindow.dismiss() // Dismiss the popup when the button is clicked
        }

        // Show the PopupWindow
        popupWindow.isFocusable = true // Allow interaction with the popup
        popupWindow.showAtLocation(anchorView,
            Gravity.CENTER, 0, 0)
    }


    fun showAddTraceableDialog(t : Traceable): Dialog {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.add_traceable_dialog_layout)
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.let { window ->
            val layoutParams = window.attributes
            window.setGravity(Gravity.CENTER)
            layoutParams.y = -50 // Adjust this value to move the dialog up or down
            window.attributes = layoutParams
        }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)
        dialog.findViewById<ImageButton>(R.id.generateCo2eButton)
        val categorySwitcher = dialog.findViewById<Button>(R.id.categorySwitcher)
        dialog.findViewById<ProgressBar>(R.id.progressBar)
        val amountEditText = dialog.findViewById<EditText>(R.id.amountEditText)
        val occurrenceEditText = dialog.findViewById<EditText>(R.id.occurrenceEditText)
        val nameEditText = dialog.findViewById<EditText>(R.id.nameEditText)
        val materialEditText = dialog.findViewById<EditText>(R.id.materialEditText)
        val co2eEditText = dialog.findViewById<EditText>(R.id.co2eEditText)

        with(t) {
            nameEditText.setText(name)
            occurrenceEditText.setText(occurrence)
            amountEditText.setText(amount)
            materialEditText.setText(material)
            co2eEditText.setText(co2e)
        }

        val editTextList = listOf<EditText>(nameEditText, materialEditText, amountEditText, occurrenceEditText, co2eEditText)
        editTextList.forEach { editText ->
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus){
                    editText.setSelection(editText.text.length)
                }
            }
        }

        materialEditText.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP)
            {
                // Handle the Enter key press here
                categorySwitcher.performClick()
                return@OnKeyListener true // Consume the event
            }
            false // Let the system handle other key events
        })

        categorySwitcher.setOnClickListener {
            val popupMenu = PopupMenu(this, categorySwitcher)

            // Inflating popup menu from popup_menu.xml file
            popupMenu.menuInflater.inflate(R.menu.category_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                // Toast message on menu item clicked
                val category = when(menuItem.itemId){
                    R.id.menu_item_groceries -> {
                        GROCERIES
                    }
                    R.id.menu_item_consumer_products ->{
                        CONSUMER_PRODUCTS
                    }
                    R.id.menu_item_electronics ->{
                        ELECTRONICS
                    }
                    R.id.menu_item_transport ->{
                        TRANSPORT
                    }
                    R.id.menu_item_misc ->{
                        MISC
                    }

                    else -> {
                        MISC
                    }
                }
                categorySwitcher.text = TraceableAdapter.categories[category]
                categorySwitcher.setBackgroundColor(traceableAdapter.pieChartColors[category])
                t.category = category
                    amountEditText.requestFocus()
                true
            }
            popupMenu.show()
        }
        var fullResponse = ""
        val showFullResponseBtn = dialog.findViewById<ImageButton>(R.id.show_full_response_button)
        showFullResponseBtn.setOnClickListener {
            showPopupWindow(it, fullResponse)
        }
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        dialog.findViewById<ImageButton>(R.id.generateCo2eButton).setOnClickListener {
            showFullResponseBtn.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                try {
                    updateTraceableFromEditTextList(t, editTextList)
                    val response = traceableAdapter.model.Tracer().generateCo2e(this@MainActivity , t, fullResponse = true)
                    val calculatedCO2e = traceableAdapter.convertToKg(
                        traceableAdapter.removeUnwantedChars(
                            response[0]!!
                        )
                    )
                    fullResponse = response[1]!!
                    co2eEditText.setText(calculatedCO2e)
                    updateTraceableFromEditTextList(t, editTextList)
                }catch (e: UnknownException){
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Unable to reach Gemini >_<", Toast.LENGTH_SHORT).show()
                    }
                }catch (e : Exception){
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity, "Response from the AI was inconclusive >_<", Toast.LENGTH_SHORT).show()
                    }
                }finally {
                    progressBar.visibility = View.GONE
                    showFullResponseBtn.visibility = View.VISIBLE

                }


            }
        }

        dialog.findViewById<ImageButton>(R.id.dialog_cancel_button).setOnClickListener { dialog.dismiss()}
        dialog.findViewById<ImageButton>(R.id.dialog_add_button).setOnClickListener {

            lifecycleScope.launch {
                updateTraceableFromEditTextList(t, editTextList)
                traceableListObject.insertTraceable(t)
                updateListFromDatabase()
            }

            dialog.dismiss()
        }


        dialog.show()
        return dialog
    }
    private suspend fun updateTraceableFromEditTextList(t: Traceable, editTextList: List<EditText>){
        withContext(Dispatchers.Main) {
            editTextList.forEachIndexed { index, editText ->
                val text = editText.text.toString()
                if (index < TraceableAdapter.propertyNames.size) {
                    val propertyName = TraceableAdapter.propertyNames[index]
                    val field = t.javaClass.getDeclaredField(propertyName)
                    field.isAccessible = true // Set the field accessible
                    field.set(t, text)
                }

                println("Currently processing EditText at index: $index")

            }
        }
    }

//TODO: USE


    private suspend fun configFileExists(): Boolean {
        return  withContext(Dispatchers.IO){
            // Get the file from internal storage
            val file = File(filesDir, "config.json")
            file.exists() // Check if the file exists
        }
    }




    fun checkAndRequestPermissions(permissions: Array<String>): Boolean {
        return if (!checkPermissions(permissions)) {
            // Launch the permission request
            requestMultiplePermissionsLauncher.launch(permissions)
            false // Indicate that permissions were not granted yet
        } else {
            true // All permissions are already granted
        }
    }

    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "Missing Permissions, exiting :(", Toast.LENGTH_SHORT).show()
            finish() // Exit the app or handle as needed
        }
    }

    private fun checkPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

}


