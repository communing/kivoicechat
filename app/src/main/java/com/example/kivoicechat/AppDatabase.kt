package com.example.kivoicechat
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

// Version auf 2 erhöht, da sich die Struktur geändert hat
@Database(entities = [ChatMessage::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context, dbPassword: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "encrypted_chat.db")
                    .openHelperFactory(SupportFactory(dbPassword))
                    .fallbackToDestructiveMigration() // NEU: Baut die DB neu auf, falls sich die Version ändert (verhindert Abstürze)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
