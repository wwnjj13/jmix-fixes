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

package io.jmix.flowui.component.propertyfilter;

import io.jmix.core.metamodel.datatype.impl.EnumClass;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.textfield.TypedTextField;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Operation representing corresponding filtering condition.
 */
public enum FilteringOperation implements EnumClass<String> {
    EQUAL(Type.VALUE),
    NOT_EQUAL(Type.VALUE),
    GREATER(Type.VALUE),
    GREATER_OR_EQUAL(Type.VALUE),
    LESS(Type.VALUE),
    LESS_OR_EQUAL(Type.VALUE),
    CONTAINS(Type.VALUE),
    NOT_CONTAINS(Type.VALUE),
    STARTS_WITH(Type.VALUE),
    ENDS_WITH(Type.VALUE),
    IS_SET(Type.UNARY),
    IS_NOT_SET(Type.UNARY),
    IN_LIST(Type.LIST),
    NOT_IN_LIST(Type.LIST),
    DATE_INTERVAL(Type.INTERVAL),
    ;

    private final Type type;

    FilteringOperation(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Nullable
    public static FilteringOperation fromId(String id) {
        for (FilteringOperation operation : FilteringOperation.values()) {
            if (Objects.equals(id, operation.getId())) {
                return operation;
            }
        }
        return null;
    }

    @Override
    public String getId() {
        return name();
    }

    /**
     * Operation type representing the required field type for editing
     * a property value with the given operation.
     */
    public enum Type {

        /**
         * Requires a field suitable for editing a property value, e.g.
         * {@link TypedTextField} for String, {@link JmixComboBox} for enum.
         */
        VALUE,

        /**
         * Requires a field suitable for choosing unary value, e.g. true/false, YES/NO.
         */
        UNARY,

        /**
         * Requires a field suitable for selecting multiple values of
         * the same type as the property value.
         */
        LIST,

        /**
         * Requires a field suitable for selecting a range of values of
         * the same type as the property value.
         */
        INTERVAL
    }
}
