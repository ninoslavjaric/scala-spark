package org.schema

import com.fasterxml.jackson.module.scala.deser.overrides
import org.utils.Hash

final case class Competition (
    href: String,
    name: String,
    gender: String,
    tier: String,
    governingBody: String,
) {
    override def toString: String = Hash.Md5.uuid(href, name, gender, tier, governingBody)
}
