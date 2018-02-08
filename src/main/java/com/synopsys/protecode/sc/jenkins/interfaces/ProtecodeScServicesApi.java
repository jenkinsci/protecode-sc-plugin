/*
 * Copyright (c) 2018 pajunen.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    pajunen - initial API and implementation and/or initial documentation
 */
package com.synopsys.protecode.sc.jenkins.interfaces;

import retrofit2.Call;
import retrofit2.http.HEAD;

/**
 * Used for manually checking connection. 
 * TODO: Study whether to use interceptor perhaps. 
 */
public interface ProtecodeScServicesApi {
  
  @HEAD("/")
  public Call<Void> head();
  
}
