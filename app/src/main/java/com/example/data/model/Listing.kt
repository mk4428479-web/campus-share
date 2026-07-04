package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "listings")
data class Listing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val price: Double, // 0.0 means Free/Donation
    val category: String, // "Books", "Notes", "Papers", "Manuals", "Calculators", "Other"
    val condition: String, // "New", "Like New", "Good", "Fair"
    val listingType: String, // "Sell", "Donate", "Exchange"
    val sellerName: String,
    val sellerCampus: String,
    val sellerContactCall: String,
    val sellerContactWhatsApp: String,
    val imageResName: String, // Local drawable resource name for premium demo feel
    val isFavorite: Boolean = false,
    val viewsCount: Int = 12,
    val isVerifiedSeller: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
