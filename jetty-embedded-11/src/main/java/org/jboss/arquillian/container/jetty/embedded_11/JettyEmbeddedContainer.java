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

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.AppLifeCycle;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.http.CookieCompliance;
import org.eclipse.jetty.http.MimeTypes;
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
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import jakarta.servlet.ServletContext;

/**
 * <p>
 * Jetty Embedded 11.x container for the Arquillian project.
 * </p>
 * <p>
 * <p>
 * This container only supports a WebArchive deployment. The context path of the deployed application is always set to
 * "/test", which is expected by the
 * Arquillian servlet protocol.
 * </p>
 * <p>
 * <p>
 * Another known issue is that the container configuration process logs an exception when running in-container. However,
 * the container is still configured
 * properly during setup.
 * </p>
 *
 * @author Dan Allen
 * @author Ales Justin
 * @author Martin Kouba
 */
public class JettyEmbeddedContainer implements DeployableContainer<JettyEmbeddedConfiguration> {
    private static final Logger log = Logger.getLogger(JettyEmbeddedContainer.class.getName());

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

    @Inject
    private Instance<ServiceLoader> serviceLoader;

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.arquillian.spi.client.container.DeployableContainer#getConfigurationClass()
     */
    public Class<JettyEmbeddedConfiguration> getConfigurationClass() {
        return JettyEmbeddedConfiguration.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.arquillian.spi.client.container.DeployableContainer#getDefaultProtocol()
     */
    public ProtocolDescription getDefaultProtocol() {
        // Jetty 9 is a Servlet 3.1 container.
        // However, Arquillian "Protocol" actual means "Packaging"
        return new ProtocolDescription("Servlet 5.0");
    }

    public void setup(JettyEmbeddedConfiguration containerConfig) {
        this.containerConfig = containerConfig;
    }

    public void start() throws LifecycleException {
        EnvUtil.assertMinimumJettyVersion(Server.getVersion(), "11.0");

        try {
            server = new Server();

            // Setup HTTP Configuration
            HttpConfiguration httpConfig = containerConfig.getHttpConfiguration();
            if (httpConfig == null) {
                httpConfig = new HttpConfiguration();
                if (this.containerConfig.isHeaderBufferSizeSet()) {
                    httpConfig.setRequestHeaderSize(containerConfig.getHeaderBufferSize());
                    httpConfig.setResponseHeaderSize(containerConfig.getHeaderBufferSize());
                }
                if(this.containerConfig.getRequestCookieCompliance()!=null) {
                    httpConfig.setRequestCookieCompliance(CookieCompliance.valueOf(containerConfig.getRequestCookieCompliance()));
                }
                if(this.containerConfig.getResponseCookieCompliance()!=null) {
                    httpConfig.setResponseCookieCompliance(CookieCompliance.valueOf(containerConfig.getResponseCookieCompliance()));
                }
            }

            ConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);
            // Setup Connector
            ServerConnector connector = new ServerConnector(server, connectionFactory);

            connector.setHost(containerConfig.getBindAddress());
            connector.setPort(containerConfig.getBindHttpPort());
            connector.setIdleTimeout(containerConfig.getIdleTimeoutMillis());
            server.setConnectors(new Connector[] {connector});

            // Handler Tree location for all webapps
            ContextHandlerCollection contexts = new ContextHandlerCollection();

            // Deployment Management
            deployer = new DeploymentManager();
            deployer.setContexts(contexts);
            Collection<WebAppContextProcessor> webAppContextProcessors = serviceLoader.get().all(WebAppContextProcessor.class);
            appProvider = new ArquillianAppProvider(containerConfig, webAppContextProcessors);
            deployer.addAppProvider(appProvider);
            server.addBean(deployer);

            // Handler Tree
            HandlerCollection handlers = new HandlerCollection();
            handlers.addHandler(contexts);
            handlers.addHandler(new DefaultHandler());
            server.setHandler(handlers);

            if (containerConfig.isRealmPropertiesFileSet()) {
                String realmName = getRealmName();
                HashLoginService hashUserRealm =
                    new HashLoginService(realmName, containerConfig.getRealmProperties().getAbsolutePath());
                server.addBean(hashUserRealm);
            }

            if (containerConfig.areInferredEncodings()) {
                containerConfig.getInferredEncodings().forEach((s, s2) -> MimeTypes.getInferredEncodings().put(s, s2));
            }

            server.setDumpAfterStart(containerConfig.isDumpServerAfterStart());
            log.info("Starting Jetty Embedded Server " + Server.getVersion() + " [id:" + server.hashCode() + "]");
            server.start();

            listeningHost = connector.getHost();
            if (listeningHost == null) {
                listeningHost = containerConfig.getBindAddress();
            }
            listeningPort = connector.getLocalPort();
        } catch (Exception e) {
            throw new LifecycleException("Could not start container", e);
        }
    }

