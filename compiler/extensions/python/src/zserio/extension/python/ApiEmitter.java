package zserio.extension.python;

import java.util.TreeMap;

import zserio.ast.BitmaskType;
import zserio.ast.ChoiceType;
import zserio.ast.Constant;
import zserio.ast.EnumType;
import zserio.ast.Package;
import zserio.ast.PackageName;
import zserio.ast.PubsubType;
import zserio.ast.Root;
import zserio.ast.ServiceType;
import zserio.ast.SqlDatabaseType;
import zserio.ast.SqlTableType;
import zserio.ast.StructureType;
import zserio.ast.Subtype;
import zserio.ast.UnionType;
import zserio.ast.ZserioType;
import zserio.extension.common.ZserioExtensionException;

public class ApiEmitter extends PythonDefaultEmitter
{
    public ApiEmitter(PythonExtensionParameters pythonParameters)
    {
        super(pythonParameters);
    }

    @Override
    public void endRoot(Root root) throws ZserioExtensionException
    {
        for (ApiEmitterTemplateData packageTemplateData : packageMapping.values())
        {
            final PackageName packageName = packageTemplateData.getPackageName();
            processTemplate(API_TEMPLATE, packageTemplateData, packageName, API_FILENAME_ROOT);
        }
    }

    @Override
    public void beginPackage(Package zserioPackage) throws ZserioExtensionException
    {
        super.beginPackage(zserioPackage);

        final PackageName packageName = zserioPackage.getPackageName();
        if (packageName.isEmpty())
            addEmptyPackageMapping();
        else
            addPackageMapping(packageName);
    }

    @Override
    public void beginChoice(ChoiceType choiceType) throws ZserioExtensionException
    {
        addTypeMapping(choiceType);
    }

    @Override
    public void beginConst(Constant constant) throws ZserioExtensionException
    {
        addSymbolMapping(constant.getName(), constant.getPackage().getPackageName());
    }

    @Override
    public void beginEnumeration(EnumType enumType) throws ZserioExtensionException
    {
        addTypeMapping(enumType);
    }

    @Override
    public void beginBitmask(BitmaskType bitmaskType) throws ZserioExtensionException
    {
        addTypeMapping(bitmaskType);
    }

    @Override
    public void beginSubtype(Subtype subtype) throws ZserioExtensionException
    {
        addTypeMapping(subtype);
    }

    @Override
    public void beginUnion(UnionType unionType) throws ZserioExtensionException
    {
        addTypeMapping(unionType);
    }

    @Override
    public void beginStructure(StructureType structureType) throws ZserioExtensionException
    {
        addTypeMapping(structureType);
    }

    @Override
    public void beginSqlTable(SqlTableType sqlTableType) throws ZserioExtensionException
    {
        if (getWithSqlCode())
            addTypeMapping(sqlTableType);
    }

    @Override
    public void beginSqlDatabase(SqlDatabaseType sqlDatabaseType) throws ZserioExtensionException
    {
        if (getWithSqlCode())
            addTypeMapping(sqlDatabaseType);
    }

    @Override
    public void beginService(ServiceType serviceType) throws ZserioExtensionException
    {
        if (getWithServiceCode())
            addModuleMapping(serviceType);
    }

    @Override
    public void beginPubsub(PubsubType pubsubType) throws ZserioExtensionException
    {
        if (getWithPubsubCode())
            addTypeMapping(pubsubType);
    }

    private void addEmptyPackageMapping() throws ZserioExtensionException
    {
        if (!packageMapping.isEmpty())
            throw new ZserioExtensionException("ApiEmitter: Empty package shall be first!");
        packageMapping.put(PackageName.EMPTY,
                new ApiEmitterTemplateData(getTemplateDataContext(), PackageName.EMPTY));
    }

    private void addPackageMapping(PackageName mappedPackageName)
    {
        ApiEmitterTemplateData prevPackageTemplateData = packageMapping.get(PackageName.EMPTY);
        final PackageName.Builder currentPackageNameBuilder = new PackageName.Builder();
        final TemplateDataContext context = getTemplateDataContext();
        for (String id : mappedPackageName.getIdList())
        {
            currentPackageNameBuilder.addId(id);
            ApiEmitterTemplateData apiEmitterTemplateData = packageMapping.get(currentPackageNameBuilder.get());
            if (apiEmitterTemplateData == null)
            {
                apiEmitterTemplateData = new ApiEmitterTemplateData(context, currentPackageNameBuilder.get());
                packageMapping.put(currentPackageNameBuilder.get(), apiEmitterTemplateData);
            }

            if (prevPackageTemplateData != null)
                prevPackageTemplateData.addSubpackage(id);
            prevPackageTemplateData = apiEmitterTemplateData;
        }
    }

    private void addModuleMapping(ZserioType zserioType) throws ZserioExtensionException
    {
        final PackageName packageName = zserioType.getPackage().getPackageName();
        final ApiEmitterTemplateData packageTemplateData = packageMapping.get(packageName);
        if (packageTemplateData == null)
            throw new ZserioExtensionException("ApiEmitter: Package not yet mapped!");
        packageTemplateData.addModule(zserioType);
    }

    private void addTypeMapping(ZserioType zserioType) throws ZserioExtensionException
    {
        final PackageName packageName = zserioType.getPackage().getPackageName();
        final ApiEmitterTemplateData packageTemplateData = packageMapping.get(packageName);
        if (packageTemplateData == null)
            throw new ZserioExtensionException("ApiEmitter: Package not yet mapped!");
        packageTemplateData.addType(zserioType);
    }

    private void addSymbolMapping(String zserioSymbolName, PackageName packageName) throws ZserioExtensionException
    {
        final ApiEmitterTemplateData packageTemplateData = packageMapping.get(packageName);
        if (packageTemplateData == null)
            throw new ZserioExtensionException("ApiEmitter: Package not yet mapped!");
        packageTemplateData.addSymbol(zserioSymbolName);
    }

    private static final String API_TEMPLATE = "api.py.ftl";
    private static final String API_FILENAME_ROOT = "api";

    private final TreeMap<PackageName, ApiEmitterTemplateData> packageMapping =
            new TreeMap<PackageName, ApiEmitterTemplateData>();
}
