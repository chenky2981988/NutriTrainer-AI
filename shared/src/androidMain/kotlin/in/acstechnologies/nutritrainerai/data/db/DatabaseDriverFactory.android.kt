package `in`.acstechnologies.nutritrainerai.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(NutriDb.Schema, context, NUTRI_DB_FILE_NAME)
}
