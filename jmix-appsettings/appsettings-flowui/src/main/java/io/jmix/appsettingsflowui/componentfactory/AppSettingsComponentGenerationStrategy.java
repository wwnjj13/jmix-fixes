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

package io.jmix.appsettingsflowui.componentfactory;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.PasswordField;
import io.jmix.appsettingsflowui.componentfactory.EntityUtils;
import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.datatype.DatatypeRegistry;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.flowui.Actions;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.entitypicker.EntityClearAction;
import io.jmix.flowui.action.entitypicker.EntityLookupAction;
import io.jmix.flowui.component.ComponentGenerationContext;
import io.jmix.flowui.component.ComponentGenerationStrategy;
import io.jmix.flowui.component.UiComponentsGenerator;
import io.jmix.flowui.component.factory.AbstractComponentGenerationStrategy;
import io.jmix.flowui.component.factory.EntityFieldCreationSupport;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.data.SupportsValueSource;
import io.jmix.flowui.view.OpenMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import javax.annotation.Nullable;
import java.util.List;

@org.springframework.stereotype.Component("appsettings_AppSettingsComponentGenerationStrategy")
public class AppSettingsComponentGenerationStrategy
        extends AbstractComponentGenerationStrategy implements Ordered {

    private static final int MAX_TEXT_FIELD_STRING_LENGTH = 255;

    private static final Integer MAX_CAPTION_LENGTH = 50;


    public AppSettingsComponentGenerationStrategy(UiComponents uiComponents,
                                                      Metadata metadata,
                                                      MetadataTools metadataTools,
                                                      Actions actions,
                                                      DatatypeRegistry datatypeRegistry,
                                                      Messages messages,
                                                      EntityFieldCreationSupport entityFieldCreationSupport) {
        super(uiComponents, metadata, metadataTools, actions, datatypeRegistry, messages, entityFieldCreationSupport);
    }

    @Nullable
    @Override
    public Component createComponent(ComponentGenerationContext context) {
//        String property = context.getProperty();
//        MetaClass orderMetaClass = metadata.getClass(Order.class);
//
//        if (orderMetaClass.equals(context.getMetaClass())
//                && "date".equals(property)
//                && context.getClass() != null
//                && Form.class.isAssignableFrom(context.getClass())) {
//            DatePicker<Date> datePicker = uiComponents.create(DatePicker.TYPE_DATE);
//
//            ValueSource valueSource = context.getValueSource();
//            if (valueSource != null) {
//                datePicker.setValueSource(valueSource);
//            }
//
//            return datePicker;
//        }
//
//        return null;
        context.get
        AbstractField field = (AbstractField) uiComponents.generate(componentContext);

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
        String caption = messages.getMessage(metaClass.getJavaClass(), metaProperty.getName());
        if (caption.length() < MAX_CAPTION_LENGTH) {
            return caption;
        } else {
            return caption.substring(0, MAX_CAPTION_LENGTH);
        }
    }

    @Override
    public int getOrder() {
        return 50;
    }


}
