package com.maxalarm

import android.content.Context

/** System services this app uses always exist on phones; fail loudly if one somehow doesn't. */
inline fun <reified T : Any> Context.service(): T =
    checkNotNull(getSystemService(T::class.java)) { "Missing system service ${T::class.java.simpleName}" }
