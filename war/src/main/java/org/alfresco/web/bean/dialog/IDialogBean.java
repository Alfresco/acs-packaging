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
package org.alfresco.web.bean.dialog;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;

/**
 * Interface that defines the contract for a dialog backing bean
 * 
 * @author gavinc
 */
public interface IDialogBean extends Serializable
{
   /**
    * Initialises the dialog bean
    * 
    * @param parameters Map of parameters for the dialog
    */
   public void init(Map<String, String> parameters);
   
   /**
    * Called when the dialog is restored after a nested dialog is closed
    */
   public void restored();
   
   /**
    * Method handler called when the cancel button of the dialog is pressed
    * 
    * @return The outcome to return
    */
   public String cancel();
   
   /**
    * Method handler called when the finish button of the dialog is pressed
    * 
    * @return The outcome to return
    */
   public String finish();
   
   /**
    * Returns a list of additional buttons to display in the dialog.
    * 
    * @return List of button configurations, null if there are no buttons
    */
   public List<DialogButtonConfig> getAdditionalButtons();
   
   /**
    * Returns the label to use for the cancel button
    * 
    * @return The cancel button label
    */
   public String getCancelButtonLabel();
   
   /**
    * Returns the label to use for the finish button
    * 
    * @return The finish button label
    */
   public String getFinishButtonLabel();
   
   /**
    * Determines whether the finish button on the dialog should be disabled
    * 
    * @return true if the button should be disabled
    */
   public boolean getFinishButtonDisabled();
   
   /**
    * Returns the title to be used for the dialog
    * <p>If this returns null the DialogManager will
    * lookup the title via the dialog configuration</p>
    * 
    * @return The title or null if the title is to be acquired via configuration
    */
   public String getContainerTitle();
   
   /**
    * Returns the subtitle to be used for the dialog
    * <p>If this returns null the DialogManager will
    * lookup the subtitle via the dialog configuration</p>
    * 
    * @return The subtitle or null if the subtitle is to be acquired via configuration
    */
   public String getContainerSubTitle();
   
   /**
    * Returns the description to be used for the dialog
    * <p>If this returns null the DialogManager will
    * lookup the description via the dialog configuration</p>
    * 
    * @return The title or null if the title is to be acquired via configuration
    */
   public String getContainerDescription();
   
   /**
    * Returns the object to use as the context for the main and more
    * actions that may be configured by the dialog
    * 
    * @return Object to use as the context for actions
    */
   public Object getActionsContext();
   
   /**
    * Returns the id of an action group to use for the main actions
    * 
    * @return Id of an action group
    */
   public String getActionsConfigId();
   
   /**
    * Returns the id of an action group to use for the more actions
    * 
    * @return Id of an action group
    */
   public String getMoreActionsConfigId();
   
   /**
    * Determines whether the has finished
    *  
    * @return true if the wizard has finished
    */
   public boolean isFinished();
}
