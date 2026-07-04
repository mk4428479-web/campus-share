package com.example.data.repository

import com.example.data.local.ListingDao
import com.example.data.local.ChatMessageDao
import com.example.data.model.Listing
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MarketplaceRepository(
    private val listingDao: ListingDao,
    private val chatMessageDao: ChatMessageDao
) {
    val allListings: Flow<List<Listing>> = listingDao.getAllListings()
    val favoriteListings: Flow<List<Listing>> = listingDao.getFavoriteListings()
    val allMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages()

    fun getListingById(id: Int): Flow<Listing?> = listingDao.getListingById(id)
    fun getMessagesForListing(listingId: Int): Flow<List<ChatMessage>> = chatMessageDao.getMessagesForListing(listingId)

    suspend fun insertListing(listing: Listing): Long = listingDao.insertListing(listing)
    suspend fun updateListing(listing: Listing) = listingDao.updateListing(listing)
    suspend fun toggleFavorite(id: Int, isFavorite: Boolean) = listingDao.toggleFavorite(id, isFavorite)
    suspend fun deleteListing(listing: Listing) = listingDao.deleteListing(listing)

    suspend fun insertMessage(message: ChatMessage): Long = chatMessageDao.insertMessage(message)
    suspend fun deleteMessagesForListing(listingId: Int) = chatMessageDao.deleteMessagesForListing(listingId)

    suspend fun prepopulateIfEmpty() {
        val count = listingDao.getListingsCount()
        if (count == 0) {
            val sampleListings = listOf(
                Listing(
                    title = "Stewart Calculus - 9th Edition",
                    description = "Used for Math 1A/1B. In excellent condition with no highlighting or scribbles. Includes online webassign access code unused. Perfect for engineering and physics majors.",
                    price = 45.0,
                    category = "Books",
                    condition = "Like New",
                    listingType = "Sell",
                    sellerName = "Alex Rivera",
                    sellerCampus = "UC Berkeley",
                    sellerContactCall = "+15105550143",
                    sellerContactWhatsApp = "15105550143",
                    imageResName = "img_hero_banner", // Premium hero banner
                    isFavorite = false,
                    viewsCount = 42,
                    isVerifiedSeller = true
                ),
                Listing(
                    title = "Organic Chemistry I - Master Notes",
                    description = "Comprehensive, handwritten and scanned color notes for CHEM 101. Covers stereochemistry, substitution/elimination, and synthesis pathways. Extremely neat, guaranteed to help you pass!",
                    price = 15.0,
                    category = "Notes",
                    condition = "New",
                    listingType = "Sell",
                    sellerName = "Maya Lin",
                    sellerCampus = "Stanford University",
                    sellerContactCall = "+16505550188",
                    sellerContactWhatsApp = "16505550188",
                    imageResName = "img_app_icon", // App logo
                    isFavorite = true,
                    viewsCount = 118,
                    isVerifiedSeller = true
                ),
                Listing(
                    title = "TI-84 Plus CE Graphing Calculator",
                    description = "Sleek Mint Green edition TI-84. Used for two semesters in statistics. Screen has zero scratches, comes with original rechargeable battery, charging cable, and sliding case.",
                    price = 70.0,
                    category = "Calculators",
                    condition = "Like New",
                    listingType = "Sell",
                    sellerName = "Sarah Jenkins",
                    sellerCampus = "UCLA",
                    sellerContactCall = "+13105550192",
                    sellerContactWhatsApp = "3105550192",
                    imageResName = "img_hero_banner",
                    isFavorite = false,
                    viewsCount = 89,
                    isVerifiedSeller = false
                ),
                Listing(
                    title = "Intro to Algorithms (CLRS) - 4th Ed",
                    description = "The bible of Computer Science. Hardcover, absolutely flawless. Helped me survive CS 170. Giving away because I am graduating this semester.",
                    price = 55.0,
                    category = "Books",
                    condition = "Like New",
                    listingType = "Sell",
                    sellerName = "James Chen",
                    sellerCampus = "MIT",
                    sellerContactCall = "+16175550155",
                    sellerContactWhatsApp = "6175550155",
                    imageResName = "img_hero_banner",
                    isFavorite = false,
                    viewsCount = 156,
                    isVerifiedSeller = true
                ),
                Listing(
                    title = "General Physics Lab Manual & Kit",
                    description = "Giving away for FREE to any student in need. Includes lab instructions, worksheets, and unused carbon copy sheets. Perfect for PHYS 2A lab course.",
                    price = 0.0,
                    category = "Manuals",
                    condition = "Good",
                    listingType = "Donate",
                    sellerName = "David Kim",
                    sellerCampus = "Harvard University",
                    sellerContactCall = "+16175550112",
                    sellerContactWhatsApp = "6175550112",
                    imageResName = "img_app_icon",
                    isFavorite = false,
                    viewsCount = 37,
                    isVerifiedSeller = false
                ),
                Listing(
                    title = "CS101 Python Cheat Sheets & Exam Papers",
                    description = "Looking to exchange my curated CS101 exam review guides and previous solved midterm papers for a mechanical pencil or notebook. Really useful summaries for freshman coding.",
                    price = 0.0,
                    category = "Papers",
                    condition = "Good",
                    listingType = "Exchange",
                    sellerName = "Chloe Vance",
                    sellerCampus = "NYU",
                    sellerContactCall = "+12125550199",
                    sellerContactWhatsApp = "2125550199",
                    imageResName = "img_app_icon",
                    isFavorite = false,
                    viewsCount = 64,
                    isVerifiedSeller = true
                )
            )

            for (listing in sampleListings) {
                val listingId = listingDao.insertListing(listing)
                // Add some initial messages to populate chat
                if (listing.title.contains("Calculus")) {
                    chatMessageDao.insertMessage(
                        ChatMessage(
                            listingId = listingId.toInt(),
                            senderName = "Alex Rivera",
                            messageText = "Hi! Is the Calculus book still available?",
                            timestamp = System.currentTimeMillis() - 3600000 * 2,
                            isFromMe = true
                        )
                    )
                    chatMessageDao.insertMessage(
                        ChatMessage(
                            listingId = listingId.toInt(),
                            senderName = "Alex Rivera",
                            messageText = "Yes, it is! I can meet you at the campus library today.",
                            timestamp = System.currentTimeMillis() - 3600000,
                            isFromMe = false
                        )
                    )
                }
            }
        }
    }
}
