package com.example.apacheMQLabs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.example.apacheMQLabs.mqProducer;

public class AppTest {

    @Test
    public void handleRequest_shouldReturnConstantValue() {
        mqProducer function = new mqProducer();
        Object result = function.handleRequest("echo", null);
        assertEquals("echo", result);
    }
}
