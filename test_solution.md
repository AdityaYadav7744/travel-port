---
title: Prefill core component based adaptive form
description: Learn how to prefill adaptive form with data
feature: Adaptive Forms
type: Documentation
role: Developer
level: Beginner
version: Experience Manager as a Cloud Service
topic: Integrations
jira: KT-14675
duration: 23
exl-id: a94deebd-e86e-4360-b0ed-193f13197ee2
TQID: https://experienceleague.adobe.com/-YqQxzmiNHqUTS5nzx-u3gTRIAr2tSmv3iTIT2qYouE
product_v2:
  - id: fd1f54a9-f50c-467d-8956-cebbaf4f3eb8
    internal-label: Experience Manager
feature_v2:
  - id: ae478996-b206-4712-9b0c-dc78a2644453
    internal-label: Integrations
role_v2:
  - id: ff6a42d2-313e-452e-93a6-792e4fad9ff8
    internal-label: Developer
level_v2:
  - id: e8ccd51f-da0d-4e3b-939b-e30d5ebb1ea5
    internal-label: Beginner
---
# Test the solution

After the code is deployed, create an adaptive form based on core components. Associate the adaptive form with the prefill service as shown in the screenshot below.
![prefill-service](assets/pre-fill-service.png)

Every time the form is rendered, the associated prefill service will be executed and the form will be populated with the data returned by the prefill service.

For example to prefill the form with the data associated with the guid **d815a2b3-5f4c-4422-8197-d0b73479bf0e**, the following url is used. 
The code in the prefill service will extract the value of guid parameter and fetch the data associated with the guid from the data source.

``` html
http://localhost:4502/content/dam/formsanddocuments/contactus/jcr:content?wcmmode=disabled&guid=d815a2b3-5f4c-4422-8197-d0b73479bf0e
```
