

package com.example.sanmolsoftware.popularmovies.utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public final class SelectionBuilder {

    private String mTable = null;
    private Map<String, String> mProjectionMap = new HashMap<>();
    private StringBuilder mSelection = new StringBuilder();
    private ArrayList<String> mSelectionArgs = new ArrayList<>();
    private String mGroupBy = null;
    private String mHaving = null;

    /**
     * Reset any internal state, allowing this builder to be recycled.
     */
    public SelectionBuilder reset() {
        mTable = null;
        mGroupBy = null;
        mHaving = null;
        mSelection.setLength(0);
        mSelectionArgs.clear();
        return this;
    }


    public SelectionBuilder where(String selection, String... selectionArgs) {
        if (TextUtils.isEmpty(selection)) {
            if (selectionArgs != null && selectionArgs.length > 0) {
                throw new IllegalArgumentException(
                        "Valid selection required when including arguments=");
            }

            // Shortcut when clause is empty
            return this;
        }

        if (mSelection.length() > 0) {
            mSelection.append(" AND ");
        }

        mSelection.append("(").append(selection).append(")");
        if (selectionArgs != null) {
            Collections.addAll(mSelectionArgs, selectionArgs);
        }

        return this;
    }

    public SelectionBuilder groupBy(String groupBy) {
        mGroupBy = groupBy;
        return this;
    }

    public SelectionBuilder having(String having) {
        mHaving = having;
        return this;
    }

    public SelectionBuilder table(String table) {
        mTable = table;
        return this;
    }

    /**
     * Replace positional params in table. Use for JOIN ON conditions.
     */
    public SelectionBuilder table(String table, String... tableParams) {
        if (tableParams != null && tableParams.length > 0) {
            String[] parts = table.split("[?]", tableParams.length + 1);
            StringBuilder sb = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                sb.append('"').append(tableParams[i - 1]).append('"')
                        .append(parts[i]);
            }
            mTable = sb.toString();
        } else {
            mTable = table;
        }
        return this;
    }

    private void assertTable() {
        if (mTable == null) {
            throw new IllegalStateException("Table not specified");
        }
    }

    public SelectionBuilder mapToTable(String column, String table) {
        mProjectionMap.put(column, table + "." + column);
        return this;
    }

    public SelectionBuilder map(String fromColumn, String toClause) {
        mProjectionMap.put(fromColumn, toClause + " AS " + fromColumn);
        return this;
    }

    /**
     * Return selection string for current internal state.
     *
     * @see #getSelectionArgs()
     */
    public String getSelection() {
        return mSelection.toString();
    }


    public String[] getSelectionArgs() {
        return mSelectionArgs.toArray(new String[mSelectionArgs.size()]);
    }

    private void mapColumns(String[] columns) {
        for (int i = 0; i < columns.length; i++) {
            final String target = mProjectionMap.get(columns[i]);
            if (target != null) {
                columns[i] = target;
            }
        }
    }

    @Override
    public String toString() {
        return "SelectionBuilder[table=" + mTable + ", selection=" + getSelection()
                + ", selectionArgs=" + Arrays.toString(getSelectionArgs())
                + "projectionMap = " + mProjectionMap + " ]";
    }


    public Cursor query(SQLiteDatabase db, String[] columns, String orderBy) {
        return query(db, false, columns, orderBy, null);
    }


    public Cursor query(SQLiteDatabase db, boolean distinct, String[] columns, String orderBy,
                        String limit) {
        assertTable();
        if (columns != null) mapColumns(columns);
        return db.query(distinct, mTable, columns, getSelection(), getSelectionArgs(), mGroupBy,
                mHaving, orderBy, limit);
    }


    public int update(SQLiteDatabase db, ContentValues values) {
        assertTable();
        return db.update(mTable, values, getSelection(), getSelectionArgs());
    }


    public int delete(SQLiteDatabase db) {
        assertTable();
        return db.delete(mTable, getSelection(), getSelectionArgs());
    }
}
