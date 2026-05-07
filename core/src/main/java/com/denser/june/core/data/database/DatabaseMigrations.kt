package com.denser.june.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `tags` (
                    `tagId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `name` TEXT NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `journal_tag_cross_ref` (
                    `id` INTEGER NOT NULL, 
                    `tagId` INTEGER NOT NULL, 
                    PRIMARY KEY(`id`, `tagId`)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_tag_cross_ref_tagId` ON `journal_tag_cross_ref` (`tagId`)")
            db.execSQL("ALTER TABLE `journals` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `journals_new` (
                    `id` TEXT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `content` TEXT NOT NULL, 
                    `emoji` TEXT, 
                    `images` TEXT NOT NULL, 
                    `location` TEXT, 
                    `songDetails` TEXT, 
                    `tags` TEXT NOT NULL, 
                    `createdAt` INTEGER NOT NULL DEFAULT 0, 
                    `updatedAt` INTEGER, 
                    `dateTime` INTEGER NOT NULL, 
                    `isBookmarked` INTEGER NOT NULL, 
                    `isArchived` INTEGER NOT NULL, 
                    `isDraft` INTEGER NOT NULL, 
                    `deletedAt` INTEGER, 
                    `syncedAt` INTEGER DEFAULT NULL, 
                    `cloudId` TEXT DEFAULT NULL, 
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
            
            db.execSQL("""
                INSERT INTO journals_new (id, title, content, emoji, images, location, songDetails, tags, createdAt, updatedAt, dateTime, isBookmarked, isArchived, isDraft)
                SELECT CAST(id AS TEXT), title, content, emoji, images, location, songDetails, tags, createdAt, updatedAt, dateTime, isBookmarked, isArchived, isDraft FROM journals
            """.trimIndent())
            
            db.execSQL("DROP TABLE journals")
            db.execSQL("ALTER TABLE journals_new RENAME TO journals")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `journal_tag_cross_ref_new` (
                    `id` TEXT NOT NULL, 
                    `tagId` INTEGER NOT NULL, 
                    PRIMARY KEY(`id`, `tagId`)
                )
            """.trimIndent())
            
            db.execSQL("""
                INSERT INTO journal_tag_cross_ref_new (id, tagId)
                SELECT CAST(id AS TEXT), tagId FROM journal_tag_cross_ref
            """.trimIndent())
            
            db.execSQL("DROP TABLE journal_tag_cross_ref")
            db.execSQL("ALTER TABLE journal_tag_cross_ref_new RENAME TO journal_tag_cross_ref")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_tag_cross_ref_tagId` ON `journal_tag_cross_ref` (`tagId`)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `deleted_journal_tombstones` (
                    `id` TEXT NOT NULL, 
                    `deletedAt` INTEGER NOT NULL, 
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `tags_new` (
                    `name` TEXT NOT NULL,
                    PRIMARY KEY(`name`)
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `journal_tag_cross_ref_new` (
                    `id` TEXT NOT NULL,
                    `tagName` TEXT NOT NULL,
                    PRIMARY KEY(`id`, `tagName`)
                )
            """.trimIndent())

            db.execSQL("""
                INSERT OR IGNORE INTO journal_tag_cross_ref_new (id, tagName)
                SELECT ref.id, t.name
                FROM journal_tag_cross_ref ref
                INNER JOIN tags t ON ref.tagId = t.tagId
            """.trimIndent())

            db.execSQL("""
                INSERT OR IGNORE INTO tags_new (name)
                SELECT DISTINCT name FROM tags
            """.trimIndent())

            db.execSQL("DROP TABLE journal_tag_cross_ref")
            db.execSQL("ALTER TABLE journal_tag_cross_ref_new RENAME TO journal_tag_cross_ref")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_tag_cross_ref_tagName` ON `journal_tag_cross_ref` (`tagName`)")

            db.execSQL("DROP TABLE tags")
            db.execSQL("ALTER TABLE tags_new RENAME TO tags")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `journals` ADD COLUMN `aiFormattedContent` TEXT")
            db.execSQL("ALTER TABLE `journals` ADD COLUMN `aiReflection` TEXT")
            db.execSQL("ALTER TABLE `journals` ADD COLUMN `aiColorWeight` TEXT")
            db.execSQL("ALTER TABLE `journals` ADD COLUMN `dominantEmotion` TEXT")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `improvements` (
                    `id` TEXT NOT NULL,
                    `journalId` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `isCompleted` INTEGER NOT NULL,
                    `isArchived` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `micro_wins` (
                    `id` TEXT NOT NULL,
                    `journalId` TEXT NOT NULL,
                    `action` TEXT NOT NULL,
                    `proof` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
        }
    }
}
