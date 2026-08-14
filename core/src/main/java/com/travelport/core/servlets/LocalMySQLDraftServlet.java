package com.travelport.core.servlets;

import com.google.gson.Gson;
import com.travelport.core.service.MySQLDraftService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component(service = Servlet.class)
@SlingServletPaths("/bin/travelport/local-mysql-draft")
public class LocalMySQLDraftServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Reference
    private MySQLDraftService draftService;

    @Override
    protected void doPost(final SlingHttpServletRequest req, final SlingHttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String action = req.getParameter("action");
        if ("delete".equalsIgnoreCase(action)) {
            doDelete(req, resp);
            return;
        }

        String formPath = req.getParameter("formPath");
        if (StringUtils.isBlank(formPath)) {
            formPath = "/content/forms/af/job-application-form";
        }

        String userId = req.getParameter("userId");
        if (StringUtils.isBlank(userId)) {
            if (req.getResourceResolver() != null) {
                userId = req.getResourceResolver().getUserID();
            }
        }
        if (StringUtils.isBlank(userId)) {
            userId = "anonymous";
        }

        String draftId = req.getParameter("draftId");

        String title = req.getParameter("title");
        if (StringUtils.isBlank(title)) {
            title = extractFormTitle(formPath);
        }

        String formData = req.getParameter("data");
        if (StringUtils.isBlank(formData)) {
            formData = req.getParameter("formData");
        }

        Map<String, Object> result = new HashMap<>();

        if (StringUtils.isBlank(formData)) {
            result.put("status", "error");
            result.put("message", "Form data (parameter 'data') is required");
            resp.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
        } else {
            boolean saved = draftService.saveDraft(draftId, formPath, userId, title, formData);
            if (saved) {
                result.put("status", "success");
                result.put("userId", userId);
                result.put("formPath", formPath);
                result.put("message", "Draft saved/updated successfully for user");
            } else {
                result.put("status", "error");
                result.put("message", "Failed to save draft");
                resp.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        resp.getWriter().write(new Gson().toJson(result));
    }

    @Override
    protected void doGet(final SlingHttpServletRequest req, final SlingHttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String draftId = req.getParameter("draftId");
        Map<String, Object> result = new HashMap<>();

        if (StringUtils.isNotBlank(draftId)) {
            Map<String, Object> draft = draftService.getDraft(draftId);
            if (draft != null && !draft.isEmpty()) {
                result.put("status", "success");
                result.put("draft", draft);
            } else {
                result.put("status", "error");
                result.put("message", "Draft not found for draftId: " + draftId);
                resp.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            String userId = req.getParameter("userId");
            if (StringUtils.isBlank(userId)) {
                if (req.getResourceResolver() != null) {
                    userId = req.getResourceResolver().getUserID();
                }
            }
            if (StringUtils.isBlank(userId)) {
                userId = "anonymous";
            }

            List<Map<String, Object>> drafts = draftService.getDraftsForUser(userId);
            result.put("status", "success");
            result.put("userId", userId);
            result.put("drafts", drafts);
        }

        resp.getWriter().write(new Gson().toJson(result));
    }

    @Override
    protected void doDelete(final SlingHttpServletRequest req, final SlingHttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String draftId = req.getParameter("draftId");
        Map<String, Object> result = new HashMap<>();

        if (StringUtils.isNotBlank(draftId)) {
            boolean deleted = draftService.deleteDraft(draftId);
            if (deleted) {
                result.put("status", "success");
                result.put("message", "Draft deleted successfully");
            } else {
                result.put("status", "error");
                result.put("message", "Draft not found or could not be deleted");
                resp.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            result.put("status", "error");
            result.put("message", "draftId parameter is required for deletion");
            resp.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
        }

        resp.getWriter().write(new Gson().toJson(result));
    }

    private String extractFormTitle(String formPath) {
        if (StringUtils.isBlank(formPath)) {
            return "Form Draft";
        }
        String filename = formPath.substring(formPath.lastIndexOf('/') + 1);
        if (filename.endsWith(".html")) {
            filename = filename.substring(0, filename.length() - 5);
        }
        String[] words = filename.split("[-_]");
        StringBuilder titleBuilder = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                titleBuilder.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return titleBuilder.toString().trim();
    }
}
