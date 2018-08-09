package com.example;

/**
 * Enumeration of some choices.
 */
public enum Choice {

    /**
     * Do I stay?
     */
    STAY,

    /**
     * Do I go now?
     */
    GO;

    /**
     * Setting that specifies the default choice.
     * @cfg.default STAY
     */
    public static final String CFG_DEFAULT_CHOICE = "app.choice.default";

}