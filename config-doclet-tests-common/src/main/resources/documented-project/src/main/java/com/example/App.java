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
    public static final String CFG_DESTIONATION = "app.destination";

    /**
     * The default message.
     */
    public static final String DEFAULT_MESSAGE = "Hello, world!";

    /**
     * Setting specifying number of repetitions. A value of N means that the message is printed N times.
     * @cfg.default {@link #DEFAULT_NUM_REPETITIONS}
     */
    public static final String CFG_NUM_REPS = "cfg.numRepetitions";


    private static final int DEFAULT_NUM_REPETITIONS = 1;

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
