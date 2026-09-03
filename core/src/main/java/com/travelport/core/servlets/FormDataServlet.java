package com.travelport.core.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/bin/formdata",
                "sling.servlet.methods=" + HttpConstants.METHOD_POST
        }
)
public class FormDataServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(FormDataServlet.class);

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        LOG.info("=== FormDataServlet Execution Started ===");
        LOG.info("Content-Type: {}", request.getContentType());
        String param1 = request.getParameter("textbox");
        LOG.info("Extra configuration {} ",param1);
        String dataParam = request.getParameter("data");
        if (StringUtils.isNotBlank(dataParam)) {
            LOG.info("Form Data from parameter 'data': {}", dataParam);
        }

        // 2. Try reading from request body
        StringBuilder bodyBuilder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                bodyBuilder.append(line);
            }
        } catch (Exception e) {
            LOG.debug("Could not read body: {}", e.getMessage());
        }

        String bodyData = bodyBuilder.toString();
        if (StringUtils.isNotBlank(bodyData)) {
            LOG.info("Form Data from body payload: {}", bodyData);
        }

        // 3. Log all request parameters
        Map<String, String[]> paramMap = request.getParameterMap();
        if (paramMap != null && !paramMap.isEmpty()) {
            paramMap.forEach((k, v) -> LOG.info("Request Parameter [{}]: {}", k, Arrays.toString(v)));
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(SlingHttpServletResponse.SC_OK);
        response.getWriter().write(
                "{\"status\":\"success\",\"message\":\"Form data logged successfully\"}"
        );
    }
}
