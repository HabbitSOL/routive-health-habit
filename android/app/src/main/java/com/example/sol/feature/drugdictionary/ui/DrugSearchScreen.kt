package com.example.sol.feature.drugdictionary.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.sol.feature.drugdictionary.viewmodel.*

@Composable
fun DrugSearchScreen(model: DrugDictionaryViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val state = model.state
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    var tools by remember { mutableStateOf(false) }
    LaunchedEffect(state.submitted) { listState.scrollToItem(0) }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { DrugHeader("의약품 사전", onBack) }
        item {
            Surface(color = DrugMint, shape = RoundedCornerShape(28.dp)) {
                Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("KNOW YOUR MEDICINE", color = DrugInk, style = MaterialTheme.typography.labelSmall)
                        Text("궁금한 약 정보를\n차근차근 살펴봐요", color = DrugInk, style = MaterialTheme.typography.titleLarge)
                        Text("이름과 업체명으로 검색해 보세요", color = DrugInk, style = MaterialTheme.typography.bodySmall)
                    }
                    DrugArtwork()
                }
            }
        }
        item {
            OutlinedTextField(value = state.query, onValueChange = model::edit,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), singleLine = true,
                label = { Text("의약품 이름 · 업체명") }, placeholder = { Text("예: 새봄") },
                isError = state.inputError != null,
                supportingText = { Text(state.inputError ?: "가상 예시만 검색됩니다. 실제 의약품 검색은 연동 예정이에요.") },
                trailingIcon = { if (state.query.isNotEmpty()) TextButton(onClick = { model.edit("") }) { Text("지우기") } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); model.submit() }))
        }
        when (state.status) {
            DrugLoad.IDLE -> {
                if (model.recent.isNotEmpty()) item {
                    DrugPanel("최근 검색어") {
                        model.recent.forEach { value ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { model.edit(value) }, modifier = Modifier.weight(1f)) { Text(value) }
                                TextButton(onClick = { model.removeRecent(value) }) { Text("삭제") }
                            }
                        }
                        TextButton(onClick = model::clearRecent) { Text("전체 삭제") }
                        Text("최근 검색어는 이번 데모 실행 중에만 유지돼요.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    DrugPanel("검색을 체험해 보세요") {
                        Text("아래 단어로 예시 목록과 상세 화면을 볼 수 있어요.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("새봄", "하루", "SOL").forEach { query ->
                                SuggestionChip(onClick = { model.edit(query) }, label = { Text(query) })
                            }
                        }
                    }
                }
            }
            DrugLoad.LOADING -> item { DrugLoading("검색 결과를 찾고 있어요") }
            DrugLoad.EMPTY -> item { DrugMessage("검색 결과가 없어요", "이름을 확인하거나 ‘새봄’, ‘하루’로 데모를 체험해 보세요.", "검색어 지우기", { model.edit("") }) }
            DrugLoad.ERROR -> item { DrugMessage("검색 정보를 불러오지 못했어요", "입력한 검색어는 유지했어요. 다시 시도해 주세요.", "다시 시도", model::submit) }
            DrugLoad.READY -> {
                item { Text("검색 결과 ${state.total}개", style = MaterialTheme.typography.titleMedium) }
                items(state.items, key = { it.drugId }) { drug ->
                    Card(onClick = { keyboard?.hide(); onOpen(drug.drugId) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(24.dp)) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            DrugArtwork(Modifier.size(52.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(drug.dosageForm ?: "제형 정보 없음", color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium)
                                Text(drug.name, style = MaterialTheme.typography.titleMedium)
                                Text(drug.manufacturer ?: "업체 정보 없음", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("›", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
                if (state.hasNext) item {
                    if (state.moreError) Text("추가 목록을 불러오지 못했어요. 기존 결과는 계속 볼 수 있어요.")
                    OutlinedButton(onClick = model::more, enabled = !state.loadingMore, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.loadingMore) "추가 목록 불러오는 중…" else if (state.moreError) "추가 목록 다시 시도" else "더 보기")
                    }
                }
            }
        }
        item {
            Text("의약품 정보는 진단이나 처방을 대신하지 않습니다.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { tools = !tools }) { Text(if (tools) "데모 도구 닫기" else "데모 상태 체험") }
            if (tools) Row(verticalAlignment = Alignment.CenterVertically) {
                Text("검색 오류 재현", modifier = Modifier.weight(1f))
                Switch(checked = model.simulateSearchError, onCheckedChange = model::searchFailure)
            }
            if (tools) Text("오류 스위치를 끄면 정상 검색으로 돌아갑니다. 없는 단어를 입력하면 빈 결과를 볼 수 있어요.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
