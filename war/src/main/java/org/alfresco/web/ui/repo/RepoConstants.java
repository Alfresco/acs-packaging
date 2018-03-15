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
package org.alfresco.web.ui.repo;

/**
 * @author Kevin Roast
 */
public final class RepoConstants
{
   // TODO: move these into the respective components as static members - as per JSF spec
   
   public static final String ALFRESCO_FACES_ASSOCIATION          = "org.alfresco.faces.Association";
   public static final String ALFRESCO_FACES_CHILD_ASSOCIATION    = "org.alfresco.faces.ChildAssociation";
   public static final String ALFRESCO_FACES_PROPERTY             = "org.alfresco.faces.Property";
   public static final String ALFRESCO_FACES_SEPARATOR            = "org.alfresco.faces.Separator";
   public static final String ALFRESCO_FACES_SPACE_SELECTOR       = "org.alfresco.faces.SpaceSelector";
   public static final String ALFRESCO_FACES_ASSOC_EDITOR         = "org.alfresco.faces.AssociationEditor";
   public static final String ALFRESCO_FACES_CHILD_ASSOC_EDITOR   = "org.alfresco.faces.ChildAssociationEditor";
   public static final String ALFRESCO_FACES_DATE_PICKER_RENDERER = "org.alfresco.faces.DatePickerRenderer";
   public static final String ALFRESCO_FACES_CATEGORY_SELECTOR    = "org.alfresco.faces.CategorySelector";
   public static final String ALFRESCO_FACES_IMAGE_PICKER         = "org.alfresco.faces.ImagePicker";
   public static final String ALFRESCO_FACES_LIST_ITEMS           = "org.alfresco.faces.ListItems";
   public static final String ALFRESCO_FACES_MULTIVALUE_EDITOR    = "org.alfresco.faces.MultiValueEditor";
   public static final String ALFRESCO_FACES_FIELD_RENDERER       = "org.alfresco.faces.Field";
   public static final String ALFRESCO_FACES_SELECTOR_RENDERER    = "org.alfresco.faces.Selector";
   public static final String ALFRESCO_FACES_RADIO_PANEL_RENDERER = "org.alfresco.faces.RadioPanel";
   public static final String ALFRESCO_FACES_XMLDATE_CONVERTER    = "org.alfresco.faces.XMLDateConverter";
   public static final String ALFRESCO_FACES_MULTIVALUE_CONVERTER = "org.alfresco.faces.MultiValueConverter";
   public static final String ALFRESCO_FACES_BOOLEAN_CONVERTER    = "org.alfresco.faces.BooleanLabelConverter";
   public static final String ALFRESCO_FACES_MLTEXT_RENDERER      = "org.alfresco.faces.MultilingualText";
   public static final String ALFRESCO_FACES_MLTEXTAREA_RENDERER  = "org.alfresco.faces.MultilingualTextArea";
   public static final String ALFRESCO_FACES_AJAX_TAG_PICKER      = "org.alfresco.faces.AjaxTagPicker";
   public static final String ALFRESCO_FACES_TAG_SELECTOR         = "org.alfresco.faces.TagSelector"; 
   
   public static final String GENERATOR_LABEL = "LabelGenerator";
   public static final String GENERATOR_TEXT_FIELD = "TextFieldGenerator";
   public static final String GENERATOR_MLTEXT_FIELD = "MultilingualTextFieldGenerator";
   public static final String GENERATOR_TEXT_AREA = "TextAreaGenerator";
   public static final String GENERATOR_CHECKBOX = "CheckboxGenerator";
   public static final String GENERATOR_DATE_PICKER = "DatePickerGenerator";
   public static final String GENERATOR_DATETIME_PICKER = "DateTimePickerGenerator";
   public static final String GENERATOR_CATEGORY_SELECTOR = "CategorySelectorGenerator";
   public static final String GENERATOR_ASSOCIATION = "AssociationGenerator";
   public static final String GENERATOR_CHILD_ASSOCIATION = "ChildAssociationGenerator";
   public static final String GENERATOR_SEPARATOR = "SeparatorGenerator";
   public static final String GENERATOR_HEADER_SEPARATOR = "HeaderSeparatorGenerator";
   
   /**
    * Private constructor
    */
   private RepoConstants()
   {
   }
}
