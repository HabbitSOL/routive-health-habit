package com.example.sol.feature.drugdictionary.data

import com.example.sol.feature.drugdictionary.model.*
import kotlinx.coroutines.delay

interface DrugRepository {
    suspend fun search(keyword: String, page: Int, size: Int): DrugPage
    suspend fun detail(drugId: String): DrugDetailResponse?
}

/** Fictional fixtures only: never provides clinical instructions or medication decisions. */
class MockDrugRepository : DrugRepository {
    private val samples = listOf("새봄정", "새봄캡슐", "새봄시럽", "하루정", "하루캡슐", "맑은정")
        .mapIndexed { index, name ->
            DrugDetailResponse(
                drugId = "00000000-0000-4000-8000-${(index + 1).toString().padStart(12, '0')}",
                name = "$name · 가상 예시", medicationType = null,
                manufacturer = "SOL 예시제약 (가상)", efficacy = null, usage = null,
                precautions = "화면 확인용 가상 항목입니다. 실제 의약품을 검색하거나 복용하는 데 사용하지 마세요.",
                sideEffects = null, storage = null,
                appearance = DrugAppearance(listOf("정제", "캡슐", "시럽")[index % 3], null, null, null),
                ingredients = emptyList(), sourceName = "SOL 화면 시연용 Mock 데이터",
                sourceUpdatedAt = null, disclaimer = "이 정보는 진단이나 처방을 대신하지 않습니다.",
                imageUrl = null, interactions = null)
        }

    override suspend fun search(keyword: String, page: Int, size: Int): DrugPage {
        delay(450)
        val matches = samples.filter { it.name.contains(keyword, true) || it.manufacturer.orEmpty().contains(keyword, true) }
        return DrugPage(matches.drop(page * size).take(size).map {
            DrugSearchItem(it.drugId, it.name, it.manufacturer, it.appearance?.dosageForm, it.imageUrl)
        }, page, size, matches.size.toLong(), (matches.size + size - 1) / size, (page + 1) * size < matches.size)
    }

    override suspend fun detail(drugId: String): DrugDetailResponse? {
        delay(350)
        return samples.find { it.drugId == drugId }
    }
}
