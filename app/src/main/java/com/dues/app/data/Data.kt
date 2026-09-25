package com.dues.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

enum class CycleUnit { DAY, WEEK, MONTH, YEAR }

object TagKind {
    const val LIST = "list"
    const val CATEGORY = "category"
    const val PAYMENT = "payment"
}

@Entity(tableName = "subs")
data class Sub(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Base price; later changes live in [PriceChange]. */
    val price: Double,
    /** Website, used for the link and the favicon logo. */
    val domain: String = "",
    /** Non-empty overrides the favicon logo. */
    val emoji: String = "",
    /** First (or trial-end) payment date; later payments step from here. */
    val startDate: LocalDate,
    val cycleUnit: CycleUnit = CycleUnit.MONTH,
    val cycleCount: Int = 1,
    val freeTrial: Boolean = false,
    val listName: String = DEFAULT_LIST,
    val category: String = DEFAULT_CATEGORY,
    val paymentMethod: String = "",
    /** -1 = no reminder. */
    val notifyDaysBefore: Int = 1,
    val notes: String = "",
    val cancelledOn: LocalDate? = null,
)

@Entity(tableName = "price_changes")
data class PriceChange(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subId: Long,
    val date: LocalDate,
    val price: Double,
)

@Entity(tableName = "tags", primaryKeys = ["kind", "name"])
data class Tag(val kind: String, val name: String)

const val DEFAULT_LIST = "Personal"
const val DEFAULT_CATEGORY = "Other"
val DEFAULT_CATEGORIES = listOf(
    "Streaming", "Music", "AI", "Productivity", "Cloud storage", "Gaming", "News",
    "Fitness", "Education", "Shopping", "Utilities", "Housing", DEFAULT_CATEGORY,
)
private val DEFAULT_TAGS = mapOf(
    TagKind.LIST to listOf(DEFAULT_LIST),
    TagKind.CATEGORY to DEFAULT_CATEGORIES,
    TagKind.PAYMENT to listOf("Credit card", "Debit card", "Google Play", "PayPal"),
)

fun Sub.tag(kind: String) = when (kind) {
    TagKind.LIST -> listName
    TagKind.CATEGORY -> category
    else -> paymentMethod
}

fun Sub.withTag(kind: String, name: String) = when (kind) {
    TagKind.LIST -> copy(listName = name)
    TagKind.CATEGORY -> copy(category = name)
    else -> copy(paymentMethod = name)
}

class Converters {
    @TypeConverter fun toDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)
    @TypeConverter fun fromDate(d: LocalDate?): Long? = d?.toEpochDay()
}

@Dao
interface SubDao {
    @Query("SELECT * FROM subs ORDER BY name COLLATE NOCASE") fun subs(): Flow<List<Sub>>
    @Query("SELECT * FROM price_changes ORDER BY date") fun changes(): Flow<List<PriceChange>>
    @Query("SELECT * FROM tags ORDER BY rowid") fun tags(): Flow<List<Tag>>

    @Query("SELECT * FROM subs") suspend fun subsOnce(): List<Sub>
    @Query("SELECT * FROM price_changes") suspend fun changesOnce(): List<PriceChange>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(s: Sub): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAll(s: List<Sub>)
    @Delete suspend fun delete(s: Sub)
    @Query("DELETE FROM price_changes WHERE subId = :subId") suspend fun deleteChangesOf(subId: Long)

    @Insert suspend fun insert(p: PriceChange)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveChanges(p: List<PriceChange>)
    @Delete suspend fun delete(p: PriceChange)

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(t: Tag)
    @Delete suspend fun delete(t: Tag)
    @Query("UPDATE tags SET name = :newName WHERE kind = :kind AND name = :old")
    suspend fun renameTag(kind: String, old: String, newName: String)

    @Query("DELETE FROM subs") suspend fun deleteAllSubs()
    @Query("DELETE FROM price_changes") suspend fun deleteAllChanges()
}

@Database(entities = [Sub::class, PriceChange::class, Tag::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): SubDao

    companion object {
        @Volatile private var instance: AppDb? = null

        fun get(ctx: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "dues.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        DEFAULT_TAGS.forEach { (kind, names) ->
                            names.forEach { db.execSQL("INSERT INTO tags(kind, name) VALUES(?, ?)", arrayOf(kind, it)) }
                        }
                    }
                })
                .build().also { instance = it }
        }
    }
}
