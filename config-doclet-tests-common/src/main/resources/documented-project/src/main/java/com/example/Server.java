package com.example;

import java.util.concurrent.Future;

/**
 * Interface that is here as an example of an interface, to prove that you can define a
 * setting constant as a member of an interface.
 */
public interface Server {

    /**
     * Setting that determines what style attire the server will be wearing.
     */
    String CFG_SERVER_ATTIRE = "app.server.attire";

    /**
     * Places an order for food with the server
     * @param request the request in plain language
     * @return a future that resolves with the food, hopefully
     */
    Future<Object> placeOrder(String request);

}