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

package io.jmix.quartzflowui.view.jobs;

import com.google.common.base.Strings;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxBase;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.action.list.EditAction;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import io.jmix.quartz.model.*;
import io.jmix.quartz.service.QuartzService;
import io.jmix.quartz.util.QuartzJobClassFinder;
import io.jmix.quartzflowui.view.trigger.TriggerModelDetailView;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Route(value = "quartz/jobmodels/:id", layout = DefaultMainViewParent.class)
@ViewController("quartz_JobModel.detail")
@ViewDescriptor("job-model-detail-view.xml")
@EditedEntityContainer("jobModelDc")
@DialogMode(width = "60em", height = "37.5em", resizable = true)
public class JobModelDetailView extends StandardDetailView<JobModel> {

    protected static final String VIEW_ACTION_ID = "view";

    @ViewComponent
    protected CollectionContainer<JobDataParameterModel> jobDataParamsDc;

    @ViewComponent
    protected CollectionContainer<TriggerModel> triggerModelDc;

    @ViewComponent
    protected TextField jobNameField;

    @ViewComponent
    protected ComboBox<String> jobGroupField;

    @ViewComponent
    protected ComboBox<String> jobClassField;

    @ViewComponent
    protected DataGrid<TriggerModel> triggerModelTable;

    @ViewComponent
    protected Tabs jobDetailsTabs;

    @ViewComponent
    protected VerticalLayout triggersTab;

    @ViewComponent
    protected VerticalLayout jobDataParamsTab;

    @ViewComponent
    protected DataGrid<JobDataParameterModel> jobDataParamsTable;

    @ViewComponent
    protected Button addDataParamButton;

    @Autowired
    protected QuartzService quartzService;

    @Autowired
    protected QuartzJobClassFinder quartzJobClassFinder;

    @Autowired
    protected DialogWindows dialogWindows;

    @Autowired
    protected MessageBundle messageBundle;

    @Autowired
    protected UnconstrainedDataManager dataManager;

    protected boolean replaceJobIfExists = true;
    protected boolean deleteObsoleteJob = false;
    protected String obsoleteJobName = null;
    protected String obsoleteJobGroup = null;
    protected List<String> jobGroupNames;

    @Subscribe
    protected void onInit(View.InitEvent event) {
        jobDetailsTabs.addSelectedChangeListener(this::onSelectedTabChange);
        triggerModelTable.addColumn(entity -> entity.getLastFireDate() != null ?
                        new SimpleDateFormat(messageBundle.getMessage("dateTimeWithSeconds"))
                                .format(entity.getLastFireDate()) : "").setResizable(true)
                .setHeader(messageBundle.getMessage("column.lastFireDate.header"));
        triggerModelTable.addColumn(entity -> entity.getStartDate() != null ?
                        new SimpleDateFormat(messageBundle.getMessage("dateTimeWithSeconds"))
                                .format(entity.getStartDate()) : "").setResizable(true)
                .setHeader(messageBundle.getMessage("column.startDate.header"));
        triggerModelTable.addColumn(entity -> entity.getNextFireDate() != null ?
                        new SimpleDateFormat(messageBundle.getMessage("dateTimeWithSeconds"))
                                .format(entity.getNextFireDate()) : "").setResizable(true)
                .setHeader(messageBundle.getMessage("column.nextFireDate.header"));

        jobGroupNames = quartzService.getJobGroupNames();
        jobGroupField.setItems(jobGroupNames);
        List<String> existedJobsClassNames = quartzJobClassFinder.getQuartzJobClassNames();
        jobClassField.setItems(existedJobsClassNames);

    }

    @Subscribe("jobGroupField")
    protected void onJobGroupFieldValueSet(ComboBoxBase.CustomValueSetEvent<ComboBox<String>> event) {
        String newJobGroupName = event.getDetail();
        if (!Strings.isNullOrEmpty(newJobGroupName)
                && !jobGroupNames.contains(newJobGroupName)) {
            jobGroupNames.add(newJobGroupName);
            jobGroupField.setItems(jobGroupNames);
            jobGroupField.setValue(newJobGroupName);
        }
        if (!Strings.isNullOrEmpty(obsoleteJobGroup)
                && !Strings.isNullOrEmpty(newJobGroupName)
                && !obsoleteJobGroup.equals(newJobGroupName)) {
            deleteObsoleteJob = true;
        }
    }

    @Subscribe("jobGroupField")
    protected void onJobGroupFieldChange(AbstractField.ComponentValueChangeEvent<ComboBox<String>, String> event) {
        String currentValue = event.getValue();
        if (!Strings.isNullOrEmpty(obsoleteJobGroup)
                && !Strings.isNullOrEmpty(currentValue)
                && !obsoleteJobGroup.equals(currentValue)) {
            deleteObsoleteJob = true;
        }
    }

    @Subscribe("jobNameField")
    protected void onjobNameFieldChange(AbstractField.ComponentValueChangeEvent<TextField, String> event) {
        String currentValue = event.getValue();
        if (!Strings.isNullOrEmpty(obsoleteJobName)
                && !Strings.isNullOrEmpty(currentValue)
                && !obsoleteJobName.equals(currentValue)) {
            deleteObsoleteJob = true;
        }
    }


