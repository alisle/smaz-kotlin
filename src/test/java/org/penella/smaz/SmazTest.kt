package org.penella.smaz

import org.junit.Test
import org.junit.Assert
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Created by alisle on 10/21/16.
 */
class SmazTest {
    @Test
    fun testCompressDecompress() {
        val compress = Smaz()
        val test = "Hello World, I am a Test String"
        val testBytes = test.toByteArray(Charsets.UTF_8)

        val compressedBuffer = ByteArray(test.length)
        val compressedSize = compress.compress(testBytes, compressedBuffer)
        val compressedBytes = compressedBuffer.slice(0 until compressedSize).toByteArray()

        val decompressedBuffer = ByteArray(1024)
        val decompressedSize = compress.decompress(compressedBytes, decompressedBuffer)
        val decompressedBytes = decompressedBuffer.slice(0 until decompressedSize).toByteArray()

        Assert.assertTrue(decompressedSize >= compressedSize)
        Assert.assertArrayEquals(decompressedBytes, testBytes)

        val decompressString = decompressedBytes.toString(Charsets.UTF_8)

        assertEquals(test, decompressString)

    }

    @Test
    fun testCompressDecompressString() {
        val compress = Smaz()
        val test = "Hello World"
        val compressed = compress.compressString(test)
        val decompressed = compress.decompressString(compressed)

        assertEquals(test, decompressed)
    }

    @Test
    fun testDecompressSmallBuffer() {
        val compress = Smaz()
        val test = "I am not a very big string, but I will have a very small buffer"
        val compressed = compress.compressString(test)
        try {
            val decompressed = compress.decompressString(compressed, 10)
        } catch( e: BufferTooSmallException ) {
            return
        }

        assertTrue(false)
    }


}