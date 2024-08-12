package com.example.carbontracerrevised.tracer

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracerrevised.ConfigFile
import com.example.carbontracerrevised.MainActivity
import com.example.carbontracerrevised.R
import com.example.carbontracerrevised.SharedViewModel
import com.example.carbontracerrevised.chat.ChatHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TracerFragment : Fragment() {
    private lateinit var traceableAdapter: TraceableAdapter
    private suspend fun updateListFromDatabase() =
        (activity as MainActivity).updateListFromDatabase()

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatHistory: ChatHistory
    private lateinit var viewModel: SharedViewModel

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val tracerFragment = inflater.inflate(R.layout.tracer_fragment, container, false)
        recyclerView = tracerFragment.findViewById(R.id.traceableRecycler)
        recyclerView.layoutManager = LinearLayoutManager(context)
        chatHistory = ChatHistory(requireContext())
        viewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        recyclerView.adapter = traceableAdapter
        val optionsAndClearBtn =
            tracerFragment.findViewById<ImageButton>(R.id.options_and_clear_button)
        val addAndUnselectBtn =
            tracerFragment.findViewById<ImageButton>(R.id.add_and_unselect_button)
        val selectAllBtn = tracerFragment.findViewById<Button>(R.id.selectAllButton)
        traceableAdapter.optionsAndClearBtn = optionsAndClearBtn
        traceableAdapter.addAndUnselectBtn = addAndUnselectBtn
        traceableAdapter.selectAllBtn = selectAllBtn
        traceableAdapter.tracerTitleTextView = tracerFragment.findViewById(R.id.tracerTitle)

        val sortingPopMenu = PopupMenu(requireContext(), optionsAndClearBtn)

        // Inflating popup menu from popup_menu.xml file
        sortingPopMenu.menuInflater.inflate(R.menu.sorting_menu, sortingPopMenu.menu)
        sortingPopMenu.setOnMenuItemClickListener { menuItem ->
            lifecycleScope.launch {
                val sortedTraceables = when (menuItem.itemId) {
                    R.id.menu_item_sort_by_name -> {
                        traceableAdapter.sortTraceablesBy(SORT_BY_NAME)
                    }

                    R.id.menu_item_sort_by_category -> {
                        traceableAdapter.sortTraceablesBy(SORT_BY_CATEGORY)
                    }

                    R.id.menu_item_sort_by_co2e -> {
                        traceableAdapter.sortTraceablesBy(SORT_BY_CO2E)
                    }

                    else -> {
                        traceableAdapter.traceableList
                    }
                }
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.Default) {
                            traceableAdapter.traceableList.clear()
                            traceableAdapter.traceableList.addAll(sortedTraceables)
                        }
                        withContext(Dispatchers.Main) {
                            traceableAdapter.notifyDataSetChanged()
                        }
                    } catch (e: Exception) {
                        // Handle the exception (e.g., log it or show a message to the user)
                    }
                }

            }

            true
        }
        val settingsPopMenu = PopupMenu(requireContext(), optionsAndClearBtn)

        // Inflating popup menu from popup_menu.xml file
        settingsPopMenu.menuInflater.inflate(R.menu.options_menu, settingsPopMenu.menu)
        settingsPopMenu.setOnMenuItemClickListener { menuItem ->
            // Toast message on menu item clicked
            when (menuItem.itemId) {
                //TODO: implement all
                R.id.menu_item_help -> {

                }

                R.id.menu_item_footprint -> {
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            (activity as MainActivity).viewPager.currentItem = 0
                            viewModel.setString(traceableAdapter.tracerListString())
                        }

                    }

                }

                R.id.menu_item_select_all -> {
                    selectAllTraceables()
                }

                R.id.menu_item_sort_by -> {
                    sortingPopMenu.show()
                }

                R.id.menu_item_api_key -> {
                    showApiKeyDialog()
                }
            }

            true
        }

        optionsAndClearBtn.setOnClickListener {
            when (traceableAdapter.selectModeEnabled) {
                false -> {
                    //TODO: display options for traceable
                    // Initializing the popup menu and giving the reference as current context
                    // Generate all, Help, sort
                    // Showing the popup menu
                    settingsPopMenu.show()
                }

                true -> {
                    lifecycleScope.launch {
                        (requireActivity() as MainActivity).traceableListObject.deleteTraceable(
                            traceableAdapter.selectedItems
                        )
                        withContext(Dispatchers.IO) {
                            traceableAdapter.selectedItems.clear()
                        }
                        withContext(Dispatchers.Main) {
                            for (i in 0 until recyclerView.childCount) {
                                val view: View? = recyclerView.getChildAt(i)

                                view?.foreground = null
                            }
                            updateListFromDatabase()
                            traceableAdapter.toggleSelectMode()
                        }
                    }
                }
            }

        }

        addAndUnselectBtn.setOnClickListener {
            if (traceableAdapter.selectModeEnabled) {
                traceableAdapter.selectedItems.clear()
                for (i in 0 until recyclerView.childCount) {
                    val view: View? = recyclerView.getChildAt(i)

                    view?.foreground = null
                }
            } else {
                (activity as MainActivity).showAddTraceableDialog(Traceable.newEmptyTraceable())

            }

            traceableAdapter.toggleSelectMode()
        }

        selectAllBtn.setOnClickListener {
            selectAllTraceables()
        }

        return tracerFragment
    }

    private fun selectAllTraceables() {
        traceableAdapter.selectedItems.clear()
        traceableAdapter.selectedItems.addAll(traceableAdapter.traceableList)
        for (i in 0 until recyclerView.childCount) {
            val view: View? = recyclerView.getChildAt(i)

            view?.foreground =
                ContextCompat.getDrawable(requireContext(), R.drawable.traceable_selected)
        }
        traceableAdapter.toggleSelectMode()
    }

    // Function to show the dialog
    private fun showApiKeyDialog() {
        // Create a Dialog
        lifecycleScope.launch {
            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_api_key)
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            // Find the EditText and Buttons
            val apiKeyEditText = dialog.findViewById<EditText>(R.id.editTextApiKey)
            apiKeyEditText.setText(
                ConfigFile.getJsonAttribute(
                    ConfigFile.read(requireContext()),
                    "apiKey"
                )
            )
            val okButton = dialog.findViewById<ImageButton>(R.id.ok_button)
            val cancelButton = dialog.findViewById<ImageButton>(R.id.cancel_button)

            // Set click listeners
            okButton.setOnClickListener {
                val apiKey = apiKeyEditText.text.toString().trim()
                if (apiKey.isEmpty()) {
                    // Show error message
                    apiKeyEditText.error = "This field is required"
                } else {
                    onApiKeyEntered(apiKey) // Handle the API key input
                    dialog.dismiss()
                }
            }
            cancelButton.setOnClickListener {
                dialog.cancel()
            }

            // Show the dialog
            dialog.show()
        }
    }


    private fun onApiKeyEntered(apiKey: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ConfigFile.updateJsonAttribute(requireContext(), "apiKey", apiKey)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            updateListFromDatabase()
        }
    }

    companion object {
        fun newInstance(adapter: TraceableAdapter): TracerFragment {
            val fragment = TracerFragment()
            fragment.traceableAdapter = adapter // Set the adapter
            return fragment
        }

        const val SORT_BY_NAME = 0
        const val SORT_BY_CATEGORY = 1
        const val SORT_BY_CO2E = 2
    }

}