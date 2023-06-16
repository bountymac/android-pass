/*
 * Copyright (c) 2023 Proton AG
 * This file is part of Proton AG and Proton Pass.
 *
 * Proton Pass is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Pass is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Pass.  If not, see <https://www.gnu.org/licenses/>.
 */

package proton.android.pass.data.impl.db

import androidx.room.DeleteTable
import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.extension.addTableColumn
import me.proton.core.key.data.db.PublicAddressDatabase
import me.proton.core.keytransparency.data.local.KeyTransparencyDatabase
import me.proton.core.observability.data.db.ObservabilityDatabase
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.usersettings.data.db.OrganizationDatabase
import proton.android.pass.data.impl.db.entities.ItemEntity
import proton.android.pass.data.impl.db.entities.ShareEntity
import proton.android.pass.data.impl.db.entities.ShareKeyEntity
import proton.pass.domain.ITEM_TYPE_LOGIN

@Suppress("ClassNaming")
object AppDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ObservabilityDatabase.MIGRATION_0.migrate(database)
        }
    }

    @DeleteTable.Entries(value = [DeleteTable(tableName = "SelectedShareEntity")])
    class MIGRATION_2_3 : AutoMigrationSpec

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            OrganizationDatabase.MIGRATION_2.migrate(database)
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("UPDATE AccountMetadataEntity SET product = 'Pass'")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AddressDatabase.MIGRATION_4.migrate(database)
            PublicAddressDatabase.MIGRATION_2.migrate(database)
            KeyTransparencyDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                    ALTER TABLE ${ShareEntity.TABLE}
                    ADD COLUMN ${ShareEntity.Columns.IS_ACTIVE} INTEGER NOT NULL DEFAULT 1
                """.trimIndent()
            )
            database.execSQL(
                """
                    ALTER TABLE ${ShareKeyEntity.TABLE}
                    ADD COLUMN ${ShareKeyEntity.Columns.USER_KEY_ID} TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )
            database.execSQL(
                """
                    ALTER TABLE ${ShareKeyEntity.TABLE}
                    ADD COLUMN ${ShareKeyEntity.Columns.IS_ACTIVE} INTEGER NOT NULL DEFAULT 1
                """.trimIndent()
            )
            database.execSQL(
                """
                    UPDATE ${ShareKeyEntity.TABLE}
                    SET ${ShareKeyEntity.Columns.USER_KEY_ID} = (
                        SELECT keyId
                        FROM UserKeyEntity
                        WHERE UserKeyEntity.userId = ${ShareKeyEntity.TABLE}.${ShareKeyEntity.Columns.USER_ID}
                          AND UserKeyEntity.isPrimary = 1
                          LIMIT 1
                    )
                """.trimIndent()
            )
        }
    }

    @RenameTable.Entries(
        value = [RenameTable(fromTableName = "PlanLimitsEntity", toTableName = "PlanEntity"), ]
    )
    class MIGRATION_11_12 : AutoMigrationSpec

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.addTableColumn(
                table = ItemEntity.TABLE,
                column = ItemEntity.Columns.HAS_TOTP,
                type = "INTEGER",
                defaultValue = null,
            )
            // Set all items that are not login to HAS_TOTP = false
            database.execSQL(
                """
                    UPDATE ${ItemEntity.TABLE}
                    SET ${ItemEntity.Columns.HAS_TOTP} = 0
                    WHERE ${ItemEntity.Columns.HAS_TOTP} IS NULL
                      AND ${ItemEntity.Columns.ITEM_TYPE} != $ITEM_TYPE_LOGIN 
                    
                """.trimIndent()
            )
        }
    }
}
