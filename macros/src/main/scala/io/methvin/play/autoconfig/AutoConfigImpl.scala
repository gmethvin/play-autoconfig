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
    // Look at all constructors
    val constructors: Seq[MethodSymbol] = tpe.members.collect {
      case m if m.isConstructor && !m.fullName.endsWith("$init$") =>
        m.asMethod
    }.toSeq

    val configConstructors = constructors.filter(_.annotations.exists(_.tree.tpe =:= typeOf[ConfigConstructor]))
    val constructor = configConstructors match {
      case Seq() =>
        // Find the primary constructor
        constructors
          .find(_.isPrimaryConstructor)
          .getOrElse(c.abort(c.enclosingPosition, s"No primary constructor found!"))
      case Seq(cons) =>
        cons
      case _ =>
        // multiple annotated constructors found
        c.abort(c.enclosingPosition, s"Multiple constructors found annotated with @ConfigConstructor")
    }

    val confTerm = TermName(c.freshName("conf$"))
    val argumentLists = constructor.paramLists.map { params =>
      params.map { p =>
        val name = p.annotations.collectFirst {
          case ann if ann.tree.tpe =:= typeOf[ConfigName] =>
            ann.tree.children.collectFirst { case Literal(Constant(str: String)) => str }.get
        }.getOrElse(p.name.decodedName.toString)
        q"implicitly[_root_.play.api.ConfigLoader[${p.typeSignature}]].load($confTerm, $name)"
      }
    }
    q"""
      new _root_.play.api.ConfigLoader[$tpe] {
        override def load(config: _root_.com.typesafe.config.Config, path: String) = {
          val $confTerm = if (path.isEmpty) config else config.getConfig(path)
          new $tpe(...$argumentLists)
        }
      }
    """
  }

}
