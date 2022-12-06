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

package io.jmix.appsettingsflowui.componentfactory;

import io.jmix.core.annotation.Secret;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.metamodel.model.MetaProperty;

import java.util.UUID;

public final class EntityUtils {

    private EntityUtils() {
        //to prevent instantiation
    }

    public static boolean isSecret(MetaProperty metaProperty) {
        return metaProperty.getAnnotatedElement().isAnnotationPresent(Secret.class);
    }

    public static boolean isMany(MetaProperty metaProperty) {
        return metaProperty.getRange().getCardinality().isMany();
    }

    static boolean isByteArray(MetaProperty metaProperty) {
        return metaProperty.getRange().asDatatype().getJavaClass().equals(byte[].class);
    }

    static boolean isUuid(MetaProperty metaProperty) {
        return metaProperty.getRange().asDatatype().getJavaClass().equals(UUID.class);
    }

    static boolean isBoolean(MetaProperty metaProperty) {
        return metaProperty.getRange().isDatatype()
                && metaProperty.getRange().asDatatype().getJavaClass().equals(Boolean.class);
    }

    static boolean requireTextArea(MetaProperty metaProperty, Object item, int maxTextFieldLength) {
        if (!String.class.equals(metaProperty.getJavaType())) {
            return false;
        }

        Integer textLength = (Integer) metaProperty.getAnnotations().get("length");
        boolean isLong = textLength != null && textLength > maxTextFieldLength;

        Object value = EntityValues.getValue(item, metaProperty.getName());
        boolean isContainsSeparator = value != null && containsSeparator((String) value);

        return isLong || isContainsSeparator;
    }

    static boolean containsSeparator(String s) {
        return s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
    }
}
