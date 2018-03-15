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
package org.alfresco.web.ui.repo.tag;

import javax.faces.component.UIComponent;

import org.alfresco.web.ui.common.tag.HtmlComponentTag;

/**
 * Tag class for using the Yahoo tree component on a JSP page.
 * 
 * @author gavinc
 */
public class YahooTreeTag extends HtmlComponentTag
{
   private String rootNodes;
   private String retrieveChildrenUrl;
   private String nodeCollapsedUrl;
   private String nodeExpandedCallback;
   private String nodeCollapsedCallback;
   private String nodeSelectedCallback;
   
   /**
    * @see javax.faces.webapp.UIComponentTag#getComponentType()
    */
   public String getComponentType()
   {
      return "org.alfresco.faces.Tree";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#getRendererType()
    */
   public String getRendererType()
   {
      return "org.alfresco.faces.Yahoo";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
    */
   protected void setProperties(UIComponent component)
   {
      super.setProperties(component);
      
      setStringBindingProperty(component, "rootNodes", this.rootNodes);
      setStringBindingProperty(component, "retrieveChildrenUrl", this.retrieveChildrenUrl);
      setStringBindingProperty(component, "nodeCollapsedUrl", this.nodeCollapsedUrl);
      setStringBindingProperty(component, "nodeExpandedCallback", this.nodeExpandedCallback);
      setStringBindingProperty(component, "nodeCollapsedCallback", this.nodeCollapsedCallback);
      setStringBindingProperty(component, "nodeSelectedCallback", this.nodeSelectedCallback);
   }
   
   /**
    * @see org.alfresco.web.ui.common.tag.HtmlComponentTag#release()
    */
   public void release()
   {
      super.release();
      
      this.rootNodes = null;
      this.retrieveChildrenUrl = null;
      this.nodeCollapsedUrl = null;
      this.nodeExpandedCallback = null;
      this.nodeCollapsedCallback = null;
      this.nodeSelectedCallback = null;
   }
   
   /**
    * Set the root nodes for the tree
    *
    * @param rootNodes
    */
   public void setRootNodes(String rootNodes)
   {
      this.rootNodes = rootNodes;
   }

   /**
    * Set the name of the Javascript function to handle the node collapsed event
    * 
    * @param nodeCollapsedCallback
    */
   public void setNodeCollapsedCallback(String nodeCollapsedCallback)
   {
      this.nodeCollapsedCallback = nodeCollapsedCallback;
   }

   /**
    * Set the name of the Javascript function to handle the node expanded event
    * 
    * @param nodeExpandedCallback
    */
   public void setNodeExpandedCallback(String nodeExpandedCallback)
   {
      this.nodeExpandedCallback = nodeExpandedCallback;
   }

   /**
    * Set the name of the Javascript function to handle the node selected event
    * 
    * @param nodeSelectedCallback
    */
   public void setNodeSelectedCallback(String nodeSelectedCallback)
   {
      this.nodeSelectedCallback = nodeSelectedCallback;
   }

   /**
    * Set the URL to use to retrieve child nodes
    * 
    * @param retrieveChildrenUrl
    */
   public void setRetrieveChildrenUrl(String retrieveChildrenUrl)
   {
      this.retrieveChildrenUrl = retrieveChildrenUrl;
   }
   
   /**
    * Set the URL to use to inform the server that a node has been collapsed
    * 
    * @param nodeCollapsedUrl
    */
   public void setNodeCollapsedUrl(String nodeCollapsedUrl)
   {
      this.nodeCollapsedUrl = nodeCollapsedUrl;
   }
}
