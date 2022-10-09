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

import java.io.File;
import java.net.URI;

import org.eclipse.jetty.server.HttpConfiguration;
import org.jboss.arquillian.container.jetty.AbstractJettyEmbeddedConfiguration;

/**
 * A {@link org.jboss.arquillian.container.spi.client.container.ContainerConfiguration} implementation for the Jetty Embedded
 * containers.
 *
 * @author Dan Allen
 * @author Ales Justin
 */
public class JettyEmbeddedConfiguration extends AbstractJettyEmbeddedConfiguration {
    public enum ClassLoaderBehavior

    {
        /**
         * Default behavior for Java Spec (server classloader, then webapp). 
         *
         * Also the default for Arquillian.
         */
        JAVA_SPEC,
            /** Default behavior for Servlet Spec (webapp classloader, then server) */
            SERVLET_SPEC
    }

    /**
     * Classloader Search Order behavior.
     * <p>
     * Default for Arquillian is {@link JAVA_SPEC}.
     */
    private ClassLoaderBehavior classloaderBehavior = ClassLoaderBehavior.JAVA_SPEC;

    /**
     * Optional override for the default servlet spec descriptor
     */
    private URI defaultsDescriptor;

    /**
     * Dump, to System.err, the server state tree after the server has successfully started up.
     */
    private boolean dumpServerAfterStart = false;

    /**
     * Optional HttpConfiguration for the ServerConnector that Arquillian
     * creates.
     */
    private HttpConfiguration httpConfiguration;

    /**
     * Idle Timeout (in milliseconds) for active connections.
     * <p>
     * Default: 30,000ms
     */
    private long idleTimeoutMillis = 30000;

    /**
     * Base directory for all temp files that Jetty will manage.
     */
    private File tempDirectory;

    public ClassLoaderBehavior getClassloaderBehavior() {
        return classloaderBehavior;
    }

    public URI getDefaultsDescriptor() {
        return defaultsDescriptor;
    }

    public HttpConfiguration getHttpConfiguration() {
        return httpConfiguration;
    }

    public long getIdleTimeoutMillis() {
        return idleTimeoutMillis;
    }

    public File getTempDirectory() {
        return tempDirectory;
    }

    public boolean hasDefaultsDescriptor() {
        return (defaultsDescriptor != null);
    }

    public boolean isDumpServerAfterStart() {
        return dumpServerAfterStart;
    }

    public void setClassloaderBehavior(ClassLoaderBehavior classloaderBehavior) {
        this.classloaderBehavior = classloaderBehavior;
    }

    public void setDefaultsDescriptor(URI defaultsDescriptor) {
        this.defaultsDescriptor = defaultsDescriptor;
    }

    public void setDumpServerAfterStart(boolean serverDumpAfterStart) {
        this.dumpServerAfterStart = serverDumpAfterStart;
    }

    public void setHttpConfiguration(HttpConfiguration httpConfiguration) {
        this.httpConfiguration = httpConfiguration;
    }

    public void setIdleTimeoutMillis(long milliseconds) {
        this.idleTimeoutMillis = milliseconds;
    }

    public void setTempDirectory(File tempDirectory) {
        this.tempDirectory = tempDirectory;
    }
}
