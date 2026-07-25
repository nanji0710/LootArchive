package com.nanji.lootarchive.util

/** Flow combine() 最多支持 5 个流，嵌套 combine 时需要元组装载中间结果 */

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

data class Quintet<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

data class Sextet<A, B, C, D, E, F>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F)
