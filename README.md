# Kotlin port of Smaz Shared Dictionary Compression Algorithm. 

![Build Status](https://api.travis-ci.org/alisle/smaz-kotlin.png)

This is a kotlin implementation of the Smaz shared dictionary compression algorithm. This algorithm produces good results for strings which are typically too small to be compressed using Huffman based algorithms. This port is based off the Java implementation which can be found at Xiao [link](https://github.com/ayende/Xiao). 

The original C implemenation of the algorithm can be found here [link](https://github.com/antirez/smaz).
