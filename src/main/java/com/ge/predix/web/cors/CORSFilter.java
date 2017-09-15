/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.ge.predix.web.cors;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;

@Component
public class CORSFilter
        extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(CORSFilter.class);
	/**
     * A comma delimited list of regular expression patterns that defines which
     * URIs allow the "X-Requested-With" header in CORS requests.
     */
    @Value("#{'${cors.xhr.allowed.uris:^$}'.split(',')}")
    private List<String> corsXhrAllowedUris;

    private final List<Pattern> corsXhrAllowedUriPatterns = new ArrayList<>();

    /**
     * A comma delimited list of regular expression patterns that define which
     * origins are allowed to use the "X-Requested-With" header in CORS
     * requests.
     */
    @Value("#{'${cors.xhr.allowed.origins:^$}'.split(',')}")
    private List<String> corsXhrAllowedOrigins;

    private final List<Pattern> corsXhrAllowedOriginPatterns = new ArrayList<>();

    @Value("#{'${cors.xhr.allowed.headers:Accept,Authorization}'.split(',')}")
    private List<String> allowedHeaders;
    
    @Value(value = "${cors.xhr.controlmaxage:1728000}")
	private String maxAge;

    @Value("#{'${cors.xhr.allowed.methods:GET,OPTIONS}'.split(',')}")
	private List<String> allowedMethods;

    @PostConstruct
    public void initialize() {

        if (this.corsXhrAllowedUris != null) {
            for (String allowedUri : this.corsXhrAllowedUris) {
                this.corsXhrAllowedUriPatterns.add(Pattern.compile(allowedUri));
                if (LOG.isDebugEnabled()) {
                	LOG.debug(String
                        .format("URI '%s' allows 'X-Requested-With' header in CORS requests.", allowedUri));
                }
            }
        }

        if (this.corsXhrAllowedOrigins != null) {
            for (String allowedOrigin : this.corsXhrAllowedOrigins) {
                this.corsXhrAllowedOriginPatterns.add(Pattern.compile(allowedOrigin));
                if (LOG.isDebugEnabled()) {
                	LOG.debug(String.format("Origin '%s' allowed 'X-Requested-With' header in CORS requests.",
                        allowedOrigin));
                }
            }
        }
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        if (!isCrossOriginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isXhrRequest(request)) {
            String method = request.getMethod();
            if (!isCorsXhrAllowedMethod(method)) {
                response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
                return;
            }
            String origin = request.getHeader(HttpHeaders.ORIGIN);
            // Validate the origin so we don't reflect back any potentially dangerous content.
            URI originURI;
            try {
                originURI = new URI(origin);
            } catch (URISyntaxException e) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                return;
            }

            String requestUri = request.getRequestURI();
            if (!isCorsXhrAllowedRequestUri(requestUri) || !isCorsXhrAllowedOrigin(origin)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                return;
            }
            response.addHeader("Access-Control-Allow-Origin", originURI.toString());
            if ("OPTIONS".equals(request.getMethod())) {
                buildCorsXhrPreFlightResponse(request, response);
            } else {
                filterChain.doFilter(request, response);
            }
            return;
        }

        response.addHeader("Access-Control-Allow-Origin", "*");
        if (request.getHeader("Access-Control-Request-Method") != null && "OPTIONS".equals(request.getMethod())) {
            // CORS "pre-flight" request
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
            response.addHeader("Access-Control-Allow-Headers", "Authorization");
            response.addHeader("Access-Control-Max-Age", "1728000");
        } else {
            filterChain.doFilter(request, response);
        }
    }

    static boolean isXhrRequest(final HttpServletRequest request) {
        String xRequestedWith = request.getHeader("X-Requested-With");
        String accessControlRequestHeaders = request.getHeader("Access-Control-Request-Headers");
        return StringUtils.hasText(xRequestedWith)
                || (StringUtils.hasText(accessControlRequestHeaders) && containsHeader(
                        accessControlRequestHeaders, "X-Requested-With"));
    }

    private boolean isCrossOriginRequest(final HttpServletRequest request) {
        if (StringUtils.isEmpty(request.getHeader(HttpHeaders.ORIGIN))) {
            return false;
        } 
        return true;
    }

    void buildCorsXhrPreFlightResponse(final HttpServletRequest request, final HttpServletResponse response) {
        String accessControlRequestMethod = request.getHeader("Access-Control-Request-Method");
        if (null == accessControlRequestMethod) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }
        if (!"GET".equalsIgnoreCase(accessControlRequestMethod)) {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
            return;
        }
        response.addHeader("Access-Control-Allow-Methods", "GET");

        String accessControlRequestHeaders = request.getHeader("Access-Control-Request-Headers");
        if (null == accessControlRequestHeaders) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }
        if (!headersAllowed(accessControlRequestHeaders)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }
        response.addHeader("Access-Control-Allow-Headers", "Authorization, X-Requested-With");
        response.addHeader("Access-Control-Max-Age", this.maxAge);
    }

    private static boolean containsHeader(final String accessControlRequestHeaders, final String header) {
        List<String> headers = Arrays.asList(accessControlRequestHeaders.replace(" ", "").toLowerCase().split(","));
        return headers.contains(header.toLowerCase());
    }

    private boolean headersAllowed(final String accessControlRequestHeaders) {
        List<String> headers = Arrays.asList(accessControlRequestHeaders.replace(" ", "").split(","));
        for (String header : headers) {
            if (!"X-Requested-With".equalsIgnoreCase(header) && !this.allowedHeaders.contains(header)) {
                return false;
            }
        }
        return true;
    }

    private boolean isCorsXhrAllowedMethod(final String method) {
    	for (String allowedMethod : this.allowedMethods) {
            if (allowedMethod.equalsIgnoreCase(method)) {
                return true;
            }
        }
       return false;
    }

    private boolean isCorsXhrAllowedRequestUri(final String uri) {
        if (StringUtils.isEmpty(uri)) {
            return false;
        }

        for (Pattern pattern : this.corsXhrAllowedUriPatterns) {
            // Making sure that the pattern matches
            if (pattern.matcher(uri).find()) {
                return true;
            }
        }
        if (LOG.isDebugEnabled()) {
        	LOG.debug(String.format("The '%s' URI does not allow CORS requests with the 'X-Requested-With' header.",
                    uri));
        }
        return false;
    }

    private boolean isCorsXhrAllowedOrigin(final String origin) {
        for (Pattern pattern : this.corsXhrAllowedOriginPatterns) {
            // Making sure that the pattern matches
            if (pattern.matcher(origin).find()) {
                return true;
            }
        }
        if (LOG.isDebugEnabled()) {
        	LOG.debug(String.format(
                    "The '%s' origin is not allowed to make CORS requests with the 'X-Requested-With' header.",
                    origin));
        }
        return false;
    }

    public void setCorsXhrAllowedUris(final List<String> corsXhrAllowedUris) {
        this.corsXhrAllowedUris = corsXhrAllowedUris;
    }

    public void setCorsXhrAllowedOrigins(final List<String> corsXhrAllowedOrigins) {
        this.corsXhrAllowedOrigins = corsXhrAllowedOrigins;
    }

    public void setAllowedHeaders(final List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }
    
    public void setAllowedMethods(final List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }
    
    public void setMaxAge(final String maxAge) {
        this.maxAge = maxAge;
    }
}
