package io.github.mike10004.configdoclet;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;

public class MyNewDocletTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File getTestProjectPomFile() throws URISyntaxException, IOException {
        URL pomResource = getClass().getResource("/documented-project/pom.xml");
        checkState(pomResource != null, "pom not found");
        File pomFile = new File(pomResource.toURI());
        return pomFile;
    }

    private static Object invoke(Class<?> clazz, String methodName, Object instance, Class<?>[] methodParameterTypes, Object[] args) throws ReflectiveOperationException {
        Method method = clazz.getDeclaredMethod(methodName, methodParameterTypes);
        method.setAccessible(true);
        Object returnValue = method.invoke(instance, args);
        return returnValue;
    }

    private int invokeJavadocStart(String[] commandLineArgs) throws Exception {
        Class<?> contextClass = Class.forName("com.sun.tools.javac.util.Context");
        Object context = contextClass.getConstructor().newInstance();
        invoke(Class.forName("jdk.javadoc.internal.tool.Messager"), "preRegister", null, new Class[]{contextClass, String.class}, new Object[]{context, "SomeProgram"});
        System.out.format("registered log in %s%n", context);
        Class<?> startClass = Class.forName("jdk.javadoc.internal.tool.Start");
        Constructor<?>[] ctors = startClass.getConstructors();
        Constructor<?> ctor = Stream.of(ctors).filter(c -> c.getParameters().length == 1 && contextClass.equals(c.getParameters()[0].getType()))
                .findFirst().orElseThrow(() -> new IllegalStateException("constructor accepting Context arg not found"));
        ctor.setAccessible(true);
        Object startInstance = ctor.newInstance(context);
        Method beginMethod = startClass.getDeclaredMethod("begin", String[].class);
        beginMethod.setAccessible(true);
        Object[] invokeMethodArgs = {
                commandLineArgs
        };
        Object result = beginMethod.invoke(startInstance, invokeMethodArgs);
        System.out.println(result);
        int exitCode = parseExitCode(result);
        return exitCode;
    }

    @Test
    public void gardenPath() throws Exception  {
        File sourcepath = getTestProjectPomFile().getParentFile().toPath().resolve("src/main/java").toFile();
        File docletClasspath = new File(Tests.config().get("project.build.outputDirectory"));
        File outputDir = temporaryFolder.newFolder();
        System.out.format("docletClasspath = %s%n", docletClasspath);
        int exitCode = invokeJavadocStart(new String[]{"-doclet", MyNewDoclet.class.getName(),
                "-docletpath", docletClasspath.getAbsolutePath(),
                "-charset", "UTF-8",
                "-sourcepath", sourcepath.getAbsolutePath(),
                "-d", outputDir.getAbsolutePath(),
                "com.example",
        });
        assertEquals("exit code", 0, exitCode);
        Collection<File> filesInOutputDir = FileUtils.listFiles(outputDir, null, true);
        assertEquals("one file in output dir", 1, filesInOutputDir.size());
        File outputFile = filesInOutputDir.iterator().next();
        com.google.common.io.Files.copy(outputFile, System.out);
    }

    private static int parseExitCode(Object result) {
        Matcher m = Pattern.compile("\\w+\\((\\d+)\\)").matcher(result.toString());
        checkState(m.find());
        return Integer.parseInt(m.group(1));
    }
}