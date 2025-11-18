package com.example.myapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DocumentTagDao - Data Access Object for DocumentTag junction table
 */
@Dao
public interface DocumentTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DocumentTag documentTag);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<DocumentTag> documentTags);

    @Delete
    int delete(DocumentTag documentTag);

    @Query("DELETE FROM document_tags WHERE document_id = :documentId AND tag_id = :tagId")
    int deleteByIds(long documentId, long tagId);

    @Query("DELETE FROM document_tags WHERE document_id = :documentId")
    int deleteByDocument(long documentId);

    @Query("DELETE FROM document_tags WHERE tag_id = :tagId")
    int deleteByTag(long tagId);

    @Query("SELECT * FROM document_tags WHERE document_id = :documentId")
    List<DocumentTag> getByDocument(long documentId);

    @Query("SELECT * FROM document_tags WHERE tag_id = :tagId")
    List<DocumentTag> getByTag(long tagId);

    @Query("SELECT * FROM document_tags WHERE document_id = :documentId")
    LiveData<List<DocumentTag>> getByDocumentLive(long documentId);

    @Query("SELECT COUNT(*) FROM document_tags WHERE document_id = :documentId")
    int getTagCountForDocument(long documentId);

    @Query("SELECT COUNT(*) FROM document_tags WHERE tag_id = :tagId")
    int getDocumentCountForTag(long tagId);

    @Query("SELECT EXISTS(SELECT 1 FROM document_tags WHERE document_id = :documentId AND tag_id = :tagId)")
    boolean hasTag(long documentId, long tagId);
}

