/*
 * Copyright 2020 Haulmont.
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

package io.jmix.ui.component.impl;

import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.ui.component.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GroupFilterImpl extends CompositeComponent<GroupBoxLayout> implements GroupFilter {

//    protected int columnsCount;

    /*@Override
    public int getColumnsCount() {
        return columnsCount;
    }

    @Override
    public void setColumnsCount(int columnsCount) {
        if (this.columnsCount != columnsCount) {
            this.columnsCount = columnsCount;

            updateConditionsLayout();
        }
    }*/

    /*@Override
    public void setFrame(@Nullable Frame frame) {
        super.setFrame(frame);

        if (frame != null) {
            for (FilterComponent component : ownFilterComponentsOrder) {
                if (component instanceof BelongToFrame
                        && ((BelongToFrame) component).getFrame() == null) {
                    ((BelongToFrame) component).setFrame(frame);
                } else {
                    attachToFrame(component);
                }
            }
        }
    }

    protected void attachToFrame(Component childComponent) {
        ((FrameImplementation) frame).registerComponent(childComponent);
    }*/



    protected void addLogicalFilterComponentToConditionsLayoutRow(LogicalFilterComponent logicalFilterComponent,
                                                                  ResponsiveGridLayout.Row row) {
        ResponsiveGridLayout.Column column = createLogicalFilterComponentColumn(row);
        logicalFilterComponent.setParent(null);
        ComponentsHelper.getComposition(logicalFilterComponent).setParent(null);
        column.setComponent(logicalFilterComponent);

        if (logicalFilterComponent instanceof SupportsCaptionPosition) {
            ((SupportsCaptionPosition) logicalFilterComponent).setCaptionPosition(getCaptionPosition());
        }

        if (logicalFilterComponent instanceof SupportsColumnsCount) {
            ((SupportsColumnsCount) logicalFilterComponent).setColumnsCount(getColumnsCount());
        }
    }

    protected ResponsiveGridLayout.Column createLogicalFilterComponentColumn(ResponsiveGridLayout.Row row) {
        ResponsiveGridLayout.Column column = row.addColumn();
        column.setColumns(ResponsiveGridLayout.Breakpoint.XL, ResponsiveGridLayout.ColumnsValue.columns(12));

        int columnIndex = row.getColumns().indexOf(column);
        if (columnIndex != 0) {
            column.addStyleName("pt-2");
        }

        return column;
    }

    protected void addFilterComponentToConditionsLayoutRow(FilterComponent filterComponent,
                                                           ResponsiveGridLayout.Row row) {
        ResponsiveGridLayout.Column conditionValueColumn = createFilterComponentColumn(row);

        filterComponent.setParent(null);
        ComponentsHelper.getComposition(filterComponent).setParent(null);

        filterComponent.setWidthFull();
        if (filterComponent instanceof SupportsCaptionPosition) {
            ((SupportsCaptionPosition) filterComponent).setCaptionPosition(getCaptionPosition());
        }

        conditionValueColumn.setComponent(filterComponent);
    }

    protected ResponsiveGridLayout.Column createFilterComponentColumn(ResponsiveGridLayout.Row row) {
        boolean logicalFilterComponentAdded = row.getColumns().stream()
                .anyMatch(rowColumn -> rowColumn.getComponent() instanceof LogicalFilterComponent);

        ResponsiveGridLayout.Column column = row.addColumn();

        int columnIndex = row.getColumns().indexOf(column);
        if (columnIndex != 0) {
            column.addStyleName("pt-2");
        }

        if (!logicalFilterComponentAdded) {
            int columnsCount = getColumnsCount();

            if (columnIndex == 1 && columnsCount > 1) {
                column.addStyleName("pt-lg-0");
            }

            if (columnIndex == 2 && columnsCount > 2) {
                column.addStyleName("pt-xl-0");
            }
        }

        return column;
    }
}
