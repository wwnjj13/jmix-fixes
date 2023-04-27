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

package io.jmix.flowui.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.vaadin.flow.server.communication.AtmospherePushConnection;

import java.util.concurrent.TimeoutException;

public class TimeoutExceptionLogbackFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy == null) {
            return FilterReply.NEUTRAL;
        }
        if (!AtmospherePushConnection.class.getName().equals(event.getLoggerName())) {
            return FilterReply.NEUTRAL;
        }
        if (throwableProxy instanceof ThrowableProxy) {
            Throwable throwable = ((ThrowableProxy) throwableProxy).getThrowable();
            if (throwable instanceof TimeoutException) {
                return FilterReply.DENY;
            }
        }

        return FilterReply.NEUTRAL;
    }
}
