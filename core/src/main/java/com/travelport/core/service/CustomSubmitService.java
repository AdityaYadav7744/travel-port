package com.travelport.core.service;

import com.adobe.aemds.guide.model.FormSubmitInfo;
import com.adobe.aemds.guide.service.FormSubmitActionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component(
    service = FormSubmitActionService.class,
    immediate = true
)
public class CustomSubmitService implements FormSubmitActionService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomSubmitService.class);
    private static final String SERVICE_NAME = "Custom Submit Action";

    @Reference
    private DataSource dataSource;

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public Map<String, Object> submit(FormSubmitInfo formSubmitInfo) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (formSubmitInfo == null) {
                setResponseStatus(result, false, "FormSubmitInfo is null");
                return result;
            }

            String jsonData = formSubmitInfo.getData();
            String formPath = formSubmitInfo.getFormContainerResource() != null 
                    ? formSubmitInfo.getFormContainerResource().getPath() 
                    : "/content/forms/af/form";

            String userId = (formSubmitInfo.getFormContainerResource() != null 
                    && formSubmitInfo.getFormContainerResource().getResourceResolver() != null)
                    ? formSubmitInfo.getFormContainerResource().getResourceResolver().getUserID()
                    : "anonymous";

            LOG.info("Custom Submit Action triggered for formPath: {}, user: {}", formPath, userId);
            LOG.info("Submitted Data: {}", jsonData);

            boolean saved = saveSubmissionToDatabase(formPath, userId, jsonData);

            if (saved) {
                LOG.info("Successfully saved form submission to Database table 'af_submissions'");
                setResponseStatus(result, true, "Form submitted and saved to Database successfully!");
            } else {
                LOG.warn("Form data submitted, but DataSource was unavailable or save failed.");
                setResponseStatus(result, true, "Form submitted successfully!");
            }

        } catch (Exception e) {
            LOG.error("Error processing custom submit action", e);
            setResponseStatus(result, false, e.getMessage());
        }

        return result;
    }

    private void setResponseStatus(Map<String, Object> result, boolean success, String message) {
        result.put("FormSubmissionComplete", success);
        result.put("guideSubmitStatus", success);
        result.put("submitStatus", success);
        result.put("status", success ? "success" : "error");

        if (success) {
            result.put("thankYouMessage", message);
            result.put("thankYouOption", "message");
        } else {
            result.put("guideSubmitError", message);
        }
    }

    /**
     * Saves submitted form JSON payload to the MySQL database table 'af_submissions'.
     */
    private boolean saveSubmissionToDatabase(String formPath, String userId, String jsonData) {
        if (dataSource == null) {
            LOG.warn("DataSource is null. Skipping database save.");
            return false;
        }

        String createTableSql = "CREATE TABLE IF NOT EXISTS travelport_db.af_submissions ("
                + "submission_id VARCHAR(64) PRIMARY KEY, "
                + "form_path VARCHAR(512), "
                + "user_id VARCHAR(128), "
                + "form_data LONGTEXT, "
                + "submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        String insertSql = "INSERT INTO travelport_db.af_submissions (submission_id, form_path, user_id, form_data) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection()) {

            // Auto-create database schema and table if needed
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS travelport_db");
                stmt.executeUpdate(createTableSql);
            } catch (SQLException e) {
                LOG.debug("Schema/table initialization notice: {}", e.getMessage());
            }

            // Insert submission record
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, formPath);
                ps.setString(3, userId);
                ps.setString(4, jsonData);

                int rows = ps.executeUpdate();
                LOG.info("Inserted submission record into 'af_submissions'. Rows affected: {}", rows);
                return rows > 0;
            }

        } catch (SQLException e) {
            LOG.error("SQLException while saving form submission to Database", e);
            return false;
        }
    }
}