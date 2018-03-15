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

import javax.faces.context.FacesContext;

import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.Utils;

/**
 * Base class for all wizard beans providing common functionality
 * 
 * @author gavinc
 */
public abstract class BaseWizardBean extends BaseDialogBean implements IWizardBean
{
   private static final String MSG_NOT_SET = "value_not_set";
   
   public String next()
   {
      if (isFinished())
      {
         Utils.addErrorMessage(Application.getMessage(FacesContext.getCurrentInstance(), 
                  "error_wizard_completed_already"));
      }
      
      return null;
   }
   
   public String back()
   {
      if (isFinished())
      {
         Utils.addErrorMessage(Application.getMessage(FacesContext.getCurrentInstance(), 
                  "error_wizard_completed_already"));
      }
      
      return null;
   }
   
   public boolean getNextButtonDisabled()
   {
      return false;
   }
   
   public String getNextButtonLabel()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), "next_button");
   }
   
   public String getBackButtonLabel()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), "back_button");
   }

   public String getFinishButtonLabel()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), "finish_button");
   }

   public String getStepTitle()
   {
      return null;
   }

   public String getStepDescription()
   {
      return null;
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
      
      StringBuilder buf = new StringBuilder(512);
      
      buf.append("<table cellspacing='4' cellpadding='2' border='0' class='summary'>");
      for (int i=0; i<labels.length; i++)
      {
         String value = values[i];
         buf.append("<tr><td valign='top'><b>");
         buf.append(labels[i]);
         buf.append(":</b></td><td>");
         buf.append(value != null ? value : notSetMsg);
         buf.append("</td></tr>");
      }
      buf.append("</table>");
      
      return buf.toString();
   }
   
   @Override
   protected String getDefaultCancelOutcome()
   {
      return AlfrescoNavigationHandler.CLOSE_WIZARD_OUTCOME;
   }

   @Override
   protected String getDefaultFinishOutcome()
   {
      return AlfrescoNavigationHandler.CLOSE_WIZARD_OUTCOME;
   }
}
