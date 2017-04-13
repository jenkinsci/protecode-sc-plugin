/*******************************************************************************
* Copyright (c) 2016 Synopsys, Inc
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Synopsys, Inc - initial implementation and documentation
*******************************************************************************/

package com.synopsys.protecode.sc.jenkins;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.net.ssl.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.synopsys.protecode.sc.jenkins.exceptions.ApiAuthenticationException;
import com.synopsys.protecode.sc.jenkins.exceptions.ApiException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.net.UrlEscapers;
import com.synopsys.protecode.sc.jenkins.ProtecodeSc.Status;

public class HttpApiConnector {

    private String protecodeScHost;
    private String protecodeScGroup;
    private String protecodeScUser;
    private String protecodeScPass;
    private boolean doNotCheckCerts;
    private Artifact artifact;
    private boolean useSsl;
    private PrintStream log;
    private Client client;

    public HttpApiConnector(PrintStream log, Artifact artifact,
            String protecodeScHost, String protecodeScGroup,
            String protecodeScUser, String protecodeScPass,
            boolean doNotCheckCerts) {
        this.log = log;
        if (!protecodeScHost.endsWith("/")) {
            this.protecodeScHost = protecodeScHost + "/";
        } else {
            this.protecodeScHost = protecodeScHost;
        }
        this.useSsl = this.protecodeScHost.startsWith("https");
        this.artifact = artifact;
        this.protecodeScUser = protecodeScUser;
        this.protecodeScGroup = protecodeScGroup;
        this.protecodeScPass = protecodeScPass;
        this.doNotCheckCerts = doNotCheckCerts;
    }

