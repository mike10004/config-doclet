package io.github.mike10004.configdoclet;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;

public class MyNewDocletInvocationTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void invoke() throws Exception {
        InvocationRequest request = new DefaultInvocationRequest();

        request.setInputStream(new ByteArrayInputStream(new byte[0]));
        request.setPomFile(getTestProjectPomFile());
//        request.setDebug(true);
        request.setGoals(Collections.singletonList("javadoc:javadoc"));
//        request.setDebug(true);
        Properties properties = new Properties();
        properties.setProperty("java.home", System.getProperty("java.home"));
        io.github.mike10004.configdoclet.MyNewDoclet.class.getName();
        File docletClasspath = new File(Tests.config().get("project.build.outputDirectory"));
        System.out.format("docletClasspath = %s%n", docletClasspath);
        properties.setProperty("docletPath", docletClasspath.getAbsolutePath());
        request.setProperties(properties);
        System.out.format("passing JAVA_HOME=%s%n", System.getProperty("java.home"));
        request.addShellEnvironment("JAVA_HOME", System.getProperty("java.home"));

        DefaultInvoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File("/usr/share/maven"));
        invoker.setWorkingDirectory(temporaryFolder.newFolder());
        InvocationResult result = invoker.execute(request);
        CommandLineException exception = result.getExecutionException();
        if (exception != null) {
            exception.printStackTrace(System.out);
        }
        assertEquals("exit code", 0, result.getExitCode());
    }

    private File getTestProjectPomFile() throws URISyntaxException, IOException {
        URL pomResource = getClass().getResource("/documented-project/pom.xml");
        checkState(pomResource != null, "pom not found");
        File pomFile = new File(pomResource.toURI());
        return pomFile;
    }

    private static Object invoke(Class<?> clazz, String methodName, Object instance, Class<?>[] methodParameterTypes, Object[] args) throws ReflectiveOperationException {
        Method method = clazz.getDeclaredMethod(methodName, methodParameterTypes);
        Object returnValue = method.invoke(instance, args);
        return returnValue;
    }

    @Test
    public void doMain() throws Exception  {
//        Start jdoc = new Start();
//        return jdoc.begin(args).exitCode;
        Class<?> contextClass = Class.forName("com.sun.tools.javac.util.Context");
        Class<?> contextKeyClass = Class.forName("com.sun.tools.javac.util.Context$Key");
        Object context = contextClass.getConstructor().newInstance();
        Method putMethod = contextClass.getDeclaredMethod("put", contextKeyClass, Object.class);
        Class<?> logClass = Class.forName("com.sun.tools.javac.util.Log");
//        Method getLogMethod = logClass.getDeclaredMethod("instance", contextClass);
        invoke(Class.forName("jdk.javadoc.internal.tool.Messager"), "preRegister", null, new Class[]{contextClass, String.class}, new Object[]{context, "SomeProgram"});
//        Object logKey = logClass.getField("logKey").get(null);
//        putMethod.invoke(context, logKey, logInstance);
        System.out.format("registered log in %s%n", context);
        Class<?> startClass = Class.forName("jdk.javadoc.internal.tool.Start");
        Constructor<?>[] ctors = startClass.getConstructors();
        Constructor<?> ctor = Stream.of(ctors).filter(c -> c.getParameters().length == 1 && contextClass.equals(c.getParameters()[0].getType()))
                .findFirst().orElseThrow(() -> new IllegalStateException("constructor accepting Context arg not found"));
        ctor.setAccessible(true);
        Object startInstance = ctor.newInstance(context);
        Method beginMethod = startClass.getDeclaredMethod("begin", String[].class);
        beginMethod.setAccessible(true);
        File docletClasspath = new File(Tests.config().get("project.build.outputDirectory"));
        System.out.format("docletClasspath = %s%n", docletClasspath);
        File sourcepath = getTestProjectPomFile().getParentFile().toPath().resolve("src/main/java").toFile();
        Object[] invokeMethodArgs = {
                new String[]{"-doclet", MyNewDoclet.class.getName(),
                "-docletpath", docletClasspath.getAbsolutePath(),
                "-charset", "UTF-8",
                "-sourcepath", sourcepath.getAbsolutePath(),
                "com.example",
            }
        };
        Object result = beginMethod.invoke(startInstance, invokeMethodArgs);
        System.out.println(result);
        int exitCode = parseExitCode(result);
        assertEquals("exit code", 0, exitCode);
    }

    private static int parseExitCode(Object result) {
        Matcher m = Pattern.compile("\\w+\\((\\d+)\\)").matcher(result.toString());
        checkState(m.find());
        return Integer.parseInt(m.group(1));
    }
}