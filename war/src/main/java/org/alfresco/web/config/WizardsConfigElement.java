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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.extensions.config.ConfigElement;
import org.springframework.extensions.config.ConfigException;
import org.springframework.extensions.config.element.ConfigElementAdapter;
import org.springframework.extensions.surf.util.ParameterCheck;

/**
 * Custom config element that represents the config data for a property sheet
 * 
 * @author gavinc
 */
public class WizardsConfigElement extends ConfigElementAdapter
{
   public static final String CONFIG_ELEMENT_ID = "wizards";

   private Map<String, WizardConfig> wizards = new LinkedHashMap<String, WizardConfig>(8, 10f);
   
   /**
    * Default constructor
    */
   public WizardsConfigElement()
   {
      super(CONFIG_ELEMENT_ID);
   }
   
   /**
    * Constructor
    * 
    * @param name Name of the element this config element represents
    */
   public WizardsConfigElement(String name)
   {
      super(name);
   }
   
   /**
    * @see ConfigElement#getChildren()
    */
   public List<ConfigElement> getChildren()
   {
      throw new ConfigException("Reading the wizards config via the generic interfaces is not supported");
   }
   
   /**
    * @see ConfigElement#combine(org.springframework.extensions.config.ConfigElement)
    */
   public ConfigElement combine(ConfigElement configElement)
   {
      WizardsConfigElement combined = new WizardsConfigElement();
      
      // add all the wizards from this element
      for (WizardConfig wizard : this.getWizards().values())
      {
         combined.addWizard(wizard);
      }
      
      // add all the wizards from the given element
      for (WizardConfig wizard : ((WizardsConfigElement)configElement).getWizards().values())
      {
         combined.addWizard(wizard);
      }
      
      return combined;
   }
   
   /**
    * Returns the named wizard
    * 
    * @param name The name of the wizard to retrieve
    * @return The WizardConfig object for the requested wizard or null if it doesn't exist
    */
   public WizardConfig getWizard(String name)
   {
      return this.wizards.get(name);
   }
   
   /**
    * @return Returns a map of the wizards
    */
   public Map<String, WizardConfig> getWizards()
   {
      return this.wizards;
   }
   
   /**
    * Adds a wizard
    * 
    * @param wizardConfig A pre-configured wizard config object
    */
   /*package*/ void addWizard(WizardConfig wizardConfig)
   {
      this.wizards.put(wizardConfig.getName(), wizardConfig);
   }
   
   public abstract static class AbstractConfig implements Serializable
   {
      protected String title;
      protected String titleId;
      protected String description;
      protected String descriptionId;
      
      
      
      protected AbstractConfig()
      {
         super();
      }

      public AbstractConfig(String title, String titleId,
                            String description, String descriptionId)
      {
         this.title = title;
         this.titleId = titleId;
         this.description = description;
         this.descriptionId = descriptionId;
      }
      
      public String getDescription()
      {
         return this.description;
      }
      
      public String getDescriptionId()
      {
         return this.descriptionId;
      }
      
      public String getTitle()
      {
         return this.title;
      }
      
      public String getTitleId()
      {
         return this.titleId;
      }
   }
   
   /**
    * Represents the configuration of a single wizard i.e. the &lt;wizard&gt; element
    */
   public static class WizardConfig extends AbstractConfig implements Serializable
   {
      private static final long serialVersionUID = -3377339374041580932L;

      protected String subTitle;
      protected String subTitleId;
      protected String name;
      protected String managedBean;
      protected String icon;
      protected String errorMsgId = "error_wizard";
      
      protected Map<String, StepConfig> steps = new LinkedHashMap<String, StepConfig>(4);

      protected WizardConfig()
      {
         super();
      }
      
      public WizardConfig(String name, String bean, String icon,
                          String title, String titleId,
                          String subTitle, String subTitleId,
                          String description, String descriptionId,
                          String errorMsgId)
      {
         super(title, titleId, description, descriptionId);

         // check we have a name
         ParameterCheck.mandatoryString("name", name);
         
         this.subTitle = subTitle;
         this.subTitleId = subTitleId;
         this.name = name;
         this.managedBean = bean;
         this.icon = icon;
         
         if (errorMsgId != null && errorMsgId.length() > 0)
         {
            this.errorMsgId = errorMsgId;
         }
      }
      
      public String getName()
      {
         return this.name;
      }
      
      public String getManagedBean()
      {
         return this.managedBean;
      }
      
      public String getSubTitle()
      {
         return this.subTitle;
      }
      
      public String getSubTitleId()
      {
         return this.subTitleId;
      }
      
      public String getIcon()
      {
         return this.icon;
      }
      
      public String getErrorMessageId()
      {
         return this.errorMsgId;
      }
      
      public int getNumberSteps()
      {
         return this.steps.size();
      }
      
      public Map<String, StepConfig> getSteps()
      {
         return this.steps;
      }
      
      public List<StepConfig> getStepsAsList()
      {
         List<StepConfig> stepList = new ArrayList<StepConfig>(this.steps.size());
         
         for (StepConfig stepCfg : this.steps.values())
         {
            stepList.add(stepCfg);
         }
         
         return stepList;
      }
      
      public StepConfig getStepByName(String name)
      {
         return this.steps.get(name);
      }
      
      /*package*/ void addStep(StepConfig step)
      {
         this.steps.put(step.getName(), step);
      }
      
