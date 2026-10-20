package com.example.sol.feature.drugdictionary.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sol.core.ui.SolButton
import com.example.sol.feature.drugdictionary.viewmodel.*

@Composable
fun DrugDetailScreen(model: DrugDictionaryViewModel, onBack: () -> Unit) {
    var registrationNotice by remember { mutableStateOf(false) }
    var tools by remember { mutableStateOf(false) }
    val drug = model.selected
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { DrugHeader("의약품 상세", onBack) }
        when (model.detailStatus) {
            DrugLoad.LOADING, DrugLoad.IDLE -> item { DrugLoading("상세 정보를 불러오고 있어요") }
            DrugLoad.ERROR -> item { DrugMessage("상세 정보를 불러오지 못했어요", "목록으로 돌아가거나 다시 시도해 주세요.", "다시 시도", model::retryDetail) }
            DrugLoad.EMPTY -> item { DrugMessage("찾는 항목이 없어요", "목록으로 돌아가 다른 항목을 선택해 주세요.", "검색 목록으로", onBack) }
            DrugLoad.READY -> if (drug != null) {
                item {
                    Surface(color = DrugMint, shape = RoundedCornerShape(28.dp)) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DrugArtwork()
                            Text(drug.name, color = DrugInk, style = MaterialTheme.typography.headlineSmall)
                            Text(drug.manufacturer ?: "업체 정보 없음", color = DrugInk)
                            Text("실제 제품 사진이 아닌 장식 그림입니다.", color = DrugInk, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item { DrugMessage("화면 체험용 가상 정보", "실제 의약품의 효능·용법은 제공하지 않습니다. ${drug.disclaimer.orEmpty()}") }
                item {
                    DrugPanel("기본 정보") {
                        Text("제형  ·  ${drug.appearance?.dosageForm ?: "정보 없음"}")
                        Text("모양  ·  ${drug.appearance?.shape ?: "정보 없음"}")
                        Text("색상  ·  ${drug.appearance?.color ?: "정보 없음"}")
                        Text("분할선  ·  ${drug.appearance?.scoreLine ?: "정보 없음"}")
                        Text("분류  ·  ${drug.medicationType ?: "정보 없음"}")
                    }
                }
                item { DrugPanel("성분") {
                    if (drug.ingredients.isEmpty()) Text("정보 없음")
                    else drug.ingredients.forEach { Text("${it.name} · ${it.amount ?: "함량 정보 없음"} ${it.unitCode.orEmpty()}") }
                } }
                listOf("효능" to drug.efficacy, "사용법" to drug.usage, "주의사항" to drug.precautions,
                    "부작용" to drug.sideEffects, "상호작용" to drug.interactions, "보관법" to drug.storage).forEach { (title, value) ->
                    item { DrugPanel(title) { Text(value?.takeIf { it.isNotBlank() } ?: "정보 없음", style = MaterialTheme.typography.bodyLarge) } }
                }
                item { DrugPanel("정보 출처") {
                    Text(drug.sourceName ?: "정보 없음")
                    Text("갱신일 · ${drug.sourceUpdatedAt ?: "정보 없음"}")
                    Text("공공 의약품 데이터 연결은 마지막 연동 단계에서 진행합니다.", style = MaterialTheme.typography.bodySmall)
                } }
                item {
                    SolButton("개인 복약으로 등록 · 준비 중", { registrationNotice = true })
                    Text("복약 등록은 후속 개발 단계에서 연결돼요.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        item {
            TextButton(onClick = { tools = !tools }) { Text(if (tools) "데모 도구 닫기" else "상세 오류 체험") }
            if (tools) Row(verticalAlignment = Alignment.CenterVertically) {
                Text("상세 오류 재현", modifier = Modifier.weight(1f))
                Switch(checked = model.simulateDetailError, onCheckedChange = model::detailFailure)
            }
        }
    }
    if (registrationNotice) AlertDialog(onDismissRequest = { registrationNotice = false },
        title = { Text("복약 등록은 준비 중이에요") },
        text = { Text("이번 2주차는 검색과 상세 화면을 체험하는 단계입니다. 선택한 항목은 저장되지 않았고, 복약 일정도 생성되지 않았어요.") },
        confirmButton = { TextButton(onClick = { registrationNotice = false }) { Text("확인") } })
}
