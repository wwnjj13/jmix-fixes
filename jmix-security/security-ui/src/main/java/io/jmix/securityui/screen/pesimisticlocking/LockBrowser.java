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
package io.jmix.securityui.screen.pesimisticlocking;


import io.jmix.core.Messages;
import io.jmix.core.pessimisticlocking.LockInfo;
import io.jmix.core.pessimisticlocking.LockManager;
import io.jmix.ui.Notifications;
import io.jmix.ui.component.GroupTable;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.navigation.Route;
import io.jmix.ui.screen.LookupComponent;
import io.jmix.ui.screen.Screen;
import io.jmix.ui.screen.Subscribe;
import io.jmix.ui.screen.UiController;
import io.jmix.ui.screen.UiDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import java.util.Collection;

@UiController("sec_LockBrowser.browse")
@UiDescriptor("lock-browser.xml")
@Route("lockbrowser")
public class LockBrowser extends Screen {

    @Autowired
    protected LockManager service;

    @Autowired
    protected Notifications notifications;

    @Autowired
    protected Messages messages;

    @Inject
    protected CollectionContainer<LockInfo> locksDs;

//    @Named("locks")
    protected GroupTable<LockInfo> table;

    @Subscribe
    public void onInit(InitEvent event) {

        refresh();
    }

////    @Named("setupTable.create")
//    protected CreateAction createAction;
//
////    @Named("setupTable.edit")
//    protected EditAction editAction;


    public void unlock() {
        LockInfo lockInfo = table.getSingleSelected();
        if (lockInfo != null) {
            service.unlock(lockInfo.getObjectType(), lockInfo.getObjectId());
            refresh();
            if (lockInfo.getObjectId() != null) {
                notifications.create().withCaption(
                        messages.formatMessage(LockBrowser.class,
                                "hasBeenUnlockedWithId",
                                lockInfo.getObjectType(),
                                lockInfo.getId()))
                        .withType(Notifications.NotificationType.HUMANIZED)
                        .show();
            } else {
                notifications.create().withCaption(
                        messages.formatMessage(LockBrowser.class,
                        "hasBeenUnlockedWithoutId",
                        lockInfo.getObjectType()))
                        .withType(Notifications.NotificationType.HUMANIZED)
                        .show();
            }
        }
    }

    public void refresh() {
        locksDs.getMutableItems().clear();
        Collection<LockInfo> locks = service.getCurrentLocks();
        locksDs.getMutableItems().addAll(locks);
    }

    public void reloadConfig() {
        service.reloadConfiguration();
        notifications.create().withCaption(
                        messages.getMessage(LockBrowser.class,
                                "locksConfigurationHasBeenReloaded"))
                .withType(Notifications.NotificationType.HUMANIZED)
                .show();
    }
}
