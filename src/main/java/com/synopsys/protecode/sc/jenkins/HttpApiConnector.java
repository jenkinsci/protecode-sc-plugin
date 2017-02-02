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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

        log.println("Uploading file to appcheck at " + protecodeScHost);

        HttpAuthenticationFeature feature = HttpAuthenticationFeature
                .basicBuilder().credentials(protecodeScUser, protecodeScPass)
                .build();
        client.register(feature);
        client.register(jaxbProvider);
        return client;
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

    public String init(Map<String, String> scanMetaData)
            throws KeyManagementException, NoSuchAlgorithmException,
            IOException, ApiAuthenticationException {
        client = createClient();
        WebTarget target = client.target(protecodeScHost);

        String filename = artifact.getName();
        String protecodeScFileName = sanitizeArtifactFileName(filename);
        InputStream inputStream = new BufferedInputStream(artifact.getData());
        Builder requestBuilder = target

                .path("api/upload/" + UrlEscapers.urlPathSegmentEscaper()
                        .escape(protecodeScFileName))
                .request().header("Group-Appcheck", protecodeScGroup)
                .header("Delete-Binary-Appcheck", "true");

        for (Entry<String, String> e : scanMetaData.entrySet()) {
            requestBuilder = requestBuilder.header("META-" + e.getKey(),
                    e.getValue());
        }
        Response r = requestBuilder.put(Entity.entity(inputStream, "*/*"));
        if (r.getStatus() == 401) {
            log.println("Protecode SC upload failed, authorization error");
            throw new ApiAuthenticationException("Protecode SC authorization failed");
        }

        ProtecodeSc response = r.readEntity(ProtecodeSc.class);
        String identifier = response.getResults().getSha1sum();
        try {
            inputStream.close();
        } catch (IOException ignore) {
            // intentionally left blank
        }
        return identifier;
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
            return new PollResult(true, response.getResults().getSummary()
                    .getVulnCount().getExact() == 0, response,
                    artifact.getName());
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
