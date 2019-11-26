package zserio.emit.cpp98;

import zserio.ast.ArrayInstantiation;
import zserio.ast.AstNode;
import zserio.ast.Constant;
import zserio.ast.DynamicBitFieldInstantiation;
import zserio.ast.DynamicBitFieldType;
import zserio.ast.ExternType;
import zserio.ast.FixedBitFieldType;
import zserio.ast.InstantiateType;
import zserio.ast.PackageName;
import zserio.ast.TypeInstantiation;
import zserio.ast.UnionType;
import zserio.ast.BooleanType;
import zserio.ast.ChoiceType;
import zserio.ast.CompoundType;
import zserio.ast.ZserioAstDefaultVisitor;
import zserio.ast.ZserioType;
import zserio.ast.EnumType;
import zserio.ast.FloatType;
import zserio.ast.ServiceType;
import zserio.ast.StructureType;
import zserio.ast.SqlDatabaseType;
import zserio.ast.SqlTableType;
import zserio.ast.StdIntegerType;
import zserio.ast.StringType;
import zserio.ast.Subtype;
import zserio.ast.TypeReference;
import zserio.ast.VarIntegerType;
import zserio.emit.common.PackageMapper;
import zserio.emit.common.ZserioEmitException;
import zserio.emit.cpp98.symbols.CppNativeSymbol;
import zserio.emit.cpp98.types.CppNativeType;
import zserio.emit.cpp98.types.NativeArrayType;
import zserio.emit.cpp98.types.NativeBooleanType;
import zserio.emit.cpp98.types.NativeCompoundType;
import zserio.emit.cpp98.types.NativeDoubleType;
import zserio.emit.cpp98.types.NativeEnumType;
import zserio.emit.cpp98.types.NativeFloatType;
import zserio.emit.cpp98.types.NativeHeapOptionalHolderType;
import zserio.emit.cpp98.types.NativeInPlaceOptionalHolderType;
import zserio.emit.cpp98.types.NativeInstantiateType;
import zserio.emit.cpp98.types.NativeIntegralArrayType;
import zserio.emit.cpp98.types.NativeIntegralType;
import zserio.emit.cpp98.types.NativeObjectArrayType;
import zserio.emit.cpp98.types.NativeOptimizedOptionalHolderType;
import zserio.emit.cpp98.types.NativeOptionalHolderType;
import zserio.emit.cpp98.types.NativeServiceType;
import zserio.emit.cpp98.types.NativeStdIntType;
import zserio.emit.cpp98.types.NativeStringType;
import zserio.emit.cpp98.types.NativeSubType;
import zserio.tools.ZserioToolPrinter;

public class CppNativeMapper
{
    /**
     * Constructor from package mapper.
     *
     * @param cppPackageMapper Package mapper to construct from.
     */
    public CppNativeMapper(PackageMapper cppPackageMapper)
    {
        this.cppPackageMapper = cppPackageMapper;
    }

    /**
     * Returns a C++ symbol that can hold an instance of Zserio symbol.
     *
     * @param symbol Zserio symbol.
     *
     * @return C++ symbol.
     *
     * @throws ZserioEmitException If the Zserio symbol cannot be mapped to any C++ symbol.
     */
    public CppNativeSymbol getCppSymbol(AstNode symbol) throws ZserioEmitException
    {
        if (symbol instanceof Constant)
        {
            final Constant constant = (Constant)symbol;
            final PackageName packageName = cppPackageMapper.getPackageName(constant.getPackage());
            final String name = constant.getName();
            final String includeFileName = getIncludePath(packageName, name);
            return new CppNativeSymbol(packageName, name, includeFileName);
        }
        else
        {
            throw new ZserioEmitException("Unhandled symbol '" + symbol.getClass().getName() +
                    "' in CppNativeMapper!");
        }
    }

