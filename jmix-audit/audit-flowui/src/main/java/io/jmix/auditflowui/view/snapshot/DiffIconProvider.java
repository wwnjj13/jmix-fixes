/*
 * Copyright 2021 Haulmont.
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

package io.jmix.auditflowui.view.snapshot;

import io.jmix.audit.snapshot.model.EntityPropertyDifferenceModel;

import javax.annotation.Nullable;
import java.util.function.Function;

public class DiffIconProvider implements Function<EntityPropertyDifferenceModel, String> {
    @Override
    public String apply(EntityPropertyDifferenceModel entityPropertyDifferenceModel) {
        return null;
    }

}
