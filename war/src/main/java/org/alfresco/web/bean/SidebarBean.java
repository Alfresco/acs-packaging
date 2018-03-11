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
package org.alfresco.web.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.springframework.extensions.config.Config;
import org.alfresco.web.app.Application;
import org.alfresco.web.config.SidebarConfigElement;
import org.alfresco.web.config.SidebarConfigElement.SidebarPluginConfig;
import org.alfresco.web.ui.common.component.UIListItem;
import org.alfresco.web.ui.common.component.UIModeList;

/**
 * Managed bean used by the sidebar component to manage it's state.
 * 
 * @author gavinc
 */
public class SidebarBean implements Serializable
{
   private static final long serialVersionUID = -2613219657864672539L;
   
   protected String activePlugin;
   protected List<UIListItem> plugins;
   protected SidebarConfigElement sidebarConfig;
   
   /**
    * Default constructor
    */
   public SidebarBean()
   {
      // get the sidebar config object
      this.sidebarConfig = getSidebarConfig(FacesContext.getCurrentInstance());
      
      // make sure we found the config
      if (this.sidebarConfig == null)
      {
         throw new IllegalStateException("Failed to find configuration for the sidebar");
      }
      
      // build the list of plugins available and check we have at least one
      List<UIListItem> items = this.getPlugins();
      if (items.size() == 0)
      {
         throw new IllegalStateException("Failed to find configuration for any sidebar plugins, at least one must be defined!");
      }
      
      // determine the default plugin
      this.activePlugin = this.sidebarConfig.getDefaultPlugin();
      if (this.activePlugin == null)
      {
         this.activePlugin = (String)items.get(0).getValue();
      }
   }
   
   // ------------------------------------------------------------------------------
   // Event handlers
   
   public void pluginChanged(ActionEvent event)
   {
      UIModeList pluginList = (UIModeList)event.getComponent();
      
      // get the selected plugin
      this.activePlugin = pluginList.getValue().toString();
   }
   
   // ------------------------------------------------------------------------------
   // Bean Getters and Setters
   
   /**
    * Returns a list of configured plugins
    * 
    * @return List of UIListItem's representing the plugins available
    */
   public List<UIListItem> getPlugins()
   {
      if (this.plugins == null)
      {
         FacesContext context = FacesContext.getCurrentInstance();
         this.plugins = new ArrayList<UIListItem>();
         
         // create a list entry for each configured plugin
         for (String pluginId : this.sidebarConfig.getPlugins().keySet())
         {
            SidebarPluginConfig plugin = this.sidebarConfig.getPlugin(pluginId);
            
            // resolve the label for the plugin
            String label = plugin.getlabelId();
            if (label != null)
            {
               label = Application.getMessage(context, label);
            }
            if (label == null)
            {
               label = plugin.getlabel();
            }
            if (label == null)
            {
               label = plugin.getId();
            }
            
            // resolve the description (tooltip for the plugin)
            String tooltip = plugin.getDescriptionId();
            if (tooltip != null)
            {
               tooltip = Application.getMessage(context, tooltip);
            }
            if (tooltip == null)
            {
               tooltip = plugin.getDescription();
            }
            
            UIListItem item = new UIListItem();
            item.setValue(plugin.getId());
            item.setLabel(label);
            if (tooltip != null)
            {
               item.setTooltip(tooltip);
            }
            
            this.plugins.add(item);
         }
      }
      
      return this.plugins;
   }
   
   /**
    * Returns the id of the currently active plugin
    * 
    * @return Id of the current plugin
    */
   public String getActivePlugin()
   {
      return activePlugin;
   }
   
   /**
    * Returns the path of the JSP to use for the current plugin
    * 
    * @return JSP to use for the current plugin
    */
   public String getActivePluginPage()
   {
      return this.sidebarConfig.getPlugin(this.activePlugin).getPage();
   }
   
   // ------------------------------------------------------------------------------
   // Helper methods
   
   /**
    * Returns the SidebarConfigElement for the application
    * 
    * @param context Faces context
    * @return The SidebarConfigElement object or null if it's not found
    */
   public static SidebarConfigElement getSidebarConfig(FacesContext context)
   {
      SidebarConfigElement config = null;
      
      Config cfg = Application.getConfigService(context).getConfig("Sidebar");
      if (cfg != null)
      {
         config = (SidebarConfigElement)cfg.getConfigElement(SidebarConfigElement.CONFIG_ELEMENT_ID);
      }
      
      return config;
   }
}
