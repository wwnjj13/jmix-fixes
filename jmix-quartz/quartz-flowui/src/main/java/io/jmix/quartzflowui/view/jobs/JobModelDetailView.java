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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
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
@DialogMode(width = "50em", height = "37.5em")
public class JobModelDetailView extends StandardDetailView<JobModel> {

    private static final String VIEW_ACTION_ID = "view";

    @ViewComponent
    private CollectionContainer<JobDataParameterModel> jobDataParamsDc;

    @ViewComponent
    private CollectionContainer<TriggerModel> triggerModelDc;

    @ViewComponent
    private TextField jobNameField;

    @ViewComponent
    private ComboBox<String> jobGroupField;

    @ViewComponent
    private ComboBox<String> jobClassField;

    @ViewComponent
    private DataGrid<TriggerModel> triggerModelTable;

    @ViewComponent
    private Tabs jobDetailsTabs;

    @ViewComponent
    private VerticalLayout triggersTab;

    @ViewComponent
    private VerticalLayout jobDataParamsTab;

    @ViewComponent
    private DataGrid<JobDataParameterModel> jobDataParamsTable;

    @ViewComponent
    private Button addDataParamButton;

    @Autowired
    private QuartzService quartzService;

    @Autowired
    private QuartzJobClassFinder quartzJobClassFinder;

    @Autowired
    private DialogWindows dialogWindows;

    @Autowired
    private MessageBundle messageBundle;

    @Autowired
    private UnconstrainedDataManager dataManager;

    private boolean replaceJobIfExists = true;
    private boolean deleteObsoleteJob = false;
    private String obsoleteJobName = null;
    private String obsoleteJobGroup = null;

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

        List<String> jobGroupNames = quartzService.getJobGroupNames();
        jobGroupField.setItems(jobGroupNames);
        List<String> existedJobsClassNames = quartzJobClassFinder.getQuartzJobClassNames();
        jobClassField.setItems(existedJobsClassNames);

        jobGroupField.addCustomValueSetListener(item -> {
            String newJobGroupName = item.getDetail();
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
        });

        jobGroupField.addValueChangeListener(e -> {
            String currentValue = e.getValue();
            if (!Strings.isNullOrEmpty(obsoleteJobGroup)
                    && !Strings.isNullOrEmpty(currentValue)
                    && !obsoleteJobGroup.equals(currentValue)) {
                deleteObsoleteJob = true;
            }
        });

        jobNameField.addValueChangeListener(e -> {
            String currentValue = e.getValue();
            if (!Strings.isNullOrEmpty(obsoleteJobName)
                    && !Strings.isNullOrEmpty(currentValue)
                    && !obsoleteJobName.equals(currentValue)) {
                deleteObsoleteJob = true;
            }
        });

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
    public void onInitEntity(StandardDetailView.InitEntityEvent<JobModel> event) {
        JobModel entity = event.getEntity();
        if (entity.getJobSource() == null) {
            entity.setJobSource(JobSource.USER_DEFINED);
            replaceJobIfExists = false;
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        //allow editing only not active and user-defined jobs
        boolean readOnly = JobState.NORMAL.equals(getEditedEntity().getJobState())
                || JobSource.PREDEFINED.equals(getEditedEntity().getJobSource());
        setReadOnly(readOnly);
        jobNameField.setEnabled(!readOnly);
        jobGroupField.setEnabled(!readOnly);
        jobClassField.setEnabled(!readOnly);
        triggerModelTable.getAction(EditAction.ID).setVisible(!readOnly);
        triggerModelTable.getAction(VIEW_ACTION_ID).setVisible(readOnly);
        addDataParamButton.setEnabled(!readOnly);
        jobDataParamsTable.setEnabled(!readOnly);

        obsoleteJobName = getEditedEntity().getJobName();
        obsoleteJobGroup = getEditedEntity().getJobGroup();

    }

    @Subscribe("triggerModelTable.view")
    public void onTriggerModelGroupTableView(ActionPerformedEvent event) {
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
    public void onValidation(ValidationEvent event) {
        ValidationErrors errors = event.getErrors();

        JobModel jobModel = getEditedEntity();
        String currentJobName = jobModel.getJobName();
        String currentJobGroup = jobModel.getJobGroup();

        //if jobKey is changed it is necessary to delete job by it old jobKey and create new one
        //job should be deleted only if it is possible to create new one
        if (deleteObsoleteJob && quartzService.checkJobExists(currentJobName, currentJobGroup)) {
            errors.add(messageBundle.formatMessage("jobAlreadyExistsValidationMessage", currentJobName, Strings.isNullOrEmpty(currentJobGroup) ? "DEFAULT" : currentJobGroup));
        }

        if (!replaceJobIfExists) {
            if (quartzService.checkJobExists(currentJobName, currentJobGroup)) {
                errors.add(messageBundle.formatMessage("jobAlreadyExistsValidationMessage", currentJobName, Strings.isNullOrEmpty(currentJobGroup) ? "DEFAULT" : currentJobGroup));
            }

            getEditedEntity().getTriggers().stream()
                    .filter(triggerModel -> !Strings.isNullOrEmpty(triggerModel.getTriggerName()))
                    .filter(triggerModel -> quartzService.checkTriggerExists(triggerModel.getTriggerName(), triggerModel.getTriggerGroup()))
                    .forEach(triggerModel -> errors.add(
                            messageBundle.formatMessage(
                                    "triggerAlreadyExistsValidationMessage",
                                    triggerModel.getTriggerName(),
                                    Strings.isNullOrEmpty(triggerModel.getTriggerGroup()) ? "DEFAULT" : triggerModel.getTriggerGroup())
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
    public void onBeforeCommitChanges(BeforeSaveEvent event) {
        if (deleteObsoleteJob) {
            quartzService.deleteJob(obsoleteJobName, obsoleteJobGroup);
        }

        quartzService.updateQuartzJob(getEditedEntity(), jobDataParamsDc.getItems(), triggerModelDc.getItems(), replaceJobIfExists);
    }

    @Subscribe("jobDataParamsTable.addNewDataParam")
    public void onJobDataParamsTableCreate(ActionPerformedEvent event) {
        List<JobDataParameterModel> currentItems = new ArrayList<>(jobDataParamsDc.getItems());

        JobDataParameterModel itemToAdd = dataManager.create(JobDataParameterModel.class);
        currentItems.add(itemToAdd);
        jobDataParamsDc.setItems(currentItems);
        jobDataParamsTable.select(itemToAdd);
    }

}
