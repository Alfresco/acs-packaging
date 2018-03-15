/*
 * #%L
 * Alfresco Repository WAR Community
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.web.app.servlet.command;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Command Factory helper
 * 
 * @author Kevin Roast
 */
public final class CommandFactory
{
   private static Log logger = LogFactory.getLog(CommandFactory.class);
   
   private static CommandFactory instance = new CommandFactory();
   
   private static Map<String, Class> registry = new HashMap<String, Class>(16, 1.0f);
   
   /**
    * Private constructor - protect the singleton instance
    */
   private CommandFactory()
   {
   }
   
   /**
    * @return the singleton CommandFactory instance
    */
   public static CommandFactory getInstance()
   {
      return instance;
   }
   
   /**
    * Register a command name against an implementation
    * 
    * @param name       Unique name of the command
    * @param clazz      Class implementation of the command
    */
   public void registerCommand(String name, Class clazz)
   {
      registry.put(name, clazz);
   }
   
   /**
    * Create a command instance of the specified command name
    * 
    * @param name       Name of the command to create (must be registered)
    * 
    * @return the Command instance or null if not found
    */
   public Command createCommand(String name)
   {
      Command result = null;
      
      // lookup command by name in the registry
      Class clazz = registry.get(name);
      if (clazz != null)
      {
         try
         {
            Object obj = clazz.newInstance();
            if (obj instanceof Command)
            {
               result = (Command)obj;
            }
         }
         catch (Throwable err)
         {
            // return default if this occurs
            logger.warn("Unable to create workflow command instance '" + name +
                  "' with classname '" + clazz.getName() + "' due to error: " + err.getMessage());
         }
      }
      
      return result;
   }
}
