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

package io.jmix.securityui.screen.pesimisticlocking;

import io.jmix.core.MessageTools;
import io.jmix.core.Metadata;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.ui.component.formatter.Formatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("sec_LockNameFormatter")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class LockNameFormatter implements Formatter<String> {

    @Autowired
    protected Metadata metadata;
    @Autowired
    protected MessageTools messageTools;

    @Override
    public String apply(String value) {
        MetaClass metaClass = metadata.getSession().getClass(value);
        if (metaClass != null) {
            return messageTools.getEntityCaption(metaClass);
        } else {
            return value;
        }
    }
}
