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

package io.jmix.multitenancy.data;

import io.jmix.core.UnsafeDataManager;
import io.jmix.multitenancy.entity.TenantAssigmentEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("mten_TenantAssigmentRepository")
public class TenantAssigmentRepositoryImpl implements TenantAssigmentRepository {

    private final UnsafeDataManager dataManager;

    public TenantAssigmentRepositoryImpl(UnsafeDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public Optional<TenantAssigmentEntity> findAssigmentByUsername(String username) {
        return dataManager.load(TenantAssigmentEntity.class)
                .query("select t from mten_TenantAssigmentEntity t where t.username = :username")
                .parameter("username", username)
                .optional();
    }
}
