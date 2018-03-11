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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.alfresco.web.config.DialogsConfigElement.DialogConfig;
import org.alfresco.web.ui.common.component.UIActionLink;

/**
 * Bean that manages the dialog framework
 * 
 * @author gavinc
 */
public final class DialogManager implements Serializable
{
   private static final long serialVersionUID = -8690844273074627353L;

   public final static String BEAN_NAME = "DialogManager";
   
   private DialogState currentDialogState;
   private Map<String, String> paramsToApply;
   
   /**
    * Sets the parameters the next dialog will initialise with
    * 
    * @param params The parameters
    */
   public void setupParameters(Map<String, String> params)
   {
      this.paramsToApply = params;
   }
   
   /**
    * Action handler used to setup parameters for the dialog being launched
    * 
    * @param event The event containing the parameters
    */
   public void setupParameters(ActionEvent event)
   {
      // check the component the event come from was an action link
      UIComponent component = event.getComponent();
      if (component instanceof UIActionLink)
      {
         // store the parameters
         this.paramsToApply = ((UIActionLink)component).getParameterMap();
         
         // make sure "null" parameters are actually null, this can occur
         // when null parameters are sent to the client and posted back
         for (String name : this.paramsToApply.keySet())
         {
            String value = this.paramsToApply.get(name);
            if (value != null && value.equalsIgnoreCase("null"))
            {
               this.paramsToApply.put(name, null);
            }
         }
      }
   }
   
