package org.penella.smaz

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


/**
 * This is a kotlin implementation of the Smaz shared dictionary compression algorithm.
 * The original algorithm can be found at:
 * https://github.com/antirez/smaz
 *
 * And this is based off the Java implementation:
 * https://github.com/ayende/Xiao/blob/master/XiaoJava/Xiao.java
 *
 */
class BufferTooSmallException() : Exception("Buffer too small!")

class Smaz {
    companion object {
        val defaultTerms = arrayOf(
                " ", "the", "e", "t", "a", "of", "o", "and", "i", "n", "s", "e ", "r", " th",
                " t", "in", "he", "th", "h", "he ", "to", "l", "s ", "d", " a", "an",
                "er", "c", " o", "d ", "on", " of", "re", "of ", "t ", ", ", "is", "u", "at",
                "   ", "n ", "or", "which", "f", "m", "as", "it", "that",  "was", "en",
                "  ", " w", "es", " an", " i", "f ", "g", "p", "nd", " s", "nd ", "ed ",
                "w", "ed", "http://","https://", "for", "te", "ing", "y ", "The", " c", "ti", "r ", "his",
                "st", " in", "ar", "nt", ",", " to", "y", "ng", " h", "with", "le", "al", "to ",
                "b", "ou", "be", "were", " b", "se", "o ", "ent", "ha", "ng ", "their", "\"",
                "hi", "from", " f", "in ", "de", "ion", "me", "v", ".", "ve", "all", "re ",
                "ri", "ro", "is ", "co", "f t", "are", "ea", ". ", "her", " m", "er ", " p",
                "es ", "by", "they", "di", "ra", "ic", "not", "s, ", "d t", "at ", "ce", "la",
                "h ", "ne", "as ", "tio", "on ", "n t", "io", "we", " a ", "om", ", a", "s o",
                "ur", "li", "ll", "ch", "had", "this", "e t", "g ", " wh", "ere",
                " co", "e o", "a ", "us", " d", "ss", " be", " e",
                "s a", "ma", "one", "t t", "or ", "but", "el", "so", "l ", "e s", "s,", "no",
                "ter", " wa", "iv", "ho", "e a", " r", "hat", "s t", "ns", "ch ", "wh", "tr",
                "ut", "/", "have", "ly ", "ta", " ha", " on", "tha", "-", " l", "ati", "en ",
                "pe", " re", "there", "ass", "si", " fo", "wa", "ec", "our", "who", "its", "z",
                "fo", "rs", "ot", "un", "im", "th ", "nc", "ate", "ver", "ad", "html", "xhtml",
                " we", "ly", "ee", " n", "id", " cl", "ac", "il", "rt", " wi",
                "e, ", " it", "whi", " ma", "ge", "x", "e c", "men", ".com", "rdf", "rdfs"
        )
    }

    private val termTableBytes : Array<ByteArray?>
    private val termTableStrings : Array<String>
    private val hashTable : Array<ByteArray?>
    private var maxTermSize : Int = 0
    private val maxVerbatimLength : Int
    private val MAX_BYTE_SIZE = 255

    constructor(terms : Array<String> = defaultTerms) {
        termTableStrings = terms
        if(termTableStrings.size + 8 > MAX_BYTE_SIZE) throw RuntimeException("Term table too large!")

        termTableBytes = arrayOfNulls(termTableStrings.size)
        maxVerbatimLength = MAX_BYTE_SIZE - termTableStrings.size
        hashTable = arrayOfNulls<ByteArray>(MAX_BYTE_SIZE)

        for(x in 0 until termTableStrings.size) {
            val bytes = termTableStrings[x].toByteArray()
            if(bytes.size > MAX_BYTE_SIZE) throw RuntimeException("Term ${termTableStrings[x]}  is too large!");
            termTableBytes[x] = bytes
            val buffer = ByteArray(bytes.size + 2)
            buffer[0] = bytes.size.toByte()
            buffer[buffer.size - 1] = x.toByte()
            System.arraycopy(bytes, 0, buffer, 1, bytes.size)
            maxTermSize = Math.max(maxTermSize, bytes.size)


            var h = (bytes[0].toInt() shl 3).toInt()
            this.addHash(h, buffer)
            if(bytes.size == 1) continue

            h += bytes[1]
            this.addHash(h, buffer)

            if(bytes.size == 2) continue

            h = h xor bytes[2].toInt()
            this.addHash(h, buffer)
        }

        for(x in 0 until hashTable.size) if(hashTable[x] == null) hashTable[x] = ByteArray(0)
    }

