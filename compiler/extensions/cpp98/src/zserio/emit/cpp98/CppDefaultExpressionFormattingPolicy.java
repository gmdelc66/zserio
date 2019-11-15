package zserio.emit.cpp98;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import zserio.ast.AstNode;
import zserio.ast.Constant;
import zserio.ast.Function;
import zserio.ast.ZserioType;
import zserio.ast.EnumItem;
import zserio.ast.EnumType;
import zserio.ast.Expression;
import zserio.ast.Field;
import zserio.ast.Package;
import zserio.ast.Parameter;
import zserio.emit.common.DefaultExpressionFormattingPolicy;
import zserio.emit.common.StringEscapeConverter;
import zserio.emit.common.ZserioEmitException;
import zserio.emit.cpp98.AccessorNameFormatter;
import zserio.emit.cpp98.symbols.CppNativeSymbol;
import zserio.emit.cpp98.types.CppNativeType;

/**
 * Default expressions formatting policy for C++ expressions.
 *
 * TODO Please note that literal formatters are duplicitly implemented by native types as well. The correct
 * solution would be to pass formatted type to Expression Formatter to be able to call literal formatters from
 * native types.
 */
public abstract class CppDefaultExpressionFormattingPolicy extends DefaultExpressionFormattingPolicy
{
    public CppDefaultExpressionFormattingPolicy(CppNativeMapper cppNativeMapper,
            IncludeCollector includeCollector)
    {
        this.cppNativeMapper = cppNativeMapper;
        this.includeCollector = includeCollector;
    }

    @Override
    public String getDecimalLiteral(Expression expr, boolean isNegative)
    {
        // decimal literals in C++ are the same but append "(U)L(L)" for (unsigned) long (long) values
        String decLiteral = expr.getText();
        final BigInteger decInBigInteger = new BigInteger(decLiteral);

        String minIntWorkaround = getMinIntWorkaround(isNegative, decInBigInteger);
        if (minIntWorkaround != null)
            return minIntWorkaround;

        return decLiteral + getIntegerLiteralSuffix(decInBigInteger, isNegative);
    }

    @Override
    public String getBinaryLiteral(Expression expr, boolean isNegative)
    {
        // binary literals are not supported by C++ => use hexadecimal instead of it
        String binaryLiteral = expr.getText();
        final BigInteger binaryInBigInteger = new BigInteger(binaryLiteral, 2);

        String minIntWorkaround = getMinIntWorkaround(isNegative, binaryInBigInteger);
        if (minIntWorkaround != null)
            return minIntWorkaround;

        binaryLiteral = CPP_HEXADECIMAL_LITERAL_PREFIX + binaryInBigInteger.toString(16);

        return binaryLiteral;
    }

    @Override
    public String getHexadecimalLiteral(Expression expr, boolean isNegative)
    {
        // hexadecimal literals in C++ are the same (with prefix "0x") but append "(U)L(L)" for
        // (unsigned) long (long) values
        String hexLiteral = expr.getText();
        final BigInteger hexInBigInteger = new BigInteger(hexLiteral, 16);

        String minIntWorkaround = getMinIntWorkaround(isNegative, hexInBigInteger);
        if (minIntWorkaround != null)
            return minIntWorkaround;

        hexLiteral = CPP_HEXADECIMAL_LITERAL_PREFIX + hexLiteral +
                getIntegerLiteralSuffix(hexInBigInteger, isNegative);

        return hexLiteral;
    }

    @Override
    public String getOctalLiteral(Expression expr, boolean isNegative)
    {
        String octalLiteral = expr.getText();
        final BigInteger octalInBigInteger = new BigInteger(octalLiteral, 8);

        String minIntWorkaround = getMinIntWorkaround(isNegative, octalInBigInteger);
        if (minIntWorkaround != null)
            return minIntWorkaround;

        // octal literals in C++ are the same (with prefix '0')
        return CPP_OCTAL_LITERAL_PREFIX + expr.getText();
    }

    @Override
    public String getFloatLiteral(Expression expr, boolean isNegative)
    {
        // float literals in C++ are the same (with suffix 'f')
        return expr.getText() + CPP_FLOAT_LITERAL_SUFFIX;
    }

    @Override
    public String getDoubleLiteral(Expression expr, boolean isNegative)
    {
        // double literals in C++ are the same (without suffix)
        return expr.getText();
    }

    @Override
    public String getBoolLiteral(Expression expr)
    {
        // bool literals in C++ are the same
        return expr.getText();
    }