    private String getRealmName() {
        File realmProperties = containerConfig.getRealmProperties();
        String fileName = realmProperties.getName();
        int index;
        if ((index = fileName.indexOf('.')) > -1) {
            fileName = fileName.substring(0, index);
        }
        return fileName;
    }

    public void stop() throws LifecycleException {
        try {
            log.info("Stopping Jetty Embedded Server [id:" + server.hashCode() + "]");
            server.stop();
        } catch (Exception e) {
            throw new LifecycleException("Could not stop container", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.arquillian.spi.client.container.DeployableContainer#deploy(org.jboss.shrinkwrap.descriptor.api.Descriptor)
     */
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.arquillian.spi.client.container.DeployableContainer#undeploy(org.jboss.shrinkwrap.descriptor.api.Descriptor)
     */
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException {
        try {
            App app = appProvider.createApp(archive);
            deployer.removeApp(app);
            WebAppContext webAppContext = getWebAppContext(app);

            if (containerConfig.areMimeTypesSet()) {
                MimeTypes mimeTypes = getMimeTypes();
                webAppContext.setMimeTypes(mimeTypes);
            }

            deployer.addApp(app);
            deployer.requestAppGoal(app, AppLifeCycle.STARTED);

            webAppContextProducer.set(app);
            servletContextInstanceProducer.set(webAppContext.getServletContext());

            HTTPContext httpContext = new HTTPContext(listeningHost, listeningPort);
            ServletHandler servletHandler = webAppContext.getServletHandler();
            for (ServletHolder servlet : servletHandler.getServlets()) {
                httpContext.add(new Servlet(servlet.getName(), servlet.getContextPath()));
            }
            return new ProtocolMetaData().addContext(httpContext);
        } catch (Exception e) {
            throw new DeploymentException("Could not deploy " + archive.getName(), e);
        }
    }

    private WebAppContext getWebAppContext(App app) throws Exception {
        ContextHandler handler = app.getContextHandler();
        WebAppContext webAppContext;
        if (handler instanceof WebAppContext) {
            webAppContext = (WebAppContext) handler;
        } else {
            throw new DeploymentException("Deployment of raw ContextHandler's not supported by Arquillian");
        }
        return webAppContext;
    }

    private MimeTypes getMimeTypes() {
        Map<String, String> configuredMimeTypes = containerConfig.getMimeTypes();
        Set<Map.Entry<String, String>> entries = configuredMimeTypes.entrySet();
        MimeTypes mimeTypes = new MimeTypes();
        entries.forEach(stringStringEntry ->
            mimeTypes.addMimeMapping(stringStringEntry.getKey(), stringStringEntry.getValue()));
        return mimeTypes;
    }

    public void undeploy(Archive<?> archive) throws DeploymentException {
        App app = webAppContextProducer.get();
        if (app != null) {
            deployer.requestAppGoal(app, AppLifeCycle.UNDEPLOYED);
        }
    }
}
