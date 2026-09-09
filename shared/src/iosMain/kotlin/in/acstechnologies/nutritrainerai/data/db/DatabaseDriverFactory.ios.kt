package `in`.acstechnologies.nutritrainerai.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(NutriDb.Schema, NUTRI_DB_FILE_NAME)
}
