[![Maven Central](https://img.shields.io/maven-central/v/com.github.mike10004/config-doclet.svg)](https://repo1.maven.org/maven2/com/github/mike10004/config-doclet/)
[![Travis build status](https://img.shields.io/travis/mike10004/config-doclet.svg)](https://travis-ci.org/mike10004/config-doclet)
[![AppVeyor build status](https://ci.appveyor.com/api/projects/status/1t5d502vko29srw4?svg=true)](https://ci.appveyor.com/project/mike10004/config-doclet)

config-doclet
=============

Javadoc Doclet that produces a configuration help file for your project. This uses
the JDK 9 Doclet API, with all the fun that entails.

If you're like me, your programs often read configuration parameters from text 
files. The settings contained in these text files are often keyed by string 
constants that are defined in my source code. This doclet allows you to 
generate documentation about your configuration settings by adding Javadoc 
comments to the static final fields that represent your configuration keys.
This helps you keep your program's documentation complete and in sync with
the code while not requiring that your users dig into the API docs.

The plugin assumes that you want output appropriate for a properties file as 
suitable to be loaded by `java.util.Properties`, but as an alternative, you 
can generate JSON-formatted output and use your own subsequent plugins or 
programs to transform that into human-readable documentation.    

Overview
------------------------------------ 

Consider this example class:

    public class App {
    
        /**
         * Setting that specifies the message to print. 
         * @cfg.default hello
         * @cfg.example Goodbye!
         */
        public static final String CFG_MESSAGE = "app.message";
    
        private Properties config;
    
        public App(Properties config) {
            this.config = config;
        }
    
        public void printMessage() {
            String message = config.getProperty(CFG_MESSAGE, "hello");
            System.out.println(message);
        }
    
    }

If we execute the doclet on this source code with the default settings, the 
generated file will contain the following contents:

    # Setting that specifies the message to print.
    # Example: Goodbye!
    #app.message = hello 

The doclet examines each static final field whose name has the prefix `CFG_`. 
For each one the description is taken from the regular comment body, and that 
can be fine-tuned with the tags described below.

Tag Reference
-------------

* `@cfg.default` specifies the setting's default value
* `@cfg.example` provides an example of an acceptable value for the setting
* `@cfg.description` specifies text to override the regular comment body for the description  
* `@cfg.key` specifies a string that overrides the value of the constant; use this if your constant value is not available at compile-time 
* `@cfg.include` includes a setting that would otherwise be exclude because it is annotated as deprecated
* `@cfg.sortKey` specifies a string to use instead of the setting key when sorting the settings for printing 

Doclet Options
--------------

* **--field-names** restricts the static final fields examined to those whose names match the argument pattern 
* **--field-names-regex** restricts the static final fields examined to those whose names match the argument regex
* **--output-filename** sets the output filename
* **--output-format** sets the output format (one of `properties` or `json`; default is `properties`)
* **--assign-value** in properties output, determines whether a value is assigned (and not commented-out); argument must be `auto`, `always`, or `never`  
* **-header** in properties output, prepend the given header string; use a `file:` URL to read the string from file 
* **-footer** in properties output, append the given footer string; use a `file:` URL to read the string from file

Using as a Maven plugin
-----------------------

This is not a Maven plugin, but the **maven-javadoc-plugin** can be configured to use this doclet.

    <build>
        <plugins>
            <plugin>
                <artifactId>maven-javadoc-plugin</artifactId>
                <version>3.0.1</version>
                <executions>
                    <execution>
                        <id>generate-config-help</id>
                        <goals>
                            <goal>javadoc</goal>
                        </goals>
                        <phase>generate-resources</phase>
                        <configuration>
                            <doclet>io.github.mike10004.configdoclet.ConfigDoclet</doclet>
                            <docletArtifact>
                                <groupId>com.github.mike10004</groupId>
                                <artifactId>config-doclet-core</artifactId>
                                <version>LATEST</version> <!-- see Maven badge above -->
                            </docletArtifact>
                            <reportOutputDirectory>${project.build.directory}/help</reportOutputDirectory>
                            <additionalOptions>
                                <arg>'--field-names=MY_CFG_CONST_PREFIX_*'</arg>
                            </additionalOptions>
                            <show>private</show>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

This will generate a file named `target/help/config-doclet-output.properties` in your 
project directory.

If you are also generating regular API docs, you may need to add some 
configuration parameters to ignore this doclet's tags (which all have prefix 
`cfg.`), or use `<doclint>none</doclint>` to ignore non-fatal errors during 
Javadoc generation.

Run from the command line
-------------------------

Running from the command line is a bit burdernsome because the doclet has 
some dependencies, but it can be done with a command like this:

    javadoc -doclet io.github.mike10004.configdoclet.ConfigDoclet \
         -docletpath /path/to/config-doclet.jar:/path/to/jsr305-3.0.2.jar:/path/to/commons-lang3-3.6.jar:/path/to/gson-2.8.5.jar \
         '--field-names=CFG_*' com.example

Adjust the command arguments to contain the correct values for the locations 
of your JAR files, the names of your constant fields, and your source packages.
