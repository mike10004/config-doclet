package com.example;

/**
 * Class that prints a message to the console.
 * 
 * @author tyler
 * @since 1.0
 */
public class App {

    /**
     * Message configuration property name. This second sentence contains some detail about the property.
     * @cfg.default {@link #DEFAULT_MESSAGE}
     * @cfg.example Looking good, Billy Ray!
     * @cfg.example Feeling good, Louis!
     * @cfg.type string
     */
    public static final String CFG_MESSAGE = "app.message";

    /**
     * Destination configuration property name.
     * @cfg.description this description overrides the sentences
     * @cfg.default stdout
     * @cfg.example stderr
     * @cfg.example null
     */
    public static final String CFG_DESTINATION = "app.destination";

    /**
     * The default message.
     */
    public static final String DEFAULT_MESSAGE = "Hello, world!";

    /**
     * Setting specifying number of repetitions. A value of N means that the message is printed N times.
     * @cfg.default {@link #DEFAULT_NUM_REPETITIONS}
     */
    public static final String CFG_NUM_REPS = "app.numRepetitions";

    /**
     * Wacky setting to demonstrate restricting which fields are included.
     */
    private static final String WACKY_SETTING = "wacky.setting.name";

    private static final int DEFAULT_NUM_REPETITIONS = 1;

    private static final String CFG_UNDOCUMENTED = "app.undocumentedSetting";

    /**
     * Setting whose description is a bit long and complex. How long, you ask? Well, long
     * enough that it spans multiple lines and has a blank line, below which is a new
     * paragraph. This of course means that it includes some HTML markup.
     *
     * <p>That markup is even more complex, because of the following
     * <ul>
     * <li>there is an unordered list</li>
     * <li>there are multiple list items
     * <li>one of those even has a {@link #CFG_UNDOCUMENTED link} to another field
     * <li>and those links come in {@link #WACKY_SETTING} various forms</li>
     * </ul>
     *
     * On top of all that, at the end of the comment, there is a concluding line.
     */
    private static final String CFG_COMPLEX_DESCRIPTION = "app.complexDescription";

    private java.io.PrintStream output;

    /**
     * Constructs an instance of the class, assigning standard output.
     * @see System#out
     */
    public App() {
        this(System.out);
    }

    /**
     * Constructs an instance of the class.
     * @param output the output stream to use
     */
    public App(java.io.PrintStream output) {
        this.output = output;
    }

    /**
     * Prints a message to this instance's output stream.
     * @param message the message to print
     */
    public void printMessage(String message) {
        output.println(message);
    }

    /**
     * Main method for the program.
     * @param args argument array
     */
    public static void main( String[] args )
    {
        new App().printMessage(DEFAULT_MESSAGE);
    }

}
