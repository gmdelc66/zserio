package zserio.extension.cpp;

import java.util.Locale;

import zserio.ast.Field;
import zserio.ast.Function;
import zserio.ast.Parameter;

public class AccessorNameFormatter
{
    public static String getGetterName(Field field)
    {
        return getAccessorName(GETTER_NAME_PREFIX, field.getName());
    }

    public static String getGetterName(Parameter param)
    {
        return getAccessorName(GETTER_NAME_PREFIX, param.getName());
    }

    public static String getSetterName(Field field)
    {
        return getAccessorName(SETTER_NAME_PREFIX, field.getName());
    }

    public static String getReaderName(Field field)
    {
        return getAccessorName(READER_NAME_PREFIX, field.getName());
    }

    public static String getSetterName(Parameter param)
    {
        return getAccessorName(SETTER_NAME_PREFIX, param.getName());
    }

    public static String getResetterName(Field field)
    {
        return getAccessorName(RESETTER_NAME_PREFIX, field.getName());
    }

    public static String getIndicatorName(Field field)
    {
        return getAccessorName(INDICATOR_NAME_PREFIX, field.getName());
    }

    public static String getFunctionName(Function function)
    {
        return getAccessorName(FUNCTION_NAME_PREFIX, function.getName());
    }

    private static String getAccessorName(String accessorNamePrefix, String memberName)
    {
        StringBuilder accessorName = new StringBuilder(accessorNamePrefix);
        if (!memberName.isEmpty())
        {
            final String firstMemberNameChar = String.valueOf(memberName.charAt(0));
            final String restMemberNameChars = memberName.substring(1, memberName.length());
            accessorName.append(firstMemberNameChar.toUpperCase(Locale.ENGLISH));
            accessorName.append(restMemberNameChars);
        }

        return accessorName.toString();
    }

    private final static String GETTER_NAME_PREFIX = "get";
    private final static String SETTER_NAME_PREFIX = "set";
    private final static String READER_NAME_PREFIX = "read";
    private final static String RESETTER_NAME_PREFIX = "reset";
    private final static String INDICATOR_NAME_PREFIX = "has";
    private final static String FUNCTION_NAME_PREFIX = "func";
}
