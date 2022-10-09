/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.jetty.embedded_12_ee10;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.webapp30.WebAppDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.servlet.ServletContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Client test case for the Jetty Embedded 12 container
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @author Dan Allen
 */
@ExtendWith(ArquillianExtension.class)
public class JettyEmbeddedClientTestCase {
    /**
     * Deployment for the test
     */
    @Deployment(testable = false)
    public static WebArchive getTestArchive() {
        return ShrinkWrap.create(WebArchive.class, "client-http.war")
            .addClass(MyServlet.class)
            .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class)
                .version("4.0")
                .createServlet()
                .servletClass(MyServlet.class.getName())
                .servletName("MyServlet").up()
                .createServletMapping()
                .servletName("MyServlet")
                .urlPattern(MyServlet.URL_PATTERN).up()
                .exportAsString()));
    }

    /**
     * Deployment for the test
     */
    @Deployment(testable = false, name = "webapp-https") @TargetsContainer("https")
    public static WebArchive getTestArchiveHttps() {
        return ShrinkWrap.create(WebArchive.class, "client-https.war")
            .addClass(MyServlet.class)
            .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class)
                .version("4.0")
                .createServlet()
                .servletClass(MyServlet.class.getName())
                .servletName("MyServlet").up()
                .createServletMapping()
                .servletName("MyServlet")
                .urlPattern(MyServlet.URL_PATTERN).up()
                .exportAsString()));
    }

    @ArquillianResource
    ServletContext servletContext;

    @ArquillianResource URL url;

    @ArquillianResource @OperateOnDeployment("webapp-https") URL urlHttps;

    private HttpClient httpClient;
    private final SslContextFactory.Client clientSslContextFactory = new SslContextFactory.Client();

    @BeforeEach
    public void setup() throws Exception {
        clientSslContextFactory.setTrustAll(true);
        ClientConnector clientConnector = new ClientConnector();
        clientConnector.setSelectors(1);
        clientConnector.setSslContextFactory(clientSslContextFactory);
        httpClient = new HttpClient(new HttpClientTransportOverHTTP(clientConnector));
        httpClient.start();
    }

    @AfterEach
    public void shutdown() throws Exception {
        if(httpClient.isRunning()) {
            httpClient.stop();
        }
    }

    @Test
    public void shouldBeAbleToInvokeServletInDeployedWebApp() throws Exception {

        String body = httpClient.GET(new URL(url, MyServlet.URL_PATTERN).toURI()).getContentAsString();

        assertThat(
            "Verify that the servlet was deployed and returns expected result",
            body,
            Matchers.is(MyServlet.MESSAGE));
    }

    @Test
    public void shouldBeAbleToInvokeServletInDeployedWebAppHttps() throws Exception {
        URL url = new URL("https", urlHttps.getHost(), urlHttps.getPort(), urlHttps.getPath() + MyServlet.URL_PATTERN);
        String body = httpClient.GET(url.toURI()).getContentAsString();

        assertThat(
            "Verify that the servlet was deployed and returns expected result",
            body,
            Matchers.is(MyServlet.MESSAGE));
    }

    @Test
    public void shouldEnrichTestWithServletContext() {
        assertThat(servletContext, notNullValue());
    }

    public static String readAllAndClose(InputStream is) throws Exception {
        try (is;ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int read;
            while ((read = is.read()) != -1) {
                out.write(read);
            }
            return out.toString();
        }
    }
}
