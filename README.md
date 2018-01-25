# Play AutoConfig

[![Travis CI](https://travis-ci.org/gmethvin/play-autoconfig.svg?branch=master)](https://travis-ci.org/gmethvin/play-autoconfig) [![Maven](https://img.shields.io/maven-central/v/io.methvin.play/autoconfig-macros_2.12.svg)](https://mvnrepository.com/artifact/io.methvin.play/autoconfig-macros)

`AutoConfig` is a utility for type-safe configuration in Play (2.6.0 and later). The library provides a convenient macro to generate `ConfigLoader` instances for arbitrary classes.

## Usage

To add to your sbt build:

```scala
libraryDependencies += "io.methvin.play" %% "autoconfig-macros" % playAutoConfigVersion
```

Replace `playAutoConfigVersion` with the latest version: [![maven central version](https://img.shields.io/maven-central/v/io.methvin.play/autoconfig-macros_2.12.svg)](https://mvnrepository.com/artifact/io.methvin.play/autoconfig-macros)

### Creating configuration classes

It's good practice to create type-safe classes representing your configuration instead of passing around raw `Configuration` or `Config` objects. This way your application components can simply access properties of your class rather than having to read and convert configuration individually. This also makes it easier to create instances for unit testing.

For example, suppose I want to read configuration for a hypothetical API I'm making calls to. Let's assume I need an API key, an API password, and I want to configure the request timeout. So I create a class like this:

```scala
import play.api._
import io.methvin.play.autoconfig._
import scala.concurrent.duration._

case class FooApiConfig(
  apiKey: String,
  apiPassword: String,
  requestTimeout: Duration
)
object FooApiConfig {
  implicit val loader: ConfigLoader[FooApiConfig] = AutoConfig.loader
  def fromConfiguration(conf: Configuration) = conf.get[FooApiConfig]("api.foo")
}
```

The `fromConfiguration` method shows how you'd use the `ConfigLoader` using the `Configuration#get` method. In this case it would read a configuration object that looks like this:

```
api.foo {
  apiKey = "abcdef"
  apiPassword = "secret"
  requestTimeout = 1 minute
}
```

The `loader` macro goes through each parameter of the default constructor and looks for a `ConfigLoader` in scope for that type, then uses the loader to load the key in the object of the same name. In this case Play already provides loaders for `String` and `Duration`. If you had a custom object type nested inside, you could generate a loader in the exact same way.

### Custom naming

The macro also supports custom names, for example:

```scala
import play.api._
import io.methvin.play.autoconfig._
import scala.concurrent.duration._

case class FooApiConfig(
  @ConfigName("api-key") apiKey: String,
  @ConfigName("api-password") apiPassword: String,
  @ConfigName("request-timeout") requestTimeout: Duration
)
object FooApiConfig {
  implicit val loader: ConfigLoader[FooApiConfig] = AutoConfig.loader
  def fromConfiguration(conf: Configuration) = conf.get[FooApiConfig]("api.foo")
}
```

This will change the name of the properties used for each field:

```
api.foo {
  api-key = "abcdef"
  api-password = "secret"
  request-timeout = 1 minute
}
```

### Using an alternate constructor

You can also use an alternate constructor:

```scala
import play.api._
import io.methvin.play.autoconfig._
import scala.concurrent.duration._

case class FooApiConfig(
  apiKey: String,
  apiPassword: String,
  requestTimeout: Duration
) {
  @ConfigConstructor def this(key: String, password: String, timeout: Int) = {
    this(key, password, duration.millis)
  } 
}
```

The field names will be taken from the argument names of the alternate constructor, or their associated `@ConfigName` annotation.

### Binding configuration classes

Once you've written your config class, you'll need to make it available to the components that use it. If you're using compile-time DI, you can write something like this:

```scala
trait FooApiComponents {
  def configuration: Configuration
  lazy val fooApiConfig: FooApiConfig = configuration.get[FooApiConfig]("api.foo")
  lazy val fooApi: FooApi = new FooApi(fooApiConfig)
}
```

If using Guice you can add a `@Provides` method to your module with your config class:

```scala
class ConfigModule extends AbstractModule {
  def configure: Unit = { /* ... */ }

  @Provides def fooApiConfig(conf: Configuration): FooApiConfig =
    conf.get[FooApiConfig]("api.foo")
}
```

## License

This library is licensed under the Apache License, version 2.0. See the LICENSE file for details.
