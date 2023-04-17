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

package io.jmix.flowui.action.binder;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.shared.Registration;
import io.jmix.flowui.kit.action.Action;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractActionBindingImpl<H extends Component, A extends Action, C extends Component>
        implements ActionBinding<C, A> {

    protected ActionBinder<H> binder;
    protected final A action;
    protected final C component;

    protected List<Registration> registrations = new ArrayList<>();

    public AbstractActionBindingImpl(ActionBinder<H> binder,
                                     A action,
                                     C component,
                                     @Nullable List<Registration> registrations) {
        this.binder = binder;
        this.component = component;
        this.action = action;

        if (registrations != null) {
            this.registrations.addAll(registrations);
        }
    }

    @Override
    public C getComponent() {
        return component;
    }

    @Override
    public A getAction() {
        return action;
    }

    @Override
    public void unbind() {
        registrations.forEach(Registration::remove);
        registrations.clear();

        if (binder != null) {
            binder.removeBindingInternal(this);
            binder = null;
        }
    }
}
