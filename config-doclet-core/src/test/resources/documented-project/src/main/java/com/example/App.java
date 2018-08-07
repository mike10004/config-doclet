package com.example;

/**
 * Class that prints a message to the console.
 * 
 * @author tyler
 * @since 1.0
 */
public class App {

    /**
     * Message configuration property name.
     * @cfg.description the message to print
     * @cfg.default {@link #GOOD_MESSAGE}
     * @cfg.example Looking good, Billy Ray!
     * @cfg.example Feeling good, Louis!
     * @cfg.type string
     */
    public static final String CFG_MESSAGE = "app.message";

    /**
     * Destination configuration property name.
     * @cfg.description output destination
     * @cfg.default stdout
     * @cfg.example stderr
     * @cfg.example null
     */
    public static final String CFG_DESTIONATION = "app.destination";

    /**
     * The default message.
     */
    public static final String GOOD_MESSAGE = "Hello, world!";

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
        new App().printMessage(GOOD_MESSAGE);
    }

}