    @Override
    public String getStringLiteral(Expression expr)
    {
        // string literals in C++ does not support unicode escapes from interval <'\u0000', '\u0031'>
        return StringEscapeConverter.convertUnicodeToHexEscapes(expr.getText());
    }

    @Override
    public String getIndex(Expression expr)
    {
        return "_index";
    }

    @Override
    public String getIdentifier(Expression expr, boolean isLastInDot, boolean isSetter)
            throws ZserioEmitException
    {
        // first try Zserio types then try identifier symbol objects
        final StringBuilder result = new StringBuilder();
        final String symbol = expr.getText();
        final AstNode resolvedSymbol = expr.getExprSymbolObject();
        if (resolvedSymbol instanceof ZserioType)
        {
            formatTypeIdentifier(result, (ZserioType)resolvedSymbol);
        }
        else
        {
            if (!(resolvedSymbol instanceof Package))
                formatSymbolIdentifier(result, symbol, expr.isMostLeftId(), resolvedSymbol,
                        expr.getExprZserioType(), isSetter);
        }

        // ignore package identifiers, they will be a part of the following Zserio type

        return result.toString();
    }

    @Override
    public UnaryExpressionFormatting getFunctionCall(Expression expr)
    {
        return new UnaryExpressionFormatting(getAccessPrefix(), "()");
    }

    @Override
    public UnaryExpressionFormatting getLengthOf(Expression expr)
    {
        return new UnaryExpressionFormatting("", ".size()");
    }

    @Override
    public UnaryExpressionFormatting getValueOf(Expression expr)
    {
        return new UnaryExpressionFormatting("", "");
    }

    @Override
    public UnaryExpressionFormatting getNumBits(Expression expr)
    {
        includeCollector.addCppSystemIncludes(BUILD_IN_OPERATORS_INCLUDE);

        return new UnaryExpressionFormatting("zserio::getNumBits(", ")");
    }

    @Override
    public BinaryExpressionFormatting getArrayElement(Expression expr, boolean isSetter)
    {
        return new BinaryExpressionFormatting("", ".at(", (isSetter) ? ") = _value" : ")");
    }

    @Override
    public BinaryExpressionFormatting getDot(Expression expr)
    {
        // ignore dots between package identifiers
        if (expr.op1().getExprZserioType() == null)
            return new BinaryExpressionFormatting("");

        // do special handling for enumeration items
        if (expr.op2().getExprSymbolObject() instanceof EnumItem)
            return new BinaryExpressionFormatting("::");

        return new BinaryExpressionFormatting(".");
    }

    protected abstract String getAccessPrefixForCompoundType();

    private void formatTypeIdentifier(StringBuilder result, ZserioType resolvedType)
            throws ZserioEmitException
    {
        final CppNativeType nativeSubtype = cppNativeMapper.getCppType(resolvedType);
        result.append(nativeSubtype.getFullName());
        includeCollector.addCppIncludesForType(nativeSubtype);
    }

    private void formatSymbolIdentifier(StringBuilder result, String symbol, boolean isMostLeftId,
            AstNode resolvedSymbol, ZserioType exprType, boolean isSetter) throws ZserioEmitException
    {
        if (resolvedSymbol instanceof Parameter)
        {
            // [Parameter]
            final Parameter param = (Parameter)resolvedSymbol;
            formatParameterAccessor(result, isMostLeftId, param);
        }
        else if (resolvedSymbol instanceof Field)
        {
            // [Field]
            final Field field = (Field)resolvedSymbol;
            formatFieldAccessor(result, isMostLeftId, field, isSetter);
        }
        else if (resolvedSymbol instanceof EnumItem)
        {
            // EnumType.[ENUM_ITEM]
            final EnumItem enumItem = (EnumItem)resolvedSymbol;
            formatEnumItem(result, isMostLeftId, enumItem, exprType);
        }
        else if (resolvedSymbol instanceof Function)
        {
            // [FunctionCall]()
            final Function function = (Function)resolvedSymbol;
            result.append(AccessorNameFormatter.getFunctionName(function));
        }
        else if (resolvedSymbol instanceof Constant)
        {
            // [Constant]
            final Constant constant = (Constant)resolvedSymbol;
            formatConstant(result, constant);
        }
        else
        {
            // this could happen for "explicit identifier" expressions
            result.append(symbol);
        }
    }

