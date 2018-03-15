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
package org.alfresco.web.config;

import java.util.Iterator;

import org.springframework.extensions.config.ConfigElement;
import org.springframework.extensions.config.ConfigException;
import org.springframework.extensions.config.xml.elementreader.ConfigElementReader;
import org.dom4j.Element;

/**
 * Custom element reader to parse config for the sidebar
 * 
 * @author gavinc
 */
public class SidebarElementReader implements ConfigElementReader
{
   public static final String ELEMENT_SIDEBAR = "sidebar";
   public static final String ELEMENT_PLUGINS = "plugins";
   public static final String ELEMENT_PLUGIN = "plugin";
   public static final String ELEMENT_DEFAULT_PLUGIN = "default-plugin";
   public static final String ATTR_ID = "id";
   public static final String ATTR_LABEL = "label";
   public static final String ATTR_LABEL_ID = "label-id";
   public static final String ATTR_DESCRIPTION = "description";
   public static final String ATTR_DESCRIPTION_ID = "description-id";
   public static final String ATTR_PAGE = "page";
   public static final String ATTR_ICON = "icon";
   public static final String ATTR_ACTIONS_CONFIG_ID = "actions-config-id";

   /**
    * @see org.springframework.extensions.config.xml.elementreader.ConfigElementReader#parse(org.dom4j.Element)
    */
   @SuppressWarnings("unchecked")
   public ConfigElement parse(Element element)
   {
      SidebarConfigElement configElement = null;
      
      if (element != null)
      {
         String elementName = element.getName();
         if (elementName.equals(ELEMENT_SIDEBAR) == false)
         {
            throw new ConfigException("SidebarElementReader can only parse " +
                  ELEMENT_SIDEBAR + "elements, the element passed was '" + 
                  elementName + "'");
         }
         
         configElement = new SidebarConfigElement();
         
         // go through the plugins that make up the sidebar
         Element pluginsElem = element.element(ELEMENT_PLUGINS);
         if (pluginsElem != null)
         {
            Iterator<Element> plugins = pluginsElem.elementIterator(ELEMENT_PLUGIN);
            while (plugins.hasNext())
            {
               Element plugin = plugins.next();
               
               String id = plugin.attributeValue(ATTR_ID);
               String page = plugin.attributeValue(ATTR_PAGE);
               String label = plugin.attributeValue(ATTR_LABEL);
               String labelId = plugin.attributeValue(ATTR_LABEL_ID);
               String description = plugin.attributeValue(ATTR_DESCRIPTION);
               String descriptionId = plugin.attributeValue(ATTR_DESCRIPTION_ID);
               String actionsConfigId = plugin.attributeValue(ATTR_ACTIONS_CONFIG_ID);
               String icon = plugin.attributeValue(ATTR_ICON);
               
               SidebarConfigElement.SidebarPluginConfig cfg = 
                     new SidebarConfigElement.SidebarPluginConfig(id, page, 
                           label, labelId, description, descriptionId, 
                           actionsConfigId, icon);
               
               configElement.addPlugin(cfg);
            }
         }
         
         // see if a default plugin is specified
         Element defaultPlugin = element.element(ELEMENT_DEFAULT_PLUGIN);
         if (defaultPlugin != null)
         {
            configElement.setDefaultPlugin(defaultPlugin.getTextTrim());
         }
      }
      
      return configElement;
   }
}
