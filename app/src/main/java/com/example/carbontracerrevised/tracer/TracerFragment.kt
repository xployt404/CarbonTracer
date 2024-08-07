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
import kotlinx.coroutines.launch


class TracerFragment(private val traceableAdapter: TraceableAdapter) : Fragment() {
    private fun updateListFromDatabase() = (activity as MainActivity).updateListFromDatabase()
    private lateinit var recyclerView: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val tracerFragment = inflater.inflate(R.layout.tracer_fragment, container, false)
        recyclerView = tracerFragment.findViewById(R.id.traceableRecycler)
        recyclerView.adapter = traceableAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        val optionsAndClearBtn = tracerFragment.findViewById<ImageButton>(R.id.options_and_clear_button)
        val addAndUnselectBtn = tracerFragment.findViewById<ImageButton>(R.id.add_and_unselect_button)
        val selectAllBtn = tracerFragment.findViewById<Button>(R.id.select_all_button)
        traceableAdapter.optionsAndClearBtn = optionsAndClearBtn
        traceableAdapter.addAndUnselectBtn = addAndUnselectBtn
        traceableAdapter.selectAllBtn = selectAllBtn

        optionsAndClearBtn.setOnClickListener {
            when(traceableAdapter.selectModeEnabled){
                false -> {
                    //TODO: display options for traceable
                    // Initializing the popup menu and giving the reference as current context
                    val popupMenu = PopupMenu(requireContext(), optionsAndClearBtn)

                    // Inflating popup menu from popup_menu.xml file
                    popupMenu.menuInflater.inflate(R.menu.options_menu, popupMenu.menu)
                    popupMenu.setOnMenuItemClickListener { menuItem ->
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
                        }

                        true
                    }
                    // Showing the popup menu
                    popupMenu.show()
                }
                true -> {
                    lifecycleScope.launch {
                        (requireActivity() as MainActivity).traceableListObject.deleteTraceable(traceableAdapter.selectedItems)
                    }
                    traceableAdapter.selectedItems.clear()
                    updateListFromDatabase()
                    traceableAdapter.toggleSelectMode()
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
}