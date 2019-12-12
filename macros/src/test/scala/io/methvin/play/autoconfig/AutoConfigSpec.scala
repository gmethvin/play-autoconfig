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

import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpec}
import play.api.{ConfigLoader, Configuration}

import scala.concurrent.duration._

class AutoConfigSpec extends WordSpec with Matchers {

  "AutoConfig.loader" should {
    "work with a simple case class" in {
      case class Foo(str: String, int: Int)

      implicit val fooLoader: ConfigLoader[Foo] = AutoConfig.loader[Foo]

      val config = Configuration(ConfigFactory.parseString("""
          |foo = {
          |  str = string
          |  int = 7
          |}
        """.stripMargin))

      config.get[Foo]("foo") should === { Foo("string", 7) }
    }
    "work with a simple case class with weird property names" in {
      case class SnowmanConfig(`☃`: String, `snowman-age`: FiniteDuration)

      implicit val fooLoader: ConfigLoader[SnowmanConfig] = AutoConfig.loader[SnowmanConfig]

      val config = Configuration(ConfigFactory.parseString("""
          |snowman = {
          |  ☃ = snowman
          |  snowman-age = 5 hours
          |}
        """.stripMargin))

      config.get[SnowmanConfig]("snowman") should === { SnowmanConfig("snowman", 5.hours) }
    }
    "work with multiple argument lists" in {
      case class Bar(a: String, b: String)(c: Double) {
        assert(c >= 0)
      }
      implicit val loader: ConfigLoader[Bar] = AutoConfig.loader
      val config = Configuration(ConfigFactory.parseString("""
          |bar = {
          |  a = hello
          |  b = goodbye
          |  c = 4.2
          |}
        """.stripMargin))

      config.get[Bar]("bar") should === { Bar("hello", "goodbye")(4.2) }
    }
    "work with the default constructor for non-case classes" in {
      final class Baz(val a: String, val b: String, val c: Double) {

        def this(a: String, b: String) = this(a, b, 0)

      }
      implicit val loader: ConfigLoader[Baz] = AutoConfig.loader
      val config = Configuration(ConfigFactory.parseString("""
          |baz = {
          |  a = hello
          |  b = goodbye
          |  c = 4.2
          |}
        """.stripMargin))

      val baz = config.get[Baz]("baz")
      (baz.a, baz.b, baz.c) should === { ("hello", "goodbye", 4.2) }
    }
    "work with a class with an alternate constructor" in {
      final class Qux(val a: String, val b: String, val c: Double) {
        // this constructor should be used since it's the only public constructor
        @ConfigConstructor def this(a: String, b: String) = this(a, b, 0)
      }
      implicit val loader: ConfigLoader[Qux] = AutoConfig.loader
      val config = Configuration(ConfigFactory.parseString("""
          |qux = {
          |  a = hello
          |  b = goodbye
          |  c = 4.2
          |}
        """.stripMargin))

      val qux = config.get[Qux]("qux")
      (qux.a, qux.b, qux.c) should === { ("hello", "goodbye", 0) }
    }
    "work with an annotated case class" in {
      case class FooApiConfig(
        @ConfigName("api-key") apiKey: String,
        @ConfigName("api-password") apiPassword: String,
        @ConfigName("request-timeout") requestTimeout: Duration
      )
      implicit val loader: ConfigLoader[FooApiConfig] = AutoConfig.loader
      def fromConfiguration(conf: Configuration) = conf.get[FooApiConfig]("api.foo")

      val conf = Configuration(ConfigFactory.parseString("""
          |api.foo {
          |  api-key = "abcdef"
          |  api-password = "secret"
          |  request-timeout = 1 minute
          |}
        """.stripMargin))

      fromConfiguration(conf) should === { FooApiConfig("abcdef", "secret", 1.minute) }
    }
    "work with an annotated regular class" in {
      final class BarApiConfig(
        @ConfigName("api-key") val apiKey: String,
        @ConfigName("api-password") val apiPassword: String,
        @ConfigName("request-timeout") val requestTimeout: Duration
      ) {
        override def equals(that: Any): Boolean = that match {
          case c: BarApiConfig =>
            (c.apiKey, c.apiPassword, c.requestTimeout) == ((this.apiKey, this.apiPassword, this.requestTimeout))
          case _ =>
            false
        }
      }
      object BarApiConfig {
        implicit val loader: ConfigLoader[BarApiConfig] = AutoConfig.loader
        def fromConfiguration(conf: Configuration) = conf.get[BarApiConfig]("api.foo")
      }

      val conf = Configuration(ConfigFactory.parseString("""
          |api.foo {
          |  api-key = "abcdef"
          |  api-password = "secret"
          |  request-timeout = 1 minute
          |}
        """.stripMargin))

      BarApiConfig.fromConfiguration(conf) should === { new BarApiConfig("abcdef", "secret", 1.minute) }
    }
    "work with a nested config" in {
      case class FooNestedConfig(@ConfigName("nested.str") str: String, @ConfigName("nested.deep.int") int: Int)

      implicit val fooLoader: ConfigLoader[FooNestedConfig] = AutoConfig.loader[FooNestedConfig]

      val config = Configuration(ConfigFactory.parseString("""
          |foo = {
          |  nested.str = string
          |  nested.deep.int = 7
          |}
        """.stripMargin))

      config.get[FooNestedConfig]("foo") should === { FooNestedConfig("string", 7) }
    }
    "work with the config root" in {
      case class Foo(str: String, int: Int)

      implicit val fooLoader: ConfigLoader[Foo] = AutoConfig.loader[Foo]

      val config = Configuration(ConfigFactory.parseString("""
          |str = string
          |int = 7
        """.stripMargin))

      config.get[Foo]("") should === { Foo("string", 7) }
    }
  }

}
