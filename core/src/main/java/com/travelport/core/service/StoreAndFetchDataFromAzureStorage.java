package com.travelport.core.service;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = StoreAndFetchDataFromAzureStorage.class,
        immediate = true
)
public class StoreAndFetchDataFromAzureStorage {

    private static final Logger LOG = LoggerFactory.getLogger(StoreAndFetchDataFromAzureStorage.class);

    public String getBlobData(String guid) {
        LOG.info("StoreAndFetchDataFromAzureStorage.getBlobData invoked with guid: {}", guid);
        return "{\n" +
                "  \"afData\": {\n" +
                "    \"afUnboundData\": {\n" +
                "      \"data\": {\n" +
                "        \"firstName\": \"John\",\n" +
                "        \"lastName\": \"Doe\",\n" +
                "        \"email\": \"john.doe@example.com\",\n" +
                "        \"salary\": 85000,\n" +
                "        \"panelcontainer\": {\n" +
                "          \"firstName\": \"John\",\n" +
                "          \"lastName\": \"Doe\",\n" +
                "          \"email\": \"john.doe@example.com\",\n" +
                "          \"salary\": 85000\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"data\": {\n" +
                "    \"firstName\": \"John\",\n" +
                "    \"lastName\": \"Doe\",\n" +
                "    \"email\": \"john.doe@example.com\",\n" +
                "    \"salary\": 85000\n" +
                "  },\n" +
                "  \"firstName\": \"John\",\n" +
                "  \"lastName\": \"Doe\",\n" +
                "  \"email\": \"john.doe@example.com\",\n" +
                "  \"salary\": 85000\n" +
                "}";
    }
}
