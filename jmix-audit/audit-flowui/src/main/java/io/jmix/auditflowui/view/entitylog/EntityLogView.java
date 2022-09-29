/*
 * Copyright 2020 Haulmont.
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

package io.jmix.auditflowui.view.entitylog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import io.jmix.audit.EntityLog;
import io.jmix.audit.entity.EntityLogAttr;
import io.jmix.audit.entity.EntityLogItem;
import io.jmix.audit.entity.LoggedAttribute;
import io.jmix.audit.entity.LoggedEntity;
import io.jmix.core.*;
import io.jmix.core.annotation.TenantId;
import io.jmix.core.metamodel.datatype.impl.EnumClass;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.metamodel.model.Range;
import io.jmix.core.security.AccessDeniedException;
import io.jmix.core.security.UserRepository;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.DefaultMainViewParent;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.DialogWindow;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import io.jmix.security.constraint.PolicyStore;
import io.jmix.security.constraint.SecureOperations;
import io.jmix.securityflowui.model.ResourcePolicyModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "entitylogview", layout = DefaultMainViewParent.class)
@ViewController("entityLog.browse")
@ViewDescriptor("entity-log-browser.xml")
@LookupComponent("entityLogTable")
@DialogMode(width = "50em", height = "37.5em")
    public class EntityLogView extends StandardListView<EntityLogItem> {

    protected static final String SELECT_ALL_CHECK_BOX = "selectAllCheckBox";

    @Autowired
    protected Messages messages;
    @Autowired
    protected Metadata metadata;
    @Autowired
    protected TimeSource timeSource;
    @Autowired
    protected ReferenceToEntitySupport referenceToEntitySupport;
    @Autowired
    protected ExtendedEntities extendedEntities;
    @Autowired
    protected EntityLog entityLog;
    @Autowired
    protected UiComponents uiComponents;
    @Autowired
    protected MetadataTools metadataTools;
    @Autowired
    protected Dialogs dialogs;
    @Autowired
    protected Notifications notifications;
//    @Autowired
//    protected ScreenBuilders screenBuilders;
    @Autowired
    protected PolicyStore policyStore;

    @ViewComponent
    protected CollectionContainer<LoggedEntity> loggedEntityDc;
    @ViewComponent
    protected CollectionLoader<LoggedEntity> loggedEntityDl;
    @ViewComponent
    protected CollectionLoader<EntityLogItem> entityLogDl;
    @ViewComponent
    protected CollectionContainer<LoggedAttribute> loggedAttrDc;
    @ViewComponent
    protected CollectionLoader<LoggedAttribute> loggedAttrDl;
    @ViewComponent
    protected ComboBox changeTypeField;
    @ViewComponent
    protected ComboBox<String> entityNameField;
    @ViewComponent
    protected ComboBox<String> userField;
    @ViewComponent
    protected ComboBox<String> filterEntityNameField;
//    @Autowired
//    protected EntityPicker<Object> instancePicker;
    @ViewComponent
    protected DataGrid<EntityLogItem> entityLogTable;
    @ViewComponent
    protected DataGrid<LoggedEntity> loggedEntityTable;
    @ViewComponent
    protected DataGrid<EntityLogAttr> entityLogAttrTable;
    @ViewComponent
    protected Checkbox manualCheckBox;
    @ViewComponent
    protected Checkbox autoCheckBox;
    @ViewComponent
    protected VerticalLayout actionsPaneLayout;
    @ViewComponent
    protected Scroller attributesBoxScroll;
    @ViewComponent
    protected DateTimePicker tillDateField;
    @ViewComponent
    protected DateTimePicker fromDateField;
    @ViewComponent
    protected Button cancelBtn;
    @ViewComponent
    protected Checkbox selectAllCheckBox;
    @ViewComponent
    protected UserRepository userRepository;
    @ViewComponent
    protected MessageTools messageTools;

    @ViewComponent
    private Tabs tabsheet;
    @ViewComponent
    private VerticalLayout viewWrapper;
    @ViewComponent
    private VerticalLayout setupWrapper;


    protected TreeMap<String, String> entityMetaClassesMap;


    // allow or not selectAllCheckBox to change values of other checkboxes
    protected boolean canSelectAllCheckboxGenerateEvents = true;
    @Autowired
    private SecureOperations secureOperations;

    private void onSelectedTabChange(Tabs.SelectedChangeEvent event) {
        String tabId = event.getSelectedTab().getId()
                .orElse("<no_id>");

        switch (tabId) {
            case "view":
                viewWrapper.setVisible(true);
                setupWrapper.setVisible(false);
                break;
            case "setup":
                viewWrapper.setVisible(false);
                setupWrapper.setVisible(true);
                break;
            default:
                viewWrapper.setVisible(false);
                setupWrapper.setVisible(false);
        }
    }

    @Subscribe
    protected void onInit(View.InitEvent event) {
        tabsheet.addSelectedChangeListener(this::onSelectedTabChange);
        viewWrapper.setVisible(true);
        setupWrapper.setVisible(false);

        loggedEntityDl.load();

        Map<String, Object> changeTypeMap = new LinkedHashMap<>();
        changeTypeMap.put(messages.getMessage(EntityLogView.class, "createField"), "C");
        changeTypeMap.put(messages.getMessage(EntityLogView.class, "modifyField"), "M");
        changeTypeMap.put(messages.getMessage(EntityLogView.class, "deleteField"), "D");
        changeTypeMap.put(messages.getMessage(EntityLogView.class, "restoreField"), "R");

        entityMetaClassesMap = getEntityMetaClasses();
        entityNameField.setItems(entityMetaClassesMap.keySet());
        changeTypeField.setItems(changeTypeMap.keySet());

//        userField.setItems((searchString, searchParams) -> {
//            List<? extends UserDetails> users = userRepository.getByUsernameLike(searchString);
//            return users.stream()
//                    .map(UserDetails::getUsername)
//                    .collect(Collectors.toList());
//        });
//        filterEntityNameField.setOptionsMap(entityMetaClassesMap);

        disableControls();
        setDateFieldTime();

//        instancePicker.setEnabled(false);

        entityNameField.addValueChangeListener(e -> {
            if (entityNameField.isEnabled())
                fillAttributes(e.getValue(), null, true);
        });

        loggedEntityDc.addItemChangeListener(e -> {
            if (e.getItem() != null) {
                loggedAttrDl.setParameter("entityId", e.getItem().getId());
                loggedAttrDl.load();
                fillAttributes(e.getItem().getName(), e.getItem(), false);
                checkAllCheckboxes();
            } else {
                setSelectAllCheckBox(false);
                clearAttributes();
            }
        });
//
//        filterEntityNameField.addValueChangeListener(e -> {
//            if (e.getValue() != null) {
//                instancePicker.setEnabled(true);
//                MetaClass metaClass = metadata.getSession().getClass(e.getValue());
//                instancePicker.setMetaClass(metaClass);
//            } else {
//                instancePicker.setEnabled(false);
//            }
//            instancePicker.setValue(null);
//        });
//        selectAllCheckBox.addValueChangeListener(e -> {
//            if (e.getValue() != null) {
//                enableAllCheckBoxes(e.getValue());
//            }
//        });
    }

//    @Install(to = "entityLogTable.entityId", subject = "columnGenerator")
//    protected Component entityLogTableEntityIdColumnGenerator(EntityLogItem entityLogItem) {
//        if (entityLogItem.getEntityRef().getObjectEntityId() != null) {
//            return new Table.PlainTextCell(entityLogItem.getEntityRef().getObjectEntityId().toString());
//        }
//        return null;
//    }

//    @Install(to = "entityLogTable.displayedEntityName", subject = "columnGenerator")
//    protected Component entityLogTableDisplayedEntityNameColumnGenerator(EntityLogItem entityLogItem) {
//        String entityName = evaluateEntityLogItemDisplayedEntityName(entityLogItem);
//        if (entityName != null) {
//            return new Table.PlainTextCell(entityName);
//        }
//        return null;
//    }

    protected String evaluateEntityLogItemDisplayedEntityName(EntityLogItem entityLogItem) {
        String entityName = entityLogItem.getEntity();
        MetaClass metaClass = getClassFromEntityName(entityName);
        if (metaClass != null) {
            return messageTools.getEntityCaption(metaClass);
        } else {
            return entityName;
        }
    }

//    @Install(to = "entityLogAttrTable.name", subject = "valueProvider")
//    protected String entityLogAttrTableDisplayNameValueProvider(EntityLogAttr entityLogAttr) {
//        String entityName = entityLogAttr.getLogItem().getEntity();
//        MetaClass metaClass = getClassFromEntityName(entityName);
//        if (metaClass != null) {
//            return messageTools.getPropertyCaption(metaClass, entityLogAttr.getName());
//        } else {
//            return entityLogAttr.getName();
//        }
//    }
//
//    @Install(to = "entityLogAttrTable.value", subject = "valueProvider")
//    protected String entityLogAttrTableDisplayValueValueProvider(EntityLogAttr entityLogAttr) {
//        return evaluateEntityLogItemAttrDisplayValue(entityLogAttr, entityLogAttr.getValue());
//    }
//
//    @Install(to = "entityLogAttrTable.oldValue", subject = "valueProvider")
//    protected String entityLogAttrTableDisplayOldValueValueProvider(EntityLogAttr entityLogAttr) {
//        return evaluateEntityLogItemAttrDisplayValue(entityLogAttr, entityLogAttr.getOldValue());
//    }

    protected String evaluateEntityLogItemAttrDisplayValue(EntityLogAttr entityLogAttr, String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        final String entityName = entityLogAttr.getLogItem().getEntity();
        MetaClass metaClass = getClassFromEntityName(entityName);
        if (metaClass != null) {
            MetaProperty property = metaClass.findProperty(entityLogAttr.getName());
            if (property != null) {
                if (property.getRange().isDatatype()) {
                    return value;
                } else if (property.getRange().isEnum() && EnumClass.class.isAssignableFrom(property.getJavaType())) {
                    Enum en = getEnumById(property.getRange().asEnumeration().getValues(), value);
                    if (en != null) {
                        return messages.getMessage(en);
                    } else {
                        String nameKey = property.getRange().asEnumeration().getJavaClass().getSimpleName() + "." + value;
                        String packageName = property.getRange().asEnumeration().getJavaClass().getPackage().getName();
                        return messages.getMessage(packageName, nameKey);
                    }
                } else {
                    return value;
                }
            } else {
                return value;
            }
        } else {
            return value;
        }
    }

    @Nullable
    protected MetaClass getClassFromEntityName(String entityName) {
        MetaClass metaClass = metadata.findClass(entityName);
        return metaClass == null ? null : extendedEntities.getEffectiveMetaClass(metaClass);
    }

    protected Enum getEnumById(List<Enum> enums, String id) {
        for (Enum e : enums) {
            if (e instanceof EnumClass) {
                Object enumId = ((EnumClass) e).getId();
                if (id.equals(String.valueOf(enumId))) {
                    return e;
                }
            }
        }
        return null;
    }

//    @Subscribe("instancePicker.lookup")
//    public void onInstancePickerLookup(Action.ActionPerformedEvent event) {
//        final MetaClass metaClass = instancePicker.getMetaClass();
//        if (instancePicker.isEditable()) {
//            if (metaClass == null) {
//                throw new IllegalStateException("Please specify metaclass or property for PickerField");
//            }
//            if (!secureOperations.isEntityReadPermitted(metaClass, policyStore)) {
//                notifications.create(Notifications.NotificationType.ERROR)
//                        .withCaption(messages.getMessage(EntityLogView.class, "entityAccessDeniedMessage"))
//                        .show();
//                return;
//            }
//            try {
//                Screen lookup = screenBuilders.lookup(instancePicker)
//                        .withSelectHandler(items -> {
//                            if (!items.isEmpty()) {
//                                Object item = items.iterator().next();
//                                instancePicker.setValue(item);
//                            }
//                        })
//                        .build();
//
//                lookup.addAfterCloseListener(afterCloseEvent -> instancePicker.focus());
//                lookup.show();
//            } catch (AccessDeniedException ex) {
//                notifications.create(Notifications.NotificationType.ERROR)
//                        .withCaption(messages.getMessage(EntityLogView.class, "entityScreenAccessDeniedMessage"))
//                        .show();
//                return;
//            }
//        }
//    }
//    @Subscribe("loggedEntityTable.create")
//    private void onLoggedEntityTableCreate(ActionPerformedEvent event) {
//
//    }
//
//    @Subscribe("loggedEntityTable.edit")
//    private void onLoggedEntityTableEdit(ActionPerformedEvent event) {
//
//    }
//
//    @Subscribe("loggedEntityTable.remove")
//    private void onLoggedEntityTableRemove(ActionPerformedEvent event) {
//
//    }
//
//    @Subscribe("loggedEntityTable.reload")
//    private void onLoggedEntityTableReload(ActionPerformedEvent event) {
//
//    }

    public TreeMap<String, String> getEntityMetaClasses() {
        TreeMap<String, String> options = new TreeMap<>();

        for (MetaClass metaClass : metadataTools.getAllJpaEntityMetaClasses()) {
            if (extendedEntities.getExtendedClass(metaClass) == null) {
                MetaClass originalMetaClass = extendedEntities.getOriginalOrThisMetaClass(metaClass);
                String originalName = originalMetaClass.getName();
                Class javaClass = originalMetaClass.getJavaClass();
                if (metadataTools.hasCompositePrimaryKey(metaClass) && !metadataTools.hasUuid(originalMetaClass)) {
                    continue;
                }
                String caption = messages.getMessage(javaClass, javaClass.getSimpleName()) + " (" + originalName + ")";
                options.put(caption, originalName);
            }
        }
        return options;
    }

//    protected void enableControls() {
//        loggedEntityTable.setEnabled(false);
//        entityNameField.setEditable(true);
//        autoCheckBox.setEditable(true);
//        manualCheckBox.setEditable(true);
//        for (Component c : attributesBoxScroll.getComponents())
//            ((CheckBox) c).setEditable(true);
//        actionsPaneLayout.setVisible(true);
//    }

    protected void disableControls() {
        entityNameField.setEnabled(false);
        loggedEntityTable.setEnabled(true);
        autoCheckBox.setEnabled(false);
        manualCheckBox.setEnabled(false);
        for (Component c : attributesBoxScroll.getContent().getChildren().collect(Collectors.toList()))
            ((Checkbox) c).setEnabled(false);
        actionsPaneLayout.setVisible(false);
    }

    protected void fillAttributes(String metaClassName, LoggedEntity item, boolean editable) {
        clearAttributes();
        setSelectAllCheckBox(false);

        if (metaClassName != null) {
            MetaClass metaClass = extendedEntities.getEffectiveMetaClass(
                    metadata.getClass(metaClassName));
            List<MetaProperty> metaProperties = new ArrayList<>(metaClass.getProperties());
            selectAllCheckBox.setEnabled(editable);
            Set<LoggedAttribute> enabledAttr = null;
            if (item != null)
                enabledAttr = item.getAttributes();
            for (MetaProperty property : metaProperties) {
                if (allowLogProperty(property)) {
                    if (metadataTools.isEmbedded(property)) {
                        MetaClass embeddedMetaClass = property.getRange().asClass();
                        for (MetaProperty embeddedProperty : embeddedMetaClass.getProperties()) {
                            if (allowLogProperty(embeddedProperty)) {
                                addAttribute(enabledAttr,
                                        String.format("%s.%s", property.getName(), embeddedProperty.getName()), metaClass, editable);
                            }
                        }
                    } else {
                        addAttribute(enabledAttr, property.getName(), metaClass, editable);
                    }
                }
            }

            Collection<MetaProperty> additionalProperties = metadataTools.getAdditionalProperties(metaClass);
            if (additionalProperties != null) {
                for (MetaProperty property : additionalProperties) {
                    if (allowLogProperty(property)) {
                        addAttribute(enabledAttr, property.getName(), metaClass, editable);
                    }
                }
            }
        }
    }

    protected void addAttribute(Set<LoggedAttribute> enabledAttributes, String name, MetaClass metaclass, boolean editable) {
        Checkbox checkBox = uiComponents.create(Checkbox.class);
        if (enabledAttributes != null && isEntityHaveAttribute(name, metaclass, enabledAttributes)) {
            checkBox.setValue(true);
        }
        checkBox.setId(name);
        checkBox.setLabel(name);
        checkBox.setEnabled(editable);
        checkBox.addValueChangeListener(e -> checkAllCheckboxes());

//        attributesBoxScroll.add(checkBox);
    }

//    protected void enableAllCheckBoxes(boolean b) {
//        if (canSelectAllCheckboxGenerateEvents) {
//            for (Component box : attributesBoxScroll.getComponents())
//                ((CheckBox) box).setValue(b);
//        }
//    }

    protected void checkAllCheckboxes() {
//        Checkbox selectAllCheckBox = (Checkbox) attributesBoxScroll.getOwnComponent(SELECT_ALL_CHECK_BOX);
        if (selectAllCheckBox != null) {
            for (Component c : attributesBoxScroll.getChildren().collect(Collectors.toList())) {
                if (!c.equals(selectAllCheckBox)) {
                    Checkbox checkBox = (Checkbox) c;
                    if (!checkBox.getValue()) {
                        setSelectAllCheckBox(false);
                        return;
                    }
                }
            }
            if (attributesBoxScroll.getChildren().toArray().length != 1)
                setSelectAllCheckBox(true);
        }
    }

    public void setSelectAllCheckBox(boolean value) {
        canSelectAllCheckboxGenerateEvents = false;
        boolean isEditable = selectAllCheckBox.isEnabled();
        try {
            selectAllCheckBox.setEnabled(true);
            selectAllCheckBox.setValue(value);
        } finally {
            canSelectAllCheckboxGenerateEvents = true;
            selectAllCheckBox.setEnabled(isEditable);
        }
    }

    public void setDateFieldTime() {
        Date date = timeSource.currentTimestamp();
        fromDateField.setValue(LocalDateTime.ofInstant(DateUtils.addDays(date, -1).toInstant(),
                ZoneId.systemDefault()));
        tillDateField.setValue(LocalDateTime.ofInstant(DateUtils.addDays(date, 1).toInstant(),
                ZoneId.systemDefault()));
    }
//
    public void clearAttributes() {
        for (Component c : attributesBoxScroll.getChildren().collect(Collectors.toList()))
            if (!SELECT_ALL_CHECK_BOX.equals(c.getId())) {}
//                attributesBoxScroll.remove(c);
    }
//
    public boolean isEntityHaveAttribute(String propertyName, MetaClass metaClass, Set<LoggedAttribute> enabledAttr) {
        if (enabledAttr != null && (metaClass.findProperty(propertyName) == null || !metadataTools.isSystem(metaClass.getProperty(propertyName)))) {
            for (LoggedAttribute logAttr : enabledAttr)
                if (logAttr.getName().equals(propertyName))
                    return true;
        }
        return false;
    }
//
//    public LoggedAttribute getLoggedAttribute(String name, Set<LoggedAttribute> enabledAttr) {
//        for (LoggedAttribute atr : enabledAttr)
//            if (atr.getName().equals(name))
//                return atr;
//        return null;
//    }
//
//    @Subscribe("loggedEntityTable.create")
//    public void onLoggedEntityTableCreate(Action.ActionPerformedEvent event) {
//        LoggedEntity entity = metadata.create(LoggedEntity.class);
//        entity.setAuto(false);
//        entity.setManual(false);
//        setSelectAllCheckBox(false);
//        loggedEntityDc.getMutableItems().add(entity);
//        loggedEntityTable.setEditable(true);
//        loggedEntityTable.setSelected(entity);
//
//        enableControls();
//
//        entityNameField.setEditable(true);
//        entityNameField.focus();
//    }
//
//    @Subscribe("loggedEntityTable.edit")
//    public void onLoggedEntityTableEdit(Action.ActionPerformedEvent event) {
//        enableControls();
//
//        loggedEntityTable.setEnabled(false);
//        cancelBtn.focus();
//    }
//
//    @Subscribe("searchBtn")
//    public void onSearchBtnClick(Button.ClickEvent event) {
//        Object entity = instancePicker.getValue();
//        if (entity != null) {
//            Object entityId = referenceToEntitySupport.getReferenceId(entity);
//            if (entityId instanceof UUID) {
//                entityLogDl.setParameter("entityId", entityId);
//            } else if (entityId instanceof String) {
//                entityLogDl.setParameter("stringEntityId", entityId);
//            } else if (entityId instanceof Integer) {
//                entityLogDl.setParameter("intEntityId", entityId);
//            } else if (entityId instanceof Long) {
//                entityLogDl.setParameter("longEntityId", entityId);
//            }
//        } else {
//            entityLogDl.removeParameter("entityId");
//            entityLogDl.removeParameter("stringEntityId");
//            entityLogDl.removeParameter("intEntityId");
//            entityLogDl.removeParameter("longEntityId");
//        }
//        if (userField.getValue() != null) {
//            entityLogDl.setParameter("user", userField.getValue());
//        } else {
//            entityLogDl.removeParameter("user");
//        }
//        if (changeTypeField.getValue() != null) {
//            entityLogDl.setParameter("changeType", changeTypeField.getValue());
//        } else {
//            entityLogDl.removeParameter("changeType");
//        }
//        if (filterEntityNameField.getValue() != null) {
//            entityLogDl.setParameter("entityName", filterEntityNameField.getValue());
//        } else {
//            entityLogDl.removeParameter("entityName");
//        }
//        if (fromDateField.getValue() != null) {
//            entityLogDl.setParameter("fromDate", fromDateField.getValue());
//        } else {
//            entityLogDl.removeParameter("fromDate");
//        }
//        if (tillDateField.getValue() != null) {
//            entityLogDl.setParameter("tillDate", tillDateField.getValue());
//        } else {
//            entityLogDl.removeParameter("tillDate");
//        }
//        entityLogDl.load();
//    }
//
//    @Subscribe("clearEntityLogTableBtn")
//    public void onClearEntityLogTableBtnClick(Button.ClickEvent event) {
//        userField.setValue(null);
//        filterEntityNameField.setValue(null);
//        changeTypeField.setValue(null);
//        instancePicker.setValue(null);
//        fromDateField.setValue(null);
//        tillDateField.setValue(null);
//    }
//
//    @Subscribe("reloadBtn")
//    public void onReloadBtnClick(Button.ClickEvent event) {
//        entityLog.invalidateCache();
//        notifications.create()
//                .withCaption(messages.getMessage(EntityLogView.class, "changesApplied"))
//                .withType(Notifications.NotificationType.HUMANIZED)
//                .show();
//    }
//
    protected boolean allowLogProperty(MetaProperty metaProperty /*, CategoryAttribute categoryAttribute*/) {
        if (metadataTools.isSystem(metaProperty)
                //log system property tenantId
                && !metadataTools.isAnnotationPresent(metaProperty.getDomain().getJavaClass(), metaProperty.getName(), TenantId.class)) {
            return false;
        }
        Range range = metaProperty.getRange();
        if (range.isClass() && metadataTools.hasCompositePrimaryKey(range.asClass()) &&
                !metadataTools.hasUuid(range.asClass())) {
            return false;
        }
        if (range.isClass() && range.getCardinality().isMany()) {
            return false;
        }
        //todo DynamicAttributes (until Haulmont/jmix-ui#272 & others will be finished and it can be tested)
//        if (categoryAttribute != null &&
//                BooleanUtils.isTrue(categoryAttribute.getIsCollection())) {
//            return false;
//        }
        return true;
    }
