package com.slowpoke.model.row;

import com.slowpoke.model.table.TableDesc;
import com.slowpoke.model.field.Field;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * tuple, a row in table
 *
 * @author zhangjw, Dr. Chen
 * @version 1.0
 */
public class Row implements Serializable {
    private static final long serialVersionUID = 3508867799019762862L;

    /**
     * fields in row
     */
    private Field[] fields;

    private TableDesc tableDesc;

    /**
     * ref pageID
     */
    private RowID rowID;

    public Row(TableDesc tableDesc, RowID rowID) {
        this.tableDesc = tableDesc;
        this.rowID = rowID;
    }

    public Row(TableDesc tableDesc) {
        this.tableDesc = tableDesc;
        this.fields = new Field[tableDesc.getAttributesNum()];
    }

    public TableDesc getTableDesc() {
        return tableDesc;
    }

    public void setTableDesc(TableDesc tableDesc) {
        this.tableDesc = tableDesc;
    }

    public void setField(int index, Field field) {
        fields[index] = field;
    }

    public Field getField(int index) {
        return fields[index];
    }

    public void setFields(Field[] fields) {
        this.fields = fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields.toArray(new Field[0]);
    }

    public RowID getRowID() {
        return rowID;
    }

    public void setRowID(RowID rowID) {
        this.rowID = rowID;
    }

    public static Row merge(Row left, Row right) {
        Row row = new Row(TableDesc.merge(left.getTableDesc(), right.getTableDesc()));
        List<Field> leftFields = left.getFields();
        List<Field> rightFields = right.getFields();
        ArrayList<Field> fields = new ArrayList<>(leftFields.size() + rightFields.size());
        fields.addAll(leftFields);
        fields.addAll(rightFields);
        Field[] fieldsArray = fields.toArray(new Field[leftFields.size() + rightFields.size()]);
        row.setFields(fieldsArray);
        return row;
    }
    @Override
    public String toString() {
        StringBuilder column = new StringBuilder().append("[");
        for (int i = 0; i < fields.length - 1; i++) {
            column.append(fields[i].toString()).append("\t");
        }
        column.append(fields[fields.length - 1]).append("]");
        return column.toString();
    }

    public List<Field> getFields() {
        return Stream.of(fields).collect(Collectors.toList());
    }
}
