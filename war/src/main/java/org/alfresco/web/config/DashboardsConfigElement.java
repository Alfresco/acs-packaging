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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.extensions.config.ConfigElement;
import org.springframework.extensions.config.ConfigException;
import org.springframework.extensions.config.element.ConfigElementAdapter;

/**
 * Dashboard config element.
 * 
 * @author Kevin Roast
 */
public class DashboardsConfigElement extends ConfigElementAdapter
{
   public static final String CONFIG_ELEMENT_ID = "dashboards";
   
   private Map<String, LayoutDefinition> layoutDefs = new LinkedHashMap<String, LayoutDefinition>(4, 1.0f);
   private Map<String, DashletDefinition> dashletDefs = new LinkedHashMap<String, DashletDefinition>(8, 1.0f);
   private List<String> defaultDashlets = null;
   private boolean allowGuestConfig = false;
   
   /**
    * Default constructor
    */
   public DashboardsConfigElement()
   {
      super(CONFIG_ELEMENT_ID);
   }
   
   /**
    * @param name String
    */
   public DashboardsConfigElement(String name)
   {
      super(name);
   }

   /**
    * @see org.springframework.extensions.config.element.ConfigElementAdapter#getChildren()
    */
   public List<ConfigElement> getChildren()
   {
      throw new ConfigException("Reading the Dashboards config via the generic interfaces is not supported");
   }
   
   /**
    * @see org.springframework.extensions.config.element.ConfigElementAdapter#combine(org.springframework.extensions.config.ConfigElement)
    */
   public ConfigElement combine(ConfigElement configElement)
   {
      DashboardsConfigElement newElement = (DashboardsConfigElement)configElement;
      DashboardsConfigElement combinedElement = new DashboardsConfigElement();
      
      // put all into combined from this and then from new to override any already present
      combinedElement.dashletDefs.putAll(this.dashletDefs);
      combinedElement.dashletDefs.putAll(newElement.dashletDefs);
      
      combinedElement.layoutDefs.putAll(this.layoutDefs);
      combinedElement.layoutDefs.putAll(newElement.layoutDefs);
      
      if (newElement.allowGuestConfig != combinedElement.allowGuestConfig)
      {
         combinedElement.allowGuestConfig = newElement.allowGuestConfig;
      }
      
      // the default-dashlets list is completely replaced if config is overriden
      if (newElement.defaultDashlets != null)
      {
         combinedElement.defaultDashlets =
            (List<String>)((ArrayList<String>)newElement.defaultDashlets).clone();
      }
      else if (this.defaultDashlets != null)
      {
         combinedElement.defaultDashlets =
            (List<String>)((ArrayList<String>)this.defaultDashlets).clone();
      }
      
      return combinedElement;
   }
   
   /*package*/ void setAllowGuestConfig(boolean allow)
   {
      this.allowGuestConfig = allow;
   }
   
   public boolean getAllowGuestConfig()
   {
      return this.allowGuestConfig;
   }
   
   /*package*/ void addLayoutDefinition(LayoutDefinition def)
   {
      this.layoutDefs.put(def.Id, def);
   }
   
   public LayoutDefinition getLayoutDefinition(String id)
   {
      return this.layoutDefs.get(id);
   }
   
   /*package*/ void addDashletDefinition(DashletDefinition def)
   {
      this.dashletDefs.put(def.Id, def);
   }
   
   public DashletDefinition getDashletDefinition(String id)
   {
      return this.dashletDefs.get(id);
   }
   
   public Collection<LayoutDefinition> getLayouts()
   {
      return this.layoutDefs.values();
   }
   
   public Collection<DashletDefinition> getDashlets()
   {
      return this.dashletDefs.values();
   }
   
   /*package*/ void addDefaultDashlet(String id)
   {
      if (this.defaultDashlets == null)
      {
         this.defaultDashlets = new ArrayList<String>(2);
      }
      this.defaultDashlets.add(id);
   }
   
   public Collection<String> getDefaultDashlets()
   {
      return this.defaultDashlets;
   }
   
   /**
    * Structure class for the definition of a dashboard page layout 
    */
   public static class LayoutDefinition implements Serializable
   {
      private static final long serialVersionUID = -3014156293576142077L;
    
      LayoutDefinition(String id)
      {
         this.Id = id;
      }
      
      public String Id;
      public String Image;
      public int Columns;
      public int ColumnLength;
      public String Label;
      public String LabelId;
      public String Description;
      public String DescriptionId;
      public String JSPPage;
   }
   
   /**
    * Structure class for the definition of a dashboard dashlet component
    */
   public static class DashletDefinition implements Serializable
   {
      private static final long serialVersionUID = -5755903997700459631L;
      
      DashletDefinition(String id)
      {
         this.Id = id;
      }
      
      public String Id;
      public boolean AllowNarrow = true;
      public String Label;
      public String LabelId;
      public String Description;
      public String DescriptionId;
      public String JSPPage;
      public String ConfigJSPPage;
   }
}
