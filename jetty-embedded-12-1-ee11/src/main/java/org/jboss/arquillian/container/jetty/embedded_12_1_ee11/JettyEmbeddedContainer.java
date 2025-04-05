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
package org.jboss.arquillian.container.jetty.embedded_12_1_ee11;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.plexus.util.ReflectionUtils;
import org.eclipse.jetty.deploy.Deployer;
import org.eclipse.jetty.deploy.StandardDeployer;
import org.eclipse.jetty.ee11.cdi.CdiDecoratingListener;
import org.eclipse.jetty.ee11.cdi.CdiServletContainerInitializer;
import org.eclipse.jetty.http.CookieCompliance;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.ee11.servlet.ServletHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.jboss.arquillian.container.jetty.EnvUtil;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
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
    private Deployer deployer;
    private ArquillianAppProvider appProvider;

    private JettyEmbeddedConfiguration containerConfig;

    @Inject
    @ApplicationScoped
    private InstanceProducer<WebAppContext> webAppContextInstanceProducer;

    @Inject
    @ApplicationScoped
    private InstanceProducer<ServletContext> servletContextInstanceProducer;

    @Inject
    private Instance<ServiceLoader> serviceLoader;

    @Override
    public Class<JettyEmbeddedConfiguration> getConfigurationClass() {
        return JettyEmbeddedConfiguration.class;
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        // Jetty 9 is a Servlet 3.1 container.
        // However, Arquillian "Protocol" actual means "Packaging"
        return new ProtocolDescription("Servlet 5.0");
    }

    public void setup(JettyEmbeddedConfiguration containerConfig) {
        this.containerConfig = containerConfig;
    }

    @Override
    public void start() throws LifecycleException {
        EnvUtil.assertMinimumJettyVersion(Server.getVersion(), "12.1");

        try {
            server = new Server();

            HttpConfiguration httpConfig = getHttpConfiguration();
            httpConfig.setUseInputDirectByteBuffers(false);
            httpConfig.setUseOutputDirectByteBuffers(false);

            ConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);
            // Setup Connector
            ServerConnector connector;
            if (containerConfig.isSsl()) {
                SslContextFactory.Server sslContextFactory = getSslContextFactory();
                server.addBean(sslContextFactory);
                SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString());
                sslConnectionFactory.setEnsureSecureRequestCustomizer(containerConfig.isSniRequired());
                connector = new ServerConnector(server, sslConnectionFactory, connectionFactory);
            } else {
                connector = new ServerConnector(server, connectionFactory);
            }

            if (containerConfig.isH2cEnabled()) {
                HTTP2CServerConnectionFactory http2CServerConnectionFactory = new HTTP2CServerConnectionFactory(httpConfig);
                connector.addConnectionFactory(http2CServerConnectionFactory);
            }

            connector.setHost(containerConfig.getBindAddress());
            connector.setPort(containerConfig.getBindHttpPort());
            connector.setIdleTimeout(containerConfig.getIdleTimeoutMillis());
            server.setConnectors(new Connector[] {connector});

            // Handler Tree location for all webapps
            ContextHandlerCollection contexts = new ContextHandlerCollection();

            // Deployment Management
            deployer = new StandardDeployer(contexts);
            Collection<WebAppContextProcessor> webAppContextProcessors = serviceLoader.get().all(WebAppContextProcessor.class);
            appProvider = new ArquillianAppProvider(containerConfig, webAppContextProcessors);
            server.addBean(deployer);

            // Handler Collection
            server.setHandler(contexts);

            if (containerConfig.isRealmPropertiesFileSet()) {
                String realmName = getRealmName();
                HashLoginService hashUserRealm =
                    new HashLoginService(realmName,
                        ResourceFactory.of(server)
                            .newResource(Path.of(containerConfig.getRealmProperties().getAbsolutePath())));
                server.addBean(hashUserRealm);
            }

            if (containerConfig.areInferredEncodings()) {
                containerConfig.getInferredEncodings().forEach((s, s2) -> server.getMimeTypes().addInferred(s, s2));
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

    @Override
    public void stop() throws LifecycleException {
        try {
            log.info("Stopping Jetty Embedded Server [id:" + server.hashCode() + "]");
            server.stop();
        } catch (Exception e) {
            throw new LifecycleException("Could not stop container", e);
        }
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        deployer.undeploy(webAppContextInstanceProducer.get());
    }

    @Override
    public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException {
        try {
            //ContextHandler contextHandler = ;

            WebAppContext webAppContext = getWebAppContext(appProvider.createWebAppContext(archive));

            webAppContextInstanceProducer.set(webAppContext);

            // Jetty setup telling Jetty's CdiDecoratingListener how to operate.
            webAppContext.setInitParameter(CdiServletContainerInitializer.CDI_INTEGRATION_ATTRIBUTE, CdiDecoratingListener.MODE);
            //webAppContext.setInitParameter(CdiServletContainerInitializer.CDI_INTEGRATION_ATTRIBUTE, CdiDecoratingListener.MODE);

            // jetty setup for layer between Weld and Jetty.
            webAppContext.addServletContainerInitializer(new CdiServletContainerInitializer());
            webAppContext.setInitParameter(org.jboss.weld.Container.CONTEXT_ID_KEY, webAppContext.getContextPath());


            if (containerConfig.areMimeTypesSet()) {
                containerConfig.getMimeTypes().forEach((s, s2) -> webAppContext.getMimeTypes().addMimeMapping(s, s2));
            }

            servletContextInstanceProducer.set(webAppContext.getServletContext());
            webAppContextInstanceProducer.set(webAppContext);
            deployer.deploy(webAppContext);
            HTTPContext httpContext = new HTTPContext(listeningHost, listeningPort);
            ServletHandler servletHandler = webAppContext.getServletHandler();
            for (ServletHolder servlet : servletHandler.getServlets()) {
                if(servlet.getServletContext() == null) {
                    httpContext.add(new Servlet(servlet.getName(), "/"));
                } else {
                    httpContext.add(new Servlet(servlet.getName(), servlet.getServletContext().getContextPath()));
                }
            }

            return new ProtocolMetaData().addContext(httpContext);
        } catch (Exception e) {
            throw new DeploymentException("Could not deploy " + archive.getName(), e);
        }
    }

    private WebAppContext getWebAppContext(ContextHandler handler) throws Exception {
        WebAppContext webAppContext;
        if (handler instanceof WebAppContext) {
            webAppContext = (WebAppContext) handler;
        } else {
            throw new DeploymentException("Deployment of raw ContextHandler's not supported by Arquillian");
        }
        return webAppContext;
    }

    /**
     * Setup HTTP Configuration
     * @return HttpConfiguration
     */
    private HttpConfiguration getHttpConfiguration() {
        HttpConfiguration httpConfig = containerConfig.getHttpConfiguration();
        if (httpConfig == null) {
            httpConfig = new HttpConfiguration();
            if (this.containerConfig.isHeaderBufferSizeSet()) {
                httpConfig.setRequestHeaderSize(containerConfig.getHeaderBufferSize());
                httpConfig.setResponseHeaderSize(containerConfig.getHeaderBufferSize());
            }
            if(this.containerConfig.getRequestCookieCompliance()!=null) {
                httpConfig.setRequestCookieCompliance(CookieCompliance.from(containerConfig.getRequestCookieCompliance()));
            }
            if(this.containerConfig.getResponseCookieCompliance()!=null) {
                httpConfig.setResponseCookieCompliance(CookieCompliance.from(containerConfig.getResponseCookieCompliance()));
            }

            if(containerConfig.getHttpConfigurationProperties()!=null){
                for(Map.Entry<String, String> propertyEntry:containerConfig.getHttpConfigurationProperties().entrySet()){
                    Method setter = ReflectionUtils.getSetter(propertyEntry.getKey(), httpConfig.getClass());
                    Class<?> setterClass = ReflectionUtils.getSetterType(setter);
                    Object value = TypeUtil.valueOf(setterClass, propertyEntry.getValue());
                    try {
                        setter.invoke(httpConfig, value);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        log.log(Level.WARNING, "Ignore error setting field with name " + propertyEntry.getKey() + " with value " + propertyEntry.getValue(), e);
                    }
                }
            }

        }

        SecureRequestCustomizer secureRequestCustomizer = httpConfig.getCustomizer(SecureRequestCustomizer.class);
        if (secureRequestCustomizer == null) {
            secureRequestCustomizer = new SecureRequestCustomizer();
            httpConfig.addCustomizer(secureRequestCustomizer);
        }
        secureRequestCustomizer.setSniHostCheck(containerConfig.isSniHostCheck());
        secureRequestCustomizer.setSniRequired(containerConfig.isSniRequired());
        return httpConfig;
    }

    private SslContextFactory.Server getSslContextFactory() {
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        if (containerConfig.getKeystorePath() != null) {
            sslContextFactory.setKeyStorePath(new File(containerConfig.getKeystorePath()).getAbsolutePath());
        }
        if (containerConfig.getKeystorePassword() != null) {
            sslContextFactory.setKeyStorePassword(containerConfig.getKeystorePassword());
        }
        if(containerConfig.getTrustStorePath() != null) {
            sslContextFactory.setTrustStorePath(new File(containerConfig.getTrustStorePath()).getAbsolutePath());
        }
        if(containerConfig.getTrustStorePassword() != null) {
            sslContextFactory.setTrustStorePassword(containerConfig.getTrustStorePassword());
        }
        sslContextFactory.setNeedClientAuth(containerConfig.isNeedClientAuth());
        sslContextFactory.setSniRequired(containerConfig.isSniRequired());

        return sslContextFactory;
    }
}
