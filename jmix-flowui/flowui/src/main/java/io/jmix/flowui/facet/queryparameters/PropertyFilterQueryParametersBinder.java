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

package io.jmix.flowui.facet.queryparameters;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.router.QueryParameters;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.MetadataTools;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.metamodel.model.MetaPropertyPath;
import io.jmix.core.metamodel.model.Range;
import io.jmix.flowui.component.propertyfilter.PropertyFilter;
import io.jmix.flowui.component.propertyfilter.PropertyFilter.Operation;
import io.jmix.flowui.facet.QueryParametersFacet.QueryParametersChangeEvent;
import io.jmix.flowui.view.navigation.UrlParamSerializer;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PropertyFilterQueryParametersBinder extends AbstractQueryParametersBinder {

    public static final String NAME = "propertyFilter";

    public static final String SETTINGS_SEPARATOR = "_";

    protected PropertyFilter<?> filter;

    protected String filterParam;

    protected ApplicationContext applicationContext;
    protected UrlParamSerializer urlParamSerializer;
    protected MetadataTools metadataTools;
    protected DataManager dataManager;

    public PropertyFilterQueryParametersBinder(PropertyFilter<?> filter,
                                               UrlParamSerializer urlParamSerializer,
                                               ApplicationContext applicationContext) {
        this.filter = filter;
        this.urlParamSerializer = urlParamSerializer;
        this.applicationContext = applicationContext;

        autowireDependencies();
        initComponent(filter);
    }

    protected void autowireDependencies() {
        metadataTools = applicationContext.getBean(MetadataTools.class);
    }

    protected void initComponent(PropertyFilter<?> filter) {
        filter.addValueChangeListener(this::onValueChange);
        filter.addOperationChangeListener(this::onOperationChange);
    }

    @SuppressWarnings("rawtypes")
    protected void onValueChange(AbstractField.ComponentValueChangeEvent event) {
        updateQueryParameters();
    }

    protected void onOperationChange(PropertyFilter.OperationChangeEvent<?> event) {
        updateQueryParameters();
    }

    protected void updateQueryParameters() {
        String serializedOperation = urlParamSerializer
                .serialize(convertFromEnumName(filter.getOperation().name().toLowerCase()));
        String serializedValue = urlParamSerializer
                .serialize(getSerializableValue(filter.getValue()));

        String paramValue = serializedOperation + SETTINGS_SEPARATOR + serializedValue;
        QueryParameters queryParameters = QueryParameters
                .simple(ImmutableMap.of(getFilterParam(), paramValue));

        fireQueryParametersChanged(new QueryParametersChangeEvent(this, queryParameters));
    }

    // TODO: gg, extract
    protected String convertToEnumName(String value) {
        return value.replace("-", "_");
    }

    protected String convertFromEnumName(String value) {
        return value.replace("_", "-");
    }

    protected Object getSerializableValue(@Nullable Object value) {
        if (value == null) {
            return "";
        } else if (EntityValues.isEntity(value)) {
            Object id = EntityValues.getId(value);
            return id != null ? id : "";
        } else if (value instanceof Enum) {
            return convertFromEnumName(((Enum<?>) value).name());
        } else {
            return value;
        }
    }

    @Override
    public void updateState(QueryParameters queryParameters) {
        Map<String, List<String>> parameters = queryParameters.getParameters();
        if (parameters.containsKey(getFilterParam())) {
            String serializedSettings = parameters.get(getFilterParam()).get(0);
            String[] values = serializedSettings.split(SETTINGS_SEPARATOR);
            if (values.length < 1) {
                throw new IllegalStateException("Can't parse property filter settings: " + serializedSettings);
            }

            Operation operation = urlParamSerializer.deserialize(Operation.class, convertToEnumName(values[0]));
            filter.setOperation(operation);

            if (values.length == 2
                    && !Strings.isNullOrEmpty(values[1])) {
                Object parsedValue = parseValue(filter.getDataLoader().getContainer().getEntityMetaClass(),
                        Objects.requireNonNull(filter.getProperty()), operation.getType(), values[1]);
                //noinspection unchecked,rawtypes
                ((PropertyFilter) filter).setValue(parsedValue);
            }
        }
    }

    // TODO: gg, extract
    protected Object parseValue(MetaClass metaClass, String property, Operation.Type operationType, String valueString) {
        MetaPropertyPath mpp = metadataTools.resolveMetaPropertyPath(metaClass, property);

        switch (operationType) {
            case UNARY:
                return urlParamSerializer.deserialize(Boolean.class, valueString);
            case VALUE:
                return parseSingleValue(property, valueString, mpp);
            case LIST:
            case INTERVAL:
                throw new UnsupportedOperationException("Not implemented yet");
            default:
                throw new IllegalArgumentException("Unknown operation type: " + operationType);
        }
    }

    protected Object parseSingleValue(String property, String valueString, MetaPropertyPath mpp) {
        Range mppRange = mpp.getRange();
        if (mppRange.isDatatype()) {
            Class<?> type = mppRange.asDatatype().getJavaClass();
            return urlParamSerializer.deserialize(type, valueString);

        } else if (mppRange.isEnum()) {
            Class<?> type = mppRange.asEnumeration().getJavaClass();
            String enumString = convertToEnumName(valueString);
            return urlParamSerializer.deserialize(type, enumString);

        } else if (mppRange.isClass()) {
            MetaClass propertyMetaClass = mppRange.asClass();
            MetaProperty idProperty = Objects.requireNonNull(metadataTools.getPrimaryKeyProperty(propertyMetaClass));
            Object idValue = urlParamSerializer.deserialize(idProperty.getJavaType(), valueString);

            return getDataManager().load(Id.of(idValue, propertyMetaClass.getJavaClass()))
                    .optional().orElseThrow(() ->
                            new IllegalArgumentException(String.format("Entity with type '%s' and id '%s' isn't found",
                                    propertyMetaClass.getJavaClass(), idValue)));

        } else {
            throw new IllegalStateException("Unsupported property: " + property);
        }
    }

    public String getFilterParam() {
        return Strings.isNullOrEmpty(filterParam)
                ? filter.getId().orElseThrow(() ->
                new IllegalStateException("Component has neither id nor explicit url query param"))
                : filterParam;
    }

    public void setFilterParam(@Nullable String filterParam) {
        this.filterParam = filterParam;
    }

    protected DataManager getDataManager() {
        if (dataManager == null) {
            dataManager = applicationContext.getBean(DataManager.class);
        }
        return dataManager;
    }
}
