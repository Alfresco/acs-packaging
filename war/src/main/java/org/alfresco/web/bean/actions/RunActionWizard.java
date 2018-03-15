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
package org.alfresco.web.bean.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.repo.action.executer.CheckInActionExecuter;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.data.IDataContainer;
import org.alfresco.web.data.QuickSort;
import org.alfresco.web.ui.common.Utils;

/**
 * Bean implementation for the "Run Action" wizard.
 * 
 * @author gavinc 
 */
public class RunActionWizard extends BaseActionWizard
{
   private static final long serialVersionUID = 975435581009378899L;
   
   protected boolean checkinActionPresent = false;
   
   // ------------------------------------------------------------------------------
   // Wizard implementation
   
   @Override
   public void init(Map<String, String> parameters)
   {
      super.init(parameters);
      
      this.checkinActionPresent = false;
   }
   
   protected String finishImpl(FacesContext context, String outcome)
         throws Exception
   {
      // execute each action added in the wizard
      for (Map<String, Serializable> actionParams : this.allActionsProperties)
      {
         // use the base class version of buildActionParams(), but for this we need 
         // to setup the currentActionProperties and action variables
         String actionName = (String)actionParams.get(PROP_ACTION_NAME);
         this.action = actionName;
         
         // remember the fact we have a checkin action
         if (actionName.equals(CheckInActionExecuter.NAME))
         {
            this.checkinActionPresent = true;
         }
         
         // get the action handler to prepare for the save
         Map<String, Serializable> repoActionParams = new HashMap<String, Serializable>();
         IHandler handler = this.actionHandlers.get(this.action);
         if (handler != null)
         {
            handler.prepareForSave(actionParams, repoActionParams);
         }
         
         // add the action to the rule
         Action action = this.getActionService().createAction(actionName);
         action.setParameterValues(repoActionParams);
         
         // execute the action on the current document node
         NodeRef nodeRef = new NodeRef(Repository.getStoreRef(), this.parameters.get("id"));
         this.getActionService().executeAction(action, nodeRef);
      }

      return outcome;
   }
   
   @Override
   public List<SelectItem> getActions()
   {
      if (this.actions == null)
      {
         NodeRef nodeRef = new NodeRef(Repository.getStoreRef(), this.parameters.get("id"));
         List<ActionDefinition> ruleActions = this.getActionService().getActionDefinitions(nodeRef);
         this.actions = new ArrayList<SelectItem>();
         for (ActionDefinition ruleActionDef : ruleActions)
         {
            String title = ruleActionDef.getTitle();
            if (title == null || title.length() == 0)
            {
               title = ruleActionDef.getName();
            }
            this.actions.add(new SelectItem(ruleActionDef.getName(), title));
         }
         
         // make sure the list is sorted by the label
         QuickSort sorter = new QuickSort(this.actions, "label", true, IDataContainer.SORT_CASEINSENSITIVE);
         sorter.sort();
         
         // add the select an action item at the start of the list
         this.actions.add(0, new SelectItem("null", 
               Application.getMessage(FacesContext.getCurrentInstance(), "select_an_action")));
      }
      
      return this.actions;
   }
   
   @Override
   protected String doPostCommitProcessing(FacesContext context, String outcome)
   {
      // reset the current document properties/aspects in case we have changed them
      // during the execution of the custom action
      Node document = this.browseBean.getDocument();
      if (document != null)
      {
         document.reset();
      }
      
      // reset the current space properties/aspects as well in case we have 
      // changed them during the execution of the custom action
      Node space = this.browseBean.getActionSpace();
      if (space != null)
      {
         space.reset();
      }
      
      // special case handling for checkin - if it was successful the working
      // copy node the Run Action Wizard was launched against will no longer
      // exist, we therefore need the client to go back to the main browse view.
      if (this.checkinActionPresent)
      {
         outcome = "browse";
      }
      
      return outcome;
   }
   
   @Override
   protected String getErrorMessageId()
   {
      return "error_actions";
   }

   /**
    * @return Returns the summary data for the wizard.
    */
   public String getSummary()
   {
      // create the summary using all the actions
      StringBuilder actionsSummary = new StringBuilder();
      for (Map<String, Serializable> props : this.allActionsProperties)
      {
         actionsSummary.append(Utils.encode(props.get(PROP_ACTION_SUMMARY).toString()));
         actionsSummary.append("<br>");
      }
      
      ResourceBundle bundle = Application.getBundle(FacesContext.getCurrentInstance());

      return buildSummary(
            new String[] {bundle.getString("actions")},
            new String[] {actionsSummary.toString()});
   }
   
   @Override
   public boolean getNextButtonDisabled()
   {
      return (this.allActionsDataModel.getRowCount() == 0);
   }

   @Override
   public boolean getFinishButtonDisabled()
   {
      return (this.allActionsDataModel.getRowCount() == 0);
   }
}
