package com.example

import technology.touchmars.ksp.Builder
import HELLO
import technology.touchmars.ksp.GoodGuy

@Builder
@GoodGuy
class AClass(private val a: Int, val b: String, val c: Double, val d: HELLO) {
    val p = "$a, $b, $c, ${d.foo()}"
    @GoodGuy fun foo() = p
}