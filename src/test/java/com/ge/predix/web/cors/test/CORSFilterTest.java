/*******************************************************************************
 * Copyright 2021 General Electric Company
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

package com.ge.predix.web.cors.test;


import static org.mockito.internal.util.reflection.Whitebox.getInternalState;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.annotations.Test;
import org.testng.Assert;

import com.ge.predix.web.cors.CORSFilter;

@Test
public class CORSFilterTest {

    @Test
    public void testRequestExpectStandardCorsResponse() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uaa/userinfo");
        request.addHeader("Origin", "example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        Assert.assertEquals("*", response.getHeaderValue("Access-Control-Allow-Origin"));
    }

    @Test
    public void testRequestWithMaliciousOrigin() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uaa/userinfo");
        request.addHeader("Origin", "<script>alert('1ee7 h@x0r')</script>");
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void testRequestExpectXhrCorsResponse() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uaa/userinfo");
        request.addHeader("Origin", "example.com");
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        Assert.assertEquals("example.com", response.getHeaderValue("Access-Control-Allow-Origin"));
    }

    @Test
    public void testSameOriginRequest() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uaa/userinfo");
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testRequestWithForbiddenOrigin() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uaa/userinfo");
        request.addHeader("Origin", "bunnyoutlet.com");
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void testRequestWithForbiddenUri() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uaa/login");
        request.addHeader("Origin", "example.com");
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void testRequestWithMethodNotAllowed() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/uaa/userinfo");
        request.addHeader("Origin", "example.com");
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        Assert.assertEquals(405, response.getStatus());
    }

    @Test
    public void testPreFlightExpectStandardCorsResponse() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/uaa/userinfo");
        request.addHeader("Access-Control-Request-Headers", "Authorization");
        request.addHeader("Access-Control-Request-Method", "GET");
        request.addHeader("Origin", "example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        assertStandardCorsPreFlightResponse(response);
    }

    @Test
    public void testPreFlightExpectXhrCorsResponse() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/uaa/userinfo");
        request.addHeader("Access-Control-Request-Headers", "Authorization, X-Requested-With");
        request.addHeader("Access-Control-Request-Method", "GET");
        request.addHeader("Origin", "example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        assertXhrCorsPreFlightResponse(response);
    }

    @Test
    public void testPreFlightWrongOriginSpecified() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/uaa/userinfo");
        request.addHeader("Access-Control-Request-Headers", "Authorization, X-Requested-With");
        request.addHeader("Access-Control-Request-Method", "GET");
        request.addHeader("Origin", "bunnyoutlet.com");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void testPreFlightRequestNoRequestMethod() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/uaa/userinfo");
        request.addHeader("Access-Control-Request-Headers", "Authorization, X-Requested-With");
        request.addHeader("Origin", "example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        Assert.assertEquals("example.com", response.getHeaderValue("Access-Control-Allow-Origin"));
    }

    @Test
    public void testPreFlightRequestMethodNotAllowed() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/uaa/userinfo");
        request.addHeader("Access-Control-Request-Headers", "Authorization, X-Requested-With");
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Origin", "example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        Assert.assertEquals(405, response.getStatus());
    }

    @Test
    public void testPreFlightRequestHeaderNotAllowed() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/uaa/userinfo");
        request.addHeader("Access-Control-Request-Headers", "Authorization, X-Requested-With, X-Not-Allowed");
        request.addHeader("Access-Control-Request-Method", "GET");
        request.addHeader("Origin", "example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void testPreFlightRequestUriNotWhitelisted() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/uaa/login");
        request.addHeader("Access-Control-Request-Method", "GET");
        request.addHeader("Access-Control-Request-Headers", "X-Requested-With");
        request.addHeader("Origin", "example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void testPreFlightOriginNotWhitelisted() throws ServletException, IOException {
        CORSFilter corsFilter = createConfiguredCORSFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/uaa/userinfo");
        request.addHeader("Access-Control-Request-Method", "GET");
        request.addHeader("Access-Control-Request-Headers", "X-Requested-With");
        request.addHeader("Origin", "bunnyoutlet.com");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void doInitializeWithNoPropertiesSet() throws ServletException, IOException {

        CORSFilter corsFilter = new CORSFilter();

        // We need to set the default value that Spring would otherwise set.
        List<String> allowedUris = new ArrayList<String>(Arrays.asList(new String[] { "^$" }));
        setInternalState(corsFilter, "corsXhrAllowedUris", allowedUris);

        // We need to set the default value that Spring would otherwise set.
        List<String> allowedOrigins = new ArrayList<String>(Arrays.asList(new String[] { "^$" }));
        setInternalState(corsFilter, "corsXhrAllowedOrigins", allowedOrigins);

        corsFilter.initialize();

        @SuppressWarnings("unchecked")
        List<Pattern> allowedUriPatterns = (List<Pattern>) getInternalState(corsFilter, "corsXhrAllowedUriPatterns");
        Assert.assertEquals(1, allowedUriPatterns.size());

        @SuppressWarnings("unchecked")
        List<Pattern> allowedOriginPatterns =
                (List<Pattern>) getInternalState(corsFilter, "corsXhrAllowedOriginPatterns");
        Assert.assertEquals(1, allowedOriginPatterns.size());

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/uaa/userinfo");
        request.addHeader("Access-Control-Request-Method", "GET");
        request.addHeader("Origin", "example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = newMockFilterChain();

        corsFilter.doFilter(request, response, filterChain);

        assertStandardCorsPreFlightResponse(response);
    }

    @Test(expectedExceptions = PatternSyntaxException.class)
    public void doInitializeWithInvalidUriRegex() {

        CORSFilter corsFilter = new CORSFilter();

        List<String> allowedUris =
                new ArrayList<String>(Arrays.asList(new String[] { "^/uaa/userinfo(", "^/uaa/logout.do$" }));
        setInternalState(corsFilter, "corsXhrAllowedUris", allowedUris);

        List<String> allowedOrigins = new ArrayList<String>(Arrays.asList(new String[] { "example.com$" }));
        setInternalState(corsFilter, "corsXhrAllowedOrigins", allowedOrigins);

        corsFilter.initialize();

    }

    @Test(expectedExceptions = PatternSyntaxException.class)
    public void doInitializeWithInvalidOriginRegex() {

        CORSFilter corsFilter = new CORSFilter();

        List<String> allowedUris =
                new ArrayList<String>(Arrays.asList(new String[] { "^/uaa/userinfo$", "^/uaa/logout.do$" }));
        setInternalState(corsFilter, "corsXhrAllowedUris", allowedUris);

        List<String> allowedOrigins = new ArrayList<String>(Arrays.asList(new String[] { "example.com(" }));
        setInternalState(corsFilter, "corsXhrAllowedOrigins", allowedOrigins);

        corsFilter.initialize();

    }

    private static CORSFilter createConfiguredCORSFilter() {
        CORSFilter corsFilter = new CORSFilter();

        List<String> allowedUris =
                new ArrayList<String>(Arrays.asList(new String[] { "^/uaa/userinfo$", "^/uaa/logout\\.do$" }));
        setInternalState(corsFilter, "corsXhrAllowedUris", allowedUris);

        List<String> allowedOrigins = new ArrayList<String>(Arrays.asList(new String[] { "example.com$" }));
        setInternalState(corsFilter, "corsXhrAllowedOrigins", allowedOrigins);

        List<String> allowedHeaders = Arrays.asList(new String[] {"Accept", "Authorization"});
        corsFilter.setAllowedHeaders(allowedHeaders);
        
        List<String> allowedMethods = Arrays.asList(new String[] {"GET", "OPTIONS"});
        corsFilter.setAllowedMethods(allowedMethods);
        
        Long maxAge = 1728000L;
        corsFilter.setMaxAge(maxAge.toString());

        corsFilter.initialize();
        return corsFilter;
    }

    private static void assertStandardCorsPreFlightResponse(final MockHttpServletResponse response) {
        Assert.assertEquals("*", response.getHeaderValue("Access-Control-Allow-Origin"));
        Assert.assertEquals("GET, POST, PUT, DELETE", response.getHeaderValue("Access-Control-Allow-Methods"));
        Assert.assertEquals("Authorization", response.getHeaderValue("Access-Control-Allow-Headers"));
        Assert.assertEquals("1728000", response.getHeaderValue("Access-Control-Max-Age"));
    }

    private static void assertXhrCorsPreFlightResponse(final MockHttpServletResponse response) {
        Assert.assertEquals("example.com", response.getHeaderValue("Access-Control-Allow-Origin"));
        Assert.assertEquals("GET", response.getHeaderValue("Access-Control-Allow-Methods"));
        Assert.assertEquals("Authorization, X-Requested-With", response.getHeaderValue("Access-Control-Allow-Headers"));
        Assert.assertEquals("1728000", response.getHeaderValue("Access-Control-Max-Age"));
    }

    private static FilterChain newMockFilterChain() {
        FilterChain filterChain = new FilterChain() {

            @Override
            public void doFilter(final ServletRequest request, final ServletResponse response)
                    throws IOException,
                    ServletException {
                // Do nothing.
            }
        };
        return filterChain;
    }
}