    protected void onSelectedTabChange(Tabs.SelectedChangeEvent event) {
        String tabId = event.getSelectedTab().getId()
                .orElse("<no_id>");

        switch (tabId) {
            case "triggers":
                triggersTab.setVisible(true);
                jobDataParamsTab.setVisible(false);
                break;
            case "jobs":
                triggersTab.setVisible(false);
                jobDataParamsTab.setVisible(true);
                break;
            default:
                triggersTab.setVisible(false);
                jobDataParamsTab.setVisible(false);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Subscribe
    protected void onInitEntity(StandardDetailView.InitEntityEvent<JobModel> event) {
        JobModel entity = event.getEntity();
        if (entity.getJobSource() == null) {
            entity.setJobSource(JobSource.USER_DEFINED);
            replaceJobIfExists = false;
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        jobNameField.setReadOnly(readOnly);
        jobGroupField.setReadOnly(readOnly);
        jobClassField.setReadOnly(readOnly);
        triggerModelTable.getAction(EditAction.ID).setVisible(!readOnly);
        triggerModelTable.getAction(VIEW_ACTION_ID).setVisible(readOnly);
        addDataParamButton.setEnabled(!readOnly);
        jobDataParamsTable.setEnabled(!readOnly);
    }

    @Subscribe
    protected void onBeforeShow(BeforeShowEvent event) {
        //allow editing only not active and user-defined jobs
        boolean readOnly = JobState.NORMAL.equals(getEditedEntity().getJobState())
                || JobSource.PREDEFINED.equals(getEditedEntity().getJobSource());
        setReadOnly(readOnly);

        obsoleteJobName = getEditedEntity().getJobName();
        obsoleteJobGroup = getEditedEntity().getJobGroup();

    }

    @Subscribe("triggerModelTable.view")
    protected void onTriggerModelGroupTableView(ActionPerformedEvent event) {
        TriggerModel triggerModel = triggerModelTable.getSingleSelectedItem();
        if (triggerModel == null) {
            return;
        }

        DialogWindow<TriggerModelDetailView> detailView = dialogWindows.detail(triggerModelTable)
                .withViewClass(TriggerModelDetailView.class)
                .withParentDataContext(getViewData().getDataContext())
                .build();

        detailView.getView().setReadOnly(true);
        detailView.open();
    }

    @Subscribe
    @SuppressWarnings("ConstantConditions")
    protected void onValidation(ValidationEvent event) {
        ValidationErrors errors = event.getErrors();

        JobModel jobModel = getEditedEntity();
        String currentJobName = jobModel.getJobName();
        String currentJobGroup = jobModel.getJobGroup();

        //if jobKey is changed it is necessary to delete job by it old jobKey and create new one
        //job should be deleted only if it is possible to create new one
        if (deleteObsoleteJob && quartzService.checkJobExists(currentJobName, currentJobGroup)) {
            errors.add(messageBundle.formatMessage("jobAlreadyExistsValidationMessage", currentJobName,
                    Strings.isNullOrEmpty(currentJobGroup) ? "DEFAULT" : currentJobGroup));
        }

        if (!replaceJobIfExists) {
            if (quartzService.checkJobExists(currentJobName, currentJobGroup)) {
                errors.add(messageBundle.formatMessage("jobAlreadyExistsValidationMessage", currentJobName,
                        Strings.isNullOrEmpty(currentJobGroup) ? "DEFAULT" : currentJobGroup));
            }

            getEditedEntity().getTriggers().stream()
                    .filter(triggerModel -> !Strings.isNullOrEmpty(triggerModel.getTriggerName()))
                    .filter(triggerModel -> quartzService.checkTriggerExists(triggerModel.getTriggerName(),
                            triggerModel.getTriggerGroup()))
                    .forEach(triggerModel -> errors.add(
                            messageBundle.formatMessage(
                                    "triggerAlreadyExistsValidationMessage",
                                    triggerModel.getTriggerName(),
                                    Strings.isNullOrEmpty(triggerModel.getTriggerGroup()) ? "DEFAULT" :
                                            triggerModel.getTriggerGroup())
                    ));
        }

        if (jobDataParamsDc.getItems().stream()
                .map(JobDataParameterModel::getKey)
                .anyMatch(Objects::isNull)) {
            errors.add(messageBundle.getMessage("jobDataParamKeyIsRequired"));
        }

        boolean jobDataMapOverlapped = jobDataParamsDc.getItems().stream()
                .map(JobDataParameterModel::getKey)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream().anyMatch(entry -> entry.getValue() > 1);
        if (jobDataMapOverlapped) {
            errors.add(messageBundle.getMessage("jobDataParamKeyAlreadyExistsValidationMessage"));
        }
    }

    @Subscribe
    protected void onBeforeCommitChanges(BeforeSaveEvent event) {
        if (deleteObsoleteJob) {
            quartzService.deleteJob(obsoleteJobName, obsoleteJobGroup);
        }

        quartzService.updateQuartzJob(getEditedEntity(), jobDataParamsDc.getItems(), triggerModelDc.getItems(), replaceJobIfExists);
    }

    @Subscribe("jobDataParamsTable.addNewDataParam")
    protected void onJobDataParamsTableCreate(ActionPerformedEvent event) {
        JobDataParameterModel itemToAdd = dataManager.create(JobDataParameterModel.class);
        jobDataParamsDc.getMutableItems().add(itemToAdd);
        jobDataParamsTable.select(itemToAdd);
    }

}
