package org.utils

import java.security.MessageDigest
import java.util.UUID

object Hash {
    object Md5 {
        def hash(params: String*): String = {
            val combined = params.mkString("|")
            // Create an MD5 hash
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(combined.getBytes("UTF-8"))
            
            digest.map("%02x".format(_)).mkString
        }

        def uuid(input: String*): String = {
            val hash = this.hash(input.mkString("|"))

            // Convert the hash to a GUID format
            UUID.fromString(
                hash.take(8) + "-" + hash.substring(8, 12) + "-" + hash.substring(12, 16) + "-" + hash.substring(16, 20) + "-" + hash.takeRight(12)
            ).toString()
        }
    }
}
