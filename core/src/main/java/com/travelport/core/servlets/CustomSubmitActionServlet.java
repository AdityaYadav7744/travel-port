package com.travelport.core.servlets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.stream.Collectors;

@Component(
        service = Servlet.class,
        property = {
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/users",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=POST"
        }
)
public class CustomSubmitActionServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CustomSubmitActionServlet.class);
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        LOG.info("Content-Type : {}", request.getContentType());
        String requestBody = request.getReader()
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));

        LOG.info("Request Body : {}", requestBody);
        if (!requestBody.isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(requestBody);
            jsonNode.fields().forEachRemaining(entry -> {
                LOG.info("{} : {}", entry.getKey(), entry.getValue().asText());
            });
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                "{\"status\":\"success\",\"message\":\"Data received successfully\"}"
        );
    }
}