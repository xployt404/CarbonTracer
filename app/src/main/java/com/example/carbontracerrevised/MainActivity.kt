package com.example.carbontracerrevised

import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.carbontracerrevised.camera.CameraFragment
import com.example.carbontracerrevised.chat.ChatFragment
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
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class MainActivity : AppCompatActivity() {
    companion object{
        const val CAMERA = 1

    }

    private var traceableList : MutableList<Traceable> = mutableListOf()
    lateinit var traceableListObject: TraceableList
    private val traceableAdapter = TraceableAdapter(this, lifecycleScope, traceableList)
    private var lastPage = CAMERA
    private lateinit var viewPager: ViewPager2

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (!allGranted) {
                //TODO: add permission handling
            } else {
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        traceableListObject = TraceableList.getInstance(this)
        setContentView(R.layout.activity_main)
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


        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        viewPager = findViewById(R.id.pager)
        val chatFragment = ChatFragment()
        val fragments = listOf(chatFragment, CameraFragment(), TracerFragment(traceableAdapter), StatisticsFragment(traceableAdapter))
        val adapter = ViewPagerAdapter(this, fragments)
        viewPager.adapter = adapter



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
    }
    fun updateListFromDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            traceableAdapter.traceableList.clear()
            traceableAdapter.traceableList.addAll(traceableListObject.readTracer())

            // Update UI on the main thread
            withContext(Dispatchers.Main) {
                traceableAdapter.notifyDataSetChanged() // Consider using more specific notify methods
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewPager.currentItem = 1
    }

    fun checkAndRequestPermissions(permissions: Array<String>) : Boolean{
        if (!checkPermissions(permissions)){
            requestMultiplePermissionsLauncher.launch(permissions)
            return false
        }else{
            return true
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

    fun showAddTraceableDialog(t : Traceable) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.add_traceable_dialog_layout)
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
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
            nameEditText.setText(objectName)
            occurrenceEditText.setText(occurrence)
            amountEditText.setText(amount)
            materialEditText.setText(material)
            co2eEditText.setText(co2e)
        }

        val editTextList = mutableListOf(nameEditText, materialEditText, amountEditText, occurrenceEditText, co2eEditText)
        editTextList.forEachIndexed { index, editText ->
            editText.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP)
                {
                    // Handle the Enter key press here
                    editTextList[index+1].requestFocus()
                    return@OnKeyListener true // Consume the event
                }
                false // Let the system handle other key events
            })
        }

        categorySwitcher.setOnClickListener {
            val popupMenu = PopupMenu(this, categorySwitcher)

            // Inflating popup menu from popup_menu.xml file
            popupMenu.menuInflater.inflate(R.menu.category_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                // Toast message on menu item clicked
                when(menuItem.itemId){
                    //TODO: implement all
                    R.id.menu_item_groceries -> {
                        categorySwitcher.text = TraceableAdapter.categories[GROCERIES]
                        categorySwitcher.setBackgroundColor(traceableAdapter.pieChartColors[GROCERIES])
                        t.category = GROCERIES

                    }
                    R.id.menu_item_consumer_products ->{
                        categorySwitcher.text = TraceableAdapter.categories[CONSUMER_PRODUCTS]
                        categorySwitcher.setBackgroundColor(traceableAdapter.pieChartColors[CONSUMER_PRODUCTS])
                        t.category = CONSUMER_PRODUCTS
                    }
                    R.id.menu_item_electronics ->{
                        categorySwitcher.text = TraceableAdapter.categories[ELECTRONICS]
                        categorySwitcher.setBackgroundColor(traceableAdapter.pieChartColors[ELECTRONICS])
                        t.category = ELECTRONICS
                    }
                    R.id.menu_item_transport ->{
                        categorySwitcher.text = TraceableAdapter.categories[TRANSPORT]
                        categorySwitcher.setBackgroundColor(traceableAdapter.pieChartColors[TRANSPORT])
                        t.category = TRANSPORT
                    }
                    R.id.menu_item_misc ->{
                        categorySwitcher.text = TraceableAdapter.categories[MISC]
                        categorySwitcher.setBackgroundColor(traceableAdapter.pieChartColors[MISC])
                        t.category = MISC
                    }
                }

                true
            }
            popupMenu.show()
        }

        dialog.findViewById<ImageButton>(R.id.dialog_cancel_button).setOnClickListener { dialog.dismiss()}
        dialog.findViewById<ImageButton>(R.id.dialog_add_button).setOnClickListener {
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
            lifecycleScope.launch {
                traceableListObject.insertTraceable(t)
            }
            updateListFromDatabase()
            dialog.dismiss()
        }


        dialog.show()
    }
    private fun isConfigFileExists(): Boolean {
        // Get the file from internal storage
        val file = File(filesDir, "config.json")
        return file.exists() // Check if the file exists
    }



}