    /**
     * Returns a C++ type that can hold an instance of referenced Zserio type.
     *
     * @param typeInstantiation Instantiation of Zserio type.
     *
     * @return C++ type which can hold the Zserio type.
     *
     * @throws ZserioEmitException If the Zserio type cannot be mapped to any C++ type.
     */
    public CppNativeType getCppType(TypeInstantiation typeInstantiation) throws ZserioEmitException
    {
        if (typeInstantiation instanceof ArrayInstantiation)
            return mapArray((ArrayInstantiation)typeInstantiation);
        else if (typeInstantiation instanceof DynamicBitFieldInstantiation)
            return mapDynamicBitField((DynamicBitFieldInstantiation)typeInstantiation);

        // don't resolve subtypes so that the subtype name (C++ typedef) will be used
        return getCppType(typeInstantiation.getType());
    }

    /**
     * Returns a C++ type that can hold an instance of referenced Zserio type.
     *
     * @param typeReference Reference to the Zserio type.
     *
     * @return  C++ type which can hold the Zserio type.
     *
     * @throws ZserioEmitException If the Zserio type cannot be mapped to any C++ type.
     */
    public CppNativeType getCppType(TypeReference typeReference) throws ZserioEmitException
    {
         // don't resolve subtypes so that the subtype name (C++ typedef) will be used
        return getCppType(typeReference.getType());
    }

    /**
     * Returns a C++ type that can hold an instance of given Zserio type.
     *
     * @param type Zserio type for mapping to C++ type.
     *
     * @return C++ type which can hold a Zserio type.
     *
     * @throws ZserioEmitException If the Zserio type cannot be mapped to any C++ type.
     */
    public CppNativeType getCppType(ZserioType type) throws ZserioEmitException
    {
        final ZserioTypeMapperVisitor visitor = new ZserioTypeMapperVisitor();
        type.accept(visitor);

        final ZserioEmitException thrownException = visitor.getThrownException();
        if (thrownException != null)
            throw thrownException;

        final CppNativeType nativeType = visitor.getCppType();
        if (nativeType == null)
        {
            throw new ZserioEmitException("Unhandled type '" + type.getClass().getName() +
                    "' in CppNativeMapper!");
        }

        return nativeType;
    }

    /**
     * Returns a C++ type that can hold an instance of given optional Zserio type or Zserio compound type.
     *
     * @param typeInstantiation     Instantiation of Zserio type for mapping to C++ type.
     * @param isOptionalField       true if the given Zserio type is optional.
     * @param useHeapOptionalHolder true to force mapping to the heap optional holder.
     *
     * @return C++ optional holder type which can hold a given Zserio type.
     *
     * @throws ZserioEmitException If the Zserio type cannot be mapped to C++ optional holder type.
     */
    public NativeOptionalHolderType getCppOptionalHolderType(TypeInstantiation typeInstantiation,
            boolean isOptionalField, boolean useHeapOptionalHolder) throws ZserioEmitException
    {
        final CppNativeType rawType = getCppType(typeInstantiation);

        NativeOptionalHolderType nativeOptionalType;
        if (!isOptionalField || rawType.isSimpleType())
            nativeOptionalType = new NativeInPlaceOptionalHolderType(ZSERIO_RUNTIME_PACKAGE_NAME,
                    ZSERIO_RUNTIME_INCLUDE_PREFIX, rawType);
        else if (useHeapOptionalHolder)
            nativeOptionalType = new NativeHeapOptionalHolderType(ZSERIO_RUNTIME_PACKAGE_NAME,
                    ZSERIO_RUNTIME_INCLUDE_PREFIX, rawType);
        else
            nativeOptionalType = new NativeOptimizedOptionalHolderType(ZSERIO_RUNTIME_PACKAGE_NAME,
                    ZSERIO_RUNTIME_INCLUDE_PREFIX, rawType);

        return nativeOptionalType;
    }

