package com.travelport.core.service.impl;

import com.travelport.core.service.MySQLDraftService;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component(service = MySQLDraftService.class)
public class MySQLDraftServiceImpl implements MySQLDraftService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLDraftServiceImpl.class);

    @Reference(target = "(|(datasource.name=myDataSource)(datasource.name=travelport_mysql))", cardinality = ReferenceCardinality.OPTIONAL)
    private DataSource dataSource;

    @Activate
    protected void activate() {
        if (dataSource == null) {
            LOGGER.warn("DataSource is not bound yet. Database initialization will be deferred.");
            return;
        }

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS travelport_db");

            String createTableSql = "CREATE TABLE IF NOT EXISTS travelport_db.af_draft ("
                    + "draft_id VARCHAR(64) PRIMARY KEY, "
                    + "form_path VARCHAR(512), "
                    + "user_id VARCHAR(128), "
                    + "title VARCHAR(255) DEFAULT 'Draft Form', "
                    + "form_data LONGTEXT, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                    + "UNIQUE KEY unique_user_form (user_id, form_path(255))"
                    + ")";
            stmt.executeUpdate(createTableSql);
            LOGGER.info("Successfully verified database travelport_db and table af_draft with unique_user_form constraint.");
        } catch (SQLException e) {
            LOGGER.error("Error during database or table initialization in activate", e);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    LOGGER.error("Error closing Statement in activate", e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.error("Error closing Connection in activate", e);
                }
            }
        }
    }

    @Override
    public boolean saveDraft(String draftId, String formPath, String userId, String title, String formData) {
        if (dataSource == null) {
            LOGGER.error("Cannot save draft: DataSource is not bound.");
            return false;
        }
        if (StringUtils.isBlank(formData)) {
            LOGGER.warn("Cannot save draft: formData is blank.");
            return false;
        }

        if (StringUtils.isBlank(formPath)) {
            formPath = "/content/forms/af/loan-form";
        }
        if (StringUtils.isBlank(userId)) {
            userId = "anonymous";
        }
        if (StringUtils.isBlank(title)) {
            title = "Draft Form";
        }

        Connection conn = null;
        PreparedStatement checkPs = null;
        ResultSet rs = null;

        // Check if a draft already exists for this user and form_path
        try {
            conn = dataSource.getConnection();
            String findExistingSql = "SELECT draft_id FROM travelport_db.af_draft WHERE user_id = ? AND form_path = ?";
            checkPs = conn.prepareStatement(findExistingSql);
            checkPs.setString(1, userId);
            checkPs.setString(2, formPath);
            rs = checkPs.executeQuery();

            if (rs.next()) {
                // Reuse existing draft_id so that only 1 draft per user per form exists
                String existingDraftId = rs.getString("draft_id");
                if (StringUtils.isNotBlank(existingDraftId)) {
                    draftId = existingDraftId;
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("Could not check existing draft for user: {} and formPath: {}", userId, formPath, e);
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) {}
            }
            if (checkPs != null) {
                try { checkPs.close(); } catch (SQLException e) {}
            }
        }

        if (StringUtils.isBlank(draftId)) {
            draftId = UUID.randomUUID().toString();
        }

        String sql = "INSERT INTO travelport_db.af_draft (draft_id, form_path, user_id, title, form_data) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE form_data = ?, title = ?, updated_at = CURRENT_TIMESTAMP";

        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, draftId);
            ps.setString(2, formPath);
            ps.setString(3, userId);
            ps.setString(4, title);
            ps.setString(5, formData);
            ps.setString(6, formData);
            ps.setString(7, title);

            int rows = ps.executeUpdate();
            LOGGER.info("Saved/Updated single draft for user: {} form: {} draftId: {}, rows affected: {}", userId, formPath, draftId, rows);
            return rows > 0;
        } catch (SQLException e) {
            LOGGER.error("Error saving draft for user: {} form: {} draftId: {}", userId, formPath, draftId, e);
            return false;
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    LOGGER.error("Error closing PreparedStatement in saveDraft", e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.error("Error closing Connection in saveDraft", e);
                }
            }
        }
    }

    @Override
    public Map<String, Object> getDraft(String draftId) {
        if (dataSource == null || StringUtils.isBlank(draftId)) {
            return null;
        }

        String sql = "SELECT draft_id, form_path, user_id, title, form_data, updated_at "
                + "FROM travelport_db.af_draft WHERE draft_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, draftId);
            rs = ps.executeQuery();

            if (rs.next()) {
                Map<String, Object> draft = new HashMap<>();
                draft.put("draftId", rs.getString("draft_id"));
                draft.put("formPath", rs.getString("form_path"));
                draft.put("userId", rs.getString("user_id"));
                draft.put("title", rs.getString("title"));
                draft.put("formData", rs.getString("form_data"));
                draft.put("updatedAt", rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toString() : "");
                return draft;
            }
        } catch (SQLException e) {
            LOGGER.error("Error retrieving draft with draftId: {}", draftId, e);
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) {}
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException e) {}
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) {}
            }
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> getDraftsForUser(String userId) {
        List<Map<String, Object>> drafts = new ArrayList<>();
        if (dataSource == null) {
            return drafts;
        }

        String sql;
        boolean fetchAll = StringUtils.isBlank(userId) || "all".equalsIgnoreCase(userId);

        if (fetchAll) {
            sql = "SELECT draft_id, form_path, user_id, title, form_data, updated_at "
                    + "FROM travelport_db.af_draft ORDER BY updated_at DESC";
        } else {
            sql = "SELECT draft_id, form_path, user_id, title, form_data, updated_at "
                    + "FROM travelport_db.af_draft WHERE user_id = ? ORDER BY updated_at DESC";
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sql);
            if (!fetchAll) {
                ps.setString(1, userId);
            }
            rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> draft = new HashMap<>();
                draft.put("draftId", rs.getString("draft_id"));
                draft.put("formPath", rs.getString("form_path"));
                draft.put("userId", rs.getString("user_id"));
                draft.put("title", rs.getString("title"));
                draft.put("formData", rs.getString("form_data"));
                draft.put("updatedAt", rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toString() : "");
                drafts.add(draft);
            }
        } catch (SQLException e) {
            LOGGER.error("Error retrieving drafts for user: {}", userId, e);
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) {}
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException e) {}
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) {}
            }
        }
        return drafts;
    }

    @Override
    public boolean deleteDraft(String draftId) {
        if (dataSource == null || StringUtils.isBlank(draftId)) {
            return false;
        }

        String sql = "DELETE FROM travelport_db.af_draft WHERE draft_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, draftId);
            int rows = ps.executeUpdate();
            LOGGER.info("Deleted draft with draftId: {}, rows affected: {}", draftId, rows);
            return rows > 0;
        } catch (SQLException e) {
            LOGGER.error("Error deleting draft with draftId: {}", draftId, e);
            return false;
        } finally {
            if (ps != null) {
                try { ps.close(); } catch (SQLException e) {}
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) {}
            }
        }
    }
}
