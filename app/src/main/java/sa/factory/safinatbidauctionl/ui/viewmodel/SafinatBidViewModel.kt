package sa.factory.safinatbidauctionl.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sa.factory.safinatbidauctionl.domain.usecase.*
import sa.factory.safinatbidauctionl.model.SafinatBidItem
import sa.factory.safinatbidauctionl.model.SafinatBidOrder
import sa.factory.safinatbidauctionl.model.SafinatBidTelemetry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SafinatBidUiState(
    val catalog: List<SafinatBidItem> = emptyList(),
    val filteredCatalog: List<SafinatBidItem> = emptyList(),
    val selectedCategory: String = "الكل",
    val cart: List<SafinatBidItem> = emptyList(),
    val telemetry: SafinatBidTelemetry? = null,
    val lastOrder: SafinatBidOrder? = null,
    val isCheckingOut: Boolean = false,
    val activeTab: String = "home"
)

class SafinatBidViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val manageCartUseCase: ManageCartUseCase,
    private val processCheckoutUseCase: ProcessCheckoutUseCase,
    private val observeHardwareTelemetryUseCase: ObserveHardwareTelemetryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SafinatBidUiState())
    val uiState: StateFlow<SafinatBidUiState> = _uiState.asStateFlow()

    init {
        // Observe Catalog
        getCatalogUseCase().onEach { list ->
            _uiState.update { it.copy(catalog = list, filteredCatalog = list) }
        }.launchIn(viewModelScope)

        // Observe Cart
        manageCartUseCase.cart.onEach { cartItems ->
            _uiState.update { it.copy(cart = cartItems) }
        }.launchIn(viewModelScope)

        // Observe Telemetry
        observeHardwareTelemetryUseCase().onEach { telem ->
            _uiState.update { it.copy(telemetry = telem) }
        }.launchIn(viewModelScope)
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val filtered = if (category == "الكل") state.catalog else state.catalog.filter { it.category == category }
            state.copy(selectedCategory = category, filteredCatalog = filtered)
        }
    }

    fun switchTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun addToCart(item: SafinatBidItem) = manageCartUseCase.add(item)
    fun removeFromCart(item: SafinatBidItem) = manageCartUseCase.remove(item)

    fun checkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true) }
            val order = processCheckoutUseCase()
            _uiState.update { it.copy(isCheckingOut = false, lastOrder = order, activeTab = "checkout") }
        }
    }
}
