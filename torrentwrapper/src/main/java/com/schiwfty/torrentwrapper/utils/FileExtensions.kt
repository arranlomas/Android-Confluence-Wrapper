package com.schiwfty.torrentwrapper.utils

import java.io.File
import java.io.InputStream

/**
 * Created by arran on 2/11/2017.
 */
fun File.copyInputStereamToFile(inputStream: InputStream){
    inputStream.use { input -> this.outputStream().use { fileOut -> input.copyTo(fileOut) } }
}