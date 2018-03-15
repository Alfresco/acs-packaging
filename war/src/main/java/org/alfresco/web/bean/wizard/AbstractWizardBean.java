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
package org.alfresco.web.bean.wizard;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.bean.BrowseBean;
import org.alfresco.web.bean.NavigationBean;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract bean used as the base class for all wizard backing beans.
 * 
 * @author gavinc
 */
public abstract class AbstractWizardBean implements Serializable
{
   private static Log logger = LogFactory.getLog(AbstractWizardBean.class);
   
   /** I18N messages */
   private static final String MSG_NOT_SET = "value_not_set";
   
   protected static final String FINISH_OUTCOME = "finish";
   protected static final String CANCEL_OUTCOME = "cancel";
   protected static final String DEFAULT_INSTRUCTION_ID = "default_instruction";
   protected static final String SUMMARY_TITLE_ID = "summary";
   protected static final String SUMMARY_DESCRIPTION_ID = "summary_desc";
   
   // common wizard properties
   protected int currentStep = 1;
   protected boolean editMode = false;
   transient private NodeService nodeService;
   transient private FileFolderService fileFolderService;
   transient private SearchService searchService;
   protected NavigationBean navigator;
   protected BrowseBean browseBean;
   
   /**
    * @return Returns the wizard description
    */
   public abstract String getWizardDescription();

   /**
    * @return Returns the wizard title
    */
   public abstract String getWizardTitle();
   
   /**
    * @return Returns the title for the current step
    */
   public abstract String getStepTitle();

   /**
    * @return Returns the description for the current step
    */
   public abstract String getStepDescription();
   
   /**
    * @return Returns the instructional text for the current step
    */
   public abstract String getStepInstructions();

   /**
    * Determines the outcome string for the given step number
    * 
    * @param step The step number to get the outcome for
    * @return The outcome
    */
   protected abstract String determineOutcomeForStep(int step);
   
   /**
    * Handles the finish button being pressed
    * 
    * @return The finish outcome
    */
   public abstract String finish();
   
   /**
    * Action listener called when the wizard is being launched allowing
    * state to be setup
    */
   public void startWizard(ActionEvent event)
   {
      // refresh the UI, calling this method now is fine as it basically makes sure certain
      // beans clear the state - so when we finish the wizard other beans will have been reset
      UIContextService.getInstance(FacesContext.getCurrentInstance()).notifyBeans();
      
      // make sure the wizard is not in edit mode
      this.editMode = false;
      
      // initialise the wizard in case we are launching 
      // after it was navigated away from
      init();
      
      if (logger.isDebugEnabled())
         logger.debug("Started wizard : " + getWizardTitle());
   }
   
   /**
    * Action listener called when the wizard is being launched for 
    * editing an existing node.
    */
   public void startWizardForEdit(ActionEvent event)
   {
      // refresh the UI, calling this method now is fine as it basically makes sure certain
      // beans clear the state - so when we finish the wizard other beans will have been reset
      UIContextService.getInstance(FacesContext.getCurrentInstance()).notifyBeans();
      
      // set the wizard in edit mode
      this.editMode = true;
      
      // populate the wizard's default values with the current value
      // from the node being edited
      init();
      populate();
      
      if (logger.isDebugEnabled())
         logger.debug("Started wizard : " + getWizardTitle() + " for editing");
   }
 
   /**
    * Determines whether the wizard is in edit mode
    * 
    * @return true if the wizard is in edit mode, false if it is in creation mode
    */
   public boolean isInEditMode()
   {
      return this.editMode;
   }
   
   /**
    * Deals with the next button being pressed
    * 
    * @return The outcome for the next step
    */
   public String next()
   {  
      this.currentStep++;
      
      // determine which page to go to next
      String outcome = determineOutcomeForStep(this.currentStep);
            
      if (logger.isDebugEnabled())
      {
         logger.debug("current step is now: " + this.currentStep);
         logger.debug("Next outcome: " + outcome);
      }
      
      // return the outcome for navigation
      return outcome;
   }
   