    /**
     * Returns a C++ integer type that can hold an instance of given Zserio integer type.
     *
     * @param typeInstantiation Instantiation of zserio integer type for mapping to C++ integer type.
     *
     * @return C++ integer type which can hold a Zserio integer type.
     *
     * @throws ZserioEmitException If the Zserio integer type cannot be mapped to any C++ integer type.
     */
    public NativeIntegralType getCppIntegralType(TypeInstantiation typeInstantiation) throws ZserioEmitException
    {
        CppNativeType nativeType = null;
        if (typeInstantiation instanceof DynamicBitFieldInstantiation)
        {
            nativeType = mapDynamicBitField((DynamicBitFieldInstantiation)typeInstantiation);
        }
        else
        {
            // use the base type to get the integral type (i.e. to resolve subtype)
            nativeType = getCppType(typeInstantiation.getBaseType());
        }

        if (!(nativeType instanceof NativeIntegralType))
            throw new ZserioEmitException("Unhandled integral type '" +
                    typeInstantiation.getBaseType().getClass().getName() + "' in CppNativeMapper!");

        return (NativeIntegralType)nativeType;
    }

    /**
     * Returns a C++ subtype that can hold an instance of given Zserio subtype.
     *
     * @param type Zserio subtype for mapping to C++ subtype.
     *
     * @return C++ subtype which can hold a Zserio subtype.
     *
     * @throws ZserioEmitException If the Zserio subtype cannot be mapped to any C++ subtype.
     */
    public NativeSubType getCppSubType(Subtype type) throws ZserioEmitException
    {
        final CppNativeType nativeType = getCppType(type);

        if (!(nativeType instanceof NativeSubType))
            throw new ZserioEmitException("Unhandled subtype '" + type.getClass().getName() +
                    "' in CppNativeMapper!");

        return (NativeSubType)nativeType;
    }

    private CppNativeType mapArray(ArrayInstantiation instantiation) throws ZserioEmitException
    {
        final TypeInstantiation elementInstantiation = instantiation.getElementTypeInstantiation();
        if (elementInstantiation instanceof DynamicBitFieldInstantiation)
            return mapDynamicBitFieldArray((DynamicBitFieldInstantiation)elementInstantiation);

        final ArrayElementTypeMapperVisitor arrayVisitor =
                new ArrayElementTypeMapperVisitor(elementInstantiation);

        /* Call visitor on the resolved type.
         *
         * This is required so that for subtypes of the simple types the correct array class is used,
         * e.g.:
         *
         * subtype uint8 MyID;
         * Compound
         * {
         *     MyID ids[10];
         * };
         *
         * must use UnsignedByteArray for the field ids whereas for the code:
         *
         * Foo
         * {
         *     uint8 blah;
         * };
         * subtype Foo MyID;
         * Compound
         * {
         *     MyID ids[10];
         * }
         *
         * the field ids should be backed by ObjectArray<MyID> (not ObjectArray<Foo>).
         */
        elementInstantiation.getBaseType().accept(arrayVisitor);

        final ZserioEmitException thrownException = arrayVisitor.getThrownException();
        if (thrownException != null)
            throw thrownException;

        final CppNativeType nativeType = arrayVisitor.getCppType();
        if (nativeType == null)
        {
            throw new ZserioEmitException("Unhandled type '" +
                    elementInstantiation.getBaseType().getClass().getName() + "' in CppNativeMapper!");
        }

        return nativeType;
    }

    private static CppNativeType mapDynamicBitField(DynamicBitFieldInstantiation instantiation)
            throws ZserioEmitException
    {
        final boolean isSigned = instantiation.getBaseType().isSigned();
        final int numBits = instantiation.getMaxBitSize();
        return isSigned ? mapSignedIntegralType(numBits) : mapUnsignedIntegralType(numBits);
    }

    private static CppNativeType mapDynamicBitFieldArray(DynamicBitFieldInstantiation instantiation)
    {
        final boolean isSigned = instantiation.getBaseType().isSigned();
        final int numBits = instantiation.getMaxBitSize();
        return isSigned ? mapSignedIntegralArray(numBits) : mapUnsignedIntegralArray(numBits);
    }

