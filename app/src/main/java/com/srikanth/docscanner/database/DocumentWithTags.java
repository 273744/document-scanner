package com.srikanth.docscanner.database;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

/**
 * DocumentWithTags - Relation class for Document with its Tags
 *
 * Features:
 * - Embedded Document entity
 * - Many-to-many relationship with Tags
 * - Junction table support
 */
public class DocumentWithTags {

    @Embedded
    public Document document;

    @Relation(
        parentColumn = "document_id",
        entityColumn = "tag_id",
        associateBy = @Junction(DocumentTag.class)
    )
    public List<Tag> tags;

    /**
     * Get tag names as comma-separated string
     */
    public String getTagNamesString() {
        if (tags == null || tags.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.size(); i++) {
            sb.append(tags.get(i).getTagName());
            if (i < tags.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Check if document has specific tag
     */
    public boolean hasTag(String tagName) {
        if (tags == null) {
            return false;
        }

        for (Tag tag : tags) {
            if (tag.getTagName().equalsIgnoreCase(tagName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get tag count
     */
    public int getTagCount() {
        return tags != null ? tags.size() : 0;
    }
}


