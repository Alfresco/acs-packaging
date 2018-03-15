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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.extensions.config.ConfigElement;
import org.springframework.extensions.config.ConfigException;
import org.springframework.extensions.config.element.ConfigElementAdapter;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.web.action.ActionEvaluator;
import org.alfresco.web.bean.repository.Repository;

/**
 * Action config element.
 * 
 * @author Kevin Roast
 */
public class ActionsConfigElement extends ConfigElementAdapter
{
   public static final String CONFIG_ELEMENT_ID = "actions";
   
   private Map<String, ActionDefinition> actionDefs = new HashMap<String, ActionDefinition>(32, 1.0f);
   private Map<String, ActionGroup> actionGroups = new HashMap<String, ActionGroup>(16, 1.0f);
   
   /**
    * Default constructor
    */
   public ActionsConfigElement()
   {
      super(CONFIG_ELEMENT_ID);
   }
   
   /**
    * @param name String
    */
   public ActionsConfigElement(String name)
   {
      super(name);
   }

   /**
    * @see org.springframework.extensions.config.element.ConfigElementAdapter#getChildren()
    */
   public List<ConfigElement> getChildren()
   {
      throw new ConfigException("Reading the Actions config via the generic interfaces is not supported");
   }
   
   /**
    * @see org.springframework.extensions.config.element.ConfigElementAdapter#combine(org.springframework.extensions.config.ConfigElement)
    */
   public ConfigElement combine(ConfigElement configElement)
   {
      ActionsConfigElement newElement = (ActionsConfigElement)configElement;
      ActionsConfigElement combinedElement = new ActionsConfigElement();
      
      // add the existing action definitions
      combinedElement.actionDefs.putAll(this.actionDefs);
      
      // overwrite any existing action definitions i.e. don't combine
      combinedElement.actionDefs.putAll(newElement.actionDefs);
      
      // add the existing action groups
      Map<String, ActionGroup> combinedActionGroups = new HashMap<String, ActionGroup>(this.actionGroups.size());
      try
      {
         for (ActionGroup group : this.actionGroups.values())
         {
            combinedActionGroups.put(group.getId(), (ActionGroup)group.clone());
         }
      }
      catch (CloneNotSupportedException e)
      {
         throw new AlfrescoRuntimeException("clone() required on ActionGroup class.", e);
      }
      combinedElement.actionGroups = combinedActionGroups;
      
      // any new action groups with the same name must be combined
      for (ActionGroup newGroup : newElement.actionGroups.values())
      {
         if (combinedElement.actionGroups.containsKey(newGroup.getId()))
         {
            // there is already a group with this id, combine it with the new one
            ActionGroup combinedGroup = combinedElement.actionGroups.get(newGroup.getId());
            if (newGroup.ShowLink != combinedGroup.ShowLink)
            {
               combinedGroup.ShowLink = newGroup.ShowLink;
            }
            if (newGroup.Style != null)
            {
               combinedGroup.Style = newGroup.Style;
            }
            if (newGroup.StyleClass != null)
            {
               combinedGroup.StyleClass = newGroup.StyleClass;
            }
            
            // add all the actions from the new group to the combined one
            for (String actionRef : newGroup.getAllActions())
            {
               combinedGroup.addAction(actionRef);
            }
            
            // add all the hidden actions from the new group to the combined one
            for (String actionRef : newGroup.getHiddenActions())
            {
               combinedGroup.hideAction(actionRef);
            }
         }
         else
         {
            // it's a new group so just add it
            combinedElement.actionGroups.put(newGroup.getId(), newGroup);
         }
      }

      return combinedElement;
   }
   
   /*package*/ void addActionDefinition(ActionDefinition actionDef)
   {
      actionDefs.put(actionDef.getId(), actionDef);
   }
   
   public ActionDefinition getActionDefinition(String id)
   {
      return actionDefs.get(id);
   }
   
   /*package*/ void addActionGroup(ActionGroup group)
   {
      actionGroups.put(group.getId(), group);
   }
   