    private static CppNativeType mapSignedIntegralType(int numBits)
    {
        if (numBits <= 8)
            return int8Type;
        else if (numBits <= 16)
            return int16Type;
        else if (numBits <= 32)
            return int32Type;
        else
            return int64Type;
    }

    private static CppNativeType mapUnsignedIntegralType(int numBits)
    {
        if (numBits <= 8)
            return uint8Type;
        else if (numBits <= 16)
            return uint16Type;
        else if (numBits <= 32)
            return uint32Type;
        else
            return uint64Type;
    }

    private static CppNativeType mapSignedIntegralArray(int numBits)
    {
        if (numBits <= 8)
            return int8ArrayType;
        else if (numBits <= 16)
            return int16ArrayType;
        else if (numBits <= 32)
            return int32ArrayType;
        else // this could be > 64 (int8 foo; bit<foo> a;) but values above 64 explode at runtime
            return int64ArrayType;
    }

    private static CppNativeType mapUnsignedIntegralArray(int numBits)
    {
        if (numBits <= 8)
            return uint8ArrayType;
        else if (numBits <= 16)
            return uint16ArrayType;
        else if (numBits <= 32)
            return uint32ArrayType;
        else // this could be > 64 (int8 foo; bit<foo> a;) but values above 64 explode at runtime
            return uint64ArrayType;
    }

    private String getIncludePathRoot(PackageName packageName)
    {
        if (packageName.isEmpty())
            return "";

        return packageName.toString(INCLUDE_DIR_SEPARATOR) + INCLUDE_DIR_SEPARATOR;
    }

    private String getIncludePath(PackageName packageName, String name)
    {
        return getIncludePathRoot(packageName) + name + HEADER_SUFFIX;
    }

    private static abstract class TypeMapperVisitor extends ZserioAstDefaultVisitor
    {
        public ZserioEmitException getThrownException()
        {
            return thrownException;
        }

        @Override
        public void visitExternType(ExternType type)
        {
            ZserioToolPrinter.printError(type.getLocation(),
                    "'extern' type is not supported by the legacy C++98 emitter!");
            setThrownException(new ZserioEmitException("Unknown 'extern' type!"));
        }

        @Override
        public void visitFixedBitFieldType(FixedBitFieldType type)
        {
            mapIntegralType(type.getBitSize(), type.isSigned(), false);
        }

        @Override
        public void visitDynamicBitFieldType(DynamicBitFieldType type)
        {
            mapIntegralType(type.getMaxBitSize(), type.isSigned(), false);
        }

        @Override
        public void visitStdIntegerType(StdIntegerType type)
        {
            mapIntegralType(type.getBitSize(), type.isSigned(), false);
        }

        @Override
        public void visitVarIntegerType(VarIntegerType type)
        {
            mapIntegralType(type.getMaxBitSize(), type.isSigned(), true);
        }

        protected void mapIntegralType(int nBits, boolean signed, boolean variable)
        {
            if (signed)
                mapSignedIntegralType(nBits, variable);
            else
                mapUnsignedIntegralType(nBits, variable);
        }

        protected void setThrownException(ZserioEmitException exception)
        {
            thrownException = exception;
        }

        protected abstract void mapSignedIntegralType(int nBits, boolean variable);
        protected abstract void mapUnsignedIntegralType(int nBits, boolean variable);

        private ZserioEmitException thrownException = null;
    }

    private class ArrayElementTypeMapperVisitor extends TypeMapperVisitor
    {
        public ArrayElementTypeMapperVisitor(TypeInstantiation originalInstantiation)
        {
            // resolve instantiations, but don't resolve subtype
            this.originalInstantiation = originalInstantiation;
        }

        public CppNativeType getCppType()
        {
            return cppType;
        }

