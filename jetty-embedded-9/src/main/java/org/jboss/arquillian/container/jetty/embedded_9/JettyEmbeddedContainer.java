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
package org.jboss.arquillian.container.jetty.embedded_9;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.AppLifeCycle;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.JavaUtilLog;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
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
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import javax.servlet.ServletContext;

/**
 * <p>
 * Jetty Embedded 9.x container for the Arquillian project.
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
 * @author Martin Kouba
 * @version $Revision: $
 */
public class JettyEmbeddedContainer implements DeployableContainer<JettyEmbeddedConfiguration>
{
    private static final Logger log = Logger.getLogger(JettyEmbeddedContainer.class.getName());

    static
    {
        // Make jetty's own logging use java.util.logging
        org.eclipse.jetty.util.log.Log.setLog(new JavaUtilLog());
    }

    private Server server;
    private String listeningHost;
    private int listeningPort;
    private DeploymentManager deployer;
    private ArquillianAppProvider appProvider;

    private JettyEmbeddedConfiguration containerConfig;

    @Inject
    @DeploymentScoped
    private InstanceProducer<App> webAppContextProducer;

    @Inject
    @ApplicationScoped
    private InstanceProducer<ServletContext> servletContextInstanceProducer;

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.arquillian.spi.client.container.DeployableContainer#getConfigurationClass()
     */
    public Class<JettyEmbeddedConfiguration> getConfigurationClass()
    {
        return JettyEmbeddedConfiguration.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.arquillian.spi.client.container.DeployableContainer#getDefaultProtocol()
     */
    public ProtocolDescription getDefaultProtocol()
    {
        // Jetty 9 is a Servlet 3.1 container.
        // However, Arquillian "Protocol" actuall means "Packaging"
        // TODO: Fix to servlet 3.1 (when available in arquillian)
        return new ProtocolDescription("Servlet 3.0");
    }

    public void setup(JettyEmbeddedConfiguration containerConfig)
    {
        this.containerConfig = containerConfig;
    }

    public void start() throws LifecycleException
    {
        EnvUtil.assertMinimumJettyVersion(Server.getVersion(),"9.0");

        try
        {
            server = new Server();

            // Use default configuration classes at the server level
            ClassList serverConf = ClassList.serverDefault(server);

            String configuredConfigurationClasses = containerConfig.getConfigurationClasses();
            if (configuredConfigurationClasses != null && configuredConfigurationClasses.trim().length() > 0)
            {
                // User provided classlist, use it as-is.
                serverConf.clear();
                for (String configClass : configuredConfigurationClasses.split(","))
                {
                    serverConf.add(configClass);
                }
            }
            else
            {
                // Arquillian assumption is that all features of Servlet 3.1 are available.
                // This means that annotation scanning is enabled by default.
                // That means jetty-plus is mandatory.

                // Applying equivalent of etc/jetty-annotations.xml
                serverConf.addBefore(JettyWebXmlConfiguration.class.getName(),
                        AnnotationConfiguration.class.getName());

                // Applying equivalent of etc/jetty-plus.xml
                serverConf.addAfter(FragmentConfiguration.class.getName()
                        ,EnvConfiguration.class.getName(),
                        PlusConfiguration.class.getName());
            }

            // Setup HTTP Configuration
            HttpConfiguration httpConfig = containerConfig.getHttpConfiguration();
            if (httpConfig == null)
            {
                httpConfig = new HttpConfiguration();
                if(this.containerConfig.isHeaderBufferSizeSet()) {
                    httpConfig.setRequestHeaderSize(containerConfig.getHeaderBufferSize());
                    httpConfig.setResponseHeaderSize(containerConfig.getHeaderBufferSize());
                }
            }

            ConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);
            // Setup Connector
            ServerConnector connector = new ServerConnector(server,connectionFactory);

            connector.setHost(containerConfig.getBindAddress());
            connector.setPort(containerConfig.getBindHttpPort());
            connector.setIdleTimeout(containerConfig.getIdleTimeoutMillis());
            server.setConnectors(new Connector[] { connector });

            // Handler Tree location for all webapps
            ContextHandlerCollection contexts = new ContextHandlerCollection();

            // Deployment Management
            deployer = new DeploymentManager();
            deployer.setContexts(contexts);
            appProvider = new ArquillianAppProvider(containerConfig);
            deployer.addAppProvider(appProvider);
            server.addBean(deployer);

            // Handler Tree
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

            server.setDumpAfterStart(containerConfig.isDumpServerAfterStart());
            log.info("Starting Jetty Embedded Server " + Server.getVersion() + " [id:" + server.hashCode() + "]");
            server.start();

            listeningHost = connector.getHost();
            if (listeningHost == null)
            {
                listeningHost = containerConfig.getBindAddress();
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
    public void deploy(Descriptor descriptor) throws DeploymentException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.arquillian.spi.client.container.DeployableContainer#undeploy(org.jboss.shrinkwrap.descriptor.api.Descriptor)
     */
    public void undeploy(Descriptor descriptor) throws DeploymentException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException
    {
        try
        {
            App app = appProvider.createApp(archive);

            WebAppContext webAppContext = getWebAppContext(app);

            if (containerConfig.areMimeTypesSet())
            {
                MimeTypes mimeTypes = getMimeTypes();
                webAppContext.setMimeTypes(mimeTypes);
            }

            deployer.addApp(app);
            deployer.requestAppGoal(app,AppLifeCycle.STARTED);

            webAppContextProducer.set(app);
            servletContextInstanceProducer.set(webAppContext.getServletContext());

            HTTPContext httpContext = new HTTPContext(listeningHost,listeningPort);
            ServletHandler servletHandler = webAppContext.getServletHandler();
            for (ServletHolder servlet : servletHandler.getServlets())
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

    private WebAppContext getWebAppContext(App app) throws Exception {
        ContextHandler handler = app.getContextHandler();
        WebAppContext webAppContext = null;
        if (handler instanceof WebAppContext)
        {
            webAppContext = (WebAppContext)handler;
        }
        else
        {
            throw new DeploymentException("Deployment of raw ContextHandler's not supported by Arquillian");
        }
        return webAppContext;
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

    public void undeploy(Archive<?> archive) throws DeploymentException
    {
        App app = webAppContextProducer.get();
        if (app != null)
        {
            deployer.requestAppGoal(app,AppLifeCycle.UNDEPLOYED);
        }
    }
}
