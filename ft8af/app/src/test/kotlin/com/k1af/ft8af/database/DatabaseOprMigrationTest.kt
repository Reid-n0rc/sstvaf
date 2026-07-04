package com.k1af.ft8af.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * DB v18 -> v19 migration (SSTVAF transformation PR 6): upgrading an existing
 * install must add the `sstv_images` table without touching the legacy tables
 * or their data.
 *
 * A named on-disk database is used (not in-memory) so a second helper instance
 * can reopen the same file at the higher version and trigger onUpgrade.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseOprMigrationTest {

    private lateinit var context: Context
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun tableExists(db: SQLiteDatabase, table: String): Boolean =
        db.rawQuery(
            "select name from sqlite_master where type='table' and name=?",
            arrayOf(table),
        ).use { it.moveToFirst() }

    private fun columnNames(db: SQLiteDatabase, table: String): List<String> =
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            val names = mutableListOf<String>()
            val idx = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) names.add(cursor.getString(idx))
            names
        }

    /**
     * Builds the on-disk pre-migration state: a database whose schema version
     * is 18 and which has NO sstv_images table (real v18 installs predate it;
     * the current onCreate creates it, so it is dropped again here).
     */
    private fun createVersion18Database() {
        val v18 = DatabaseOpr(context, dbName, null, 18)
        v18.db.execSQL("DROP TABLE IF EXISTS sstv_images")
        // Seed data that must survive the upgrade untouched.
        v18.db.execSQL("INSERT INTO config (KeyName, Value) VALUES ('callsign', 'KS3CKC')")
        v18.db.execSQL(
            "INSERT INTO QSLTable (\"call\", mode, band) VALUES ('K1AF', 'SSTV', '20m')",
        )
        v18.close()
    }

    @Test
    fun upgrade18to19_createsSstvImagesTable() {
        createVersion18Database()

        val v19 = DatabaseOpr(context, dbName, null, 19)
        try {
            assertThat(tableExists(v19.db, "sstv_images")).isTrue()
            assertThat(columnNames(v19.db, "sstv_images")).containsExactly(
                "id", "fileName", "direction", "mode", "freqHz", "utcMillis",
                "width", "height", "complete", "quality", "notes",
            )
        } finally {
            v19.close()
        }
    }

    @Test
    fun upgrade18to19_keepsLegacyTablesAndData() {
        createVersion18Database()

        val v19 = DatabaseOpr(context, dbName, null, 19)
        try {
            for (table in listOf(
                "config", "followCallsigns", "QSLTable", "QslCallsigns",
                "Messages", "SWLMessages", "SWLQSOTable", "CallsignQTH",
            )) {
                assertThat(tableExists(v19.db, table)).isTrue()
            }
            v19.db.rawQuery(
                "select Value from config where KeyName='callsign'", null,
            ).use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("KS3CKC")
            }
            v19.db.rawQuery(
                "select mode from QSLTable where \"call\"='K1AF'", null,
            ).use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("SSTV")
            }
        } finally {
            v19.close()
        }
    }

    @Test
    fun upgrade18to19_newTableAcceptsInserts() {
        createVersion18Database()

        val v19 = DatabaseOpr(context, dbName, null, 19)
        try {
            v19.db.execSQL(
                "INSERT INTO sstv_images " +
                    "(fileName, direction, mode, freqHz, utcMillis, width, height, complete, quality) " +
                    "VALUES ('a.png', 'RX', 'Scottie 1', 14230000, 1, 320, 256, 1, 0.9)",
            )
            v19.db.rawQuery("select notes from sstv_images", null).use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                // notes defaults to '' per the schema
                assertThat(cursor.getString(0)).isEmpty()
            }
        } finally {
            v19.close()
        }
    }

    @Test
    fun freshInstallAt19_alsoHasSstvImagesTable() {
        val fresh = DatabaseOpr(context, null, null, 19) // in-memory: pure onCreate path
        try {
            assertThat(tableExists(fresh.db, "sstv_images")).isTrue()
        } finally {
            fresh.close()
        }
    }
}
