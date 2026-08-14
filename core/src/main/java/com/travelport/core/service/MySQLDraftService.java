package com.travelport.core.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing form drafts in MySQL database.
 */
public interface MySQLDraftService {

    /**
     * Saves or updates a form draft in MySQL.
     *
     * @param draftId  Unique identifier for the draft
     * @param formPath Path of the form
     * @param userId   User identifier
     * @param title    Title of the draft form
     * @param formData JSON or XML content of the form data
     * @return true if save/update was successful, false otherwise
     */
    boolean saveDraft(String draftId, String formPath, String userId, String title, String formData);

    /**
     * Retrieves a draft by draft ID.
     *
     * @param draftId Unique identifier for the draft
     * @return Map containing draft attributes or empty map if not found
     */
    Map<String, Object> getDraft(String draftId);

    /**
     * Retrieves all drafts for a given user ID ordered by updated_at descending.
     *
     * @param userId User identifier
     * @return List of draft maps for the user
     */
    List<Map<String, Object>> getDraftsForUser(String userId);

    /**
     * Deletes a draft by draft ID.
     *
     * @param draftId Unique identifier for the draft
     * @return true if deletion was successful, false otherwise
     */
    boolean deleteDraft(String draftId);
}
