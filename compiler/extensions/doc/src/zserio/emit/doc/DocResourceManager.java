package zserio.emit.doc;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import zserio.ast.AstLocation;
import zserio.ast.Package;
import zserio.ast.PackageName;
import zserio.tools.HashUtil;
import zserio.tools.Parameters;

class DocResourceManager
{
    public DocResourceManager(String outputRoot, Parameters extensionParameters, String htmlContentDirectory,
            Package rootPackage)
    {
        final String pathName = extensionParameters.getPathName();
        this.sourceRoot = Paths.get(pathName != null ? pathName : "").toAbsolutePath();
        // strip out directories which can be in source file name
        final String sourceMainFileName = new File(extensionParameters.getFileName()).getName();
        this.sourceMainBase = getFileNameBase(sourceMainFileName);
        this.sourceMainExtension = getFileNameExtension(sourceMainFileName);
        this.outputRoot = Paths.get(outputRoot != null ? outputRoot : "").toAbsolutePath();
        this.topLevelPackageNameIds = extensionParameters.getTopLevelPackageNameIds();
        this.htmlContentDirectory = htmlContentDirectory;
        this.rootPackage = rootPackage;

        this.currentSourceDir = this.sourceRoot;
        this.currentOutputDir = this.outputRoot;
    }

    void setCurrentSourceDir(Path currentSourceDir)
    {
        this.currentSourceDir = currentSourceDir != null ? currentSourceDir.toAbsolutePath() : sourceRoot;
    }

    Path getCurrentSourceDir()
    {
        return currentSourceDir;
    }

    void setCurrentOutputDir(Path currentOutputDir)
    {
        this.currentOutputDir = currentOutputDir != null ? currentOutputDir.toAbsolutePath() : outputRoot;
    }

    String addResource(String destination) throws ResourceException
    {
        if (isLocalResource(destination))
        {
            final LocalResource localResource = new LocalResource(currentSourceDir, destination);
            final LocalResource mappedResource = mapLocalResource(localResource);

            return currentOutputDir.relativize(mappedResource.getFullPath()).toString() +
                    localResource.getAnchor();
        }
        else
        {
            return destination;
        }
    }

    private LocalResource mapLocalResource(LocalResource resource) throws ResourceException
    {
        // TODO[mikir] Make map source file to package
        final boolean isZserioPackage = resource.getExtension().equals(sourceMainExtension) &&
                resource.getPath().startsWith(sourceRoot);

        if (isZserioPackage)
        {
            final Path relativeSourcePath = sourceRoot.relativize(resource.getPath());
            final String resourceBaseName = resource.getBaseName();
            String packageHtmlLink;
            if (resourceBaseName.equals(sourceMainBase))
            {
                // this resource is a root package => root package is available
                // TODO[mikir] Be aware of possible directory in sourceMainBase, this does not check dirs!
                packageHtmlLink = PackageEmitter.getPackageHtmlLink(rootPackage, htmlContentDirectory);
            }
            else
            {
                // this resource is not a root package => we must reconstruct package name
                final PackageName.Builder packageNameBuilder = new PackageName.Builder();
                packageNameBuilder.addIds(topLevelPackageNameIds);
                for (Path relativeSourcePathPart : relativeSourcePath)
                    packageNameBuilder.addId(relativeSourcePathPart.toString());
                packageNameBuilder.addId(resourceBaseName);
                final PackageName packageName = packageNameBuilder.get();
                packageHtmlLink = PackageEmitter.getPackageHtmlLink(packageName.toString(),
                        htmlContentDirectory);
            }

            return new LocalResource(outputRoot, packageHtmlLink, resource.getAnchor());
        }

        if (!resource.getFullPath().toFile().exists())
            throw new ResourceException("Missing resource: '" + resource.getFullPath().toString() + "'!");

        LocalResource mappedResource = resources.get(resource);
        if (mappedResource == null)
        {
            final Path resourcesDir = outputRoot.resolve(RESOURCES_DIR);
            if (resource.getExtension().equals(MARKDOWN_EXTENSION))
            {
                mappedResource = addMappedResource(resourcesDir, resource.getBaseName(), HTML_EXTENSION);
            }
            else
            {
                mappedResource = addMappedResource(resourcesDir, resource.getBaseName(),
                        resource.getExtension());
            }

            resources.put(resource, mappedResource);
            copyResource(resource, mappedResource);
        }

        return mappedResource;
    }