    private static TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs,
                        String authType) {
                    // intentionally left blank
                }

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs,
                        String authType) {
                    // intentionally left blank
                }
            } };

    private static HostnameVerifier allowAllHostnames = new HostnameVerifier() {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    private Client createClient()
            throws KeyManagementException, NoSuchAlgorithmException {

        ClientConfig config = new ClientConfig();
        System.setProperty("jsse.enableSNIExtension", "false");
        SSLContext sc = null;
        if (doNotCheckCerts) {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        }

        config.connectorProvider(new HttpUrlConnectorProvider());
        ObjectMapper mapper = new ObjectMapper();

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
        JacksonJaxbJsonProvider jaxbProvider = new JacksonJaxbJsonProvider();
        jaxbProvider.setMapper(mapper);

        Client client = (doNotCheckCerts && useSsl)
                ? ClientBuilder.newBuilder().sslContext(sc)
                        .hostnameVerifier(allowAllHostnames).withConfig(config)
                        .build()
                : ClientBuilder.newBuilder().withConfig(config).build();

        HttpAuthenticationFeature feature = HttpAuthenticationFeature
                .basicBuilder().credentials(protecodeScUser, protecodeScPass)
                .build();
        client.register(feature);
        client.register(jaxbProvider);
        return client;
    }

    private void setTrustAllCerts() throws NoSuchAlgorithmException, KeyManagementException {
        if (doNotCheckCerts && useSsl) {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, this.trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(allowAllHostnames);
        }
    }

    public static String sanitizeArtifactFileName(String filename) {
        // Protecode SC upload API does not allow some filename characters in
        // the
        // URL, so the filename has
        // to be cleaned up from these characters.
        // Allowed characters for Protecode SC API upload filename
        // [a-zA-Z0-9_.-]+ , max length 255

        final Integer MAX_LEN = 255;
        String sanitizedFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitizedFilename.length() > MAX_LEN) {
            sanitizedFilename = sanitizedFilename.substring(0, MAX_LEN);
        }
        return sanitizedFilename;
    }


    /**
     * Initialize HTTP client
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    public void init() throws KeyManagementException, NoSuchAlgorithmException {
        client = createClient();
        setTrustAllCerts();
    }

    /**
     * Send artifact for scanning
     * @param artifact
     * @param scanMetaData
     * @return SHA1 for the file
     * @throws IOException
     * @throws NoSuchAlgorithmException
     *      When SHA-1 is not available
     * @throws ApiException
     *      When connection fails
     *      When authentication fails
     */
    public String sendFile(Artifact artifact, Map<String, String> scanMetaData) throws IOException, ApiException, NoSuchAlgorithmException, InterruptedException {
        String filename = artifact.getName();
        String protecodeScFileName = sanitizeArtifactFileName(filename);
        log.println("Uploading file to Protecode SC at " + protecodeScHost);
        InputStream fileInputStream = new BufferedInputStream(artifact.getData());

        byte[] authData = (protecodeScUser+":"+protecodeScPass).getBytes(StandardCharsets.UTF_8);
        String encodedAuthData = javax.xml.bind.DatatypeConverter.printBase64Binary(authData);

        URL url = new URL(protecodeScHost + "api/upload/" + UrlEscapers.urlPathSegmentEscaper()
                .escape(protecodeScFileName));

        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setFixedLengthStreamingMode(artifact.getSize());
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("PUT");
        httpCon.setRequestProperty("Authorization", "Basic "+encodedAuthData);
        httpCon.setRequestProperty("Group-Appcheck", protecodeScGroup);
        httpCon.setRequestProperty("Delete-Binary-Appcheck", "true");

        for (Map.Entry<String, String> e : scanMetaData.entrySet()) {
            httpCon.setRequestProperty("META-" + e.getKey(), e.getValue());
        }

        DataOutputStream out = null;
        try {
            out = new DataOutputStream(
                    httpCon.getOutputStream());
        } catch (ConnectException e) {
            throw new ApiException(e);
        }

        int buffsize = 10240;
        byte[] buff = new byte[buffsize];
        int len = 0;
        int inbuff = 0;
        // Count the SHA1 locally to poll in case scan is already started
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        String sha1 = null;

        try {
            // Do the actual file sending
            while ((len = fileInputStream.read(buff, 0, buffsize)) > 0) {
                inbuff = len;
                while ((inbuff < buffsize)) {
                    len = fileInputStream.read(buff, inbuff, buffsize - inbuff);
                    if (len == -1) {
                        break;
                    }
                    inbuff += len;
                }
                if (inbuff > 0) {
                    out.write(buff, 0, inbuff);
                    digest.update(buff, 0, inbuff);
                }
            }
            sha1 = new HexBinaryAdapter().marshal(digest.digest()).toLowerCase();
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new ApiException(e.getMessage()); // Don't mind this if running against dev-stack, behaves differently
        } finally {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                // NOP
            }
        }

        try {
            if (httpCon.getResponseCode() == 401) {
                throw new ApiAuthenticationException("Protecode SC upload failed, authorization error");
            } else if (httpCon.getResponseCode() == 400) {
                throw new ApiAuthenticationException("Protecode SC upload failed, bad request (please check your credentials and upload group)");
            } else if (httpCon.getResponseCode() == 409) {
                return sha1;
            }
        } catch (IOException e) {
            log.println(e.getMessage());
            throw new ApiException(e);
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode resultJson = mapper.readTree(httpCon.getInputStream());
            JsonNode results = resultJson.get("results");
            return results.get("sha1sum").asText();
        } catch (IOException e) {
            log.println(e.getMessage());
        } finally {
            httpCon.disconnect();
        }
        return sha1;
    }

    public PollResult poll(String identifier) {
        WebTarget target = client.target(
                protecodeScHost + "appcheck/api/app/" + identifier + "/");
        Response r = target.request().accept(MediaType.APPLICATION_JSON).get();
        ProtecodeSc response = r.readEntity(ProtecodeSc.class);

        if (response.getMeta() != null
                && !Integer.valueOf(200).equals(response.getMeta().getCode())) {
            log.println("Protecode SC polling failed, status "
                    + response.getMeta().getCode());
            return new PollResult(true, false);
        }
        Status responseStatus = response.getResults().getStatus();
        response.setArtifactName(artifact.getName());
        if (Status.R.equals(responseStatus)) {
            log.println("Artifact " + artifact.getName()
                    + " polling success, status "
                    + response.getMeta().getCode());
            if (response.getResults().getSummary().getVulnCount() != null) {
                return new PollResult(true, response.getResults().getSummary()
                        .getVulnCount().getExact() == 0, response,
                        artifact.getName());
            } else {
                return new PollResult(true, false);
            }
        }
        if (Status.B.equals(responseStatus)) {
            return new PollResult(false, false);
        }
        return new PollResult(true, false);

    }

    public void close() {
        client.close();
    }

    static class PollResult {
        private boolean ready;
        private boolean ok;
        private ProtecodeSc protecodeSc;
        private String artifactName;

        public PollResult(boolean ready, boolean ok, ProtecodeSc protecodeSc,
                String artifactName) {
            this.ready = ready;
            this.ok = ok;
            this.protecodeSc = protecodeSc;
            this.artifactName = artifactName;
        }

        public PollResult(boolean ready, boolean ok) {
            this(ready, ok, null, null);
        }

        public boolean isReady() {
            return ready;
        }

        public boolean isOk() {
            return ok;
        }

        public String getArtifactName() {
            return artifactName;
        }

        public ProtecodeSc getProtecodeSc() {
            return protecodeSc;
        }
    }
}
