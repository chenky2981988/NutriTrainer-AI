package `in`.acstechnologies.nutritrainerai.data.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform boundary for opening the local SQLite database — the "local source
 * of truth" (PRD §9 "Offline, backup and security"). Android supplies a
 * `Context`; iOS needs nothing. The rest of the data layer only sees [SqlDriver].
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

/** File name of the on-device database. */
const val NUTRI_DB_FILE_NAME: String = "nutritrainer.db"

/** Assemble the generated [NutriDb] over a platform driver. */
fun createNutriDb(factory: DatabaseDriverFactory): NutriDb =
    NutriDb(factory.createDriver())
