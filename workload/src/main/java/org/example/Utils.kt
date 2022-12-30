package org.example

import technology.touchmars.ksp.GoodGuy

@GoodGuy
class Utils {
    @GoodGuy fun convert(s: String): Int {
        var sol = 0
        s.lowercase().forEach { sol += it.digitToInt() }
        return sol
    }
}