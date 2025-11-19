package com.srikanth.docscanner.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * FolderDao - Data Access Object for Folder entity
 */
@Dao
public interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Folder folder);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Folder> folders);

    @Update
    int update(Folder folder);

    @Delete
    int delete(Folder folder);

    @Query("DELETE FROM folders WHERE folder_id = :folderId")
    int deleteById(long folderId);

    @Query("SELECT * FROM folders WHERE folder_id = :folderId")
    Folder getById(long folderId);

    @Query("SELECT * FROM folders WHERE folder_id = :folderId")
    LiveData<Folder> getByIdLive(long folderId);

    @Query("SELECT * FROM folders ORDER BY folder_name ASC")
    List<Folder> getAll();

    @Query("SELECT * FROM folders ORDER BY folder_name ASC")
    LiveData<List<Folder>> getAllLive();

    // Root folders (no parent)
    @Query("SELECT * FROM folders WHERE parent_folder_id IS NULL ORDER BY folder_name ASC")
    List<Folder> getRootFolders();

    @Query("SELECT * FROM folders WHERE parent_folder_id IS NULL ORDER BY folder_name ASC")
    LiveData<List<Folder>> getRootFoldersLive();

    // Subfolders
    @Query("SELECT * FROM folders WHERE parent_folder_id = :parentId ORDER BY folder_name ASC")
    List<Folder> getSubfolders(long parentId);

    @Query("SELECT * FROM folders WHERE parent_folder_id = :parentId ORDER BY folder_name ASC")
    LiveData<List<Folder>> getSubfoldersLive(long parentId);

    // Pinned folders
    @Query("SELECT * FROM folders WHERE is_pinned = 1 ORDER BY folder_name ASC")
    List<Folder> getPinnedFolders();

    @Query("SELECT * FROM folders WHERE is_pinned = 1 ORDER BY folder_name ASC")
    LiveData<List<Folder>> getPinnedFoldersLive();

    // Update document count
    @Query("UPDATE folders SET document_count = :count WHERE folder_id = :folderId")
    int updateDocumentCount(long folderId, int count);

    // Search
    @Query("SELECT * FROM folders WHERE folder_name LIKE '%' || :query || '%' ORDER BY folder_name ASC")
    List<Folder> search(String query);

    @Query("SELECT COUNT(*) FROM folders")
    int getCount();
}


