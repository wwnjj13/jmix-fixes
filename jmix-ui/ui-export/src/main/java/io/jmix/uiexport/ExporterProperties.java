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

package io.jmix.uiexport;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jmix.ui.exporter")
@ConstructorBinding
public class ExporterProperties {

    private Integer loadBatchSize;

    private Boolean streamInDisk;

    private Integer streamWindowSize;

    public ExporterProperties(@DefaultValue("50") Integer batchSize,
                              @DefaultValue("false") Boolean streamInDisk,
                              @DefaultValue("100") Integer streamWindowSize) {
        this.streamInDisk = streamInDisk;
        this.loadBatchSize = batchSize;
        this.streamWindowSize = streamWindowSize;
    }

    public Integer getLoadBatchSize() {
        return loadBatchSize;
    }

    public void setLoadBatchSize(Integer loadBatchSize) {
        this.loadBatchSize = loadBatchSize;
    }

    public Integer getStreamWindowSize() {
        return streamWindowSize;
    }

    public void setStreamWindowSize(Integer streamWindowSize) {
        this.streamWindowSize = streamWindowSize;
    }

    public Boolean getStreamInDisk() {
        return streamInDisk;
    }

    public void setStreamInDisk(Boolean streamInDisk) {
        this.streamInDisk = streamInDisk;
    }
}