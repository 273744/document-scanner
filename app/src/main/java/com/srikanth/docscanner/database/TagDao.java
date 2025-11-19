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
 * TagDao - Data Access Object for Tag entity
 */
@Dao
public interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Tag tag);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Tag> tags);

    @Update
    int update(Tag tag);

    @Delete
    int delete(Tag tag);

    @Query("DELETE FROM tags WHERE tag_id = :tagId AND is_system_tag = 0")
    int deleteById(long tagId);

    @Query("SELECT * FROM tags WHERE tag_id = :tagId")
    Tag getById(long tagId);

    @Query("SELECT * FROM tags WHERE tag_id = :tagId")
    LiveData<Tag> getByIdLive(long tagId);

    @Query("SELECT * FROM tags WHERE tag_name = :tagName")
    Tag getByName(String tagName);

    @Query("SELECT * FROM tags ORDER BY tag_name ASC")
    List<Tag> getAll();

    @Query("SELECT * FROM tags ORDER BY tag_name ASC")
    LiveData<List<Tag>> getAllLive();

    @Query("SELECT * FROM tags ORDER BY usage_count DESC LIMIT :limit")
    List<Tag> getPopular(int limit);

    @Query("SELECT * FROM tags WHERE is_system_tag = 1 ORDER BY tag_name ASC")
    List<Tag> getSystemTags();

    @Query("UPDATE tags SET usage_count = usage_count + 1 WHERE tag_id = :tagId")
    int incrementUsageCount(long tagId);

    @Query("UPDATE tags SET usage_count = usage_count - 1 WHERE tag_id = :tagId AND usage_count > 0")
    int decrementUsageCount(long tagId);

    @Query("SELECT COUNT(*) FROM tags")
    int getCount();

    @Query("SELECT * FROM tags WHERE tag_name LIKE '%' || :query || '%' ORDER BY tag_name ASC")
    List<Tag> search(String query);
}


