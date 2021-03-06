package zserio.extension.doc;

import zserio.ast.Root;
import zserio.extension.common.ZserioExtensionException;
import zserio.tools.Extension;
import zserio.tools.ExtensionParameters;

/**
 * The documentation extension.
 *
 * It generates HTML documentation together with SVG collaboration diagrams converted from generated dot files.
 */
public class DocExtension implements Extension
{
    @Override
    public String getName()
    {
        return "Doc Generator";
    }

    @Override
    public String getVersion()
    {
        return DocExtensionVersion.VERSION_STRING;
    }

    @Override
    public void registerOptions(org.apache.commons.cli.Options options)
    {
        DocExtensionParameters.registerOptions(options);
    }

    @Override
    public boolean isEnabled(ExtensionParameters parameters)
    {
        return DocExtensionParameters.hasOptionDoc(parameters);
    }

    @Override
    public void process(Root rootNode, ExtensionParameters parameters) throws ZserioExtensionException
    {
        final DocExtensionParameters docParameters = new DocExtensionParameters(parameters);

        // emit external files needed by HTML during runtime
        HtmlRuntimeEmitter.emit(docParameters);

        // emit CSS styles file
        StylesheetEmitter.emit(docParameters);

        // collect used by information
        final UsedByCollector usedByCollector = new UsedByCollector();
        rootNode.walk(usedByCollector);

        // emit DOT files
        SymbolCollaborationDotEmitter.emit(docParameters, usedByCollector);

        // emit HTML index file
        IndexEmitter.emit(docParameters, rootNode.getRootPackage());

        // collect package symbols
        final SymbolCollector symbolCollector = new SymbolCollector();
        rootNode.walk(symbolCollector);

        // collect packages
        final PackageCollector packageCollector = new PackageCollector();
        rootNode.accept(packageCollector);

        // collect used by choice information
        final UsedByChoiceCollector usedByChoiceCollector = new UsedByChoiceCollector();
        rootNode.walk(usedByChoiceCollector);

        // emit HTML files
        final PackageEmitter packageEmitter = new PackageEmitter(docParameters, symbolCollector,
                packageCollector, usedByCollector, usedByChoiceCollector);
        rootNode.walk(packageEmitter);
    }
}
