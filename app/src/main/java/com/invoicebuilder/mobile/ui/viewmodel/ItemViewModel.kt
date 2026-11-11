package com.invoicebuilder.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invoicebuilder.core.database.repository.ItemRepository
import com.invoicebuilder.core.model.Item
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ItemUiState(
    val items: List<Item> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class ItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<ItemUiState> = combine(
        itemRepository.observeItems(),
        _searchQuery
    ) { items, query ->
        val filtered = if (query.isBlank()) {
            items
        } else {
            items.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.description?.contains(query, ignoreCase = true) == true
            }
        }
        ItemUiState(items = filtered, searchQuery = query, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ItemUiState(isLoading = true))

    init {
        viewModelScope.launch {
            itemRepository.dedupeItemsByName()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveItem(item: Item, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            itemRepository.saveItem(item)
            onSuccess()
        }
    }

    fun archiveItem(id: Long) {
        viewModelScope.launch {
            itemRepository.archiveItem(id, true)
        }
    }
}