      /**
       * @see java.lang.Object#toString()
       */
      @Override 
      public String toString()
      {
         StringBuilder buffer = new StringBuilder(super.toString());
         buffer.append(" (name=").append(this.name);
         buffer.append(" managed-bean=").append(this.managedBean);
         buffer.append(" icon=").append(this.icon);
         buffer.append(" title=").append(this.title);
         buffer.append(" titleId=").append(this.titleId);
         buffer.append(" subTitle=").append(this.subTitle);
         buffer.append(" subTitleId=").append(this.subTitleId);
         buffer.append(" description=").append(this.description);
         buffer.append(" descriptionId=").append(this.descriptionId);
         buffer.append(" errorMsgId=").append(this.errorMsgId).append(")");
         return buffer.toString();
      }
   }
   
   /**
    * Represents the configuration of a page in a wizard i.e. the &lt;page&gt; element
    */
   public static class PageConfig extends AbstractConfig implements Serializable
   {
      private static final long serialVersionUID = 4154515148190230391L;
      
      protected String path;
      protected String instruction;
      protected String instructionId;
      
      public PageConfig(String path, 
                        String title, String titleId,
                        String description, String descriptionId,
                        String instruction, String instructionId)
      {
         super(title, titleId, description, descriptionId);

         // check we have a path
         ParameterCheck.mandatoryString("path", path);
         
         this.path = path;
         this.instruction = instruction;
         this.instructionId = instructionId;
      }
      
      public String getPath()
      {
         return this.path;
      }
      
      public String getInstruction()
      {
         return this.instruction;
      }

      public String getInstructionId()
      {
         return this.instructionId;
      }
      
      /**
       * @see java.lang.Object#toString()
       */
      @Override 
      public String toString()
      {
         StringBuilder buffer = new StringBuilder(super.toString());
         buffer.append(" (path=").append(this.path);
         buffer.append(" title=").append(this.title);
         buffer.append(" titleId=").append(this.titleId);
         buffer.append(" description=").append(this.description);
         buffer.append(" descriptionId=").append(this.descriptionId);
         buffer.append(" instruction=").append(this.instruction);
         buffer.append(" instructionId=").append(this.instructionId).append(")");
         return buffer.toString();
      }
   }
   
   /**
    * Represents the configuration of a conditional page in a wizard 
    * i.e. a &lt;page&gt; element that is placed within a &lt;condition&gt;
    * element.
    */
   public static class ConditionalPageConfig extends PageConfig
   {
      private static final long serialVersionUID = -3398913681170199314L;
      
      protected String condition;
      
      public ConditionalPageConfig(String path, String condition,
                                   String title, String titleId,
                                   String description, String descriptionId,
                                   String instruction, String instructionId)
      {
         super(path, title, titleId, description, descriptionId, instruction, instructionId);

         // check we have a path
         ParameterCheck.mandatoryString("condition", condition);
         
         this.condition = condition;
      }
      
      public String getCondition()
      {
         return this.condition;
      }
      
      /**
       * @see java.lang.Object#toString()
       */
      @Override 
      public String toString()
      {
         StringBuilder buffer = new StringBuilder(super.toString());
         buffer.append(" (path=").append(this.path);
         buffer.append(" condition=").append(this.condition);
         buffer.append(" title=").append(this.title);
         buffer.append(" titleId=").append(this.titleId);
         buffer.append(" description=").append(this.description);
         buffer.append(" descriptionId=").append(this.descriptionId);
         buffer.append(" instruction=").append(this.instruction);
         buffer.append(" instructionId=").append(this.instructionId).append(")");
         return buffer.toString();
      }
   }
   
   /**
    * Represents the configuration of a step in a wizard
    * i.e. the &lt;step&gt; element.
    */
   public static class StepConfig extends AbstractConfig implements Serializable
   {
      private static final long serialVersionUID = -3707570689181455754L;
      
      protected String name;
      protected PageConfig defaultPage;
      protected List<ConditionalPageConfig> conditionalPages = 
         new ArrayList<ConditionalPageConfig>(3);
      
      public StepConfig(String name,
                        String title, String titleId,
                        String description, String descriptionId)
      {
         super(title, titleId, description, descriptionId);
         
         // check we have a name
         ParameterCheck.mandatoryString("name", name);
         
         // check we have a title
         if (this.title == null && this.titleId == null)
         {
            throw new IllegalArgumentException("A title or title-id attribute must be supplied for a step");
         }
         
         this.name = name;
      }
      
      public String getName()
      {
         return this.name;
      }
      
      public PageConfig getDefaultPage()
      {
         return this.defaultPage;
      }
      
      public boolean hasConditionalPages()
      {
         return (this.conditionalPages.size() > 0);
      }
      
      public List<ConditionalPageConfig> getConditionalPages()
      {
         return this.conditionalPages;
      }
      
      /*package*/ void addConditionalPage(ConditionalPageConfig conditionalPage)
      {
         this.conditionalPages.add(conditionalPage);
      }
      
      /*package*/ void setDefaultPage(PageConfig page)
      {
         this.defaultPage = page;
      }

      /**
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString()
      {
         StringBuilder buffer = new StringBuilder(super.toString());
         buffer.append(" (name=").append(this.name);
         buffer.append(" title=").append(this.title);
         buffer.append(" titleId=").append(this.titleId);
         buffer.append(" description=").append(this.description);
         buffer.append(" descriptionId=").append(this.descriptionId);
         buffer.append(" defaultPage=").append(this.defaultPage);
         buffer.append(" conditionalPages=").append(this.conditionalPages).append(")");
         return buffer.toString();
      }
   }
}
