 /*******************************************************************************
  * Copyright (c) 2017 Synopsys, Inc
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Synopsys, Inc - initial implementation and documentation
  *******************************************************************************/

package com.synopsys.protecode.sc.jenkins;

import java.security.cert.CertificateException;
import javax.net.ssl.*;
import okhttp3.OkHttpClient;

public class UnsafeOkHttpClient {
  public static OkHttpClient getUnsafeOkHttpClient() {
    try {
      // Create a trust manager that does not validate certificate chains
      final TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
          @Override
          public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
          }
          
          @Override
          public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
          }
          
          @Override
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
          }
        }
      };
      
      // Install the all-trusting trust manager
      final SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
      
      // Create an ssl socket factory with our all-trusting manager
      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
      
      OkHttpClient.Builder builder = new OkHttpClient.Builder();
      builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
      builder.hostnameVerifier(new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      });
      
      OkHttpClient okHttpClient = builder.build();
      okHttpClient.dispatcher().setMaxRequests(Configuration.MAX_REQUESTS_TO_PROTECODE);
      return okHttpClient;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
