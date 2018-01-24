# Play AutoConfig

`AutoConfig` is a utility for type-safe configuration in Play (2.6.0 and later). The library provides a convenient macro to generate `ConfigLoader` instances for arbitrary classes.

## Example

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
  @AutoConfig.named("api-key") apiKey: String,
  @AutoConfig.named("api-password") apiPassword: String,
  @AutoConfig.named("request-timeout") requestTimeout: Duration
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
