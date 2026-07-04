package com.example.data.local

import androidx.room.*
import com.example.data.model.Listing
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {
    @Query("SELECT * FROM listings ORDER BY timestamp DESC")
    fun getAllListings(): Flow<List<Listing>>

    @Query("SELECT * FROM listings WHERE id = :id")
    fun getListingById(id: Int): Flow<Listing?>

    @Query("SELECT * FROM listings WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteListings(): Flow<List<Listing>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: Listing): Long

    @Update
    suspend fun updateListing(listing: Listing)

    @Query("UPDATE listings SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Int, isFavorite: Boolean)

    @Delete
    suspend fun deleteListing(listing: Listing)

    @Query("SELECT COUNT(*) FROM listings")
    suspend fun getListingsCount(): Int
}
