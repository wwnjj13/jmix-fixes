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

package io.jmix.flowui.component.jpqlfilter;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.querycondition.JpqlCondition;
import io.jmix.flowui.component.filer.AbstractSingleFilterComponent;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Collections;

import static com.google.common.base.Preconditions.checkState;
import static io.jmix.core.common.util.Preconditions.checkNotNullArgument;

public class JpqlFilter<V> extends AbstractSingleFilterComponent<V> {

    protected static final String JPQL_FILTER_CLASS_NAME = "jmix-jpql-filter";

    protected JpqlFilterSupport jpqlFilterSupport;

    protected String parameterName;
    protected Class<?> parameterClass;
    protected String where;
    protected String join;
    protected boolean hasInExpression;

    @Override
    protected void autowireDependencies() {
        super.autowireDependencies();

        jpqlFilterSupport = applicationContext.getBean(JpqlFilterSupport.class);
    }

    @Override
    protected void initRootComponent(HorizontalLayout root) {
        super.initRootComponent(root);
        root.setClassName(JPQL_FILTER_CLASS_NAME);
    }

    @Override
    protected JpqlCondition createQueryCondition() {
        return new JpqlCondition();
    }

    @Override
    public String getInnerComponentPrefix() {
        return jpqlFilterSupport.getJpqlFilterPrefix(getId());
    }

    @Override
    public JpqlCondition getQueryCondition() {
        return (JpqlCondition) queryCondition;
    }

    @Override
    protected void updateQueryCondition(@Nullable V newValue) {
        if (parameterName == null) {
            return;
        }

        if (parameterClass == Void.class) {
            if (Boolean.TRUE.equals(newValue)) {
                getQueryCondition().setWhere(where);
                getQueryCondition().setJoin(join);
            } else {
                resetQueryCondition();
            }

            if (!getQueryCondition().getParameterValuesMap().isEmpty()) {
                getQueryCondition().setParameterValuesMap(Collections.emptyMap());
            }
        } else {
            Object parameterValue = null;
            if (newValue != null) {
                if (EntityValues.isEntity(newValue)) {
                    parameterValue = EntityValues.getIdOrEntity(newValue);
                } else {
                    parameterValue = newValue;
                }
            }

            getQueryCondition().setParameterValuesMap(Collections.singletonMap(parameterName, parameterValue));
        }
    }

    @Override
    public String getParameterName() {
        return parameterName;
    }

    @Override
    public void setParameterName(String parameterName) {
        checkState(this.parameterName == null, "Parameter name has already been initialized");
        checkNotNullArgument(parameterName);

        if (StringUtils.isNotEmpty(where)) {
            getQueryCondition().setWhere(where.replace("?", ":" + parameterName));
        }

        this.parameterName = parameterName;
    }

    public Class<?> getParameterClass() {
        return parameterClass;
    }

    public void setParameterClass(Class<?> parameterClass) {
        checkState(this.parameterClass == null, "Parameter class has already been initialized");
        checkNotNullArgument(parameterClass);

        if (parameterClass == Void.class) {
            resetQueryCondition();
        }

        this.parameterClass = parameterClass;
    }

    public String getWhere() {
//        checkState(where != null, "where clause is not set");
        return where;
    }

    @Nullable
    public String getJoin() {
        return join;
    }

    public void setCondition(String where, @Nullable String join) {
        checkNotNullArgument(where);

        if (parameterClass != Void.class) {
            if (StringUtils.isNotEmpty(parameterName)) {
                getQueryCondition().setWhere(where.replace("?", ":" + parameterName));
            }

            getQueryCondition().setJoin(join);
        }

        this.where = where;
        this.join = join;
    }

    public boolean hasInExpression() {
        return hasInExpression;
    }

    public void setHasInExpression(boolean hasInExpression) {
        this.hasInExpression = hasInExpression;
    }

    protected void resetQueryCondition() {
        getQueryCondition().setWhere("");
        getQueryCondition().setJoin("");
    }
}