    private fun addHash(hash : Int, buffer: ByteArray ) {
        val index = hash % hashTable.size
        if(hashTable[index] == null) {
            hashTable[index] = buffer
        } else {
            val newBuf = ByteArray(hashTable[index]!!.size + buffer.size)
            System.arraycopy(hashTable[index], 0, newBuf, 0, hashTable[index]!!.size)
            System.arraycopy(buffer, 0, newBuf, hashTable[index]!!.size, buffer.size)
            hashTable[index] = newBuf
        }
    }

    fun compress( input : ByteArray, output: ByteArray) : Int {
        var outPosition = 0
        var verbatimStart = 0
        var verbatimSize = 0
        var i = 0
        while(i < input.size) {
            var h1 = input[i].toInt() shl 3
            var h2 = if(i + 1 < input.size) h1 + input[i + 1].toInt() else 0
            var h3 = if(i + 2 < input.size) h2 xor input[i + 2].toInt() else 0
            var totalSize = if( i + maxTermSize >= input.size) input.size - i else maxTermSize

            var found = false
            for(size in totalSize downTo 0) {
                if(found) break

                val slot = when(size) {
                    1 -> hashTable[h1 % hashTable.size]
                    2 -> hashTable[h2 % hashTable.size]
                    else -> hashTable[h3 % hashTable.size]
                }

                var pos = 0
                while(pos + 1 < slot!!.size) {
                    val termLength = slot[pos]
                    if( termLength.toInt() != size || bufferEquals(slot, pos + 1, input, i, size) == false) {
                        pos += termLength + 2
                        continue
                    }

                    if( verbatimSize > 0) {
                        val (position, start, size) = flush(input, output, verbatimStart, verbatimSize, outPosition)
                        outPosition = position
                        verbatimStart = start
                        verbatimSize = size
                    }

                    output[outPosition++] = slot[termLength + pos + 1]
                    verbatimStart = i + termLength
                    i += termLength -1
                    found = true
                    break
                }
            }
            if(!found) verbatimSize++

            i++
        }

        val (position, start, size) = flush(input, output, verbatimStart, verbatimSize, outPosition)
        outPosition = position
        verbatimStart = start
        verbatimSize = size

        return outPosition
    }

    fun decompress(input : ByteArray, output: ByteArray) : Int {
        var outPosition = 0
        var i = 0
        while( i < input.size) {
            val slot = input[i].toInt() and 0xFF

            if( slot >= termTableStrings.size) {
                val size = slot - termTableStrings.size
                System.arraycopy(input, i + 1, output, outPosition, size)
                outPosition += size
                i += size
            } else {
                val termBytes = termTableBytes[slot]!!
                val size = termBytes.size
                System.arraycopy(termBytes, 0, output, outPosition, size)
                outPosition += termBytes.size
            }

            i++
        }

        return outPosition
    }


    private fun flush(input : ByteArray, output : ByteArray, vStart : Int, vLength : Int, outPos : Int) : Triple<Int, Int, Int> {
        var outPosition = outPos
        var verbatimStart = vStart
        var verbatimLength = vLength

        while(verbatimLength > 0) {
            val size = Math.min(maxVerbatimLength - 1, verbatimLength)
            output[outPosition++] = (size + termTableStrings.size).toByte()
            System.arraycopy(input, verbatimStart, output, outPosition, size)
            verbatimStart += size
            verbatimLength -= size

            outPosition += size
        }

        return Triple(outPosition, verbatimStart, verbatimLength)
    }

    private fun bufferEquals(x : ByteArray, xStart : Int, y : ByteArray, yStart : Int, count : Int) : Boolean {
        for(i in 0 until count)  if( y[yStart + i] != x[xStart + i]) return false

        return true
    }

    fun compressString(input: String) : ByteArray {
        val buffer = ByteArray(input.length)
        val size = this.compress(input.toByteArray(Charsets.UTF_8), buffer)
        return buffer.sliceArray(0 until size)
    }

    fun decompressString(input: ByteArray, bufferSize : Int = 1024) : String {
        val buffer = ByteArray(bufferSize)
        try {
            val size = this.decompress(input, buffer)
            return buffer.sliceArray(0 until size).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            throw BufferTooSmallException()
        }
    }


}