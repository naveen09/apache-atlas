/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.util;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.LocalServletRequest;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.utils.AtlasJson;
import org.apache.atlas.utils.ParamChecker;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Utility functions for dealing with servlets.
 */
public final class Servlets {

  private static final Logger LOG = LoggerFactory.getLogger(Servlets.class);

  private Servlets() {
    /* singleton */
  }

  public static final String JSON_MEDIA_TYPE = MediaType.APPLICATION_JSON + "; charset=UTF-8";
  public static final String BINARY = MediaType.APPLICATION_OCTET_STREAM;

  private static final int QUERY_PARAM_MAX_LENGTH = AtlasConfiguration.QUERY_PARAM_MAX_LENGTH.getInt();

  /**
   * Returns the user of the given request.
   *
   * @param httpRequest    an HTTP servlet request
   * @return the user
   */
  public static String getUserFromRequest(final HttpServletRequest httpRequest) {
    String user = httpRequest.getRemoteUser();
    if (!StringUtils.isEmpty(user)) {
      return user;
    }

    user = httpRequest.getParameter("user.name"); // available in query-param
    if (!StringUtils.isEmpty(user)) {
      return user;
    }

    user = httpRequest.getHeader("Remote-User"); // backwards-compatibility
    if (!StringUtils.isEmpty(user)) {
      return user;
    }

    user = getDoAsUser(httpRequest);
    if (!StringUtils.isEmpty(user)) {
      return user;
    }

    return null;
  }

  private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
  private static final String DO_AS = "doAs";

  public static String getDoAsUser(final HttpServletRequest request) {
    if (StringUtils.isNoneEmpty(request.getQueryString())) {
      final List<NameValuePair> list = URLEncodedUtils.parse(request.getQueryString(), UTF8_CHARSET);
      if (list != null) {
        for (final NameValuePair nv : list) {
          if (DO_AS.equals(nv.getName())) {
            return nv.getValue();
          }
        }
      }
    }
    return null;
  }

  /**
   * Returns the URI of the given request.
   *
   * @param httpRequest    an HTTP servlet request
   * @return the URI, including the query string
   */
  public static String getRequestURI(final HttpServletRequest httpRequest) {
    final StringBuilder url = new StringBuilder(100).append(httpRequest.getRequestURI());
    if (httpRequest.getQueryString() != null) {
      url.append('?').append(httpRequest.getQueryString());
    }

    return url.toString();
  }

  /**
   * Returns the full URL of the given request.
   *
   * @param httpRequest    an HTTP servlet request
   * @return the full URL, including the query string
   */
  public static String getRequestURL(final HttpServletRequest httpRequest) {
    final StringBuilder url = new StringBuilder(100).append(httpRequest.getRequestURL());
    if (httpRequest.getQueryString() != null) {
      url.append('?').append(httpRequest.getQueryString());
    }

    return url.toString();
  }

  public static Response getErrorResponse(final AtlasBaseException e) {
    final String message = e.getMessage() == null ? "Failed with " + e.getClass().getName() : e.getMessage();
    final Response response = getErrorResponse(message, e.getAtlasErrorCode().getHttpCode());

    return response;
  }

  public static Response getErrorResponse(final Throwable e, final Response.Status status) {
    final String message = e.getMessage() == null ? "Failed with " + e.getClass().getName() : e.getMessage();
    final Response response = getErrorResponse(message, status);

    return response;
  }

  public static Response getErrorResponse(final String message, final Response.Status status) {
    final Object     errorEntity = escapeJsonString(message);
    final ObjectNode errorJson   = AtlasJson.createV1ObjectNode(AtlasClient.ERROR, errorEntity);

    return Response.status(status).entity(errorJson).type(JSON_MEDIA_TYPE).build();
  }

  public static String getRequestPayload(final HttpServletRequest request) throws IOException {
    //request is an instance of LocalServletRequest for calls from LocalAtlasClient
    if (request instanceof LocalServletRequest) {
      return ((LocalServletRequest) request).getPayload();
    }

    final StringWriter writer = new StringWriter();
    IOUtils.copy(request.getInputStream(), writer);
    return writer.toString();
  }

  public static String getRequestId() {
    return Thread.currentThread().getName();
  }

  public static String escapeJsonString(final String inputStr) {
    ParamChecker.notNull(inputStr, "Input String cannot be null");
    return StringEscapeUtils.escapeJson(inputStr);
  }

  public static String getHostName(final HttpServletRequest httpServletRequest) {
    return httpServletRequest.getLocalName();
  }

  public static String getUserName(final HttpServletRequest httpServletRequest) {
    return httpServletRequest.getRemoteUser();
  }

  public static Map<String, Object> getParameterMap(final HttpServletRequest request) {
    final Map<String, Object> attributes = new HashMap<>();

    if (MapUtils.isNotEmpty(request.getParameterMap())) {
      for (final Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
        final String key = e.getKey();

        if (key != null) {
          final String[] values = e.getValue();
          final String   value  = values != null && values.length > 0 ? values[0] : null;

          attributes.put(key, value);
        }
      }
    }

    return attributes;
  }

  public static void validateQueryParamLength(final String paramName, final String paramValue) throws AtlasBaseException {
    if (StringUtils.isNotEmpty(paramValue) && paramValue.length() > QUERY_PARAM_MAX_LENGTH) {
      throw new AtlasBaseException(AtlasErrorCode.INVALID_QUERY_PARAM_LENGTH, paramName);
    }
  }

  public static String decodeQueryString(final String query){
    return UriUtils.decode(query,"UTF-8");
  }
}