        @Override
        public void visitBooleanType(BooleanType type)
        {
            cppType = booleanArrayType;
        }

        @Override
        public void visitChoiceType(ChoiceType type)
        {
            mapObjectArray();
        }

        @Override
        public void visitEnumType(EnumType type)
        {
            mapObjectArray();
        }

        @Override
        public void visitFloatType(FloatType type)
        {
            switch (type.getBitSize())
            {
            case 16:
                cppType = float16ArrayType;
                break;

            case 32:
                cppType = float32ArrayType;
                break;

            case 64:
                cppType = float64ArrayType;
                break;

            default:
                break;
            }
        }

        @Override
        public void visitStringType(StringType type)
        {
            cppType = stdStringArrayType;
        }

        @Override
        public void visitStructureType(StructureType type)
        {
            mapObjectArray();
        }

        @Override
        public void visitUnionType(UnionType type)
        {
            mapObjectArray();
        }

        @Override
        protected void mapSignedIntegralType(int nBits, boolean variable)
        {
            if (variable)
            {
                switch (nBits)
                {
                case 16:
                    cppType = varInt16ArrayType;
                    break;

                case 32:
                    cppType = varInt32ArrayType;
                    break;

                case 64:
                    cppType = varInt64ArrayType;
                    break;

                case 72:
                    cppType = varIntArrayType;
                    break;

                default:
                    break;
                }
            }
            else
            {
                cppType = CppNativeMapper.mapSignedIntegralArray(nBits);
            }
        }

        @Override
        protected void mapUnsignedIntegralType(int nBits, boolean variable)
        {
            if (variable)
            {
                switch (nBits)
                {
                case 16:
                    cppType = varUInt16ArrayType;
                    break;

                case 32:
                    cppType = varUInt32ArrayType;
                    break;

                case 64:
                    cppType = varUInt64ArrayType;
                    break;

                case 72:
                    cppType = varUIntArrayType;
                    break;

                default:
                    break;
                }
            }
            else
            {
                cppType = mapUnsignedIntegralArray(nBits);
            }
        }

        private void mapObjectArray()
        {
            // use the original type so that subtype is kept
            try
            {
                cppType = new NativeObjectArrayType(ZSERIO_RUNTIME_PACKAGE_NAME,
                        ZSERIO_RUNTIME_INCLUDE_PREFIX, CppNativeMapper.this.getCppType(originalInstantiation));
            }
            catch (ZserioEmitException exception)
            {
                setThrownException(exception);
            }
        }

        private final TypeInstantiation originalInstantiation;

        private CppNativeType cppType = null;
    }

    private class ZserioTypeMapperVisitor extends TypeMapperVisitor
    {
        public CppNativeType getCppType()
        {
            return cppType;
        }

        @Override
        public void visitBooleanType(BooleanType type)
        {
            cppType = booleanType;
        }

        @Override
        public void visitChoiceType(ChoiceType type)
        {
            mapCompoundType(type);
        }

        @Override
        public void visitEnumType(EnumType type)
        {
            try
            {
                final NativeIntegralType nativeBaseType = getCppIntegralType(type.getTypeInstantiation());
                final PackageName packageName = cppPackageMapper.getPackageName(type);
                final String name = type.getName();
                final String includeFileName = getIncludePath(packageName, name);
                cppType = new NativeEnumType(packageName, name, includeFileName, nativeBaseType);
            }
            catch (ZserioEmitException exception)
            {
                setThrownException(exception);
            }
        }

        @Override
        public void visitFloatType(FloatType type)
        {
            switch (type.getBitSize())
            {
            case 16:
            case 32:
                cppType = floatType;
                break;

            case 64:
                cppType = doubleType;
                break;

            default:
                break;
            }
        }

        @Override
        public void visitServiceType(ServiceType type)
        {
            final PackageName packageName = cppPackageMapper.getPackageName(type);
            final String name = type.getName();
            final String includeFileName = getIncludePath(packageName, name);
            cppType = new NativeServiceType(packageName, name, includeFileName);
        }

