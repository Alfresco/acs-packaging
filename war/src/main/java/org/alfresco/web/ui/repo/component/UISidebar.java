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
package org.alfresco.web.ui.repo.component;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.SidebarBean;
import org.alfresco.web.config.SidebarConfigElement;
import org.alfresco.web.config.SidebarConfigElement.SidebarPluginConfig;
import org.alfresco.web.ui.common.Utils;
import org.springframework.extensions.webscripts.ui.common.component.SelfRenderingComponent;
import org.alfresco.web.ui.common.component.UIListItems;
import org.alfresco.web.ui.common.component.UIModeList;

/**
 * Component that represents the sidebar.
 * <p>
 * A sidebar consists of multiple plugins, of which only
 * one is active at one time. All registered plugins are
 * displayed in a drop down allowing the user to
 * change the active plugin. An action group can also be
 * associated with a plugin, which get rendered in the 
 * sidebar header.
 * </p>
 * 
 * @author gavinc
 */
public class UISidebar extends SelfRenderingComponent
{
   public static final String COMPONENT_TYPE = "org.alfresco.faces.Sidebar";
   
   protected String activePlugin;
   
   @Override
   public String getFamily()
   {
      return COMPONENT_TYPE;
   }

   @Override
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.activePlugin = (String)values[1];
   }
   
   @Override
   public Object saveState(FacesContext context)
   {
      // standard component attributes are saved by the super class
      return new Object[] { super.saveState(context), this.activePlugin };
   }
   
   @SuppressWarnings("unchecked")
   @Override
   public void encodeBegin(FacesContext context) throws IOException
   {
      if (!isRendered()) return;
      
      ResponseWriter out = context.getResponseWriter();
      
      out.write("<div id='sidebar' class='sidebar'>");
      
      // render the start of the header panel
      String cxPath = context.getExternalContext().getRequestContextPath();
      out.write("<table cellspacing='0' cellpadding='0' style='background-color: #ffffff;' width='100%'>" + 
                "<tr valign='top'><td width='20%'><table cellspacing='0' cellpadding='0' width='100%'>" +
                "<tr><td style='width: 5px; background-image: url(");
      out.write(cxPath);
      out.write("/images/parts/sidebar_top_grey_begin.gif)' valign='top'>" +
                "<img src=\"");
      out.write(cxPath);
      out.write("/images/parts/sidebar_grey_01.gif\" width='5' height='5' alt=''></td>" + 
                "<td style='height: 24px; background-image: url(");
      out.write(cxPath);
      out.write("/images/parts/sidebar_top_grey_bg.gif)'>");
      
      // generate the required child components if not present
      if (this.getChildCount() == 1)
      {
         // create the mode list component
         UIModeList modeList = (UIModeList)context.getApplication().
               createComponent("org.alfresco.faces.ModeList");
         modeList.setId("sidebarPluginList");
         modeList.setValue(this.getActivePlugin());
         modeList.setIconColumnWidth(2);
         modeList.setMenu(true);
         modeList.setMenuImage("/images/icons/menu.gif");
         modeList.getAttributes().put("itemSpacing", 4);
         modeList.getAttributes().put("styleClass", "moreActionsMenu");
         modeList.getAttributes().put("selectedStyleClass", "statusListHighlight");
         MethodBinding listener = context.getApplication().createMethodBinding(
               "#{SidebarBean.pluginChanged}", new Class[] {ActionEvent.class});
         modeList.setActionListener(listener);
            
         // create the child list items component
         UIListItems items = (UIListItems)context.getApplication().
               createComponent("org.alfresco.faces.ListItems");
         ValueBinding binding = context.getApplication().createValueBinding(
               "#{SidebarBean.plugins}");
         items.setValueBinding("value", binding);
         
         // add the list items to the mode list component
         modeList.getChildren().add(items);
         
         // create the actions component
         UIActions actions = (UIActions)context.getApplication().
               createComponent("org.alfresco.faces.Actions");
         actions.setId("sidebarActions");
         actions.setShowLink(false);
         setupActionGroupId(context, actions);
         
         // add components to the sidebar
         this.getChildren().add(0, modeList);
         this.getChildren().add(1, actions);
      }
      else
      {
         // update the child UIActions component with the correct 
         // action group id and clear it's current children
         UIActions actions = (UIActions)this.getChildren().get(1);
         actions.reset();
         setupActionGroupId(context, actions);
      }
   }

   @Override
   public void encodeChildren(FacesContext context) throws IOException
   {
      if (!isRendered()) return;
      
      // there should be 3 children, the modelist, the actions
      // and the plugin, get them individually and render

      if (getChildren().size() == 3)
      {
         ResponseWriter out = context.getResponseWriter();

         out.write("<table border='0' cellpadding='6' cellspacing='0' width='100%;'><tr><td>");
         
         // render the list
         UIModeList modeList = (UIModeList)getChildren().get(0);
         Utils.encodeRecursive(context, modeList);
         
         out.write("</td><td align='right'>");
         
         // render the actions
         UIActions actions = (UIActions)getChildren().get(1);
         Utils.encodeRecursive(context, actions);
         
         out.write("</td></tr></table>");
         
         // render the end of the header panel
         String cxPath = context.getExternalContext().getRequestContextPath();
         out.write("</td><td style='width: 5px; background-image: url(");
         out.write(cxPath);
         out.write("/images/parts/sidebar_top_grey_end.gif)' align='right' valign='top'>" +
                   "<img src='");
         out.write(cxPath);
         out.write("/images/parts/sidebar_grey_03.gif' width='5' height='5' alt=''></td></tr>" +
                   "</table><table cellspacing='0' cellpadding='0' width='100%'>" +
                   "<tr><td width='100%' id='pluginBox'>");
         
         // render the plugin
         UIComponent plugin = (UIComponent)getChildren().get(2);
         Utils.encodeRecursive(context, plugin);
      }
   }

   @Override
   public void encodeEnd(FacesContext context) throws IOException
   {
      if (!isRendered()) return;
      
      // render the end of the panel
      ResponseWriter out = context.getResponseWriter();
      String cxPath = context.getExternalContext().getRequestContextPath();
      out.write("</td></tr>" + 
                "</table><table cellspacing='0' cellpadding='0' width='100%'>" +
                "<tr><td style='height: 12px; width: 5px;'><img src='");
      out.write(cxPath);
      out.write("/images/parts/sidebar_bottom_grey_begin.gif' width='5' height='12' alt=''></td>" + 
                "<td style='width: 100%; background-image: url(");
      out.write(cxPath);
      out.write("/images/parts/sidebar_bottom_grey_bg.gif)'>" +
                "<img src='");
      out.write(cxPath);
      out.write("/images/parts/sidebar_bottom_grey_bg.gif' width='48' height='12' alt=''></td>" +
                "<td align='right' style='width: 5px;'><img src='");
      out.write(cxPath);
      out.write("/images/parts/sidebar_bottom_grey_end.gif' width='5' height='12' alt=''></td></tr>" + 
                "</table></td></tr></table>" +
                "</div>");
   }

   @Override
   public boolean getRendersChildren()
   {
      return true;
   }
   
   // ------------------------------------------------------------------------------
   // Strongly typed component property accessors 
   
   /**
    * Returns the id of the plugin that is currently active
    * 
    * @return The currently active plugin
    */
   public String getActivePlugin()
   {
      ValueBinding vb = getValueBinding("activePlugin");
      if (vb != null)
      {
         this.activePlugin = (String)vb.getValue(getFacesContext());
      }
      
      return this.activePlugin;
   }
   
   /**
    * Sets the active plugin the sidebar should show
    * 
    * @param activePlugin Id of the plugin to make active
    */
   public void setActivePlugin(String activePlugin)
   {
      this.activePlugin = activePlugin;
   }
   
   /**
    * Sets up the corrent actions config group id on the given actions
    * component.
    * 
    * @param context Faces context
    * @param actionsComponent The actions component to set the group id for
    */
   protected void setupActionGroupId(FacesContext context, UIActions actionsComponent)
   {
      String actionsGroupId = null;
      SidebarConfigElement config = SidebarBean.getSidebarConfig(context);
      if (config != null)
      {
         SidebarPluginConfig plugin = config.getPlugin(getActivePlugin());
         if (plugin != null)
         {
            actionsGroupId = plugin.getActionsConfigId();
         }
      }
      actionsComponent.setValue(actionsGroupId);
   }
}


