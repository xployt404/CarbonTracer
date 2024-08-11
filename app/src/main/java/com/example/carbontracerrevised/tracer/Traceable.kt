package com.example.carbontracerrevised.tracer

const val GROCERIES = 0
const val CONSUMER_PRODUCTS = 1
const val ELECTRONICS = 2
const val TRANSPORT = 3
const val MISC = 4

class Traceable (
    var id : Int,
    var name: String,
    var material: String,
    var amount: String,
    var occurrence: String,
    var category: Int = MISC,
    var co2e : String
    ){
    companion object{
        fun newEmptyTraceable(): Traceable {
            return Traceable(0,"","","","",0,"")
        }
    }
}