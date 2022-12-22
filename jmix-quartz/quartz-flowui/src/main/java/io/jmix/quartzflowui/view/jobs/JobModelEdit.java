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
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.quartz.model.*;
import io.jmix.quartzflowui.view.trigger.TriggerModelEdit;
import io.jmix.quartz.service.QuartzService;
import io.jmix.quartz.util.QuartzJobClassFinder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@UiController("quartz_JobModel.edit")
@UiDescriptor("job-model-edit.xml")
@EditedEntityContainer("jobModelDc")
public class JobModelEdit extends StandardEditor<JobModel> {

    @Autowired
    private QuartzService quartzService;

    @Autowired
    private QuartzJobClassFinder quartzJobClassFinder;

    @Autowired
    private ScreenBuilders screenBuilders;

    @Autowired
    private MessageBundle messageBundle;

    @Autowired
    private UnconstrainedDataManager dataManager;

    @Autowired
    private CollectionContainer<JobDataParameterModel> jobDataParamsDc;

    @Autowired
    private CollectionContainer<TriggerModel> triggerModelDc;

    @Autowired
    private TextField<String> jobNameField;

    @Autowired
    private ComboBox<String> jobGroupField;

    @Autowired
    private ComboBox<String> jobClassField;

    @Autowired
    private Table<TriggerModel> triggerModelTable;

    @Named("triggerModelTable.edit")
    private EditAction<TriggerModel> triggerModelTableEdit;

    @Named("triggerModelTable.view")
    private ViewAction<TriggerModel> triggerModelTableView;

    @Autowired
    private Table<JobDataParameterModel> jobDataParamsTable;

    @Autowired
    private Button addDataParamButton;

    private boolean replaceJobIfExists = true;
    private boolean deleteObsoleteJob = false;
    private String obsoleteJobName = null;
    private String obsoleteJobGroup = null;

    @SuppressWarnings("ConstantConditions")
    @Subscribe
    public void onInitEntity(InitEntityEvent<JobModel> event) {
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
        jobNameField.setEditable(!readOnly);
        jobGroupField.setEditable(!readOnly);
        jobClassField.setEditable(!readOnly);
        triggerModelTableEdit.setVisible(!readOnly);
        triggerModelTableView.setVisible(readOnly);
        addDataParamButton.setEnabled(!readOnly);
        jobDataParamsTable.setEditable(!readOnly);

        obsoleteJobName = getEditedEntity().getJobName();
        obsoleteJobGroup = getEditedEntity().getJobGroup();

        jobNameField.addValueChangeListener(e -> {
            String currentValue = e.getValue();
            if (!Strings.isNullOrEmpty(obsoleteJobName)
                    && !Strings.isNullOrEmpty(currentValue)
                    && !obsoleteJobName.equals(currentValue)) {
                deleteObsoleteJob = true;
            }
        });

        List<String> jobGroupNames = quartzService.getJobGroupNames();
        jobGroupField.setOptionsList(jobGroupNames);
        jobGroupField.setEnterPressHandler(enterPressEvent -> {
            String newJobGroupName = enterPressEvent.getText();
            if (!Strings.isNullOrEmpty(newJobGroupName)
                    && !jobGroupNames.contains(newJobGroupName)) {
                jobGroupNames.add(newJobGroupName);
                jobGroupField.setOptionsList(jobGroupNames);
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

        List<String> existedJobsClassNames = quartzJobClassFinder.getQuartzJobClassNames();
        jobClassField.setOptionsList(existedJobsClassNames);
    }

    @Subscribe("triggerModelTable.view")
    public void onTriggerModelGroupTableView(Action.ActionPerformedEvent event) {
        TriggerModel triggerModel = triggerModelTable.getSingleSelected();
        if (triggerModel == null) {
            return;
        }

        TriggerModelEdit editor = screenBuilders.editor(triggerModelTable)
                .withScreenClass(TriggerModelEdit.class)
                .withParentDataContext(getScreenData().getDataContext())
                .build();
        ((ReadOnlyAwareScreen) editor).setReadOnly(true);
        editor.show();
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
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        if (deleteObsoleteJob) {
            quartzService.deleteJob(obsoleteJobName, obsoleteJobGroup);
        }

        quartzService.updateQuartzJob(getEditedEntity(), jobDataParamsDc.getItems(), triggerModelDc.getItems(), replaceJobIfExists);
    }

    @Subscribe("jobDataParamsTable.addNewDataParam")
    public void onJobDataParamsTableCreate(Action.ActionPerformedEvent event) {
        List<JobDataParameterModel> currentItems = new ArrayList<>(jobDataParamsDc.getItems());

        JobDataParameterModel itemToAdd = dataManager.create(JobDataParameterModel.class);
        currentItems.add(itemToAdd);
        jobDataParamsDc.setItems(currentItems);
        jobDataParamsTable.setSelected(itemToAdd);
        jobDataParamsTable.requestFocus(itemToAdd, "key");
    }

}
