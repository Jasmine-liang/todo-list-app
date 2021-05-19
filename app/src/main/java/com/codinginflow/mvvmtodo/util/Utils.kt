package com.codinginflow.mvvmtodo.util

//Turn a statement into an expression
val <T> T.exhaustive: T
    get() = this