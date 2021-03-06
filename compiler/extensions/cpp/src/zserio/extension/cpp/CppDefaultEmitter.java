package zserio.extension.cpp;

import java.io.File;
import java.util.Locale;

import zserio.ast.Package;
import zserio.ast.PackageName;
import zserio.ast.ZserioType;
import zserio.extension.common.DefaultTreeWalker;
import zserio.extension.common.FreeMarkerUtil;
import zserio.extension.common.ZserioExtensionException;
import zserio.tools.ExtensionParameters;

abstract class CppDefaultEmitter extends DefaultTreeWalker
{
    public CppDefaultEmitter(String outPathName, ExtensionParameters extensionParameters)
    {
        this.outPathName = outPathName;
        this.extensionParameters = extensionParameters;
        this.context = new TemplateDataContext(extensionParameters);
    }

    @Override
    public boolean traverseTemplateInstantiations()
    {
        return true;
    }

    @Override
    public void beginPackage(Package pkg) throws ZserioExtensionException
    {
        packageSourceFileName = pkg.getLocation().getFileName();
    }

    protected void processSourceTemplate(String templateName, Object templateData, ZserioType zserioType)
            throws ZserioExtensionException
    {
        processTemplate(templateName, templateData, zserioType.getPackage().getPackageName(),
                zserioType.getName(), CPP_SOURCE_EXTENSION, true);
    }

    protected void processHeaderTemplate(String templateName, Object templateData, ZserioType zserioType)
            throws ZserioExtensionException
    {
        processHeaderTemplate(templateName, templateData, zserioType.getPackage(), zserioType.getName());
    }

    protected void processHeaderTemplate(String templateName, Object templateData, Package zserioPackage,
            String outFileName) throws ZserioExtensionException
    {
        processTemplate(templateName, templateData, zserioPackage.getPackageName(), outFileName,
                CPP_HEADER_EXTENSION, false);
    }

    protected TemplateDataContext getTemplateDataContext()
    {
        return context;
    }

    protected boolean getWithPubsubCode()
    {
        return extensionParameters.getWithPubsubCode();
    }

    protected boolean getWithServiceCode()
    {
        return extensionParameters.getWithServiceCode();
    }

    protected boolean getWithSourcesAmalgamation()
    {
        return extensionParameters.getWithSourcesAmalgamation();
    }

    protected boolean getWithSqlCode()
    {
        return extensionParameters.getWithSqlCode();
    }

    private void processTemplate(String templateName, Object templateData, PackageName packageName,
            String outFileNameRoot, String outputExtension, boolean requestAmalgamate)
                    throws ZserioExtensionException
    {
        final File outDir = new File(outPathName, packageName.toFilesystemPath());
        final boolean amalgamate = (getWithSourcesAmalgamation() && requestAmalgamate);
        final String outFileNameWithoutExtension = (amalgamate) ? getAmalgamFileNameRoot() : outFileNameRoot;
        final File outputFile = new File(outDir, outFileNameWithoutExtension + outputExtension);
        FreeMarkerUtil.processTemplate(CPP_TEMPLATE_LOCATION + templateName, templateData, outputFile,
                amalgamate);
    }

    private String getAmalgamFileNameRoot()
    {
        // strip possible directory
        final int lastSlashIndex = packageSourceFileName.lastIndexOf(File.separatorChar);
        final String sourceFileName = (lastSlashIndex == -1) ? packageSourceFileName :
                packageSourceFileName.substring(lastSlashIndex + 1);

        // strip extensions from source file name
        final int firstDotIndex = sourceFileName.indexOf('.');
        final String sourceFileNameRoot = (firstDotIndex == -1) ? sourceFileName :
                sourceFileName.substring(0, firstDotIndex);

        // convert main part of source file name to camel case
        final String[] words = sourceFileNameRoot.split("_");
        final StringBuilder builder = new StringBuilder();
        for (String word : words)
        {
            if (!word.isEmpty())
            {
                builder.append(Character.toString(word.charAt(0)).toUpperCase(Locale.ENGLISH) +
                        word.substring(1));
            }
        }

        final String amalgamFileNameRoot = builder.toString();

        return (amalgamFileNameRoot.isEmpty()) ? CPP_DEFAULT_AMALGAM_FILE_NAME_ROOT : amalgamFileNameRoot;
    }

    private static final String CPP_SOURCE_EXTENSION = ".cpp";
    private static final String CPP_HEADER_EXTENSION = ".h";
    private static final String CPP_TEMPLATE_LOCATION = "cpp/";

    private static final String CPP_DEFAULT_AMALGAM_FILE_NAME_ROOT = "Amalgamation";

    private final String outPathName;
    private final ExtensionParameters extensionParameters;
    private final TemplateDataContext context;

    private String packageSourceFileName = "";
}
