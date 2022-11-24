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

package io.jmix.appsettingsflowui.view.appsettings;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.router.Route;
import io.jmix.appsettings.AppSettings;
import io.jmix.appsettings.entity.AppSettingsEntity;
import io.jmix.appsettingsflowui.view.appsettings.util.AppSettingsGridLayoutBuilder;
import io.jmix.core.*;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.accesscontext.FlowuiEntityContext;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.model.DataComponents;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.util.OperationResult;
import io.jmix.flowui.util.UnknownOperationResult;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.jmix.flowui.view.StandardOutcome.*;

@Route(value = "app-settings", layout = DefaultMainViewParent.class)
@ViewController("appSettings.view")
@ViewDescriptor("app-settings-view.xml")
@LookupComponent("sessionsTable")
@DialogMode(width = "50em", height = "37.5em")
public class AppSettingsEntityView extends StandardView {

    private static final String SELECT_APP_SETTINGS_ENTITY_QUERY = "select e from %s e where e.id = 1";

    @Autowired
    protected AppSettings appSettings;

    @Autowired
    protected EntityStates entityStates;

    @Autowired
    protected UnconstrainedDataManager dataManager;

    @Autowired
    protected MetadataTools metadataTools;

    @Autowired
    protected AccessManager accessManager;

    @Autowired
    protected DataComponents dataComponents;

    @Autowired
    protected ViewValidation viewValidation;

    @Autowired
    protected Messages messages;

    @Autowired
    protected MessageTools messageTools;

    @Autowired
    protected Notifications notifications;

    @Autowired
    protected FetchPlans fetchPlans;

    @Autowired
    protected ComboBox<MetaClass> entitiesLookup;

    @Autowired
    protected HorizontalLayout entityGroupBoxId;
    @Autowired
    protected Scroller fieldsScrollBox;
    @Autowired
    protected HorizontalLayout actionsBox;

    private DataContext dataContext;
    private MetaClass currentMetaClass;
    private MetaClass prevMetaClass;
    private boolean isNewEntityModified = false;
    private boolean isEntityChangePrevented = false;
    private Object entityToEdit;

    @Subscribe
    public void onInit(InitEvent event) {

        entitiesLookup
                .setItemLabelGenerator((ItemLabelGenerator<MetaClass>) item -> messageTools.getEntityCaption(item) + " (" + item.getName() + ")");
        entitiesLookup.setItems(getEntitiesLookupFieldOptions());

//        entitiesLookup.setOptionsMap(getEntitiesLookupFieldOptions());
        entitiesLookup.addValueChangeListener(e -> {

            entityGroupBoxId.setVisible(e.getValue() != null);

            if (isEntityChangePrevented) {
                isEntityChangePrevented = false;
                return;
            }

            prevMetaClass = e.getOldValue();
            currentMetaClass = e.getValue();

            if (dataContext != null && hasUnsavedChanges()) {
                handleEntityLookupChangeWithUnsavedChanges();
                return;
            }

            initEntityPropertiesGridLayout();
        });
    }

    protected void initEntityPropertiesGridLayout() {
        dataContext = dataComponents.createDataContext();
        getViewData().setDataContext(dataContext);
//        getScreenData().setDataContext(dataContext);
        showEntityPropertiesGridLayout();
        dataContext.addChangeListener(changeEvent -> {
            if (entityStates.isNew(changeEvent.getEntity())) {
                this.isNewEntityModified = true;
            }
        });
    }

    protected List<MetaClass> getEntitiesLookupFieldOptions() {
        return metadataTools.getAllJpaEntityMetaClasses().stream()
                .filter(this::isApplicationSettingsEntity)
                .filter(metaClass -> !metadataTools.isSystemLevel(metaClass))
                .filter(this::readPermitted)
                .collect(Collectors.toList());
    }

    protected boolean readPermitted(MetaClass metaClass) {
        FlowuiEntityContext entityContext = new FlowuiEntityContext(metaClass);
        accessManager.applyRegisteredConstraints(entityContext);
        return entityContext.isViewPermitted();
    }

