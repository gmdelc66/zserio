package zserio.extension.cpp;

import zserio.ast.SqlDatabaseType;
import zserio.extension.common.ZserioExtensionException;
import zserio.tools.ExtensionParameters;

public class SqlDatabaseEmitter extends CppDefaultEmitter
{
    public SqlDatabaseEmitter(String outPathName, ExtensionParameters extensionParameters)
    {
        super(outPathName, extensionParameters);
    }

    @Override
    public void beginSqlDatabase(SqlDatabaseType sqlDatabaseType) throws ZserioExtensionException
    {
        if (getWithSqlCode())
        {
            final Object templateData = new SqlDatabaseEmitterTemplateData(getTemplateDataContext(),
                    sqlDatabaseType);

            processHeaderTemplate(TEMPLATE_HEADER_NAME, templateData, sqlDatabaseType);
            processSourceTemplate(TEMPLATE_SOURCE_NAME, templateData, sqlDatabaseType);
        }
    }

    private static final String TEMPLATE_SOURCE_NAME = "SqlDatabase.cpp.ftl";
    private static final String TEMPLATE_HEADER_NAME = "SqlDatabase.h.ftl";
}
