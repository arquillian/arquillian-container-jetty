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
package org.jboss.arquillian.container.jetty.embedded_7;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.JavaUtilLog;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jboss.arquillian.container.jetty.EnvUtil;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.jetty_7.api.ShrinkWrapWebAppContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * <p>
 * Jetty Embedded 7.x container for the Arquillian project.
 * </p>
 * 
 * <p>
 * This container only supports a WebArchive deployment. The context path of the deployed application is always set to "/test", which is expected by the
 * Arquillian servlet protocol.
 * </p>
 * 
 * <p>
 * Another known issue is that the container configuration process logs an exception when running in-container. However, the container is still configured
 * properly during setup.
 * </p>
 * 
 * @author Dan Allen
 * @author Ales Justin
 * @version $Revision: $
 */
public class JettyEmbeddedContainer implements DeployableContainer<JettyEmbeddedConfiguration>
{
    public static final String HTTP_PROTOCOL = "http";

    private static final Logger log = Logger.getLogger(JettyEmbeddedContainer.class.getName());

    static
    {
        // Make jetty's own logging use java.util.logging
        org.eclipse.jetty.util.log.Log.setLog(new JavaUtilLog());
    }

    private Server server;
    private String listeningHost;
    private int listeningPort;
    private ContextHandlerCollection contexts;
    private String[] webappConfigurationClasses = null;
    private JettyEmbeddedConfiguration containerConfig;

    @Inject
    @DeploymentScoped
    private InstanceProducer<WebAppContext> webAppContextProducer;