        @Override
        public void visitSqlDatabaseType(SqlDatabaseType type)
        {
            mapCompoundType(type);
        }

        @Override
        public void visitSqlTableType(SqlTableType type)
        {
            mapCompoundType(type);
        }

        @Override
        public void visitStringType(StringType type)
        {
            cppType = stringType;
        }

        @Override
        public void visitStructureType(StructureType type)
        {
            mapCompoundType(type);
        }

        @Override
        public void visitSubtype(Subtype type)
        {
            try
            {
                final CppNativeType nativeTargetType =
                        CppNativeMapper.this.getCppType(type.getTypeReference());
                final PackageName packageName = cppPackageMapper.getPackageName(type);
                final String name = type.getName();
                final String includeFileName = getIncludePath(packageName, name);
                cppType = new NativeSubType(packageName, name, includeFileName, nativeTargetType);
            }
            catch (ZserioEmitException exception)
            {
                setThrownException(exception);
            }
        }

        @Override
        public void visitUnionType(UnionType type)
        {
            mapCompoundType(type);
        }

        @Override
        public void visitInstantiateType(InstantiateType type)
        {
            final PackageName packageName = cppPackageMapper.getPackageName(type);
            final String name = type.getName(); // note that name is same as the referenced type name
            final String includeFileName = getIncludePath(packageName, name);
            cppType = new NativeInstantiateType(packageName, name, includeFileName);
        }

        @Override
        protected void mapSignedIntegralType(int nBits, boolean variable)
        {
            if (variable)
            {
                switch (nBits)
                {
                case 16:
                    cppType = int16Type;
                    break;

                case 32:
                    cppType = int32Type;
                    break;

                case 64:
                case 72:
                    cppType = int64Type;
                    break;

                default:
                    // shall not occur!
                    break;
                }
            }
            else
            {
                cppType = CppNativeMapper.mapSignedIntegralType(nBits);
            }
        }

        @Override
        protected void mapUnsignedIntegralType(int nBits, boolean variable)
        {
            if (variable)
            {
                switch (nBits)
                {
                case 16:
                    cppType = uint16Type;
                    break;

                case 32:
                    cppType = uint32Type;
                    break;

                case 64:
                case 72:
                    cppType = uint64Type;
                    break;

                default:
                    // shall not occur!
                    break;
                }
            }
            else
            {
                cppType = CppNativeMapper.mapUnsignedIntegralType(nBits);
            }
        }

        private void mapCompoundType(CompoundType type)
        {
            final PackageName packageName = cppPackageMapper.getPackageName(type);
            final String name = type.getName();
            final String includeFileName = getIncludePath(packageName, name);
            cppType = new NativeCompoundType(packageName, name, includeFileName);
        }

        private CppNativeType cppType = null;
    }

    private final PackageMapper cppPackageMapper;

    private final static String INCLUDE_DIR_SEPARATOR = "/";
    private final static String HEADER_SUFFIX = ".h";

    private final static PackageName ZSERIO_RUNTIME_PACKAGE_NAME =
            new PackageName.Builder().addId("zserio").get();
    private final static String ZSERIO_RUNTIME_INCLUDE_PREFIX = "zserio" + INCLUDE_DIR_SEPARATOR;
    private final static String BIT_FIELD_ARRAY_H = ZSERIO_RUNTIME_INCLUDE_PREFIX + "BitFieldArray" +
            HEADER_SUFFIX;
    private final static String BASIC_ARRAY_H = ZSERIO_RUNTIME_INCLUDE_PREFIX + "BasicArray" + HEADER_SUFFIX;

    private final static NativeBooleanType booleanType = new NativeBooleanType();
    private final static NativeStringType stringType = new NativeStringType();

    private final static NativeFloatType floatType = new NativeFloatType();
    private final static NativeDoubleType doubleType = new NativeDoubleType();

