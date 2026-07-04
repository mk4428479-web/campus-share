package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Listing
import com.example.data.model.ChatMessage
import com.example.data.repository.MarketplaceRepository
import com.example.data.api.GeminiService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val repository: MarketplaceRepository
) : AndroidViewModel(application) {

    // Theme state
    private val _isDarkMode = MutableStateFlow(true) // Premium dark theme by default
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // Listings State
    val allListings: StateFlow<List<Listing>> = repository.allListings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteListings: StateFlow<List<Listing>> = repository.favoriteListings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedListingType = MutableStateFlow("All") // "All", "Sell", "Donate", "Exchange"
    val selectedListingType: StateFlow<String> = _selectedListingType.asStateFlow()

    private val _selectedCampus = MutableStateFlow("All") // "All", "Stanford University", "UC Berkeley", "UCLA", etc.
    val selectedCampus: StateFlow<String> = _selectedCampus.asStateFlow()

    // Filtered listings
    val filteredListings: StateFlow<List<Listing>> = combine(
        allListings,
        searchQuery,
        selectedCategory,
        selectedListingType,
        selectedCampus
    ) { listings, query, category, type, campus ->
        listings.filter { listing ->
            val matchesQuery = query.isEmpty() || 
                    listing.title.contains(query, ignoreCase = true) ||
                    listing.description.contains(query, ignoreCase = true) ||
                    listing.sellerCampus.contains(query, ignoreCase = true)
            
            val matchesCategory = category == "All" || listing.category.equals(category, ignoreCase = true)
            val matchesType = type == "All" || listing.listingType.equals(type, ignoreCase = true)
            val matchesCampus = campus == "All" || listing.sellerCampus.equals(campus, ignoreCase = true)

            matchesQuery && matchesCategory && matchesType && matchesCampus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI recommendation state
    private val _aiRecommendation = MutableStateFlow("")
    val aiRecommendation: StateFlow<String> = _aiRecommendation.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    // Active listing for Detail Screen
    private val _selectedListing = MutableStateFlow<Listing?>(null)
    val selectedListing: StateFlow<Listing?> = _selectedListing.asStateFlow()

    // Active chat screen listing & messages
    private val _activeChatListing = MutableStateFlow<Listing?>(null)
    val activeChatListing: StateFlow<Listing?> = _activeChatListing.asStateFlow()

    val activeChatMessages: StateFlow<List<ChatMessage>> = _activeChatListing
        .flatMapLatest { listing ->
            if (listing != null) {
                repository.getMessagesForListing(listing.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Profile State (Personalized based on User Email: mk4428479@gmail.com)
    val userEmail = "mk4428479@gmail.com"
    val userName = "Muhammed"
    val userCampus = "Stanford University"
    val userBio = "Computer Science Senior. Passionate about software systems, mobile design, and sustainable study habits. Selling my reference books & pristine handwritten notes from semesters 1-6."
    val userAvatarUrl = "" // Can be styled in the UI
    val userRating = 4.9f
    val userListingsCount = allListings.map { list -> list.count { it.sellerName == userName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Pre-populate database and prepare state
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectListingType(type: String) {
        _selectedListingType.value = type
    }

    fun selectCampus(campus: String) {
        _selectedCampus.value = campus
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectListing(listing: Listing?) {
        _selectedListing.value = listing
    }

    fun selectChatListing(listing: Listing?) {
        _activeChatListing.value = listing
    }

    fun toggleFavoriteListing(id: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(id, isFavorite)
            // Update selected listing if it is currently displayed
            val currentSelected = _selectedListing.value
            if (currentSelected != null && currentSelected.id == id) {
                _selectedListing.value = currentSelected.copy(isFavorite = isFavorite)
            }
        }
    }

    fun addNewListing(
        title: String,
        description: String,
        price: Double,
        category: String,
        condition: String,
        listingType: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val newListing = Listing(
                title = title,
                description = description,
                price = price,
                category = category,
                condition = condition,
                listingType = listingType,
                sellerName = userName,
                sellerCampus = userCampus,
                sellerContactCall = "+15550199",
                sellerContactWhatsApp = "15550199",
                imageResName = "img_hero_banner", // Default beautiful picture
                isFavorite = false,
                viewsCount = 1,
                isVerifiedSeller = true
            )
            repository.insertListing(newListing)
            onSuccess()
        }
    }

    fun sendChatMessage(messageText: String) {
        val listing = _activeChatListing.value ?: return
        if (messageText.isBlank()) return

        viewModelScope.launch {
            val userMessage = ChatMessage(
                listingId = listing.id,
                senderName = userName,
                messageText = messageText,
                isFromMe = true
            )
            repository.insertMessage(userMessage)

            // Simulate quick AI seller response for premium interactive feels!
            simulateSellerResponse(listing, messageText)
        }
    }

    private fun simulateSellerResponse(listing: Listing, userMessage: String) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500) // Realistic delay

            val replyText = when {
                userMessage.contains("available", ignoreCase = true) || userMessage.contains("still", ignoreCase = true) -> {
                    "Hey! Yes, the ${listing.title} is still available. I can meet on campus tomorrow afternoon if that works?"
                }
                userMessage.contains("price", ignoreCase = true) || userMessage.contains("discount", ignoreCase = true) || userMessage.contains("cheap", ignoreCase = true) -> {
                    if (listing.price == 0.0) {
                        "Since it's listed for free, you can just have it! No price needed."
                    } else {
                        "I can do $${String.format("%.2f", listing.price * 0.9)} if you can pick it up today at the library!"
                    }
                }
                userMessage.contains("condition", ignoreCase = true) || userMessage.contains("pages", ignoreCase = true) -> {
                    "It's in ${listing.condition.lowercase()} condition. Pages are completely clean, no tears or highlighting."
                }
                else -> {
                    "Thanks for the message! I'm in class right now but I can meet you near the Student Union building at 4:00 PM today. Let me know!"
                }
            }

            val sellerMessage = ChatMessage(
                listingId = listing.id,
                senderName = listing.sellerName,
                messageText = replyText,
                isFromMe = false
            )
            repository.insertMessage(sellerMessage)
        }
    }

    fun requestAiRecommendation(interest: String) {
        if (interest.isBlank()) return
        viewModelScope.launch {
            _aiLoading.value = true
            _aiRecommendation.value = ""
            
            val listingsList = allListings.value.map { it.title }
            val recommendation = GeminiService.getBookRecommendations(interest, listingsList)
            
            _aiRecommendation.value = recommendation
            _aiLoading.value = false
        }
    }

    fun clearAiRecommendation() {
        _aiRecommendation.value = ""
        _aiLoading.value = false
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: MarketplaceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
