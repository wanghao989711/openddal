/*
 * Copyright 2014-2016 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Created on 2014年12月25日
// $Id$
package com.openddal.dbobject.index;

import com.openddal.dbobject.DbObject;
import com.openddal.dbobject.schema.SchemaObject;
import com.openddal.dbobject.table.Column;
import com.openddal.dbobject.table.IndexColumn;
import com.openddal.dbobject.table.Table;
import com.openddal.dbobject.table.TableFilter;
import com.openddal.engine.Constants;
import com.openddal.message.DbException;
import com.openddal.result.SortOrder;
import com.openddal.value.Value;

/**
 * @author <a href="mailto:jorgie.mail@gmail.com">jorgie li</a>
 */
public class Index extends SchemaObject {

    protected IndexColumn[] indexColumns;
    protected Column[] columns;
    protected int[] columnIds;
    protected Table table;
    protected IndexType indexType;

    /**
     * Initialize the base index.
     *
     * @param newTable        the table
     * @param id              the object id
     * @param name            the index name
     * @param newIndexColumns the columns that are indexed or null if this is
     *                        not yet known
     * @param newIndexType    the index type
     */
    public Index(Table newTable, int id, String name,
                     IndexColumn[] newIndexColumns, IndexType newIndexType) {
        initSchemaObjectBase(newTable.getSchema(), id, name);
        this.indexType = newIndexType;
        this.table = newTable;
        if (newIndexColumns != null) {
            this.indexColumns = newIndexColumns;
            columns = new Column[newIndexColumns.length];
            int len = columns.length;
            columnIds = new int[len];
            for (int i = 0; i < len; i++) {
                Column col = newIndexColumns[i].column;
                columns[i] = col;
                columnIds[i] = col.getColumnId();
            }
        }
    }

    /**
     * Check that the index columns are not CLOB or BLOB.
     *
     * @param columns the columns
     */
    protected static void checkIndexColumnTypes(IndexColumn[] columns) {
        for (IndexColumn c : columns) {
            int type = c.column.getType();
            if (type == Value.CLOB || type == Value.BLOB) {
                throw DbException
                        .getUnsupportedException("Index on BLOB or CLOB column: "
                                + c.column.getCreateSQL());
            }
        }
    }


    /**
     * Calculate the cost for the given mask as if this index was a typical
     * b-tree range index. This is the estimated cost required to search one
     * row, and then iterate over the given number of rows.
     *
     * @param masks     the search mask
     * @param rowCount  the number of rows in the index
     * @param filter    the table filter
     * @param sortOrder the sort order
     * @return the estimated cost
     */
    protected long getCostRangeIndex(int[] masks, long rowCount,
                                     TableFilter filter, SortOrder sortOrder) {
        rowCount += Constants.COST_ROW_OFFSET;
        long cost = rowCount;
        if (masks == null) {
            return cost;
        }
        for (int i = 0, len = columns.length; i < len; i++) {
            Column column = columns[i];
            int index = column.getColumnId();
            int mask = masks[index];
            if ((mask & IndexCondition.EQUALITY) == IndexCondition.EQUALITY) {
                if (i == columns.length - 1 && getIndexType().isUnique()) {
                    if (getIndexType().isShardingKey()) {
                        cost = rowCount / 2000;
                        break;
                    } else {
                        cost = rowCount / 500;
                        break;
                    }
                }
                cost = getIndexType().isShardingKey() ? rowCount / 1000 : rowCount / 200;
            } else if ((mask & IndexCondition.RANGE) == IndexCondition.RANGE) {
                cost = getIndexType().isShardingKey() ? rowCount / 100 : rowCount / 80;
                break;
            } else if ((mask & IndexCondition.START) == IndexCondition.START) {
                cost = getIndexType().isShardingKey() ? rowCount / 50 : rowCount / 30;
                break;
            } else if ((mask & IndexCondition.END) == IndexCondition.END) {
                cost = getIndexType().isShardingKey() ? rowCount / 10 : rowCount / 8;
                break;
            } else {
                break;
            }
        }
        // if the ORDER BY clause matches the ordering of this index,
        // it will be cheaper than another index, so adjust the cost accordingly
        if (sortOrder != null) {
            boolean sortOrderMatches = true;
            int coveringCount = 0;
            int[] sortTypes = sortOrder.getSortTypes();
            for (int i = 0, len = sortTypes.length; i < len; i++) {
                if (i >= indexColumns.length) {
                    // we can still use this index if we are sorting by more
                    // than it's columns, it's just that the coveringCount
                    // is lower than with an index that contains
                    // more of the order by columns
                    break;
                }
                Column col = sortOrder.getColumn(i, filter);
                if (col == null) {
                    sortOrderMatches = false;
                    break;
                }
                IndexColumn indexCol = indexColumns[i];
                if (col != indexCol.column) {
                    sortOrderMatches = false;
                    break;
                }
                int sortType = sortTypes[i];
                if (sortType != indexCol.sortType) {
                    sortOrderMatches = false;
                    break;
                }
                coveringCount++;
            }
            if (sortOrderMatches) {
                // "coveringCount" makes sure that when we have two
                // or more covering indexes, we choose the one
                // that covers more
                cost -= coveringCount;
            }
        }
        return cost;
    }

    public int getColumnIndex(Column col) {
        for (int i = 0, len = columns.length; i < len; i++) {
            if (columns[i].equals(col)) {
                return i;
            }
        }
        return -1;
    }

    public IndexColumn[] getIndexColumns() {
        return indexColumns;
    }

    public Column[] getColumns() {
        return columns;
    }

    public IndexType getIndexType() {
        return indexType;
    }

    @Override
    public int getType() {
        return DbObject.INDEX;
    }

    public Table getTable() {
        return table;
    }


    @Override
    public void checkRename() {

    }

}
