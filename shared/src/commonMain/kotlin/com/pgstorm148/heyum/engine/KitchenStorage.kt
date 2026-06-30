package com.pgstorm148.heyum.engine

expect class KitchenStorage() {
    fun saveIngredients(list: List<String>)
    fun getIngredients(): List<String>
    fun saveRecentIngredients(list: List<String>)
    fun getRecentIngredients(): List<String>
}
