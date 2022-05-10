package com.slowpoke.model.field;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * int field
 *
 * @author zhangjw
 * @version 1.0
 */
public class IntField implements Field, Serializable {

    private static final long serialVersionUID = -4714840292406579518L;

    private final int value;

    public IntField(int value) {
        this.value = value;
    }


    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }



    @Override
    public FieldType getType() {
        return FieldType.INT;
    }

    @Override
    public void serialize(DataOutputStream dos) throws IOException {
        dos.writeInt(value);
    }
}
