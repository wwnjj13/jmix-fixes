/*
 * Copyright 2019 Haulmont.
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

package io.jmix.core;

import jakarta.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

public class TemporalValue implements Serializable {

    private static final long serialVersionUID = 4972088045550018312L;

    public final Date date;
    public final TemporalType type;

    public TemporalValue(Date date, TemporalType type) {
        this.date = date;
        this.type = type;
    }
}
