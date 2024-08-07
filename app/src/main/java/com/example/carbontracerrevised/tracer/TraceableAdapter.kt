package com.example.carbontracerrevised.tracer

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracerrevised.GeminiModel
import com.example.carbontracerrevised.R
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TraceableAdapter(private val activity: Activity, private val lifecycleScope: CoroutineScope, val traceableList : MutableList<Traceable>) : RecyclerView.Adapter<TraceableAdapter.TraceableViewHolder>() {
    private lateinit var traceableListObject: TraceableList
    var selectModeEnabled = false
    val selectedItems = mutableListOf<Traceable>()
    private val model = GeminiModel()
    private var currentExpandedTraceable : TraceableViewHolder? = null
    lateinit var optionsAndClearBtn : ImageButton
    lateinit var addAndUnselectBtn : ImageButton
    lateinit var selectAllBtn: Button
    private lateinit var progressBarColorFilter : PorterDuffColorFilter
    val pieChartColors = ColorTemplate.MATERIAL_COLORS.toList() + Color.LTGRAY


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        progressBarColorFilter = PorterDuffColorFilter(ContextCompat.getColor(activity, R.color.light_blue_600), PorterDuff.Mode.SRC_IN)
        traceableListObject = TraceableList.getInstance(activity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TraceableViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.traceable_element, parent, false)
        return TraceableViewHolder(view)
    }

    override fun onBindViewHolder(holder: TraceableViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: is called")
        val item = traceableList[position]
        holder.bind(item)
        //TODO: Set the color of the ProgressBar to blue


        val clickToCollapse = listOf<View>(
            holder.header,
            holder.header.findViewById(R.id.objectName),
            holder.header.findViewById(R.id.co2e)
        )
        for (v in clickToCollapse){
            v.setOnClickListener {
                Log.i(TAG, "onBindViewHolder: Click")
                if (!selectModeEnabled){
                    when(holder.expanded) {
                        false -> {
                            Log.d(TAG, "onBindViewHolder: not expanded")
                            currentExpandedTraceable?.body?.visibility = View.GONE
                            currentExpandedTraceable?.expanded = false
                            holder.expandView(holder.body)
                            currentExpandedTraceable = holder
                        }
                        else -> {
                            holder.collapseView(holder.body)
                        }
                    }
                }else{
                    v.performLongClick()
                }
            }


            v.setOnLongClickListener {
                Log.i(TAG, "onBindViewHolder: Long Click")
                if (!selectedItems.contains(item)){
                    holder.itemView.foreground = ContextCompat.getDrawable(activity,R.drawable.traceable_selected)
                    selectedItems.add(item)
                }else{
                    holder.itemView.foreground = null
                    selectedItems.remove(item)
                }
                toggleSelectMode()
                true
            }
        }



        holder.editTextList.forEachIndexed { index, editText ->
            Log.d(TAG, "onBindViewHolder: FocusListener $index set")
            editText.setOnFocusChangeListener { view, hasFocus ->
                lifecycleScope.launch {
                    Log.d(TAG, "onBindViewHolder: focusChange")
                if (!hasFocus) {
                    val text = (view as EditText).text.toString()

                    if (index < propertyNames.size) {
                        val propertyName = propertyNames[index]
                        val field = item.javaClass.getDeclaredField(propertyName)
                        field.isAccessible = true // Set the field accessible
                        field.set(item, text)
                    }


                        traceableListObject.updateTraceable(item)
                    }
                }
            }
        }
        holder.categorySwitcher.setOnClickListener {
            val popupMenu = PopupMenu(activity, holder.categorySwitcher)

            // Inflating popup menu from popup_menu.xml file
            popupMenu.menuInflater.inflate(R.menu.category_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                // Toast message on menu item clicked
                when(menuItem.itemId){
                    //TODO: implement all
                    R.id.menu_item_groceries -> {
                        holder.updateCategory(item, GROCERIES)
                    }
                    R.id.menu_item_consumer_products ->{
                        holder.updateCategory(item, CONSUMER_PRODUCTS)
                    }
                    R.id.menu_item_electronics ->{
                        holder.updateCategory(item, ELECTRONICS)
                    }
                    R.id.menu_item_transport ->{
                        holder.updateCategory(item, TRANSPORT)
                    }
                    R.id.menu_item_misc ->{
                        holder.updateCategory(item, MISC)
                    }
                }

                true
            }
            popupMenu.show()
        }
        holder.generateCo2eButton.setOnClickListener {
            // Use lifecycleScope to ensure proper lifecycle management
            holder.progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                val response = convertToKg(removeUnwantedChars(model.Tracer().generateCo2e(activity.applicationContext, item)))
                item.co2e = response

                // Switch back to the main thread for UI updates
                withContext(Dispatchers.Main) {
                    holder.co2eEditText.setText(response)
                    holder.progressBar.visibility = View.INVISIBLE
                    traceableListObject.updateTraceable(item)
                }
            }
        }


    }



    private fun convertToKg(str : String) : String{
        val unit = str.replace(Regex("\\s+"), "").takeLast(2)
        return if ( unit == "kg"){ // unit is kilograms
            str
        }else if (unit.last() == 't'){ // unit is metric tons
            (str.replace(Regex("[^\\d.,]"), "").toFloat()*1000).toString() + " kg"
        }else{ // unit is grams
            (str.replace(Regex("[^\\d.,]"), "").toFloat()/1000).toString() + " kg"
        }
    }

    private fun rotateView(view: View, newRotation : Float){
    val animator = ValueAnimator.ofFloat(view.rotation, newRotation)
    animator.addUpdateListener { animation ->
        val animatedValue = animation.animatedValue as Float
        view.rotation = animatedValue
    }
    animator.duration = 200 // Set the duration of the animation in milliseconds
    animator.start() // Start the animation
}

    fun toggleSelectMode(){
        Log.d(TAG, "toggleSelectMode: toggled")
        if (selectedItems.size>0){

            optionsAndClearBtn.setImageResource(android.R.drawable.ic_menu_delete)
            rotateView(addAndUnselectBtn, 0F)
            selectAllBtn.visibility = View.VISIBLE
            selectModeEnabled = true
        }else{
            optionsAndClearBtn.setImageResource(R.drawable.ic_three_dots)
            rotateView(addAndUnselectBtn, 45F)
            selectAllBtn.visibility = View.INVISIBLE
            selectModeEnabled = false
        }
    }

    inner class TraceableViewHolder (view: View): RecyclerView.ViewHolder(view){
        var expanded: Boolean = false
        val header : ConstraintLayout
        val body: TableLayout

        private val amountEditText: EditText
        private val occurrenceEditText: EditText
        private val co2eTextView: TextView
        private val nameEditText: EditText
        private val materialEditText: EditText
         val co2eEditText: EditText
        private val nameTextView: TextView
        val generateCo2eButton: ImageButton
         val categorySwitcher: Button
         val progressBar: ProgressBar


        internal var editTextList : MutableList<View>
        init {
             header = view.findViewById(R.id.traceableHeaderLayout)
             body = view.findViewById(R.id.tracerExpandedPart)
            generateCo2eButton = view.findViewById(R.id.generateCo2eButton)
            categorySwitcher = view.findViewById(R.id.categorySwitcher)
            progressBar = view.findViewById(R.id.progressBar)

            amountEditText = view.findViewById(R.id.amountEditText)
            occurrenceEditText = view.findViewById(R.id.occurrenceEditText)
            co2eTextView = header.findViewById(R.id.co2e)
            nameEditText = view.findViewById(R.id.nameEditText)
            materialEditText = view.findViewById(R.id.materialEditText)
            co2eEditText = view.findViewById(R.id.co2eEditText)
            nameTextView = header.findViewById(R.id.objectName)

            editTextList = mutableListOf(nameEditText, materialEditText, amountEditText, occurrenceEditText, co2eEditText)
            nameEditText.doOnTextChanged { text, _, _, _ ->
                nameTextView.text = text
            }
            co2eEditText.doOnTextChanged { text, _, _, _ ->
                co2eTextView.text = text
            }
        }
        internal fun updateCategory(item: Traceable, category: Int) {
            Log.d(TAG, "updateCategory: updated")
            item.category = category
            categorySwitcher.text = categories[item.category]
            categorySwitcher.setBackgroundColor(pieChartColors[item.category])
            lifecycleScope.launch {
                traceableListObject.updateTraceable(item)
            }
        }

        fun bind(traceable: Traceable) {
            Log.d(TAG, "bind: bind")
            with(traceable) {
                nameTextView.text = traceable.objectName
                nameEditText.setText(traceable.objectName)
                materialEditText.setText(traceable.material)
                amountEditText.setText(traceable.amount)
                occurrenceEditText.setText(traceable.occurrence)
                categorySwitcher.text = categories[category]
                categorySwitcher.setBackgroundColor(pieChartColors[category])
                co2eEditText.setText(traceable.co2e)

            }
        }

        internal fun expandView(view: View) {
            Log.d(TAG, "expandView: expanding")
            view.visibility = View.VISIBLE
            expanded = true
        }

//        internal fun expandView(view: View) {
//            val transition = AutoTransition()
//            transition.doOnStart {
//                // EditText have an error which makes the text invisible when the traceable is expanded again.
//                // Resetting the text inside fixes the issue
//                for (editText in editTextList){
//                    editText.text = editText.text
//                }
//            }
//            view.layoutParams.height = 0
//            view.visibility = View.VISIBLE
//            transition.duration = 200 // Set the duration of the animation in milliseconds
//            TransitionManager.beginDelayedTransition(view.parent as ViewGroup, transition)
//
//        }


        internal fun collapseView(view: View) {
            Log.d(TAG, "collapseView: collapsing")
            view.visibility = View.GONE
            expanded = false
        }
//        internal fun collapseView(view: View) {
//            val initialHeight = view.height
//
//            val animator = ValueAnimator.ofInt(initialHeight, 0)
//            animator.addUpdateListener { animation ->
//                val animatedValue = animation.animatedValue as Int
//                val layoutParams = view.layoutParams
//                layoutParams.height = animatedValue
//                view.layoutParams = layoutParams
//            }
//            animator.addListener(object : AnimatorListenerAdapter() {
//                override fun onAnimationEnd(animation: Animator) {
//                    view.visibility = View.GONE // Hide the view after the animation ends
//                }
//            })
//            animator.duration = 200 // Set the duration of the animation in milliseconds
//            animator.start() // Start the animation
//            expanded = false
//        }
    }

    override fun getItemCount() = traceableList.size
    override fun getItemViewType(position: Int): Int {
        // Return the view type based on the boolean attribute of the object
        return position
    }

    private fun removeUnwantedChars(input: String): String {
        // Define the regex pattern to match any character that is not a digit, decimal point, or comma
        val regex = Regex("[^a-zA-Z0-9.,\\s]+")
        val result = input.replace(regex, "").replace("CO2e", "")

        // Use the replace function with the regex pattern to remove unwanted characters
        return result
    }


    companion object {
        const val TAG = "TraceableAdapter"
        val categories =
            listOf("Groceries", "Consumer Products", "Electronics", "Transport", "Misc")
        val propertyNames = listOf("objectName", "material", "amount", "occurrence", "co2e")
    }


}
