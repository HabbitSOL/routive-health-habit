package com.example.sol.feature.drugdictionary.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sol.feature.drugdictionary.data.*
import com.example.sol.feature.drugdictionary.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DrugLoad { IDLE, LOADING, READY, EMPTY, ERROR }
data class DrugSearchState(val query: String = "", val submitted: String = "",
    val status: DrugLoad = DrugLoad.IDLE, val items: List<DrugSearchItem> = emptyList(),
    val page: Int = 0, val hasNext: Boolean = false, val total: Long = 0,
    val loadingMore: Boolean = false, val moreError: Boolean = false, val inputError: String? = null)

class DrugDictionaryViewModel(private val repository: DrugRepository = MockDrugRepository()) : ViewModel() {
    var state by mutableStateOf(DrugSearchState()); private set
    var recent by mutableStateOf<List<String>>(emptyList()); private set
    var selected by mutableStateOf<DrugDetailResponse?>(null); private set
    var detailStatus by mutableStateOf(DrugLoad.IDLE); private set
    var simulateSearchError by mutableStateOf(false); private set
    var simulateDetailError by mutableStateOf(false); private set
    private var searchJob: Job? = null
    private var detailJob: Job? = null
    private var searchVersion = 0
    private var detailVersion = 0
    private var selectedId: String? = null

    fun edit(query: String) {
        searchJob?.cancel()
        val version = ++searchVersion
        state = DrugSearchState(query = query, status = if (query.isBlank()) DrugLoad.IDLE else DrugLoad.LOADING,
            inputError = if (query.length > 100) "검색어는 100자 이하로 입력해 주세요." else null)
        if (query.isBlank() || query.length > 100) { state = state.copy(status = DrugLoad.IDLE); return }
        searchJob = viewModelScope.launch { delay(350); load(query.trim(), 0, version) }
    }
    fun submit() {
        if (state.query.isBlank() || state.inputError != null) return
        if (state.status == DrugLoad.LOADING && state.submitted == state.query.trim()) return
        searchJob?.cancel()
        val version = ++searchVersion
        searchJob = viewModelScope.launch { load(state.query.trim(), 0, version) }
    }
    fun more() {
        if (!state.hasNext || state.loadingMore || state.status != DrugLoad.READY) return
        searchJob = viewModelScope.launch { load(state.submitted, state.page + 1, searchVersion) }
    }
    private suspend fun load(keyword: String, page: Int, version: Int) {
        if (page == 0) state = state.copy(submitted = keyword, status = DrugLoad.LOADING, items = emptyList(), moreError = false)
        else state = state.copy(loadingMore = true, moreError = false)
        try {
            if (simulateSearchError) { delay(450); error("Demo search failure") }
            val result = repository.search(keyword, page, 3)
            if (version != searchVersion) return
            state = state.copy(status = if (result.items.isEmpty() && page == 0) DrugLoad.EMPTY else DrugLoad.READY,
                items = if (page == 0) result.items else (state.items + result.items).distinctBy { it.drugId },
                page = result.page, hasNext = result.hasNext, total = result.totalElements, loadingMore = false)
            recent = (listOf(keyword) + recent.filterNot { it == keyword }).take(5)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (version == searchVersion) state = if (page == 0) state.copy(status = DrugLoad.ERROR)
                else state.copy(loadingMore = false, moreError = true)
        }
    }
    fun removeRecent(value: String) { recent = recent - value }
    fun clearRecent() { recent = emptyList() }
    fun searchFailure(value: Boolean) { simulateSearchError = value; edit(state.query) }
    fun detailFailure(value: Boolean) { simulateDetailError = value; selectedId?.let(::open) }
    fun open(id: String) {
        detailJob?.cancel()
        selectedId = id
        selected = null
        detailStatus = DrugLoad.LOADING
        val version = ++detailVersion
        detailJob = viewModelScope.launch {
            try {
                if (simulateDetailError) { delay(350); error("Demo detail failure") }
                val result = repository.detail(id)
                if (version == detailVersion) {
                    selected = result
                    detailStatus = if (result == null) DrugLoad.EMPTY else DrugLoad.READY
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (version == detailVersion) detailStatus = DrugLoad.ERROR }
        }
    }
    fun retryDetail() { if (detailStatus != DrugLoad.LOADING) selectedId?.let(::open) }
    fun closeDetail() { detailJob?.cancel(); ++detailVersion; selectedId = null; selected = null; detailStatus = DrugLoad.IDLE }
    fun reset() {
        searchJob?.cancel(); ++searchVersion; closeDetail()
        state = DrugSearchState(); recent = emptyList(); simulateSearchError = false; simulateDetailError = false
    }
}
