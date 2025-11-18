package com.example.myapplication.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * DocumentDatabase - Room database for document management
 * Handles database creation, version management, and migrations
 *
 * Note: This is the OLD database class. The new Room database is in AppDatabase.java
 * This class is kept for backward compatibility with existing DocumentRepository
 */
@Database(
    entities = {
        Document.class,
        Folder.class,
        Tag.class,
        DocumentTag.class
    },
    version = 1,
    exportSchema = false
)
public abstract class DocumentDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "document_scanner_db";
    private static DocumentDatabase instance;

    /**
     * Get DocumentDao
     * @return DocumentDao instance
     */
    public abstract DocumentDao documentDAO();

    /**
     * Get FolderDao
     * @return FolderDao instance
     */
    public abstract FolderDao folderDao();

    /**
     * Get TagDao
     * @return TagDao instance
     */
    public abstract TagDao tagDao();

    /**
     * Get DocumentTagDao
     * @return DocumentTagDao instance
     */
    public abstract DocumentTagDao documentTagDao();

    /**
     * Get database instance (Singleton pattern)
     * @param context Application context
     * @return DocumentDatabase instance
     */
    public static synchronized DocumentDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    DocumentDatabase.class,
                    DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // For development - remove in production
            // .addMigrations(MIGRATION_1_2) // Add migrations as needed
            .build();
        }
        return instance;
    }

    /**
     * Migration from version 1 to 2 (example)
     * Uncomment and modify as needed for future versions
     */
    /*
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Example: Add new column
            database.execSQL("ALTER TABLE documents ADD COLUMN description TEXT");
        }
    };
    */

    /**
     * Migration from version 2 to 3 (example)
     */
    /*
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Example: Create new table or modify existing
            database.execSQL("CREATE TABLE IF NOT EXISTS tags " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL)");
        }
    };
    */

    /**
     * Close database connection
     */
    public static synchronized void closeDatabase() {
        if (instance != null && instance.isOpen()) {
            instance.close();
            instance = null;
        }
    }

    /**
     * Clear all data from database (for testing/reset)
     * Must be called on a background thread
     */
    public void clearAllData() {
        if (instance != null) {
            documentDAO().deleteAllDocuments();
        }
    }
}

