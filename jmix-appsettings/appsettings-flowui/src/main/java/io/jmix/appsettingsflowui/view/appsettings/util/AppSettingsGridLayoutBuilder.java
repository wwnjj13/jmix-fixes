/*
 * Copyright 2022 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.appsettingsflowui.view.appsettings.util;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import io.jmix.appsettings.AppSettings;
import io.jmix.appsettings.AppSettingsTools;
import io.jmix.core.AccessManager;
import io.jmix.core.MessageTools;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.core.common.util.ParamsMap;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.metamodel.model.MetadataObject;
import io.jmix.core.metamodel.model.Range;
import io.jmix.flowui.Actions;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.accesscontext.FlowuiEntityAttributeContext;
import io.jmix.flowui.accesscontext.FlowuiEntityContext;
import io.jmix.flowui.action.entitypicker.EntityClearAction;
import io.jmix.flowui.action.entitypicker.EntityLookupAction;
import io.jmix.flowui.component.ComponentContainer;
import io.jmix.flowui.component.ComponentGenerationContext;
import io.jmix.flowui.component.SupportsTypedValue;
import io.jmix.flowui.component.UiComponentsGenerator;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.data.SupportsValueSource;
import io.jmix.flowui.data.ValueSource;
import io.jmix.flowui.data.value.ContainerValueSource;
import io.jmix.flowui.model.DataComponents;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.OpenMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.persistence.Convert;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "unchecked"})
@Component("appset_AppSettingsGridLayoutBuilder")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AppSettingsGridLayoutBuilder {

    private static final int MAX_TEXT_FIELD_STRING_LENGTH = 255;
    private static final Integer MAX_CAPTION_LENGTH = 50;
    private static final String FIELD_WIDTH = "350px";

    @Autowired
    protected UiComponents uiComponents;

    @Autowired
    protected AppSettings appSettings;

    @Autowired
    protected AppSettingsTools appSettingsTools;

    @Autowired
    protected MetadataTools metadataTools;

    @Autowired
    protected DataComponents dataComponents;

    @Autowired
    protected UiComponentsGenerator uiComponentsGenerator;

    @Autowired
    protected Actions actions;

    @Autowired
    protected Messages messages;

    @Autowired
    protected MessageTools messageTools;

    @Autowired
    protected AccessManager accessManager;

    private final InstanceContainer container;
    private com.vaadin.flow.component.Component ownerComponent;

    public static AppSettingsGridLayoutBuilder of(ApplicationContext applicationContext, InstanceContainer container) {
        return applicationContext.getBean(AppSettingsGridLayoutBuilder.class, container);
    }

    protected AppSettingsGridLayoutBuilder(InstanceContainer container) {
        this.container = container;
    }

    public AppSettingsGridLayoutBuilder withOwnerComponent(com.vaadin.flow.component.Component component) {
        this.ownerComponent = component;
        return this;
    }

    public FormLayout build() {
        MetaClass metaClass = container.getEntityMetaClass();
        List<MetaProperty> metaProperties = collectMetaProperties(metaClass, container.getItem()).stream()
                .sorted(Comparator.comparing(MetadataObject::getName))
                .collect(Collectors.toList());

        FormLayout formLayout = uiComponents.create(FormLayout.class);
//        formLayout.setSpacing(true);
//        formLayout.setMargin(false, true, false, false);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 2),
                new FormLayout.ResponsiveStep("40em", 3));
        //setColumns(3);
//        formLayout.setRows(metaProperties.size() + 1);

        if (ownerComponent != null) {
            ownerComponent.getElement().appendChild(formLayout.getElement());
//            ((ComponentContainer) ownerComponent)..getComponents().add(formLayout);
        }
        formLayout.add(uiComponents.create(Div.class));

        Span currentValueLabel = uiComponents.create(Span.class);
        currentValueLabel.setText(messages.getMessage(this.getClass(), "currentValueLabel"));
//        currentValueLabel.setAlignment(io.jmix.ui.component.Component.Alignment.MIDDLE_LEFT);
        formLayout.add(currentValueLabel);

        Span defaultValueLabel = uiComponents.create(Span.class);
        defaultValueLabel.setText(messages.getMessage(this.getClass(), "defaultValueLabel"));
//        currentValueLabel.setAlignment(MIDDLE_LEFT);
        formLayout.add(defaultValueLabel);

        for (int i = 0; i < metaProperties.size(); i++) {
            addRowToGrid(container, formLayout, i, metaProperties.get(i));
        }

        return formLayout;
    }

    protected List<MetaProperty> collectMetaProperties(MetaClass metaClass, Object item) {
        List<MetaProperty> result = new ArrayList<>();
        for (MetaProperty metaProperty : metaClass.getProperties()) {
            switch (metaProperty.getType()) {
                case DATATYPE:
                case ENUM:
                    //skip system properties
                    if (metadataTools.isSystem(metaProperty)) {
                        continue;
                    }
                    if (metaProperty.getType() != MetaProperty.Type.ENUM
                            && (EntityUtils.isByteArray(metaProperty) || EntityUtils.isUuid(metaProperty))) {
                        continue;
                    }
                    if (metadataTools.isAnnotationPresent(item, metaProperty.getName(), Convert.class)) {
                        continue;
                    }
                    result.add(metaProperty);
                    break;
                case COMPOSITION:
                case ASSOCIATION:
                    if (!EntityUtils.isMany(metaProperty)) {
                        result.add(metaProperty);
                    }
                    break;
                default:
                    break;
            }
        }

        return result;
    }

    protected void addRowToGrid(InstanceContainer container, FormLayout formLayout, int currentRow, MetaProperty metaProperty) {
        MetaClass metaClass = container.getEntityMetaClass();
        Range range = metaProperty.getRange();

        FlowuiEntityAttributeContext attributeContext = new FlowuiEntityAttributeContext(metaClass, metaProperty.getName());
        accessManager.applyRegisteredConstraints(attributeContext);
        if (!attributeContext.canView()) {
            return;
        }

        if (range.isClass()) {
            FlowuiEntityContext entityContext = new FlowuiEntityContext(range.asClass());
            accessManager.applyRegisteredConstraints(entityContext);
            if (!entityContext.isViewPermitted()) {
                return;
            }
        }

        //add label
        Span fieldLabel = uiComponents.create(Span.class);
        fieldLabel.setText(getPropertyCaption(metaClass, metaProperty));

//        fieldLabel.setAlignment(io.jmix.ui.component.Component.Alignment.MIDDLE_LEFT);

        formLayout.add(fieldLabel);

        //current field
        ValueSource valueSource = new ContainerValueSource<>(container, metaProperty.getName());
        ComponentGenerationContext componentContext = new ComponentGenerationContext(metaClass, metaProperty.getName());
        componentContext.setValueSource(valueSource);
        formLayout.add(createField(metaProperty, range, componentContext));

        //default value
        ComponentGenerationContext componentContextForDefaultField = new ComponentGenerationContext(metaClass, metaProperty.getName());
        ValueSource valueSourceForDefaultField = new ContainerValueSource<>(dataComponents.createInstanceContainer(metaClass.getJavaClass()), metaProperty.getName());
        componentContextForDefaultField.setValueSource(valueSourceForDefaultField);
        AbstractField defaultValueField = createField(metaProperty, range, componentContextForDefaultField);
        if (defaultValueField instanceof SupportsTypedValue) {
            ((SupportsTypedValue<?, ?, Object, ?>) defaultValueField)
                    .setTypedValue(appSettingsTools.getDefaultPropertyValue(metaClass.getJavaClass(), metaProperty.getName()));
        } else {
            defaultValueField.setValue(appSettingsTools.getDefaultPropertyValue(metaClass.getJavaClass(), metaProperty.getName()));
        }
        defaultValueField.setEnabled(false);
        formLayout.add(defaultValueField);
    }

    protected AbstractField createField(MetaProperty metaProperty, Range range, ComponentGenerationContext componentContext) {
        AbstractField field = (AbstractField) uiComponentsGenerator.generate(componentContext);

        if (EntityUtils.requireTextArea(metaProperty, this.container.getItem(), MAX_TEXT_FIELD_STRING_LENGTH)) {
            field = uiComponents.create(TypedTextField.class);
        }

        if (EntityUtils.isBoolean(metaProperty)) {
            field = createBooleanField();
        }

        if (EntityUtils.isSecret(metaProperty)) {
            field = createPasswordField();
        }

        if (range.isClass()) {
            field = createEntityPickerField();
        }

        ((SupportsValueSource) field).setValueSource(componentContext.getValueSource());
//        field.setWidth(FIELD_WIDTH);
        return field;
    }

    protected EntityPicker createEntityPickerField() {
        EntityPicker pickerField = uiComponents.create(EntityPicker.class);
        EntityLookupAction lookupAction = actions.create(EntityLookupAction.class);
        lookupAction.setOpenMode(OpenMode.DIALOG);
        pickerField.addAction(lookupAction);
        pickerField.addAction(actions.create(EntityClearAction.class));
        return pickerField;
    }

    protected AbstractField createBooleanField() {
        ComboBox field = uiComponents.create(ComboBox.class);
        field.setItems(List.of(Boolean.TRUE, Boolean.FALSE));
        field.setItemLabelGenerator((ItemLabelGenerator) item -> {
            if (item == Boolean.TRUE) {
                return messages.getMessage("trueString");
            }
            if (item == Boolean.FALSE) {
                return messages.getMessage("falseString");
            }
            return null;
        });
        field.setAllowCustomValue(false);
        return field;
    }

    protected AbstractField createPasswordField() {
        return uiComponents.create(PasswordField.class);
    }

    protected String getPropertyCaption(MetaClass metaClass, MetaProperty metaProperty) {
        String caption = messageTools.getPropertyCaption(metaClass, metaProperty.getName());
        if (caption.length() < MAX_CAPTION_LENGTH) {
            return caption;
        } else {
            return caption.substring(0, MAX_CAPTION_LENGTH);
        }
    }

}
