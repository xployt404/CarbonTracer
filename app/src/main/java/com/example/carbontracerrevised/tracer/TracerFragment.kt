package com.example.carbontracerrevised.tracer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracerrevised.MainActivity
import com.example.carbontracerrevised.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TracerFragment() : Fragment() {
    private lateinit var traceableAdapter: TraceableAdapter
    private fun updateListFromDatabase() = (activity as MainActivity).updateListFromDatabase()
    private lateinit var recyclerView: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val tracerFragment = inflater.inflate(R.layout.tracer_fragment, container, false)
        recyclerView = tracerFragment.findViewById(R.id.traceableRecycler)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = traceableAdapter
        val optionsAndClearBtn = tracerFragment.findViewById<ImageButton>(R.id.options_and_clear_button)
        val addAndUnselectBtn = tracerFragment.findViewById<ImageButton>(R.id.add_and_unselect_button)
        val selectAllBtn = tracerFragment.findViewById<Button>(R.id.select_all_button)
        traceableAdapter.optionsAndClearBtn = optionsAndClearBtn
        traceableAdapter.addAndUnselectBtn = addAndUnselectBtn
        traceableAdapter.selectAllBtn = selectAllBtn

        val sortingPopMenu = PopupMenu(requireContext(), optionsAndClearBtn)

        // Inflating popup menu from popup_menu.xml file
        sortingPopMenu.menuInflater.inflate(R.menu.sorting_menu, sortingPopMenu.menu)
        sortingPopMenu.setOnMenuItemClickListener { menuItem ->
            lifecycleScope.launch {
                val sortedTraceables =  when(menuItem.itemId){
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
            when(menuItem.itemId){
                //TODO: implement all
                R.id.menu_item_help -> {

                }
                R.id.menu_item_generate ->{

                }
                R.id.menu_item_select_all ->{
                    selectAllTraceables()
                }
                R.id.menu_item_sort_by ->{
                    sortingPopMenu.show()
                }
            }

            true
        }

        optionsAndClearBtn.setOnClickListener {
            when(traceableAdapter.selectModeEnabled){
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
                        withContext(Dispatchers.IO){
                            traceableAdapter.selectedItems.clear()
                        }
                        withContext(Dispatchers.Main){
                            updateListFromDatabase()
                            traceableAdapter.toggleSelectMode()
                        }
                    }
                }
            }

        }

        addAndUnselectBtn.setOnClickListener {
            if (traceableAdapter.selectModeEnabled){
                traceableAdapter.selectedItems.clear()
                for (i in 0 until recyclerView.childCount) {
                    val view: View? = recyclerView.getChildAt(i)

                    view?.foreground = null
                }
            }else{
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

            view?.foreground = ContextCompat.getDrawable(requireContext(), R.drawable.traceable_selected)
        }
        traceableAdapter.toggleSelectMode()
    }


    override fun onResume() {
        super.onResume()
        updateListFromDatabase()
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