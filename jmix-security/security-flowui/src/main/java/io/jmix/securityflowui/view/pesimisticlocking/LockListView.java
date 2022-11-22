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

/*
 * Copyright (c) 2008-2016 Haulmont.
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
 *
 */
package io.jmix.securityflowui.view.pesimisticlocking;


import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.Route;
import io.jmix.core.MessageTools;
import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.pessimisticlocking.LockInfo;
import io.jmix.core.pessimisticlocking.LockManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.DefaultMainViewParent;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Route(value = "sec/locks", layout = DefaultMainViewParent.class)
@ViewController("sec_Lock.list")
@ViewDescriptor("lock-list-view.xml")
@EditedEntityContainer("roleModelDc")
@DialogMode(width = "50em", height = "37.5em")
public class LockListView extends StandardView {

    @ViewComponent
    protected CollectionContainer<LockInfo> locksDs;

    @ViewComponent
    protected DataGrid<LockInfo> locksTable;

    @Autowired
    protected LockManager service;

    @Autowired
    protected Notifications notifications;

    @Autowired
    protected Messages messages;

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected MessageTools messageTools;

    @Subscribe
    public void onInit(InitEvent event) {
        locksTable.addColumn(this::lockNameFormatter)
                .setKey("objectTypeColumn")
                .setHeader(messages.getMessage(this.getClass(), "LockInfo.objectType"))
                .setSortable(true);
        List<Grid.Column<LockInfo>> columnsOrder = Arrays.asList(
                locksTable.getColumnByKey("objectTypeColumn"),
                locksTable.getColumnByKey("objectIdColumn"),
                locksTable.getColumnByKey("usernameColumn"),
                locksTable.getColumnByKey("sinceColumn")
        );
        locksTable.setColumnOrder(columnsOrder);
        refresh();
    }
    
    @Subscribe("locksTable.unlock")
    public void onLocksUnlock(ActionPerformedEvent event) {
        LockInfo lockInfo = locksTable.getSingleSelectedItem();
        if (lockInfo != null) {
            service.unlock(lockInfo.getObjectType(), lockInfo.getObjectId());
            refresh();
            if (lockInfo.getObjectId() != null) {
                notifications
                        .create(messages.formatMessage(LockListView.class,
                                "hasBeenUnlockedWithId",
                                lockInfo.getObjectType(),
                                lockInfo.getId()))
                        .withType(Notifications.Type.DEFAULT)
                        .show();
            } else {
                notifications.create(messages.formatMessage(LockListView.class,
                                "hasBeenUnlockedWithoutId",
                                lockInfo.getObjectType()))
                        .withType(Notifications.Type.DEFAULT)
                        .show();
            }
        }
    }

    public Object lockNameFormatter(LockInfo value) {
        MetaClass metaClass = metadata.getSession().getClass(value.getObjectType());
        if (metaClass != null) {
            return messageTools.getEntityCaption(metaClass);
        } else {
            return value.getObjectType();
        }
    }

    protected void refresh() {
        locksDs.getMutableItems().clear();
        Collection<LockInfo> locks = service.getCurrentLocks();
        locksDs.getMutableItems().addAll(locks);
    }

    @Subscribe("locksTable.refresh")
    public void onLocksRefresh(ActionPerformedEvent event) {
        refresh();
    }
}
