package com.travelport.core.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.forms.common.service.ContentType;
import com.adobe.forms.common.service.DataOptions;
import com.adobe.forms.common.service.DataProvider;
import com.adobe.forms.common.service.FileAttachmentWrapper;
import com.adobe.forms.common.service.FormsException;
import com.adobe.forms.common.service.PrefillData;

@Component(
        service = {DataProvider.class, CustomAFPrefillService.class},
        immediate = true
)
public class CustomAFPrefillService implements DataProvider {

    @Reference
    private DataManager dataManager;

    @Reference
    private StoreAndFetchDataFromAzureStorage storeandfetch;

    private transient Logger logger = LoggerFactory.getLogger(CustomAFPrefillService.class);

    @Override
    public PrefillData getPrefillData(DataOptions dataOptions) throws FormsException {
        logger.info("Invoking AF custom prefill service");

        String guid = null;
        if (dataOptions != null && dataOptions.getExtras() != null && dataOptions.getExtras().containsKey("guid")) {
            guid = (String) dataOptions.getExtras().get("guid");
        }

        String data = (storeandfetch != null) ? storeandfetch.getBlobData(guid) : null;
        InputStream dataInputStream = null;

        // Ensure ContentType is NOT null
        ContentType contentType = (dataOptions != null && dataOptions.getContentType() != null)
                ? dataOptions.getContentType()
                : ContentType.JSON;

        Map<String, Object> extras = (dataOptions != null) ? dataOptions.getExtras() : null;
        List<FileAttachmentWrapper> fileAttachmentWrappers = null;
        PrefillData prefillData = null;
        Map<String, String> customContext = null;

        if (extras != null && extras.containsKey(DataManager.UNIQUE_ID) && dataManager != null) {
            String dataKey = extras.get(DataManager.UNIQUE_ID).toString();
            if (dataManager.get(dataKey) != null) {
                data = (String) dataManager.get(dataKey);
                fileAttachmentWrappers = (List<FileAttachmentWrapper>) dataManager.get(DataManager.getFileAttachmentMapKey(dataKey));
            }
            customContext = dataManager.getCustomContext(dataKey);
        }

        if (StringUtils.isNotBlank(data)) {
            dataInputStream = getDataInputStream(data);
            if (fileAttachmentWrappers != null || customContext != null) {
                prefillData = new PrefillData(dataInputStream, contentType, fileAttachmentWrappers, customContext);
            } else {
                prefillData = new PrefillData(dataInputStream, contentType);
            }
            logger.info("CustomAFPrefillService returning PrefillData with contentType: {}", contentType);
        }

        return prefillData;
    }

    @Override
    public String getServiceName() {
        return "Core Custom Pre-fill Service";
    }

    @Override
    public String getServiceDescription() {
        return "Core Custom Pre-fill Service";
    }

    private InputStream getDataInputStream(String data) {
        InputStream dataInputStream = null;
        logger.info("got data: " + data);
        try {
            dataInputStream = new ByteArrayInputStream(data.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            logger.error("[Custom AF Prefill] Exception in custom af prefill service", e);
        }
        return dataInputStream;
    }
}
