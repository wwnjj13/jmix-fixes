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

package io.jmix.quartzflowui.view.trigger;

import com.google.common.base.Strings;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxBase;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.view.*;
import io.jmix.quartz.model.ScheduleType;
import io.jmix.quartz.model.TriggerModel;
import io.jmix.quartz.service.QuartzService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@ViewController("quartz_TriggerModel.detail")
@ViewDescriptor("trigger-model-detail-view.xml")
@EditedEntityContainer("triggerModelDc")
@DialogMode(width = "50em")
public class TriggerModelDetailView extends StandardDetailView<TriggerModel> {

    @ViewComponent
    private ComboBox<String> triggerGroupField;

    @ViewComponent
    private HorizontalLayout cronExpressionBox;

    @ViewComponent
    private TypedTextField<Integer> repeatCountField;

    @ViewComponent
    private TypedTextField<Long> repeatIntervalField;

    @ViewComponent
    private Select<ScheduleType> scheduleTypeField;

    @Autowired
    private QuartzService quartzService;

    @Autowired
    private Notifications notifications;

    @Autowired
    private MessageBundle messageBundle;

    private List<String> triggerGroupNames;

    @SuppressWarnings("ConstantConditions")
    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        initTriggerGroupNames();
        initFieldVisibility();
        if (getEditedEntity().getScheduleType() == null) {
            scheduleTypeField.setValue(ScheduleType.CRON_EXPRESSION);
        }
    }

    @Subscribe("scheduleTypeField")
    protected void onScheduleTypeFieldChange(
            AbstractField.ComponentValueChangeEvent<Select<ScheduleType>, ScheduleType> event) {
        initFieldVisibility();
    }

    @Subscribe("helperBtn")
    protected void onHelperButtonClick(ClickEvent<Button> event) {
        notifications.create(new Html(messageBundle.getMessage("cronExpressionHelpText")))
                .withDuration(0)
                .show();
    }

    private void initTriggerGroupNames() {
        triggerGroupNames = quartzService.getTriggerGroupNames();
        triggerGroupField.setItems(triggerGroupNames);
    }

    @Subscribe("triggerGroupField")
    protected void onJobGroupFieldValueSet(ComboBoxBase.CustomValueSetEvent<ComboBox<String>> event) {
        String newTriggerGroupName = event.getDetail();
        if (!Strings.isNullOrEmpty(newTriggerGroupName) && !triggerGroupNames.contains(newTriggerGroupName)) {
            triggerGroupNames.add(newTriggerGroupName);
            triggerGroupField.setItems(triggerGroupNames);
            triggerGroupField.setValue(newTriggerGroupName);
        }
    }

    private void initFieldVisibility() {
        boolean isSimpleTrigger = getEditedEntity().getScheduleType() == ScheduleType.SIMPLE;
        cronExpressionBox.setVisible(!isSimpleTrigger);
        repeatCountField.setVisible(isSimpleTrigger);
        repeatIntervalField.setVisible(isSimpleTrigger);
    }

}
