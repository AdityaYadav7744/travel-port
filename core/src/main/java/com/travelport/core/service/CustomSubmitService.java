package com.travelport.core.service;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import com.adobe.aemds.guide.model.FormSubmitInfo;
import com.adobe.aemds.guide.service.FormSubmitActionService;
import com.adobe.fd.output.api.OutputService;
import com.adobe.fd.output.api.PDFOutputOptions;
import com.adobe.fd.output.api.AcrobatVersion;
import com.adobe.aemfd.docmanager.Document;

import com.day.cq.dam.api.AssetManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

    @Component(
    service = FormSubmitActionService.class,
    immediate = true
        )
    public class CustomSubmitService implements FormSubmitActionService {
        private static final Logger log = LoggerFactory.getLogger(CustomSubmitService.class);
        private static final String SERVICE_NAME = "Custom Submit Action";

        // Path of the XDP template filename in AEM DAM
        private static final String TEMPLATE_PATH = "pdfGeneration.xdp";

        // Content root folder containing the XDP in DAM
        private static final String TEMPLATE_CONTENT_ROOT = "crx:///content/dam/formsanddocuments/designer-templates";

        // Path where the generated PDF will be saved in JCR
        private static final String OUTPUT_PATH = "/content/dam/travelport";

        @Reference
        private OutputService outputService;

        @Override
        public String getServiceName() {
            return SERVICE_NAME;
        }

        @Override
        public Map<String, Object> submit(FormSubmitInfo formSubmitInfo) {
            Map<String, Object> result = new HashMap<>();

            try {
                // Step 1: Get submitted JSON data
                String jsonData = formSubmitInfo.getData();
                log.info("Using custom submit action service, [data]-->{}", jsonData);

                // Step 2: Convert JSON → XML
                String xmlData = convertJsonToXml(jsonData);
                log.info("Converted XML data: {}", xmlData);

                // Step 3: Wrap XML as a Document for the OutputService
                InputStream xmlInputStream = new ByteArrayInputStream(xmlData.getBytes("UTF-8"));
                Document xmlDocument = new Document(xmlInputStream);

                // Step 4: Configure PDF Output Options
                PDFOutputOptions pdfOutputOptions = new PDFOutputOptions();
                pdfOutputOptions.setAcrobatVersion(AcrobatVersion.Acrobat_10);
                pdfOutputOptions.setContentRoot(TEMPLATE_CONTENT_ROOT);

                // Step 5: Generate PDF using OutputService
                Document pdfDocument = outputService.generatePDFOutput(
                        TEMPLATE_PATH,
                        xmlDocument,
                        pdfOutputOptions);

                log.info("PDF generated successfully using OutputService");

                // Step 6: Save the generated PDF to JCR
                ResourceResolver resourceResolver = formSubmitInfo.getFormContainerResource().getResourceResolver();
                savePdfToJcr(pdfDocument, resourceResolver);

                // AEM framework required keys — FormSubmitActionManagerServiceImpl
                // expects these Boolean/String values; missing them causes NPE.
                result.put("fd:submitStatus", Boolean.TRUE);
                result.put("thankYouMessage", "PDF generated and saved successfully");

            } catch (Exception e) {
                log.error("Error generating PDF from form submission", e);
                // Must include the Boolean key even on failure to prevent framework NPE
                result.put("fd:submitStatus", Boolean.FALSE);
                result.put("fd:submitError", e.getMessage());
            }

            return result;
        }

        /**
         * Converts a flat JSON object to an XML string.
         *
         * Example input: {"first_name":"Nithin","last_name":"T"}
         * Example output: <?xml version="1.0" encoding="UTF-8"?>
         * <data><first_name>Nithin</first_name><last_name>T</last_name></data>
         *
         * NOTE: The root element <data> must match the root binding in your XDP
         * template. Change it if your template expects a different root element name.
         */
        private String convertJsonToXml(String jsonData) throws Exception {
            JSONObject jsonObject = new JSONObject(jsonData);
            StringBuilder xmlBuilder = new StringBuilder();

            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            xmlBuilder.append("<data>"); // ✏️ Change <data> to match your XDP template's root binding

            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = jsonObject.optString(key, "");
                // Sanitize key: XML element names cannot have spaces
                String safeKey = key.trim().replace(" ", "_");
                xmlBuilder.append("<").append(safeKey).append(">")
                        .append(escapeXml(value))
                        .append("</").append(safeKey).append(">");
            }

            xmlBuilder.append("</data>");
            return xmlBuilder.toString();
        }

        /**
         * Escapes special XML characters in field values to prevent malformed XML.
         */
        private String escapeXml(String value) {
            if (value == null)
                return "";
            return value
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
        }

        /**
         * Saves the generated PDF Document to the JCR repository under OUTPUT_PATH.
         * Each submission gets a unique timestamped filename.
         */
        private void savePdfToJcr(Document pdfDocument, ResourceResolver resourceResolver) throws Exception {
            // AssetManager handles the full dam:Asset structure automatically
            AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);
            if (assetManager == null) {
                throw new Exception("Could not obtain AssetManager from ResourceResolver");
            }

            String fileName = "submission_" + System.currentTimeMillis() + ".pdf";
            String fullPath = OUTPUT_PATH + "/" + fileName;

            // One line creates the full dam:Asset with all required child nodes!
            assetManager.createAsset(
                    fullPath,
                    pdfDocument.getInputStream(),
                    "application/pdf",
                    true
            );

            log.info("PDF saved as dam:Asset to JCR at: {}", fullPath);
        }
    }