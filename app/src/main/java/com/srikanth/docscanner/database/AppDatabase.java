package com.srikanth.docscanner.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * AppDatabase - Main Room database for document management
 *
 * Features:
 * - All entities registered
 * - Version management
 * - Migration strategies
 * - Singleton pattern
 */
@Database(
    entities = {
        Document.class,
        Folder.class,
        Tag.class,
        DocumentTag.class
    },
    version = 1,
    exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "document_scanner_db";
    private static volatile AppDatabase INSTANCE;

    // DAOs
    public abstract DocumentDao documentDao();
    public abstract FolderDao folderDao();
    public abstract TagDao tagDao();
    public abstract DocumentTagDao documentTagDao();

    /**
     * Get database instance (Singleton)
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DATABASE_NAME
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(DATABASE_CALLBACK)
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Migration from version 1 to 2
     * Example: Add new column to documents table
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add new column example
            database.execSQL(
                "ALTER TABLE documents ADD COLUMN is_protected INTEGER NOT NULL DEFAULT 0"
            );

            // Add index example
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_documents_is_protected ON documents(is_protected)"
            );
        }
    };

    /**
     * Migration from version 2 to 3
     * Example: Add new table for shared documents
     */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create new table example
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS shared_documents (" +
                "share_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "document_id INTEGER NOT NULL, " +
                "shared_with TEXT NOT NULL, " +
                "shared_at INTEGER NOT NULL, " +
                "permission TEXT NOT NULL, " +
                "FOREIGN KEY(document_id) REFERENCES documents(document_id) " +
                "ON DELETE CASCADE ON UPDATE CASCADE)"
            );

            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_shared_documents_document_id " +
                "ON shared_documents(document_id)"
            );
        }
    };

    /**
     * Database callback for initialization
     */
    private static final RoomDatabase.Callback DATABASE_CALLBACK = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            // Create default folders
            db.execSQL(
                "INSERT INTO folders (folder_name, parent_folder_id, folder_path, color, created_at, modified_at) " +
                "VALUES ('Personal', NULL, '/Personal', '#2196F3', " + System.currentTimeMillis() + ", " + System.currentTimeMillis() + ")"
            );

            db.execSQL(
                "INSERT INTO folders (folder_name, parent_folder_id, folder_path, color, created_at, modified_at) " +
                "VALUES ('Work', NULL, '/Work', '#4CAF50', " + System.currentTimeMillis() + ", " + System.currentTimeMillis() + ")"
            );

            db.execSQL(
                "INSERT INTO folders (folder_name, parent_folder_id, folder_path, color, created_at, modified_at) " +
                "VALUES ('Archive', NULL, '/Archive', '#FF9800', " + System.currentTimeMillis() + ", " + System.currentTimeMillis() + ")"
            );

            // Create system tags
            db.execSQL(
                "INSERT INTO tags (tag_name, tag_color, is_system_tag, created_at, modified_at) " +
                "VALUES ('Important', '#F44336', 1, " + System.currentTimeMillis() + ", " + System.currentTimeMillis() + ")"
            );

            db.execSQL(
                "INSERT INTO tags (tag_name, tag_color, is_system_tag, created_at, modified_at) " +
                "VALUES ('Urgent', '#FF5722', 1, " + System.currentTimeMillis() + ", " + System.currentTimeMillis() + ")"
            );

            db.execSQL(
                "INSERT INTO tags (tag_name, tag_color, is_system_tag, created_at, modified_at) " +
                "VALUES ('Personal', '#2196F3', 1, " + System.currentTimeMillis() + ", " + System.currentTimeMillis() + ")"
            );

            db.execSQL(
                "INSERT INTO tags (tag_name, tag_color, is_system_tag, created_at, modified_at) " +
                "VALUES ('Work', '#4CAF50', 1, " + System.currentTimeMillis() + ", " + System.currentTimeMillis() + ")"
            );
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);

            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON");
        }
    };

    /**
     * Close database
     */
    public static void closeDatabase() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }

    /**
     * Destroy instance (for testing)
     */
    public static void destroyInstance() {
        INSTANCE = null;
    }
}


