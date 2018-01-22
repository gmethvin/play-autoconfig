/*
 * Copyright 2018 Greg Methvin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.methvin.play.autoconfig

import scala.reflect.macros.blackbox

private[autoconfig] class AutoConfigImpl(val c: blackbox.Context) {
  import c.universe._

  def loader[T](implicit t: WeakTypeTag[T]): Tree = {
    val tpe = t.tpe
    // Look at all public constructors
    val publicConstructors: Seq[MethodSymbol] = tpe.members.collect {
      case m if m.isConstructor && m.isPublic && !m.fullName.endsWith("$init$") =>
        m.asMethod
    }(collection.breakOut)

    val constructor = publicConstructors match {
      case Seq() =>
        c.abort(c.enclosingPosition, s"Public constructor not found for type $tpe")
      case Seq(cons) =>
        cons
      case constructors =>
        // If multiple public constructors are found, try to select the primary constructor, otherwise give up
        constructors.find(_.isPrimaryConstructor).getOrElse(
          c.abort(c.enclosingPosition, s"Multiple public constructors found for $tpe but one is not primary")
        )
    }

    val confTerm = TermName(c.freshName("conf$"))
    val argumentLists = constructor.paramLists.map { params =>
      params.map { p =>
        q"implicitly[_root_.play.api.ConfigLoader[${p.typeSignature}]].load($confTerm, ${p.name.decodedName.toString})"
      }
    }
    q"""
      new ConfigLoader[$tpe] {
        override def load(config: _root_.com.typesafe.config.Config, path: String) = {
          val $confTerm = config.getConfig(path)
          new $tpe(...$argumentLists)
        }
      }
    """
  }

}