    @SuppressWarnings("rawtypes")
    protected void showEntityPropertiesGridLayout() {
        fieldsScrollBox.setContent(null);
        if (currentMetaClass != null) {
            InstanceContainer container = initInstanceContainerWithDbEntity();
            FormLayout gridLayout = AppSettingsGridLayoutBuilder.of(getApplicationContext(), container)
                    .withOwnerComponent(fieldsScrollBox)
                    .build();
            fieldsScrollBox.setContent(gridLayout);

            actionsBox.setVisible(true);
        }
    }

    @SuppressWarnings("rawtypes")
    protected InstanceContainer initInstanceContainerWithDbEntity() {
        InstanceContainer container = dataComponents.createInstanceContainer(currentMetaClass.getJavaClass());
        entityToEdit = dataManager.load(currentMetaClass.getJavaClass())
                .query(String.format(SELECT_APP_SETTINGS_ENTITY_QUERY, currentMetaClass.getName()))
                .fetchPlan(fetchPlans.builder(currentMetaClass.getJavaClass()).addFetchPlan(FetchPlan.LOCAL).build())
                .hint(PersistenceHints.SOFT_DELETION, false)
                .optional()
                .orElse(null);

        if (entityToEdit == null) {
            entityToEdit = dataContext.create(currentMetaClass.getJavaClass());
        } else {
            entityToEdit = dataContext.merge(entityToEdit);
        }

        container.setItem(entityToEdit);
        return container;
    }

    protected boolean isApplicationSettingsEntity(MetaClass metaClass) {
        return AppSettingsEntity.class.isAssignableFrom(metaClass.getJavaClass());
    }

    @Subscribe("saveButtonId")
    public void onSaveButtonClick(ClickEvent<Button> event) {
        commitChanges();
    }

    @Subscribe("closeButtonId")
    public void onCloseButtonClick(ClickEvent<Button> event) {
        if (dataContext != null && hasUnsavedChanges()) {
            handleCloseBtnClickWithUnsavedChanges();
        } else {
            close(CLOSE);
        }
    }

    protected boolean hasUnsavedChanges() {
        for (Object modified : dataContext.getModified()) {
            if (!entityStates.isNew(modified)) {
                return true;
            }
        }
        //check whether "new" entity is modified in DataContext
        return isNewEntityModified;
    }

    protected void handleCloseBtnClickWithUnsavedChanges() {
        UnknownOperationResult result = new UnknownOperationResult();
        viewValidation.showSaveConfirmationDialog(this)
                .onSave(() -> result.resume(commitChanges().compose(() -> close(SAVE))))
                .onDiscard(() -> result.resume(close(DISCARD)))
                .onCancel(result::fail);
    }

    protected void handleEntityLookupChangeWithUnsavedChanges() {
        UnknownOperationResult result = new UnknownOperationResult();
        viewValidation.showUnsavedChangesDialog(this)
                .onDiscard(() -> result.resume(updateEntityLookupValue(false)))
                .onCancel(() -> result.resume(updateEntityLookupValue(true)));
    }

    protected OperationResult commitChanges() {
        ValidationErrors validationErrors = viewValidation.validateUiComponents(this.getContent());
        if (!validationErrors.isEmpty()) {
            viewValidation.showValidationErrors(validationErrors);
            return OperationResult.fail();
        }

        appSettings.save(((AppSettingsEntity) entityToEdit));
        dataContext.clear();
        isNewEntityModified = false;
        showSaveNotification();

        return OperationResult.success();
    }

    protected OperationResult updateEntityLookupValue(boolean preventEntityLookupChange) {
        isEntityChangePrevented = preventEntityLookupChange;
        if (preventEntityLookupChange) {
            entitiesLookup.setValue(prevMetaClass);
            return OperationResult.fail();
        } else {
            isNewEntityModified = false;
            initEntityPropertiesGridLayout();
            return OperationResult.success();
        }
    }

    protected void showSaveNotification() {
        String caption = messages.formatMessage(this.getClass(), "entitySaved", messageTools.getEntityCaption(currentMetaClass));
        notifications.create(caption)
                .withType(Notifications.Type.DEFAULT)
                .show();
    }

}
