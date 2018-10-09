package zserio.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import zserio.antlr.ZserioTreeWalker;
import zserio.ast.Root;

/**
 * The manager to handle all Zserio extensions.
 */
public class ExtensionManager
{
    /**
     * Constructor from command line arguments.
     *
     * @param commandLineArguments Command line arguments to construct from.
     */
    public ExtensionManager(CommandLineArguments commandLineArguments)
    {
        extensions = new ArrayList<Extension>();
        ServiceLoader<Extension> loader = ServiceLoader.load(Extension.class, getClass().getClassLoader());
        Iterator<Extension> it = loader.iterator();
        while (it.hasNext())
        {
            Extension extension = it.next();
            if (ZserioVersion.VERSION_STRING.equals(extension.getVersion()))
            {
                extensions.add(extension);
                extension.registerOptions(commandLineArguments.getOptions());
            }
            else
            {
                ZserioToolPrinter.printMessage("Ignoring '" + extension.getName() + "' extension " +
                        "because its version '" + extension.getVersion() + "' does not match " +
                        "ZserioTool version '" + ZserioVersion.VERSION_STRING + "'!");
            }
        }
    }

    /**
     * Prints list of all available extensions.
     */
    public void printExtensions()
    {
        if (extensions.isEmpty())
        {
            ZserioToolPrinter.printMessage("No extensions found!");
        }
        else
        {
            ZserioToolPrinter.printMessage("Available extensions:");
            for (Extension extension : extensions)
            {
                ZserioToolPrinter.printMessage("  " + extension.getName());
            }
        }
    }

    /**
     * Calls all available Zserio extensions to generate their output.
     *
     * @param parameters Parameters to pass to extensions.
     * @param walker     The Zserio tree walker to use for emitting.
     * @param rootNode   The root node of AST tree to use for emitting.
     */
    public void callExtensions(ExtensionParameters parameters, ZserioTreeWalker walker, Root rootNode)
    {
        if (extensions.isEmpty())
        {
            ZserioToolPrinter.printMessage("No extensions found, nothing emitted!");
        }
        else
        {
            for (Extension extension : extensions)
            {
                extension.generate(parameters, walker, rootNode);
            }
        }
    }

    private final List<Extension> extensions;
}
