/** *****************************************************************************
 * Copyright (c) 2017 Black Duck Software, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Black Duck Software, Inc - initial implementation and documentation
 ****************************************************************************** */
package com.blackduck.protecode.sc.jenkins;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.blackduck.protecode.sc.jenkins.interfaces.ProtecodeScApi;
import com.blackduck.protecode.sc.jenkins.interfaces.ProtecodeScServicesApi;
import hudson.model.Run;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.*;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProtecodeScConnection {

  private static final Logger LOGGER = Logger.getLogger(ProtecodeScConnection.class.getName());

  /**
   * A service class containing only static functions. No point in making an object out of it.
   */
  private ProtecodeScConnection() {
    // Don't instantiate me.
  }

  /**
   * Simple backend for checking the server and such. This backend doesn't use authentication. It does not
   * have declarations for any Protecode SC API calls, only server level calls.
   *
   * @param checkCertificate whether or not to check the server certificate.
   * @param url The URL which points to the Protecode SC instance.
   * @return the backend to use while communicating to the server with no authentication
   */
  public static ProtecodeScServicesApi serviceBackend(URL url, boolean checkCertificate) {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(url.toExternalForm())
      .build();

    return retrofit.create(ProtecodeScServicesApi.class);
  }

  /**
   * Main entry point for building a backend implementation in run-time.
   *
   * @param credentialsId the identifier for the credentials to be used.
   * @param url The url which points to the protecode-sc instance.
   * @param run The context for getting the credentials
   * @param checkCertificate whether or not to check the server certificate.
   * @return the backend to use while communicating to the server
   */
  public static ProtecodeScApi backend(
    String credentialsId,
    URL url,
    Run run,
    boolean checkCertificate
  ) {
    // TODO: Add a debug flag or something
    // HOW TO LOG
    // Leave these here for convenience of debugging. They bleed memory _badly_ though
    // HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
    // interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

    OkHttpClient okHttpClient = httpClientBuilder(checkCertificate).addInterceptor(
      (Interceptor.Chain chain)
      -> {
      Request originalRequest = chain.request();

      Request.Builder builder = originalRequest.newBuilder()
        .addHeader("User-Agent", Configuration.CLIENT_NAME)
        .addHeader("Connection", "close");

      builder.header("Authorization", authenticationString(credentialsId, run, url));

      Request newRequest = builder.build();
      return chain.proceed(newRequest);
    }
    ).readTimeout(Configuration.TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .connectTimeout(Configuration.TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      //.addInterceptor(interceptor)
      .build();
    // TODO: Write interceptor for checking is the error 429 (too many requests) and handle that in
    // a nice fashion.

    okHttpClient.dispatcher().setMaxRequests(Configuration.MAX_REQUESTS_TO_PROTECODE);
    LOGGER.log(Level.ALL, "Max simultaneous requests to " + Configuration.TOOL_NAME + " limited to: {0}",
      okHttpClient.dispatcher().getMaxRequests());

    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(url.toExternalForm())
      .addConverterFactory(GsonConverterFactory.create())
      .client(okHttpClient)
      .build();

    return retrofit.create(ProtecodeScApi.class);
  }

  /**
   * Method returns authentication string based on the credentials type.
   *
   * @param credentialsId the identifier for the credentials to be used.
   * @param url The url which points to the BDBA instance.
   * @param run The context for getting the credentials
   * @return The string to use with authorization header
   */
  private static String authenticationString(String credentialsId, Run<?, ?> run, URL url) {
    StandardCredentials credentials = CredentialsProvider.findCredentialById(
      credentialsId,
      StandardCredentials.class,
      run,
      URIRequirementBuilder.fromUri(url.toExternalForm()).build()
    );

    String authenticationString;

    if (credentials instanceof StandardUsernamePasswordCredentials) {
      LOGGER.fine("using credentials");
      authenticationString = Credentials.basic(
        ((StandardUsernamePasswordCredentials) credentials).getUsername(),
        ((StandardUsernamePasswordCredentials) credentials).getPassword().getPlainText()
      );
    } else if (credentials instanceof StringCredentials) {
      LOGGER.fine("using API key");
      authenticationString = "Bearer " + ((StringCredentials) credentials).getSecret();
    } else {
      return "";
    }
    return authenticationString;
  }

  /**
   * Add possible CipherSuites, connection specs and connection linked items which are completely server
   * specific here.
   *
   * @param checkCertificate whether to enforce certificate
   * @return the http client
   */
  private static OkHttpClient.Builder httpClientBuilder(boolean checkCertificate) {
    if (checkCertificate) {
      LOGGER.log(Level.INFO, "Checking certificates");
      ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_2)
        .cipherSuites(
          CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
          CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
          CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
          CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384)
        .build();
      return new OkHttpClient.Builder().connectionSpecs(Collections.singletonList(spec));
    } else {
      LOGGER.log(Level.INFO, "NOT checking certificates");
      return UnsafeOkHttpClient.getUnsafeOkHttpClient().newBuilder();
    }
  }
}
