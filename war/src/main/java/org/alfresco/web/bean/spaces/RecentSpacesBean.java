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
package org.alfresco.web.bean.spaces;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.context.IContextListener;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.bean.BrowseBean;
import org.alfresco.web.bean.NavigationBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.ClientConfigElement;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.shelf.UIRecentSpacesShelfItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This bean manages the real-time updated list of Recent Spaces in the Shelf component.
 * <p>
 * Registers itself as a UI Context Listener so it is informed as to when the current Node ID
 * has changed in the NavigationBeans. This is used to keep the list of spaces up-to-date. 
 * 
 * @author Kevin Roast
 */
public class RecentSpacesBean implements IContextListener
{
   private static final long serialVersionUID = -6405913558933664909L;

   private static Log    logger = LogFactory.getLog(RecentSpacesBean.class);
   
   /** The NavigationBean reference */
   protected NavigationBean navigator;
   
   /** The BrowseBean reference */
   protected BrowseBean browseBean;
   
   /** Maximum number of recent spaces to show */
   private Integer maxRecentSpaces = null;
   
   /** List of recent space nodes */
   private List<Node> recentSpaces = new LinkedList<Node>();
   
   
   // ------------------------------------------------------------------------------
   // Construction 
   
   /**
    * Default Constructor
    */
   public RecentSpacesBean()
   {
      UIContextService.getInstance(FacesContext.getCurrentInstance()).registerBean(this);
   }


   // ------------------------------------------------------------------------------
   // Bean property getters and setters 
   
   /**
    * @param navigator The NavigationBean to set.
    */
   public void setNavigator(NavigationBean navigator)
   {
      this.navigator = navigator;
   }

   /**
    * @param browseBean The BrowseBean to set.
    */
   public void setBrowseBean(BrowseBean browseBean)
   {
      this.browseBean = browseBean;
   }
   
   /**
    * @return the List of recent spaces
    */
   public List<Node> getRecentSpaces()
   {
      return this.recentSpaces;
   }
   
   /**
    * @param spaces     List of Nodes
    */
   public void setRecentSpaces(List<Node> spaces)
   {
      this.recentSpaces = spaces;
   }
   
   
   // ------------------------------------------------------------------------------
   // Action method handlers
   
   /**
    * Action handler bound to the recent spaces Shelf component called when a Space is clicked
    */
   public void navigate(ActionEvent event)
   {
      // work out which node was clicked from the event data
      UIRecentSpacesShelfItem.RecentSpacesEvent spaceEvent = (UIRecentSpacesShelfItem.RecentSpacesEvent)event;
      Node selectedNode = this.recentSpaces.get(spaceEvent.Index);
      NodeRef nodeRef = selectedNode.getNodeRef();
      try
      {
         // then navigate to the appropriate node in UI
         // use browse bean functionality for this as it will update the breadcrumb for us
         this.browseBean.updateUILocation(nodeRef);
      }
      catch (InvalidNodeRefException refErr)
      {
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_NODEREF), new Object[] {nodeRef.getId()}) );
         
         // remove invalid node from recent spaces list
         this.recentSpaces.remove(spaceEvent.Index);
      }
   }
   
   
   // ------------------------------------------------------------------------------
   // IContextListener implementation
   
   /**
    * @see org.alfresco.web.app.context.IContextListener#contextUpdated()
    */
   public void contextUpdated()
   {
      // We use this listener handler to refresh the recent spaces list. At the point
      // where this method is called, the current node Id in UI will probably have changed.
      Node node = this.navigator.getCurrentNode();
      
      // search for this node - if it's already in the list remove it so
      // that it appears at the top for us
      for (int i=0; i<this.recentSpaces.size(); i++)
      {
         if (node.getId().equals(this.recentSpaces.get(i).getId()))
         {
            // found same node already in the list - remove it
            this.recentSpaces.remove(i);
            break;
         }
      }
      
      // remove an item if the list is at the maximum length
      int maxItems = getMaxRecentSpaces();
      if (this.recentSpaces.size() == maxItems)
      {
         this.recentSpaces.remove(maxItems - 1);
      }
      
      if (logger.isDebugEnabled())
         logger.debug("Inserting node: " + node.getName() + " at top of recent spaces list.");
      
      // insert our Node at the top of the list so it's most relevent
      this.recentSpaces.add(0, node);
   }
   
   /**
    * @see org.alfresco.web.app.context.IContextListener#areaChanged()
    */
   public void areaChanged()
   {
      // nothing to do
   }

   /**
    * @see org.alfresco.web.app.context.IContextListener#spaceChanged()
    */
   public void spaceChanged()
   {
      // nothing to do
   }
   
   // ------------------------------------------------------------------------------
   // Helper methods
   
   /**
    * @return the max number of recent spaces to show, retrieved from client config
    */
   private int getMaxRecentSpaces()
   {
      if (maxRecentSpaces == null)
      {
         ClientConfigElement config = Application.getClientConfig(FacesContext.getCurrentInstance());
         maxRecentSpaces = Integer.valueOf(config.getRecentSpacesItems());
      }
      
      return maxRecentSpaces.intValue();
   }
}