   /**
    * Deals with the back button being pressed
    * 
    * @return The outcome for the previous step
    */
   public String back()
   {      
      this.currentStep--;
      
      // determine which page to go to next
      String outcome = determineOutcomeForStep(this.currentStep);
      
      if (logger.isDebugEnabled())
      {
         logger.debug("current step is now: " + this.currentStep);
         logger.debug("Back outcome: " + outcome);
      }
      
      // return the outcome for navigation
      return outcome;
   }
   
   /**
    * Handles the cancelling of the wizard
    * 
    * @return The cancel outcome
    */
   public String cancel()
   {
      // reset the state
      init();
      
      return CANCEL_OUTCOME;
   }
   
   /**
    * Initialises the wizard
    */
   public void init()
   {
      this.currentStep = 1;
   }
   
   /**
    * Populates the wizard's values with the current values
    * of the node about to be edited
    */
   public void populate()
   {
      // subclasses will override this method to setup accordingly
   }

   /**
    * @return Returns the nodeService.
    */
   public NodeService getNodeService()
   {
      if (nodeService == null)
      {
         nodeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNodeService();
      }
      return this.nodeService;
   }

   /**
    * @param nodeService The nodeService to set.
    */
   public void setNodeService(NodeService nodeService)
   {
      this.nodeService = nodeService;
   }

   /**
    * @param fileFolderService used to manipulate folder/folder model nodes
    */
   public void setFileFolderService(FileFolderService fileFolderService)
   {
      this.fileFolderService = fileFolderService;
   }
   
   protected FileFolderService getFileFolderService()
   {
      if (fileFolderService == null)
      {
         fileFolderService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getFileFolderService();
      }
      return fileFolderService;
   }

   /**
    * @param searchService the service used to find nodes
    */
   public void setSearchService(SearchService searchService)
   {
      this.searchService = searchService;
   }
   
   protected SearchService getSearchService()
   {
      if (searchService == null)
      {
         searchService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getSearchService();
      }
      return searchService;
   }

   /**
    * @return Returns the navigation bean instance.
    */
   public NavigationBean getNavigator()
   {
      return navigator;
   }
   
   /**
    * @param navigator The NavigationBean to set.
    */
   public void setNavigator(NavigationBean navigator)
   {
      this.navigator = navigator;
   }

   /**
    * @return The BrowseBean
    */
   public BrowseBean getBrowseBean()
   {
      return this.browseBean;
   }

   /**
    * @param browseBean The BrowseBean to set.
    */
   public void setBrowseBean(BrowseBean browseBean)
   {
      this.browseBean = browseBean;
   }
   
   /**
    * Build summary table from the specified list of Labels and Values
    * 
    * @param labels     Array of labels to display
    * @param values     Array of values to display
    * 
    * @return summary table HTML
    */
   protected String buildSummary(String[] labels, String[] values)
   {
      if (labels == null || values == null || labels.length != values.length)
      {
         throw new IllegalArgumentException("Labels and Values passed to summary must be valid and of equal length.");
      }
      
      String msg = Application.getMessage(FacesContext.getCurrentInstance(), MSG_NOT_SET);
      String notSetMsg = "&lt;" + msg + "&gt;";
      
      StringBuilder buf = new StringBuilder(256);
      
      buf.append("<table cellspacing='4' cellpadding='2' border='0' class='summary'>");
      for (int i=0; i<labels.length; i++)
      {
         String value = values[i];
         buf.append("<tr><td valign='top'><b>");
         buf.append(labels[i]);
         buf.append(":</b></td><td>");
         buf.append(value != null ? Utils.encode(value) : notSetMsg);
         buf.append("</td></tr>");
      }
      buf.append("</table>");
      
      return buf.toString();
   }
}
