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

package io.jmix.uiexport.exporter.excel;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jmix.ui.exporter.excel")
@ConstructorBinding
public class ExcelExporterProperties {

    /**
     * Batch size when loading from data source
     */
    int loadBatchSize;

    /**
     * Use write to disk as a buffer. Used for less heap memory usage
     */
    boolean useStreamingApi;

    /**
     * Size of sliding window for buffer
     */
    int streamWindowSize;

    public ExcelExporterProperties(@DefaultValue("1000") int loadBatchSize,
                                   @DefaultValue("false") boolean useStreamingApi,
                                   @DefaultValue("100") int streamWindowSize) {
        this.useStreamingApi = useStreamingApi;
        this.loadBatchSize = loadBatchSize;
        this.streamWindowSize = streamWindowSize;
    }

    public int getLoadBatchSize() {
        return loadBatchSize;
    }

    public void setLoadBatchSize(int loadBatchSize) {
        this.loadBatchSize = loadBatchSize;
    }

    public int getStreamWindowSize() {
        return streamWindowSize;
    }

    public void setStreamWindowSize(int streamWindowSize) {
        this.streamWindowSize = streamWindowSize;
    }

    public boolean isUseStreamingApi() {
        return useStreamingApi;
    }

    public void setUseStreamingApi(boolean streamInDisk) {
        this.useStreamingApi = streamInDisk;
    }
}