    private String getIntegerLiteralSuffix(BigInteger literalValue, boolean isNegative)
    {
        String literalSuffix = "";
        final long maxAbsIntValue = (isNegative) ? -(long)Integer.MIN_VALUE : 0xFFFFFFFFL;
        if (literalValue.compareTo(BigInteger.valueOf(maxAbsIntValue)) > 0)
        {
            // long long value
            literalSuffix = (isNegative) ? CPP_SIGNED_LONG_LONG_LITERAL_SUFFIX :
                CPP_UNSIGNED_LONG_LONG_LITERAL_SUFFIX;
        }
        else
        {
            final long maxAbsShortValue = (isNegative) ? -(long)Short.MIN_VALUE : 0xFFFFL;
            if (literalValue.compareTo(BigInteger.valueOf(maxAbsShortValue)) > 0)
            {
                // long value
                literalSuffix = (isNegative) ? CPP_SIGNED_LONG_LITERAL_SUFFIX :
                    CPP_UNSIGNED_LONG_LITERAL_SUFFIX;
            }
        }

        return literalSuffix;
    }

    private String getAccessPrefix()
    {
        final String accessPrefix = getAccessPrefixForCompoundType();

        return (accessPrefix.isEmpty()) ? accessPrefix : accessPrefix + ".";
    }

    private void formatParameterAccessor(StringBuilder result, boolean isFirstInDot, Parameter param)
    {
        if (isFirstInDot)
            result.append(getAccessPrefix());

        result.append(AccessorNameFormatter.getGetterName(param));
        result.append(CPP_GETTER_FUNCTION_CALL);
    }

    private void formatFieldAccessor(StringBuilder result, boolean isFirstInDot, Field field, boolean isSetter)
    {
        if (isFirstInDot)
            result.append(getAccessPrefix());

        if (isSetter)
        {
            result.append(AccessorNameFormatter.getSetterName(field));
            result.append(CPP_SETTER_FUNCTION_CALL);
        }
        else
        {
            result.append(AccessorNameFormatter.getGetterName(field));
            result.append(CPP_GETTER_FUNCTION_CALL);
        }
    }

    private void formatEnumItem(StringBuilder result, boolean isMostLeftId, EnumItem enumItem,
            ZserioType exprType) throws ZserioEmitException
    {
        // emit whole name if this is the first symbol in dot subtree, otherwise emit only enum short name
        if (isMostLeftId && exprType instanceof EnumType)
        {
            final EnumType enumType = (EnumType)exprType;
            final CppNativeType nativeEnumType = cppNativeMapper.getCppType(enumType);
            result.append(nativeEnumType.getFullName());
            result.append("::");
        }

        result.append(enumItem.getName());
    }

    private void formatConstant(StringBuilder result, Constant constant) throws ZserioEmitException
    {
        final CppNativeSymbol nativeSymbol = cppNativeMapper.getCppSymbol(constant);
        result.append(nativeSymbol.getFullName());
        includeCollector.addCppUserIncludes(Collections.singleton(nativeSymbol.getIncludeFile()));
    }

    private String getMinIntWorkaround(boolean isNegative, BigInteger literalValue)
    {
        String workaround = null;
        if (isNegative)
        {
            if (literalValue.equals(ABS_INT32_MIN))
                workaround = CPP_INT32_MIN_WORKAROUND;
            else if (literalValue.equals(ABS_INT64_MIN))
                workaround = CPP_INT64_MIN_WORKAROUND;
        }

        return workaround;
    }

    private final CppNativeMapper cppNativeMapper;
    private final IncludeCollector includeCollector;

    private final static String CPP_GETTER_FUNCTION_CALL = "()";
    private final static String CPP_SETTER_FUNCTION_CALL = "(_value)";

    private final static List<String> BUILD_IN_OPERATORS_INCLUDE = Arrays.asList("zserio/BuildInOperators.h");

    private final static String CPP_SIGNED_LONG_LITERAL_SUFFIX = "L";
    private final static String CPP_UNSIGNED_LONG_LITERAL_SUFFIX = "UL";
    private final static String CPP_SIGNED_LONG_LONG_LITERAL_SUFFIX = "LL";
    private final static String CPP_UNSIGNED_LONG_LONG_LITERAL_SUFFIX = "ULL";
    private final static String CPP_HEXADECIMAL_LITERAL_PREFIX = "0x";
    private final static String CPP_OCTAL_LITERAL_PREFIX = "0";
    private final static String CPP_FLOAT_LITERAL_SUFFIX = "f";

    private final static BigInteger ABS_INT32_MIN = BigInteger.ONE.shiftLeft(31);
    private final static BigInteger ABS_INT64_MIN = BigInteger.ONE.shiftLeft(63);
    private final static String CPP_INT32_MIN_WORKAROUND = "2147483647L - 1";
    private final static String CPP_INT64_MIN_WORKAROUND = "9223372036854775807LL - 1";
}
