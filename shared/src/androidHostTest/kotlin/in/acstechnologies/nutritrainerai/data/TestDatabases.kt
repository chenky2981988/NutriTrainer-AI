package `in`.acstechnologies.nutritrainerai.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import `in`.acstechnologies.nutritrainerai.data.db.NutriDb

/** A throwaway in-memory database with the schema applied — one per test. */
fun inMemoryNutriDb(): NutriDb {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    NutriDb.Schema.create(driver)
    return NutriDb(driver)
}
