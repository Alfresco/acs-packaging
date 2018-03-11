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
package org.alfresco.web.ui.repo.tag.shelf;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.el.MethodBinding;

import org.springframework.extensions.webscripts.ui.common.tag.BaseComponentTag;
import org.alfresco.web.ui.repo.component.shelf.UIShelf;

/**
 * @author Kevin Roast
 */
public class ShelfTag extends BaseComponentTag
{
   /**
    * @see javax.faces.webapp.UIComponentTag#getComponentType()
    */
   public String getComponentType()
   {
      return "org.alfresco.faces.Shelf";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#getRendererType()
    */
   public String getRendererType()
   {
      // self rendering component
      return null;
   }
   
   /**
    * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
    */
   protected void setProperties(UIComponent component)
   {
      super.setProperties(component);
      
      setStringProperty(component, "groupPanel", this.groupPanel);
      setStringProperty(component, "groupBgcolor", this.groupBgcolor);
      setStringProperty(component, "selectedGroupPanel", this.selectedGroupPanel);
      setStringProperty(component, "selectedGroupBgcolor", this.selectedGroupBgcolor);
      setStringProperty(component, "innerGroupPanel", this.innerGroupPanel);
      setStringProperty(component, "innerGroupBgcolor", this.innerGroupBgcolor);
      if (this.groupExpandedActionListener != null)
      {
         if (isValueReference(this.groupExpandedActionListener))
         {
            MethodBinding vb = getFacesContext().getApplication().createMethodBinding(this.groupExpandedActionListener, ACTION_CLASS_ARGS);
            ((UIShelf)component).setGroupExpandedActionListener(vb);
         }
         else
         {
            throw new FacesException("Shelf Group Expanded Action listener method binding incorrectly specified: " + this.groupExpandedActionListener);
         }
      }
   }
   
   /**
    * @see org.alfresco.web.ui.common.tag.HtmlComponentTag#release()
    */
   public void release()
   {
      super.release();
      
      this.groupPanel = null;
      this.groupBgcolor = null;
      this.selectedGroupPanel = null;
      this.selectedGroupBgcolor = null;
      this.innerGroupPanel = null;
      this.innerGroupBgcolor = null;
      this.groupExpandedActionListener = null;
   }
   
   /**
    * Set the groupPanel
    *
    * @param groupPanel     the groupPanel
    */
   public void setGroupPanel(String groupPanel)
   {
      this.groupPanel = groupPanel;
   }

   /**
    * Set the groupBgcolor
    *
    * @param groupBgcolor     the groupBgcolor
    */
   public void setGroupBgcolor(String groupBgcolor)
   {
      this.groupBgcolor = groupBgcolor;
   }

   /**
    * Set the selectedGroupPanel
    *
    * @param selectedGroupPanel     the selectedGroupPanel
    */
   public void setSelectedGroupPanel(String selectedGroupPanel)
   {
      this.selectedGroupPanel = selectedGroupPanel;
   }

   /**
    * Set the selectedGroupBgcolor
    *
    * @param selectedGroupBgcolor     the selectedGroupBgcolor
    */
   public void setSelectedGroupBgcolor(String selectedGroupBgcolor)
   {
      this.selectedGroupBgcolor = selectedGroupBgcolor;
   }

   /**
    * Set the innerGroupPanel
    *
    * @param innerGroupPanel     the innerGroupPanel
    */
   public void setInnerGroupPanel(String innerGroupPanel)
   {
      this.innerGroupPanel = innerGroupPanel;
   }

   /**
    * Set the innerGroupBgcolor
    *
    * @param innerGroupBgcolor     the innerGroupBgcolor
    */
   public void setInnerGroupBgcolor(String innerGroupBgcolor)
   {
      this.innerGroupBgcolor = innerGroupBgcolor;
   }
   
   /**
    * Set the groupExpandedActionListener
    *
    * @param groupExpandedActionListener     the groupExpandedActionListener
    */
   public void setGroupExpandedActionListener(String groupExpandedActionListener)
   {
      this.groupExpandedActionListener = groupExpandedActionListener;
   }


   /** the groupExpandedActionListener */
   private String groupExpandedActionListener;

   /** the groupPanel */
   private String groupPanel;

   /** the groupBgcolor */
   private String groupBgcolor;

   /** the selectedGroupPanel */
   private String selectedGroupPanel;

   /** the selectedGroupBgcolor */
   private String selectedGroupBgcolor;

   /** the innerGroupPanel */
   private String innerGroupPanel;

   /** the innerGroupBgcolor */
   private String innerGroupBgcolor;
}