    public JettyEmbeddedContainer()
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.arquillian.spi.client.container.DeployableContainer#getConfigurationClass()
     */
    @Override
    public Class<JettyEmbeddedConfiguration> getConfigurationClass()
    {
        return JettyEmbeddedConfiguration.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.arquillian.spi.client.container.DeployableContainer#getDefaultProtocol()
     */
    @Override
    public ProtocolDescription getDefaultProtocol()
    {
        // Jetty 7.x is Servlet 2.5
        return new ProtocolDescription("Servlet 2.5");
    }

    @Override
    public void setup(JettyEmbeddedConfiguration containerConfig)
    {
        this.containerConfig = containerConfig;
    }

    @Override
    public void start() throws LifecycleException
    {
        EnvUtil.assertMinimumJettyVersion(Server.getVersion(),"7.0");

        try
        {
            this.webappConfigurationClasses = getWebAppConfigurationClasses();

            server = new Server();

            // Setup connector
            SelectChannelConnector connector = new SelectChannelConnector();
            if(this.containerConfig.isHeaderBufferSizeSet()) {
                connector.setRequestHeaderSize(containerConfig.getHeaderBufferSize());
                connector.setResponseHeaderSize(containerConfig.getHeaderBufferSize());
            }
            connector.setHost(containerConfig.getBindAddress());
            connector.setPort(containerConfig.getBindHttpPort());
            server.setConnectors(new Connector[] { connector });

            // Setup standard handler tree
            contexts = new ContextHandlerCollection();
            HandlerCollection handlers = new HandlerCollection();
            handlers.addHandler(contexts);
            handlers.addHandler(new DefaultHandler());
            server.setHandler(handlers);

            if(containerConfig.isRealmPropertiesFileSet())
            {
                String realmName = getRealmName();
                HashLoginService hashUserRealm = new HashLoginService(realmName, containerConfig.getRealmProperties().getAbsolutePath());
                server.addBean(hashUserRealm);
            }

            // Start Server
            log.info("Starting Jetty Embedded Server " + Server.getVersion() + " [id:" + server.hashCode() + "]");
            server.start();

            // Gather actual address:port in use (post-start)
            listeningHost = containerConfig.getBindAddress();
            if (connector.getHost() != null)
            {
                listeningHost = connector.getHost();
            }
            listeningPort = connector.getLocalPort();
        }
        catch (Exception e)
        {
            throw new LifecycleException("Could not start container",e);
        }
    }

    private String getRealmName() {
        File realmProperties = containerConfig.getRealmProperties();
        String fileName = realmProperties.getName();
        int index = -1;
        if((index = fileName.indexOf('.')) > -1)
        {
            fileName = fileName.substring(0, index);
        }
        return fileName;
    }

    @Override
    public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException
    {
        try
        {
            WebAppContext wctx = archive.as(ShrinkWrapWebAppContext.class);

            if (webappConfigurationClasses != null)
            {
                wctx.setConfigurationClasses(webappConfigurationClasses);
            }
            if (containerConfig.areMimeTypesSet())
            {
                MimeTypes mimeTypes = getMimeTypes();
                wctx.setMimeTypes(mimeTypes);
            }
            // possible configuration parameters
            wctx.setExtractWAR(true);
            wctx.setLogUrlOnStart(true);

            /*
             * ARQ-242 Without this set we result in failure on loading Configuration in container. ServiceLoader finds service file from AppClassLoader, tried
             * to load JettyContainerConfiguration from AppClassLoader as a ContainerConfiguration from WebAppClassContext. ClassCastException.
             */
            wctx.setParentLoaderPriority(true);

            contexts.addHandler(wctx);
            wctx.start();
            webAppContextProducer.set(wctx);

            HTTPContext httpContext = new HTTPContext(listeningHost,listeningPort);
            for (ServletHolder servlet : wctx.getServletHandler().getServlets())
            {
                httpContext.add(new Servlet(servlet.getName(),servlet.getContextPath()));
            }

            return new ProtocolMetaData().addContext(httpContext);

        }
        catch (Exception e)
        {
            throw new DeploymentException("Could not deploy " + archive.getName(),e);
        }
    }

    private MimeTypes getMimeTypes() {
        Map<String, String> configuredMimeTypes = containerConfig.getMimeTypes();
        Set<Map.Entry<String, String>> entries = configuredMimeTypes.entrySet();
        MimeTypes mimeTypes = new MimeTypes();
        for(Map.Entry<String, String> entry : entries)
        {
            mimeTypes.addMimeMapping(entry.getKey(), entry.getValue());
        }
        return mimeTypes;
    }

    private String[] getWebAppConfigurationClasses()
    {
        // If user has specified in Container Configuration, use it.
        String configuredConfigurationClasses = containerConfig.getConfigurationClasses();
        if ((configuredConfigurationClasses != null) && (configuredConfigurationClasses.trim().length() > 0))
        {
            return configuredConfigurationClasses.split(",");
        }

        // If user has specified plus, use it.
        if (containerConfig.isJettyPlus())
        {
            List<String> configs = new ArrayList<String>();

            //
            configs.add("org.eclipse.jetty.webapp.WebInfConfiguration");
            configs.add("org.eclipse.jetty.webapp.WebXmlConfiguration");
            configs.add("org.eclipse.jetty.webapp.MetaInfConfiguration");
            configs.add("org.eclipse.jetty.webapp.FragmentConfiguration");
            configs.add("org.eclipse.jetty.plus.webapp.EnvConfiguration");
            if (EnvUtil.classExists("org.eclipse.jetty.plus.webapp.PlusConfiguration"))
            {
                configs.add("org.eclipse.jetty.plus.webapp.PlusConfiguration");
            }
            else
            {
                log.warning("Using Jetty 7 buggy plus configuration");
                configs.add("org.eclipse.jetty.plus.webapp.Configuration");
            }

            if (containerConfig.isJettyAnnotations())
            {
                if (EnvUtil.classExists("org.eclipse.jetty.plus.annotations.AnnotationConfiguration"))
                {
                    configs.add("org.eclipse.jetty.plus.annotations.AnnotationConfiguration");
                }
                else
                {
                    log.warning(containerConfig.getClass().getName() + ".isJettyAnnotations == true, but jetty-annotations.jar not in classpath");
                }
            }

            configs.add("org.eclipse.jetty.webapp.JettyWebXmlConfiguration");

            if (EnvUtil.classExists("org.eclipse.jetty.webapp.TagLibConfiguration"))
            {
                configs.add("org.eclipse.jetty.webapp.TagLibConfiguration");
            }

            String classes[] = configs.toArray(new String[configs.size()]);
            return classes;
        }

        // Otherwise use default
        return null;
    }

    @Override
    public void stop() throws LifecycleException
    {
        try
        {
            log.info("Stopping Jetty Embedded Server [id:" + server.hashCode() + "]");
            server.stop();
        }
        catch (Exception e)
        {
            throw new LifecycleException("Could not stop container",e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.arquillian.spi.client.container.DeployableContainer#deploy(org.jboss.shrinkwrap.descriptor.api.Descriptor)
     */
    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.arquillian.spi.client.container.DeployableContainer#undeploy(org.jboss.shrinkwrap.descriptor.api.Descriptor)
     */
    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException
    {
        WebAppContext wctx = webAppContextProducer.get();
        if (wctx != null)
        {
            try
            {
                wctx.stop();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                log.severe("Could not stop context " + wctx.getContextPath() + ": " + e.getMessage());
            }
            contexts.removeHandler(wctx);
        }
    }
}
