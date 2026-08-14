package co.ratmo.anreal.core.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AnrealDatabase> {
    val dbFile = context.applicationContext.getDatabasePath("anreal.db")
    return Room.databaseBuilder<AnrealDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
}
