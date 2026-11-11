package com.invoicebuilder.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invoicebuilder.core.database.repository.ClientRepository
import com.invoicebuilder.core.model.Client
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientUiState(
    val clients: List<Client> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ClientViewModel @Inject constructor(
    private val clientRepository: ClientRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<ClientUiState> = combine(
        clientRepository.observeClients(),
        _searchQuery
    ) { clients, query ->
        val filtered = if (query.isBlank()) {
            clients
        } else {
            clients.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.email?.contains(query, ignoreCase = true) == true ||
                it.phone?.contains(query, ignoreCase = true) == true ||
                it.code?.contains(query, ignoreCase = true) == true
            }
        }
        ClientUiState(clients = filtered, searchQuery = query, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClientUiState(isLoading = true))

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveClient(client: Client, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            clientRepository.saveClient(client)
            onSuccess()
        }
    }

    fun archiveClient(id: Long) {
        viewModelScope.launch {
            clientRepository.archiveClient(id, true)
        }
    }
}
