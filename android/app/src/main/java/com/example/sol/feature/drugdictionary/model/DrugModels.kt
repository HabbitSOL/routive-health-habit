package com.example.sol.feature.drugdictionary.model

// Field names follow the checked-in backend DTOs. No HTTP serialization is wired yet.
data class DrugSearchItem(val drugId: String, val name: String, val manufacturer: String?,
    val dosageForm: String?, val imageUrl: String? = null)
data class DrugPage(val items: List<DrugSearchItem>, val page: Int, val size: Int,
    val totalElements: Long, val totalPages: Int, val hasNext: Boolean)
data class DrugAppearance(val dosageForm: String?, val color: String?, val shape: String?, val scoreLine: String?)
data class DrugIngredient(val name: String, val amount: java.math.BigDecimal?, val unitCode: String?)
data class DrugDetailResponse(val drugId: String, val name: String, val medicationType: String?,
    val manufacturer: String?, val efficacy: String?, val usage: String?, val precautions: String?,
    val sideEffects: String?, val storage: String?, val appearance: DrugAppearance?,
    val ingredients: List<DrugIngredient>, val sourceName: String?, val sourceUpdatedAt: String?,
    val disclaimer: String?, val imageUrl: String?, val interactions: String?)