//
//    @Subscribe("saveBtn")
//    protected void onSaveBtnClick(Button.ClickEvent event) {
//        LoggedEntity selectedEntity = loggedEntityTable.getSelected().iterator().next();
//        selectedEntity = dataContext.merge(selectedEntity);
//        Set<LoggedAttribute> enabledAttributes = selectedEntity.getAttributes();
//        for (Component c : attributesBoxScroll.getComponents()) {
//            CheckBox currentCheckBox = (CheckBox) c;
//            if (SELECT_ALL_CHECK_BOX.equals(currentCheckBox.getId()))
//                continue;
//            Boolean currentCheckBoxValue = currentCheckBox.getValue();
//            MetaClass metaClass = metadata.getClass(selectedEntity.getName());
//            if (currentCheckBoxValue && !isEntityHaveAttribute(currentCheckBox.getId(), metaClass, enabledAttributes)) {
//                //add attribute if checked and not exist in table
//                LoggedAttribute newLoggedAttribute = dataContext.create(LoggedAttribute.class);
//                newLoggedAttribute.setName(currentCheckBox.getId());
//                newLoggedAttribute.setEntity(selectedEntity);
//            }
//            if (!currentCheckBoxValue && isEntityHaveAttribute(currentCheckBox.getId(), metaClass, enabledAttributes)) {
//                //remove attribute if unchecked and exist in table
//                LoggedAttribute removeAtr = getLoggedAttribute(currentCheckBox.getId(), enabledAttributes);
//                if (removeAtr != null)
//                    dataContext.remove(removeAtr);
//            }
//        }
//        dataContext.commit();
//
//        loggedEntityDl.load();
//        disableControls();
//        loggedEntityTable.setEnabled(true);
//        loggedEntityTable.focus();
//
//        entityLog.invalidateCache();
//    }
//
//    @Subscribe("removeBtn")
//    protected void onRemoveBtnClick(Button.ClickEvent event) {
//        Set<LoggedEntity> selectedItems = loggedEntityTable.getSelected();
//        if (!selectedItems.isEmpty()) {
//            dialogs.createOptionDialog()
//                    .withCaption(messages.getMessage("dialogs.Confirmation"))
//                    .withMessage(messages.getMessage("dialogs.Confirmation.Remove"))
//                    .withActions(
//                            new DialogAction(DialogAction.Type.YES).withHandler(e -> {
//                                for (LoggedEntity item : selectedItems) {
//                                    if (item.getAttributes() != null) {
//                                        Set<LoggedAttribute> attributes = new HashSet<>(item.getAttributes());
//                                        for (LoggedAttribute attribute : attributes) {
//                                            dataContext.remove(attribute);
//                                        }
//                                        dataContext.commit();
//                                    }
//                                    dataContext.remove(item);
//                                    dataContext.commit();
//                                }
//                                loggedEntityDc.getMutableItems().removeAll(selectedItems);
//                                entityLog.invalidateCache();
//                            }),
//                            new DialogAction(DialogAction.Type.NO)
//                    )
//                    .show();
//        }
//    }
//
//    @Subscribe("cancelBtn")
//    protected void onCancelBtnClick(Button.ClickEvent event) {
//        loggedEntityDl.load();
//        disableControls();
//        loggedEntityTable.setEnabled(true);
//        loggedEntityTable.focus();
//    }
}
