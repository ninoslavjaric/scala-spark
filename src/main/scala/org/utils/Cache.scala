package org.utils

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.io.Source
import scala.collection.mutable.StringBuilder
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import java.nio.file.StandardOpenOption.{CREATE, WRITE}
import java.io.{BufferedReader, InputStreamReader}


object Cache {
    trait Contract {
        def get(key: String): String
        def set(key: String, value: String): Unit
        def has(key: String): Boolean
    }

    object File extends Contract {
        final val CACHE_DIR = "cache"

        override def has(filePath: String): Boolean = {
            val path = Paths.get(s"${CACHE_DIR}${filePath}.html.gz")
            Files.exists(path)
        }

        def set(filePath: String, data: String): Unit = {
            val path = Paths.get(s"${CACHE_DIR}${filePath}.html.gz")

            val parentDir = path.getParent()
            if (Files.notExists(parentDir)) {
                Files.createDirectories(parentDir)
            }

            // Files.write(path, data.getBytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
            val gzipOutputStream = new GZIPOutputStream(Files.newOutputStream(path, CREATE, WRITE))
            try {
                gzipOutputStream.write(data.getBytes("UTF-8"))
            } finally {
                gzipOutputStream.close()
            }
        }

        def get(filePath: String): String = {
            val path = Paths.get(s"${CACHE_DIR}${filePath}.html.gz")
            // val source = Source.fromFile(path.toFile())
            // var lines = new StringBuilder()
            // try {
            //     for (line <- source.getLines()) {
            //         lines.append(line)
            //     }
            // } finally {
            //     source.close()
            // }

            val gzipInputStream = new GZIPInputStream(Files.newInputStream(path))
            val reader = new BufferedReader(new InputStreamReader(gzipInputStream, "UTF-8"))
            val stringBuilder = new StringBuilder

            try {
                var line: String = null
                while ({ line = reader.readLine(); line != null }) {
                    stringBuilder.append(line)
                }
            } finally {
                gzipInputStream.close()
            }

            stringBuilder.toString.trim
        }
    }
}