    private void copyResource(LocalResource srcResource, LocalResource dstResource) throws ResourceException
    {
        try
        {
            Files.createDirectories(dstResource.getPath());

            if (srcResource.getExtension().equals(MARKDOWN_EXTENSION) &&
                    dstResource.getExtension().equals(HTML_EXTENSION))
            {
                final Path origCwd = currentSourceDir;
                final Path origCurrentOutputDir = currentOutputDir;
                try
                {
                    currentSourceDir = srcResource.getPath();
                    currentOutputDir = dstResource.getPath();
                    final String markdown = new String(Files.readAllBytes(srcResource.getFullPath()),
                            StandardCharsets.UTF_8);
                    final String html = DocMarkdownToHtmlConverter.convert(this,
                            new AstLocation(srcResource.getFullPath().toString(), 0, 0), markdown);
                    Files.write(dstResource.getFullPath(), html.getBytes(StandardCharsets.UTF_8));
                }
                finally
                {
                    currentSourceDir = origCwd;
                    currentOutputDir = origCurrentOutputDir;
                }
            }
            else
            {
                Files.copy(srcResource.getFullPath(), dstResource.getFullPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e)
        {
            throw new ResourceException("Failed to copy resource: '" + srcResource.getFullPath() + "' to '" +
                    dstResource.getFullPath() + "'!");
        }
    }

    private LocalResource addMappedResource(Path path, String baseName, String extension)
    {
        LocalResource mappedResource = new LocalResource(path, baseName, extension, "");
        int duplicityMarkerIndex = 0;
        while (!mappedResources.add(mappedResource))
        {
            mappedResource = new LocalResource(
                    path, baseName + "(" + (++duplicityMarkerIndex) + ")", extension, "");
        }
        return mappedResource;
    }

    private static boolean isLocalResource(String destination)
    {
        if (destination.startsWith("#"))
            return false; // local anchor

        try
        {
            final URL url = new URL(destination);
            return url.getProtocol().equals(LOCAL_FILE_SCHEME);
        }
        catch (MalformedURLException e)
        {}

        // not an URL, supposing that it's local resource
        return true;
    }

    private static String getFileNameBase(String fileName)
    {
        final int lastDotIndex = fileName.lastIndexOf('.');

        return (lastDotIndex != -1) ? fileName.substring(0, lastDotIndex) : fileName;
    }

    private static String getFileNameExtension(String fileName)
    {
        final int lastDotIndex = fileName.lastIndexOf('.');

        return (lastDotIndex != -1) ? fileName.substring(lastDotIndex) : "" ;
    }

    private static class ResourceException extends Exception
    {
        public ResourceException(String message)
        {
            super(message);
        }

        private static final long serialVersionUID = 1L;
    }

    private static class LocalResource
    {
        public LocalResource(Path currentDir, String destination)
        {
            final Path resourcePath = currentDir.resolve(destination).toAbsolutePath().normalize();

            path = resourcePath.getParent();
            final String fileNameWithAnchor = resourcePath.getName(resourcePath.getNameCount() - 1).toString();

            final int anchorIndex = fileNameWithAnchor.lastIndexOf('#');
            anchor = anchorIndex != -1 ? fileNameWithAnchor.substring(anchorIndex) : "";

            final String fileName = anchorIndex != -1
                    ? fileNameWithAnchor.substring(0, anchorIndex) : fileNameWithAnchor;
            baseName = DocResourceManager.getFileNameBase(fileName);
            extension  = DocResourceManager.getFileNameExtension(fileName);
        }

        public LocalResource(Path path, String baseName, String extension, String anchor)
        {
            this.path = path;
            this.baseName = baseName;
            this.extension = extension;
            this.anchor = anchor;
        }

        public LocalResource(Path resourcePath, String fullFileName, String anchor)
        {
            final Path fullPath = resourcePath.resolve(fullFileName);

            this.path = fullPath.getParent();
            final String fileName = fullPath.getName(fullPath.getNameCount() - 1).toString();

            baseName = DocResourceManager.getFileNameBase(fileName);
            extension  = DocResourceManager.getFileNameExtension(fileName);

            this.anchor = anchor;
        }

        public Path getFullPath()
        {
            return path.resolve(baseName + extension);
        }

        @Override
        public String toString()
        {
            return getFullPath().toString() + anchor;
        }

        @Override
        public boolean equals(Object object)
        {
            if (object instanceof LocalResource)
            {
                final LocalResource other = (LocalResource)object;
                return getPath().equals(other.getPath()) && getBaseName().equals(other.getBaseName()) &&
                        getExtension().equals(other.getExtension());
            }

            return false;
        }

        @Override
        public int hashCode()
        {
            int hash = HashUtil.HASH_SEED;
            hash = HashUtil.hash(hash, getPath());
            hash = HashUtil.hash(hash, getBaseName());
            hash = HashUtil.hash(hash, getExtension());
            return hash;
        }

        public Path getPath()
        {
            return path;
        }

        public String getBaseName()
        {
            return baseName;
        }

        public String getExtension()
        {
            return extension;
        }

        public String getAnchor()
        {
            return anchor;
        }

        private final Path path;
        private final String baseName;
        private final String extension;
        private final String anchor;
    }

    private final HashMap<LocalResource, LocalResource> resources = new HashMap<LocalResource, LocalResource>();
    private final HashSet<LocalResource> mappedResources = new HashSet<LocalResource>();

    private final static String RESOURCES_DIR = "resources";
    private final static String LOCAL_FILE_SCHEME = "file";
    private final static String MARKDOWN_EXTENSION = ".md";
    private final static String HTML_EXTENSION = ".html";

    private final Path sourceRoot;
    private final String sourceMainBase;
    private final String sourceMainExtension;
    private final Path outputRoot;
    private final List<String> topLevelPackageNameIds;
    private final String htmlContentDirectory;
    private final Package rootPackage;

    private Path currentSourceDir;
    private Path currentOutputDir;
}