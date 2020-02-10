package com.igreenwood.loupesample.util

import com.chibatching.kotpref.KotprefModel

object Pref: KotprefModel() {
    var useSharedElements by booleanPref()
}