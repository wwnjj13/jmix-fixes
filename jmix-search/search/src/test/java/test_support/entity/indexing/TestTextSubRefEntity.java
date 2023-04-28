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

package test_support.entity.indexing;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import test_support.entity.BaseEntity;

import jakarta.persistence.*;

@JmixEntity
@Table(name = "TEST_TEXT_SUB_REF_ENTITY")
@Entity(name = "test_TextSubRefEntity")
public class TestTextSubRefEntity extends BaseEntity {
    @InstanceName
    @Column(name = "NAME")
    private String name;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "oneToOneRef")
    private TestTextRefEntity inverseOneToOneRef;

    @JoinColumn(name = "MANY_TO_ONE_REF_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private TestTextRefEntity manyToOneRef;

    public TestTextRefEntity getManyToOneRef() {
        return manyToOneRef;
    }

    public void setManyToOneRef(TestTextRefEntity manyToOneRef) {
        this.manyToOneRef = manyToOneRef;
    }

    public TestTextRefEntity getInverseOneToOneRef() {
        return inverseOneToOneRef;
    }

    public void setInverseOneToOneRef(TestTextRefEntity inverseOneToOneRef) {
        this.inverseOneToOneRef = inverseOneToOneRef;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}