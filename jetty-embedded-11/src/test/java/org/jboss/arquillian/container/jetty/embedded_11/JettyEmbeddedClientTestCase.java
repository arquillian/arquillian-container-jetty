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
package org.jboss.arquillian.container.jetty.embedded_11;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.webapp30.WebAppDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.servlet.ServletContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Client test case for the Jetty Embedded 9 container
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
        return ShrinkWrap.create(WebArchive.class, "client-test.war")
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

    @Test
    public void shouldBeAbleToInvokeServletInDeployedWebApp() throws Exception {
        String body = readAllAndClose(new URL(url, MyServlet.URL_PATTERN).openStream());

        assertThat(
            "Verify that the servlet was deployed and returns expected result",
            body,
            is(MyServlet.MESSAGE));
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
