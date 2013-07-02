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

import java.util.logging.Logger;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jboss.arquillian.container.jetty.JettyEmbeddedConfiguration;
import org.jboss.arquillian.container.jetty.VersionUtil;
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

/**
 * <p>Jetty Embedded 9.x container for the Arquillian project.</p>
 *
 * <p>This container only supports a WebArchive deployment. The context path of the
 * deployed application is always set to "/test", which is expected by the Arquillian
 * servlet protocol.</p>
 *
 * <p>Another known issue is that the container configuration process logs an exception when
 * running in-container. However, the container is still configured properly during setup.</p>
 *
 * @author Dan Allen
 * @author Ales Justin
 * @author Martin Kouba
 * @version $Revision: $
 */
public class JettyEmbeddedContainer implements DeployableContainer<JettyEmbeddedConfiguration>
{

   public static final String[] JETTY_CONFIGURATION_CLASSES =
   {
       "org.eclipse.jetty.webapp.WebInfConfiguration",
       "org.eclipse.jetty.webapp.WebXmlConfiguration",
       "org.eclipse.jetty.webapp.MetaInfConfiguration",
       "org.eclipse.jetty.webapp.FragmentConfiguration",
       "org.eclipse.jetty.webapp.JettyWebXmlConfiguration"
   };

   public static final String[] JETTY_PLUS_CONFIGURATION_CLASSES =
   {
       "org.eclipse.jetty.webapp.WebInfConfiguration",
       "org.eclipse.jetty.webapp.WebXmlConfiguration",
       "org.eclipse.jetty.webapp.MetaInfConfiguration",
       "org.eclipse.jetty.webapp.FragmentConfiguration",
       "org.eclipse.jetty.plus.webapp.EnvConfiguration",
       "org.eclipse.jetty.plus.webapp.PlusConfiguration",
       "org.eclipse.jetty.webapp.JettyWebXmlConfiguration"
   };

   private static final Logger log = Logger.getLogger(JettyEmbeddedContainer.class.getName());

   private Server server;

   private String[] configurationClasses = null;

   private JettyEmbeddedConfiguration containerConfig;

   @Inject @DeploymentScoped
   private InstanceProducer<WebAppContext> webAppContextProducer;

   /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.client.container.DeployableContainer#getConfigurationClass()
    */
   public Class<JettyEmbeddedConfiguration> getConfigurationClass()
   {
      return JettyEmbeddedConfiguration.class;
   }

   /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.client.container.DeployableContainer#getDefaultProtocol()
    */
   public ProtocolDescription getDefaultProtocol()
   {
      return new ProtocolDescription("Servlet 3.0");
   }

   public void setup(JettyEmbeddedConfiguration containerConfig)
   {
      this.containerConfig = containerConfig;
   }

   public void start() throws LifecycleException
   {
      try
      {
         if(!VersionUtil.isGraterThenOrEqual(Server.getVersion(), "9.0"))
         {
              throw new LifecycleException("Incompatible Jetty container version on the classpath: "+Server.getVersion());
         }

         String configuredConfigurationClasses = containerConfig.getConfigurationClasses();
         if (configuredConfigurationClasses != null && configuredConfigurationClasses.trim().length() > 0)
         {
            this.configurationClasses = configuredConfigurationClasses.split(",");
         }
         else
         {
            if(containerConfig.isJettyPlus()) {
                this.configurationClasses = JETTY_PLUS_CONFIGURATION_CLASSES;
            } else {
                this.configurationClasses = JETTY_CONFIGURATION_CLASSES;
            }
         }

         server = new Server();
         ServerConnector connector = new ServerConnector(server);
         connector.setHost(containerConfig.getBindAddress());
         connector.setPort(containerConfig.getBindHttpPort());
         server.setConnectors(new Connector[] { connector });
         server.setHandler(new HandlerCollection(true));
         log.info("Starting Jetty Embedded Server " + Server.getVersion() + " [id:" + server.hashCode() + "]");
         server.start();
      }
      catch (Exception e)
      {
         throw new LifecycleException("Could not start container", e);
      }
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
         throw new LifecycleException("Could not stop container", e);
      }
   }

   /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.client.container.DeployableContainer#deploy(org.jboss.shrinkwrap.descriptor.api.Descriptor)
    */
   public void deploy(Descriptor descriptor) throws DeploymentException
   {
      throw new UnsupportedOperationException("Not implemented");
   }

   /* (non-Javadoc)
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
         WebAppContext wctx = archive.as(ShrinkWrapWebAppContext.class);

         if(configurationClasses != null)
         {
            wctx.setConfigurationClasses(configurationClasses);
         }

         // possible configuration parameters
         wctx.setExtractWAR(true);
         wctx.setLogUrlOnStart(true);

         /*
          * ARQ-242 Without this set we result in failure on loading Configuration in container.
          * ServiceLoader finds service file from AppClassLoader, tried to load JettyContainerConfiguration from AppClassLoader
          * as a ContainerConfiguration from WebAppClassContext. ClassCastException.
          */
         wctx.setParentLoaderPriority(true);

         ((HandlerCollection) server.getHandler()).addHandler(wctx);
         wctx.start();
         webAppContextProducer.set(wctx);

         HTTPContext httpContext = new HTTPContext(containerConfig.getBindAddress(), containerConfig.getBindHttpPort());
         for(ServletHolder servlet : wctx.getServletHandler().getServlets())
         {
            httpContext.add(new Servlet(servlet.getName(), servlet.getContextPath()));
         }

         return new ProtocolMetaData()
            .addContext(httpContext);

      }
      catch (Exception e)
      {
         throw new DeploymentException("Could not deploy " + archive.getName(), e);
      }
   }

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
         ((HandlerCollection) server.getHandler()).removeHandler(wctx);
      }
   }

}