   public ActionGroup getActionGroup(String id)
   {
      return actionGroups.get(id);
   }
   
   
   /**
    * Simple class representing the definition of a UI action.
    * 
    * @author Kevin Roast
    */
   public static class ActionDefinition
   {
      public ActionDefinition(String id)
      {
         if (id == null || id.length() == 0)
         {
            throw new IllegalArgumentException("ActionDefinition ID is mandatory.");
         }
         this.id = id;
      }
      
      public String getId()
      {
         return id;
      }
      
      public void addAllowPermission(String permission)
      {
         if (permissionAllow == null)
         {
            permissionAllow = new ArrayList<String>(2);
         }
         permissionAllow.add(permission);
      }
      
      public void addDenyPermission(String permission)
      {
         if (permissionDeny == null)
         {
            permissionDeny = new ArrayList<String>(1);
         }
         permissionDeny.add(permission);
      }
      
      public List<String> getAllowPermissions()
      {
         return permissionAllow;
      }
      
      public List<String> getDenyPermissions()
      {
         return permissionDeny;
      }
      
      public void addParam(String name, String value)
      {
         if (params == null)
         {
            params = new HashMap<String, String>(1, 1.0f);
         }
         params.put(name, value);
      }
      
      public Map<String, String> getParams()
      {
         return params;
      }
      
      String id;
      private List<String> permissionAllow = null;
      private List<String> permissionDeny = null;
      private Map<String, String> params = null;
      
      public ActionEvaluator Evaluator = null;
      public String Label;
      public String LabelMsg;
      public String Tooltip;
      public String TooltipMsg;
      public boolean ShowLink = true;
      public String Style;
      public String StyleClass;
      public String Image;
      public String ActionListener;
      public String Action;
      public String Href;
      public String Target;
      public String Script;
      public String Onclick;
   }
   
   
   /**
    * Simple class representing a group of UI actions.
    * 
    * @author Kevin Roast
    */
   public static class ActionGroup implements Iterable<String>, Cloneable
   {
      public ActionGroup(String id)
      {
         if (id == null || id.length() == 0)
         {
            throw new IllegalArgumentException("ActionGroup ID is mandatory.");
         }
         this.id = id;
      }
      
      @Override
      protected Object clone() throws CloneNotSupportedException
      {
         ActionGroup clone = new ActionGroup(id);
         clone.actions = (Set<String>)((LinkedHashSet)actions).clone();
         clone.hiddenActions = (Set<String>)((HashSet)hiddenActions).clone();
         clone.ShowLink = ShowLink;
         clone.Style = Style;
         clone.StyleClass = StyleClass;
         return clone;
      }

      public String getId()
      {
         return id;
      }
      
      /**
       * @return Iterator over the visible ActionDefinition IDs referenced by this group
       */
      public Iterator<String> iterator()
      {
         // create a list of the visible actions and return it's iterator
         ArrayList<String> visibleActions = new ArrayList<String>(this.actions.size());
         for (String actionId : this.actions)
         {
            if (this.hiddenActions.contains(actionId) == false)
            {
               visibleActions.add(actionId);
            }
         }
         
         visibleActions.trimToSize();
         
         return visibleActions.iterator();
      }
      
      /*package*/ void addAction(String actionId)
      {
         actions.add(actionId);
      }
      
      /*package*/ void hideAction(String actionId)
      {
         this.hiddenActions.add(actionId);
      }
      
      /*package*/ Set<String> getAllActions()
      {
         return this.actions;
      }
      
      /*package*/ Set<String> getHiddenActions()
      {
         return this.hiddenActions;
      }
      
      private String id;
      
      /** the action definitions, we use a Linked HashSet to ensure we do not have more 
          than one action with the same Id and that the insertion order is preserved */
      private Set<String> actions = new LinkedHashSet<String>(16, 1.0f);
      
      /** the actions that have been hidden */
      private Set<String> hiddenActions = new HashSet<String>(4, 1.0f);
      
      public boolean ShowLink;
      public String Style;
      public String StyleClass;
   }
}
