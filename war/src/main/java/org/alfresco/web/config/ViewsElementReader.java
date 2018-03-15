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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

/**
 * Custom element reader to parse config for client views
 * 
 * @author Gavin Cornwell
 */
public class ViewsElementReader implements ConfigElementReader
{
   public static final String ELEMENT_VIEW = "view";
   public static final String ELEMENT_VIEWIMPL = "view-impl";
   public static final String ELEMENT_VIEWDEFAULTS = "view-defaults";
   public static final String ELEMENT_PAGESIZE = "page-size";
   public static final String ELEMENT_SORTCOLUMN = "sort-column";
   public static final String ELEMENT_SORTDIRECTION = "sort-direction";
   
   private static Log logger = LogFactory.getLog(ViewsElementReader.class);
   
   /**
    * @see org.springframework.extensions.config.xml.elementreader.ConfigElementReader#parse(org.dom4j.Element)
    */
   @SuppressWarnings("unchecked")
   public ConfigElement parse(Element element)
   {
      ViewsConfigElement configElement = null;
      
      if (element != null)
      {
         String name = element.getName();
         if (name.equals(ViewsConfigElement.CONFIG_ELEMENT_ID) == false)
         {
            throw new ConfigException("ViewsElementReader can only parse " +
                  ViewsConfigElement.CONFIG_ELEMENT_ID + " elements, the element passed was '" + 
                  name + "'");
         }
         
         configElement = new ViewsConfigElement();
         
         // get the configured views
         Iterator<Element> renderers = element.elementIterator(ELEMENT_VIEWIMPL);
         while (renderers.hasNext())
         {
            Element renderer = renderers.next();
            configElement.addView(renderer.getTextTrim());
         }
         
         // get all the view related default settings
         Element viewDefaults = element.element(ELEMENT_VIEWDEFAULTS);
         if (viewDefaults != null)
         {
            Iterator<Element> pages = viewDefaults.elementIterator();
            while (pages.hasNext())
            {
               Element page = pages.next();
               String pageName = page.getName();
               
               // get the default view mode for the page
               Element defaultView = page.element(ELEMENT_VIEW);
               if (defaultView != null)
               {
                  String viewName = defaultView.getTextTrim();
                  configElement.addDefaultView(pageName, viewName);
               }
               
               // get the initial sort column
               Element sortColumn = page.element(ELEMENT_SORTCOLUMN);
               if (sortColumn != null)
               {
                  String column = sortColumn.getTextTrim();
                  configElement.addDefaultSortColumn(pageName, column);
               }
               
               // get the sort direction option
               Element sortDir = page.element(ELEMENT_SORTDIRECTION);
               if (sortDir != null)
               {
                  configElement.addSortDirection(pageName, sortDir.getTextTrim());
               }
               
               // process the page-size element
               processPageSizeElement(page.element(ELEMENT_PAGESIZE), 
                     pageName, configElement);
            }
         }
      }
      
      return configElement;
   }
   
   /**
    * Processes a page-size element
    * 
    * @param pageSizeElement The element to process
    * @param page The page the page-size element belongs to
    * @param configElement The config element being populated
    */
   @SuppressWarnings("unchecked")
   private void processPageSizeElement(Element pageSizeElement, String page, 
         ViewsConfigElement configElement)
   {
      if (pageSizeElement != null)
      {
         Iterator<Element> views = pageSizeElement.elementIterator();
         while (views.hasNext())
         {
            Element view = views.next();
            String viewName = view.getName();
            String pageSize = view.getTextTrim();
            try
            {
               configElement.addDefaultPageSize(page, viewName, Integer.parseInt(pageSize));
            }
            catch (NumberFormatException nfe)
            {
               if (logger.isWarnEnabled())
               {
                  logger.warn("Failed to set page size for view '" + viewName + 
                        "' in page '" + page + "' as '" + pageSize + 
                        "' is an invalid number!");
               }
            }
         }
      }
   }
}
