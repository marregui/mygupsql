package io.crate.cli.common;

import java.awt.*;
import java.sql.Types;


public final class SqlType {

    public static String resolveName(int sqlType) {
        String type;
        switch (sqlType) {
            case Types.BOOLEAN:
                type = "boolean";
                break;

            case Types.TINYINT:
                type = "tinyint";
                break;

            case Types.SMALLINT:
                type = "smallint";
                break;

            case Types.INTEGER:
                type = "integer";
                break;

            case Types.BIGINT:
                type = "bigint";
                break;

            case Types.REAL:
                type = "real";
                break;

            case Types.DOUBLE:
                type = "double";
                break;

            case Types.DATE:
                type = "date";
                break;

            case Types.TIMESTAMP:
                type = "timestamp";
                break;

            case Types.TIMESTAMP_WITH_TIMEZONE:
                type = "timestamptz";
                break;

            case Types.TIME:
                type = "time";
                break;

            case Types.TIME_WITH_TIMEZONE:
                type = "timetz";
                break;

            case Types.ARRAY:
                type = "array";
                break;

            case Types.BLOB:
                type = "blob";
                break;

            case Types.BINARY:
                type = "binary";
                break;

            case Types.VARBINARY:
                type = "varbinary";
                break;

            case Types.CHAR:
                type = "char";
                break;

            case Types.CLOB:
                type = "clob";
                break;

            case Types.VARCHAR:
                type = "varchar";
                break;

            case Types.BIT:
                type = "bit";
                break;

            case Types.STRUCT:
                type = "struct";
                break;

            case Types.JAVA_OBJECT:
                type = "java_object";
                break;

            default:
                type = "" + sqlType;
        }
        return type;
    }

    public static Color resolveColor(int sqlType) {
        Color color = Color.BLACK;
        switch (sqlType) {
            case Types.BOOLEAN:
                color = Color.MAGENTA;
                break;

            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
                color = new Color(128, 128, 0); // olive
                break;

            case Types.REAL:
            case Types.DOUBLE:
                color = Color.GREEN;
                break;

            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                color = new Color(0, 168, 188); // cyan dull
                break;

            case Types.VARCHAR:
                color = new Color(0, 128, 128);
                break;

            default:
                System.out.println("No color for java.sql.Types type: " + sqlType);
        }
        return color;
    }

    private SqlType() {
        throw new IllegalStateException("not meant to me instantiated");
    }
}