    private final static NativeStdIntType uint8Type = new NativeStdIntType(8, false);
    private final static NativeStdIntType uint16Type = new NativeStdIntType(16, false);
    private final static NativeStdIntType uint32Type = new NativeStdIntType(32, false);
    private final static NativeStdIntType uint64Type = new NativeStdIntType(64, false);

    private final static NativeStdIntType int8Type = new NativeStdIntType(8, true);
    private final static NativeStdIntType int16Type = new NativeStdIntType(16, true);
    private final static NativeStdIntType int32Type = new NativeStdIntType(32, true);
    private final static NativeStdIntType int64Type = new NativeStdIntType(64, true);

    private final static NativeArrayType booleanArrayType =
        new NativeArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "BoolArray", BASIC_ARRAY_H, booleanType);
    private final static NativeArrayType stdStringArrayType =
        new NativeArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "StringArray", BASIC_ARRAY_H, stringType);

    private final static NativeArrayType float16ArrayType =
        new NativeArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "Float16Array", BASIC_ARRAY_H, floatType);
    private final static NativeArrayType float32ArrayType =
            new NativeArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "Float32Array", BASIC_ARRAY_H, floatType);
    private final static NativeArrayType float64ArrayType =
            new NativeArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "Float64Array", BASIC_ARRAY_H, doubleType);

    private final static NativeArrayType int8ArrayType =
        new NativeIntegralArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "Int8Array", BIT_FIELD_ARRAY_H, int8Type);
    private final static NativeArrayType int16ArrayType =
        new NativeIntegralArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "Int16Array", BIT_FIELD_ARRAY_H, int16Type);
    private final static NativeArrayType int32ArrayType =
        new NativeIntegralArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "Int32Array", BIT_FIELD_ARRAY_H, int32Type);
    private final static NativeArrayType int64ArrayType =
        new NativeIntegralArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "Int64Array", BIT_FIELD_ARRAY_H, int64Type);

    private final static NativeArrayType uint8ArrayType =
        new NativeIntegralArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "UInt8Array", BIT_FIELD_ARRAY_H, uint8Type);
    private final static NativeArrayType uint16ArrayType =
        new NativeIntegralArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "UInt16Array", BIT_FIELD_ARRAY_H, uint16Type);
    private final static NativeArrayType uint32ArrayType =
        new NativeIntegralArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "UInt32Array", BIT_FIELD_ARRAY_H, uint32Type);
    private final static NativeArrayType uint64ArrayType =
        new NativeIntegralArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "UInt64Array", BIT_FIELD_ARRAY_H, uint64Type);

    private final static NativeArrayType varInt16ArrayType =
        new NativeArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "VarInt16Array", BASIC_ARRAY_H, int16Type);
    private final static NativeArrayType varUInt16ArrayType =
        new NativeArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "VarUInt16Array", BASIC_ARRAY_H, uint16Type);

    private final static NativeArrayType varInt32ArrayType =
        new NativeArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "VarInt32Array", BASIC_ARRAY_H, int32Type);
    private final static NativeArrayType varUInt32ArrayType =
        new NativeArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "VarUInt32Array", BASIC_ARRAY_H, uint32Type);

    private final static NativeArrayType varInt64ArrayType =
        new NativeArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "VarInt64Array", BASIC_ARRAY_H, int64Type);
    private final static NativeArrayType varUInt64ArrayType =
        new NativeArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "VarUInt64Array", BASIC_ARRAY_H, uint64Type);

    private final static NativeArrayType varIntArrayType =
        new NativeArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "VarIntArray", BASIC_ARRAY_H, int64Type);
    private final static NativeArrayType varUIntArrayType =
        new NativeArrayType(ZSERIO_RUNTIME_PACKAGE_NAME, "VarUIntArray", BASIC_ARRAY_H, uint64Type);
}
