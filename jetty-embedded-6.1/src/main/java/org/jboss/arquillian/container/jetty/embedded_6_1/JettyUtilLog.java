/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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
package org.jboss.arquillian.container.jetty.embedded_6_1;

import java.util.logging.Level;

import org.mortbay.log.Logger;

/**
 * java.util.logging for Jetty 6.x
 */
public class JettyUtilLog implements Logger
{
    private Level configuredLevel;
    private java.util.logging.Logger _logger;

    public JettyUtilLog()
    {
        this("org.mortbay.log");
    }

    public JettyUtilLog(String name)
    {
        _logger = java.util.logging.Logger.getLogger(name);
        configuredLevel = _logger.getLevel();
    }

    public void debug(String msg, Object arg0, Object arg1)
    {
        if (_logger.isLoggable(Level.FINE))
        {
            _logger.log(Level.FINE,format(msg,new Object[]{arg0,arg1}));
        }
    }

    public void debug(String msg, Throwable thrown)
    {
        _logger.log(Level.FINE,msg,thrown);
    }

    private String format(String msg, Object arg0, Object arg1)
    {
        return format(msg, new Object[]{arg0,arg1});
    }

    private String format(String msg, Object[] args)
    {
        msg = String.valueOf(msg); // Avoids NPE
        String braces = "{}";
        StringBuilder builder = new StringBuilder();
        int start = 0;
        for (Object arg : args)
        {
            int bracesIndex = msg.indexOf(braces,start);
            if (bracesIndex < 0)
            {
                builder.append(msg.substring(start));
                builder.append(" ");
                builder.append(arg);
                start = msg.length();
            }
            else
            {
                builder.append(msg.substring(start,bracesIndex));
                builder.append(String.valueOf(arg));
                start = bracesIndex + braces.length();
            }
        }
        builder.append(msg.substring(start));
        return builder.toString();
    }

    public Logger getLogger(String name)
    {
        return newLogger(name);
    }

    public String getName()
    {
        return _logger.getName();
    }

    public void info(String msg, Object arg0, Object arg1)
    {
        if (_logger.isLoggable(Level.INFO))
        {
            _logger.log(Level.INFO,format(msg,arg0,arg1));
        }
    }

    public boolean isDebugEnabled()
    {
        return _logger.isLoggable(Level.FINE);
    }

    /**
     * Create a Child Logger of this Logger.
     */
    protected Logger newLogger(String fullname)
    {
        return new JettyUtilLog(fullname);
    }

    public void setDebugEnabled(boolean enabled)
    {
        if (enabled)
        {
            configuredLevel = _logger.getLevel();
            _logger.setLevel(Level.FINE);
        }
        else
        {
            _logger.setLevel(configuredLevel);
        }
    }

    public void warn(String msg, Object arg0, Object arg1)
    {
        if (_logger.isLoggable(Level.WARNING))
        {
            _logger.log(Level.WARNING,format(msg,arg0,arg1));
        }
    }

    public void warn(String msg, Throwable thrown)
    {
        _logger.log(Level.WARNING,msg,thrown);
    }
}
