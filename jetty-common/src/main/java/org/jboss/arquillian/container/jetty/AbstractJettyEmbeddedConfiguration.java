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
package org.jboss.arquillian.container.jetty;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link org.jboss.arquillian.spi.client.container.ContainerConfiguration} common base for the Jetty Embedded containers
 * 
 * @author Dan Allen
 * @author Ales Justin
 * @author Alex Soto
 * @version $Revision: $
 */
public abstract class AbstractJettyEmbeddedConfiguration implements ContainerConfiguration
{
    private String bindAddress = "localhost";

    private int bindHttpPort = 9090;

    private Map<String, String> mimeTypes;

    private int headerBufferSize = 0;

    private File realmProperties;

    /**
     * List of server configuration classes that can be used for
     * establishing the configuration tasks for the WebApp being deployed.
     */
    private String configurationClasses;

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.arquillian.spi.client.container.ContainerConfiguration#validate()
     */
    public void validate() throws ConfigurationException
    {
        if(this.realmProperties != null) {
            if(!this.realmProperties.exists()) {
                throw new ConfigurationException(String.format("Realm properties file %s must exists.", this.realmProperties.getAbsolutePath()));
            }
            if(this.realmProperties.isDirectory()) {
                throw new ConfigurationException("Realm Properties should be a file and not a directory");
            }
        }
    }

    public int getBindHttpPort()
    {
        return bindHttpPort;
    }

    public void setBindHttpPort(int bindHttpPort)
    {
        this.bindHttpPort = bindHttpPort;
    }

    public String getBindAddress()
    {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress)
    {
        this.bindAddress = bindAddress;
    }

    public String getConfigurationClasses()
    {
        return configurationClasses;
    }

    /**
     * @param configurationClasses
     *            A comma separated list of fully qualified configuration classes
     */
    public void setConfigurationClasses(String configurationClasses)
    {
        this.configurationClasses = configurationClasses;
    }

    public int getHeaderBufferSize()
    {
        return this.headerBufferSize;
    }

    public boolean isHeaderBufferSizeSet() {
        return this.headerBufferSize > 0;
    }

    public void setHeaderBufferSize(int headerBufferSize)
    {
        this.headerBufferSize = headerBufferSize;
    }

    public void setRealmProperties(String realmProperties)
    {
        this.realmProperties = new File(realmProperties);
    }

    public boolean isRealmPropertiesFileSet()
    {
        return this.realmProperties != null;
    }

    public File getRealmProperties()
    {
        return realmProperties;
    }

    public void setMimeTypes(String mimeTypes) {
        this.mimeTypes = new HashMap<String, String>();
        String[] splittedLines = mimeTypes.split(" ");
        for(int i = 0; i < splittedLines.length; i+=2)
        {
            if(i+1 >= splittedLines.length)
            {
                throw new ConfigurationException(String.format("Mime Type definition should follow the format <extension> <type>[ <extension> <type>]*, for example js application/javascript but %s definition has been found.", mimeTypes));
            }
            this.mimeTypes.put(splittedLines[i], splittedLines[i+1]);
        }
    }

    public boolean areMimeTypesSet() {
        return this.mimeTypes != null;
    }

    public Map<String, String> getMimeTypes() {
        return mimeTypes;
    }
}