   /**
    * Sets the current dialog
    * 
    * @param config The configuration for the dialog to set
    */
   public void setCurrentDialog(DialogConfig config)
   {
      // make sure the managed bean is present
      String beanName = config.getManagedBean();
      
      Object bean = FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), beanName);

      if (bean == null)
      {
         throw new AlfrescoRuntimeException("Failed to start dialog as managed bean '" + beanName + 
               "' has not been defined");
      }
      
      // make sure the bean implements the IDialogBean interface
      IDialogBean dialog = null;
      if (bean instanceof IDialogBean)
      {
         dialog = (IDialogBean)bean;
      }
      else
      {
         throw new AlfrescoRuntimeException("Failed to start dialog as managed bean '" + beanName + 
               "' does not implement the required IDialogBean interface");
      }
      
      // create the DialogState object
      this.currentDialogState = new DialogState(config, dialog);
      
      // initialise the managed bean
      dialog.init(this.paramsToApply);
      
      // reset the current parameters so subsequent dialogs don't get them
      this.paramsToApply = null;
   }
   
   /**
    * Returns the state of the currently active dialog
    * 
    * @return Current dialog's state
    */
   public DialogState getState()
   {
      return this.currentDialogState;
   }
   
   /**
    * Restores the dialog represented by the given DialogState object.
    * NOTE: The dialog's restored() method is also called during this
    * method.
    * 
    * @param state The DialogState for the dialog to restore
    */
   public void restoreState(DialogState state)
   {
      this.currentDialogState = state;
      
      // retrieve the dialog and call it's restored() method
      this.currentDialogState.getDialog().restored();
   }
   
   /**
    * Returns the config for the current dialog
    * 
    * @return The current dialog config
    */
   public DialogConfig getCurrentDialog()
   {
      return this.currentDialogState.getConfig();
   }
   
   /**
    * Returns the current dialog bean being managed
    * 
    * @return The current managed bean
    */
   public IDialogBean getBean()
   {
      return this.currentDialogState.getDialog();
   }
   
   /**
    * Returns the icon to use for the current dialog
    * 
    * @return The icon
    */
   public String getIcon()
   {
      return this.currentDialogState.getConfig().getIcon();
   }
   
   /**
    * Returns the error message to use in error conditions
    * 
    * @return The error message
    */
   public String getErrorMessage()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), 
            this.currentDialogState.getConfig().getErrorMessageId());
   }
   
   /**
    * Returns the resolved title to use for the dialog
    * 
    * @return The title
    */
   public String getTitle()
   {
      // try and get the title directly from the dialog
      String title = this.currentDialogState.getDialog().getContainerTitle();
      
      if (title == null)
      {
         // try and get the title via a message bundle key
         title = this.currentDialogState.getConfig().getTitleId();
         
         if (title != null)
         {
            title = Application.getMessage(FacesContext.getCurrentInstance(), title);
         }
         else
         {
            // try and get the title from the configuration
            title = this.currentDialogState.getConfig().getTitle();
         }
      }
      
      return title;
   }
   
   /**
    * Returns the resolved subtitle to use for the dialog
    * 
    * @return The subtitle
    */
   public String getSubTitle()
   {
      // try and get the subtitle directly from the dialog
      String subTitle = this.currentDialogState.getDialog().getContainerSubTitle();
      
      if (subTitle == null)
      {
         // try and get the title via a message bundle key
         subTitle = this.currentDialogState.getConfig().getSubTitleId();
         
         if (subTitle != null)
         {
            subTitle = Application.getMessage(FacesContext.getCurrentInstance(), subTitle);
         }
         else
         {
            // try and get the title from the configuration
            subTitle = this.currentDialogState.getConfig().getSubTitle();
         }
      }
      
      return subTitle;
   }
   
   /**
    * Returns the resolved description to use for the dialog
    * 
    * @return The description
    */
   public String getDescription()
   {
      // try and get the description directly from the dialog
      String desc = this.currentDialogState.getDialog().getContainerDescription();
      
      if (desc == null)
      {
         // try and get the description via a message bundle key
         desc = this.currentDialogState.getConfig().getDescriptionId();
         
         if (desc != null)
         {
            desc = Application.getMessage(FacesContext.getCurrentInstance(), desc);
         }
         else
         {
            // try and get the description from the configuration
            desc = this.currentDialogState.getConfig().getDescription();
         }
      }
      
      return desc;
   }
      
   /**
    * Returns the page the dialog will use
    * 
    * @return The page
    */
   public String getPage()
   {
      return this.currentDialogState.getConfig().getPage();
   }
   
   /**
    * Returns the id of a configured action group representing the actions to
    * display for the dialog
    * 
    * @return The action group id
    */
   public String getActionsId()
   {
      // first see if the dialog itself has supplied an actions id
      String configId = this.currentDialogState.getDialog().getActionsConfigId();
       
      if (configId == null)
      {
         // see if an actions id was specified in the dialog config
         configId = this.currentDialogState.getConfig().getActionsConfigId();
      }
      
      return configId; 
   }
   
   /**
    * Returns the id of a configured action group representing the 
    * 'more actions' to  display for the dialog
    * 
    * @return The action group id
    */
   public String getMoreActionsId()
   {
      // first see if the dialog itself has supplied a more actions id
      String configId = this.currentDialogState.getDialog().getMoreActionsConfigId();
      
      if (configId == null)
      {
         // see if an actions id was specified in the dialog config
         configId = this.currentDialogState.getConfig().getMoreActionsConfigId();
      }
      
      return configId;
   }
   
   /**
    * Returns the object to use as the context for the main and more
    * actions that may be configured by the dialog
    * 
    * @return Object to use as the context for actions, the current
    *         node by default
    */
   public Object getActionsContext()
   {
      return this.currentDialogState.getDialog().getActionsContext();
   }
   
   /**
    * Returns the label to use for the main actions when a menu is being used
    * 
    * @return The actions menu label
    */
   public String getActionsMenuLabel()
   {
      // try and get the label via a message bundle key
      String label = this.currentDialogState.getConfig().getActionsMenuLabelId();
         
      if (label != null)
      {
         label = Application.getMessage(FacesContext.getCurrentInstance(), label);
      }
      else
      {
         // try and get the description from the configuration
         label = this.currentDialogState.getConfig().getActionsMenuLabel();
      }
      
      // if the label is still null use the default of 'Create'
      if (label == null)
      {
         label = Application.getMessage(FacesContext.getCurrentInstance(), "create_options");
      }
         
      return label;
   }
   
   /**
    * Returns the label to use for the more actions menu
    * 
    * @return The more actions menu label
    */
   public String getMoreActionsMenuLabel()
   {
      // try and get the label via a message bundle key
      String label = this.currentDialogState.getConfig().getMoreActionsMenuLabelId();
         
      if (label != null)
      {
         label = Application.getMessage(FacesContext.getCurrentInstance(), label);
      }
      else
      {
         // try and get the description from the configuration
         label = this.currentDialogState.getConfig().getMoreActionsMenuLabel();
      }
      
      // if the label is still null use the default of 'More Actions'
      if (label == null)
      {
         label = Application.getMessage(FacesContext.getCurrentInstance(), "more_actions");
      }
         
      return label;
   }
   
   /**
    * Determines whether the main actions should be rendered as a 
    * menu
    * 
    * @return true to render the main set of actions as a menu, false
    *         to render them as a horizontal list
    */
   public boolean getActionsAsMenu()
   {
      return this.currentDialogState.getConfig().getActionsAsMenu();
   }
   
   /**
    * Determines whether the current dialog's OK button is visible
    * 
    * @return true if the OK button is visible, false if it's not
    */
   public boolean isOKButtonVisible()
   {
      return this.currentDialogState.getConfig().isOKButtonVisible();
   }
   
   /**
    * Determines whether the current dialog should display the next
    * and previous buttons in the header area
    * 
    * @return true if navigation support is enabled
    */
   public boolean isNavigationVisible()
   {
      return (getBean() instanceof NavigationSupport);
   }
   
   /**
    * Determines whether the current dialog should display the list
    * of views in the header area
    * 
    * @return true if change view support is enabled
    */
   public boolean isViewListVisible()
   {
      return (getBean() instanceof ChangeViewSupport);
   }
   
   /**
    * Determines whether the current dialog should display the list
    * of filters in the header area
    * 
    * @return true if filter support is enabled
    */
   public boolean isFilterListVisible()
   {
      return (getBean() instanceof FilterViewSupport);
   }
   
   /**
    * Returns a list of additional buttons to display in the dialog
    * 
    * @return List of button configurations
    */
   public List<DialogButtonConfig> getAdditionalButtons()
   {
      List<DialogButtonConfig> buttons = null;
      
      // get a list of buttons to display from the configuration
      List<DialogButtonConfig> cfgButtons = this.currentDialogState.getConfig().getButtons();
      
      // get a list of buttons added dynamically by the dialog
      List<DialogButtonConfig> dynButtons = this.currentDialogState.getDialog().getAdditionalButtons();

      if (cfgButtons != null && dynButtons != null)
      {
         // combine the two lists
         buttons = new ArrayList<DialogButtonConfig>(
               cfgButtons.size() + dynButtons.size());
         buttons.addAll(cfgButtons);
         buttons.addAll(dynButtons);
      }
      else if (cfgButtons != null && dynButtons == null)
      {
         buttons = cfgButtons;
      }
      else if (cfgButtons == null && dynButtons != null)
      {
         buttons = dynButtons;
      }
      
      return buttons;
   }
   
   /**
    * Returns the label to use for the cancel button
    * 
    * @return The cancel button label
    */
   public String getCancelButtonLabel()
   {
      return this.currentDialogState.getDialog().getCancelButtonLabel();
   }
   
   /**
    * Returns the label to use for the finish button
    * 
    * @return The finish button label
    */
   public String getFinishButtonLabel()
   {
      return this.currentDialogState.getDialog().getFinishButtonLabel();
   }
   
   /**
    * Determines whether the finish button on the dialog should be disabled
    * 
    * @return true if the button should be disabled
    */
   public boolean getFinishButtonDisabled()
   {
      return this.currentDialogState.getDialog().getFinishButtonDisabled();
   }
   
   /**
    * Method handler called when the finish button of the dialog is pressed
    * 
    * @return The outcome
    */
   public String finish()
   {
      return this.currentDialogState.getDialog().finish();
   }
   
   /**
    * Method handler called when the cancel button of the dialog is pressed
    * 
    * @return The outcome
    */
   public String cancel()
   {
      return this.currentDialogState.getDialog().cancel();
   }
}
