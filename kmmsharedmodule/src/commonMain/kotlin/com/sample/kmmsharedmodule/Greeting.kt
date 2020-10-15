package com.sample.kmmsharedmodule

class Greeting {
    fun greeting(): String {
        return "Hello, ${Platform().platform}!"
    }
}