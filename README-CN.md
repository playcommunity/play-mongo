# Play Mongo 介绍
Play Mongo 是一个专门为 [Play Framework](https://www.playframework.com/) 开发的 Mongodb 模块, 旨在为 Play Framework 提供一种简洁的 Mongodb 访问方式。
该项目基于 Mongodb 官方的 [Mongodb Scala Driver](https://github.com/mongodb/mongo-scala-driver) 开发，并且提供了更多实用功能，例如，

- 更简洁多样的数据库交方式
- 自动识别模型类(Model)，自动编解码
- 自动完成 JsValue 和 BsonValue 互转
- 更方便的 GridFS 交互
- Change Stream 转 Akka Stream.
- 支持关联查询(Relationship Query)

# 快速起步
打开`build.sbt`，添加如下依赖,
```
libraryDependencies += "cn.playscala" % "play-mongo_2.12" % "0.2.0"
```
打开 `conf/application.conf`, 添加数据库连接，
```
mongodb.uri = "mongodb://user:password@host:port/dbName?authMode=scram-sha1"
```
然后在应用启动时设置模型类(models)的包路径，编辑`app/Module`类，
```
class Module extends AbstractModule {
  override def configure() = {
    Mongo.setModelsPackage("models")
  }
}
```
`Mongo.setModelsPackage`方法将会查找指定包路径下的所有`case class`，自动生成相应编解码器，并添加至驱动中。
至此便可以将 `Mongo` 实例注入到任意需要的地方，
```
@Singleton
class Application @Inject()(cc: ControllerComponents, mongo: Mongo) extends AbstractController(cc) {}
```

# 为 Play Json 提供的隐式方法
借助 Play Json 提供的`Json.format`宏，我们可以很方便地为 case class 提供隐式的Reads和Writes，
```
import models._
import play.api.libs.json.Format
package object models {
  implicit val emailFormat = Json.format[Email]
  implicit val personFormat = Json.format[Person]
  ...
  implicit val addressFormat = Json.format[Address]
}
```
通常每当我们在models包创建一个新的 case class，就需要在这里添加一个相应的隐式 Format 对象。编写这些样板代码是很枯燥无味的，为此我们实现了一个 implicit macro，
只需要一行代码，便可以为所有的 case class 生成隐式的Reads和Writes，
```
import scala.language.experimental.macros
import play.api.libs.json.Format
import cn.playscala.mongo.codecs.macrocodecs.JsonFormatMacro

package object models {
  implicit def formats[T <: Product]: Format[T] = macro JsonFormatMacro.materializeJsonFormat[T]
}
```
需要注意的是，该隐式方法需要定义在 package object 下，例如当定义在 `package object models` 下时，该隐式方法将会对 models 包下所有的 case class 生效。

# Model and Collection
Model 类使用 `@Entity` 注解标注， 一个 model 实例表示 mongodb collection 中的一个文档, 一个 mongodb collection 在概念上类似于关系数据库的一张表。
```
@Entity("common-user")
case class User(_id: String, name: String, password: String, addTime: Instant)
```
`@Entity` 注解参数用于指定关联的 mongodb collection 名称, 如果未指定，则默认为 Model 类名称。 
作为约定，Model 类使用 `_id` 字段作为唯一标识， 该字段同时也是 mongodb collection 的默认主键。

我们可以通过两种方式访问 mongodb collection, 第一种方式是使用 model 类,
```
mongo.find[User]().list().map{ users => ... }
```
这里的参数类型 `User` 不仅用于指定关联的 mongodb collection, 而且用于指明返回的结果类型。 这意味着查询操作将会在 `common-user` collection 上执行, 
并且返回的结果类型是 `User`。 需要注意的是，在该方式下无法改变返回的结果类型。

第二种方式是使用 `mongo.getCollection` 方法,
```
mongo.collection("common-user").find[User]().list().map{ users => }
```
在这里， `find` 方法上的参数类型 `User` 仅仅用于指定返回的结果类型, 我们可以通过更改该参数类型设置不同的返回结果类型，
```
mongo.collection("common-user").find[JsObject]().list().map{ jsObjList => }
mongo.collection("common-user").find[CommonUser](Json.obj("userType" -> "common")).list().map{ commonUsers => }
```
当然，我们也可以使用 model 类指定关联的 mongodb collection，
```
mongo.collection[User].find[User]().list().map{ user => }
```
第1个参数类型 `User` 用于指定关联的 mongodb collection, 第2个参数类型 `User` 用于指定返回的结果类型。 我们仍然可以通过改变第2个参数类型从而改变返回的结果类型。

# 常用操作
> 以下示例代码默认执行了 `import play.api.libs.json.Json._` 导入， 所以 `Json.obj()` 可以被简写为 `obj()` 。
## Create
```
// 插入 Model
mongo.insert[User](User("0", "joymufeng", "123456", Instant.now))

// 插入 Json
val jsObj = obj("_id" -> "0", "name" -> "joymufeng", "password" -> "123456", "addTime" -> Instant.now)
mongo.collection[User].insert(jsObj)
mongo.collection("common-user").insert(jsObj)
```

## Update
```
mongo.updateById[User]("0", obj("$set" -> obj("password" -> "123321")))
mongo.updateOne[User](obj("_id" -> "0"), obj("$set" -> obj("password" -> "123321")))

mongo.collection[User].updateById("0", obj("$set" -> obj("password" -> "123321")))
mongo.collection[User].updateOne(obj("_id" -> "0"), obj("$set" -> obj("password" -> "123321")))

mongo.collection("common-user").updateById("0", obj("$set" -> obj("password" -> "123321")))
mongo.collection("common-user").updateOne(obj("_id" -> "0"), obj("$set" -> obj("password" -> "123321")))
```

## Query
```
mongo.findById[User]("0") // Future[Option[User]]
mongo.find[User](obj("_id" -> "0")).first // Future[Option[User]]

mongo.collection[User].findById[User]("0") // Future[Option[User]]
mongo.collection[User].find[User](obj("_id" -> "0")).first // Future[Option[User]]

mongo.collection[User].findById[JsObject]("0") // Future[Option[JsObject]]
mongo.collection[User].find[JsObject](obj("_id" -> "0")).first // Future[Option[JsObject]]

mongo.collection("common-user").findById[User]("0") // Future[Option[User]]
mongo.collection("common-user").find[User](obj("_id" -> "0")).first // Future[Option[User]]

mongo.collection("common-user").findById[JsObject]("0") // Future[Option[JsObject]]
mongo.collection("common-user").find[JsObject](obj("_id" -> "0")).first // Future[Option[JsObject]]
```

## Delete
```
mongo.deleteById[User]("0")
mongo.deleteOne[User](obj("_id" -> "0"))

mongo.collection[User].deleteById("0")
mongo.collection[User].deleteOne(obj("_id" -> "0"))

mongo.collection("common-user").deleteById("0")
mongo.collection("common-user").deleteOne(obj("_id" -> "0"))
```

## Upload and Download Files
```
// Upload and get the fileId
mongo.gridFSBucket.uploadFromFile("image.jpg", "image/jpg", new File("./image.jpg")).map{ fileId =>
  Ok(fileId)
}

// Download file by fileId
mongo.gridFSBucket.findById("5b1183fed3ba643a3826325f").map{
  case Some(file) =>
    Ok.chunked(file.stream.toSource)
      .as(file.getContentType)
  case None =>
    NotFound
}
```

## Change Stream
我们可以通过 `toSource` 方法将 Change Stream 转换成 Akka Source，之后便会有趣很多。例如下面的代码拥有如下几个功能：
- 将从 Change Stream 接收到的元素进行缓冲，以方便批处理，当满足其中一个条件时便结束缓冲向后传递：
  - 缓冲满10个元素
  - 缓冲时间超过了1000毫秒
- 对缓冲后的元素进行流控，每秒只允许通过1个元素
```
mongo
  .collection[User]
  .watch()
  .fullDocument
  .toSource
  .groupedWithin(10, 1000.millis)
  .throttle(elements = 1, per = 1.second, maximumBurst = 1, ThrottleMode.shaping)
  .runForeach{ seq => 
    // ...
  }
```

## Relationship Query
```
@Entity("common-article")
case class Article(_id: String, title: String, content: String, authorId: String)

@Entity("common-author")
case class Author(_id: String, name: String)

mongo.find[Article].fetch[Author]("authorId").list().map{ _.map{ t =>
    val (article, author) = t
  }
}
```
对于满足查询条件的每一个 article , 将会根据匹配条件 `article.authorId == author._id` 拉取关联的 author。

## Class, Json 和 Bson
在处理 Json 时要格外小心，因为 Json 使用 `JsNumber` 表示所有数值类型，但是 Bson 拥有更加丰富的数值类型，这导致了 Json 和 Bson 之间的转换过程是不可逆的，因为双方的类型信息并不对称。
下面我们仔细分析常见的几个场景。在讨论中将会用到如下的 Model 定义：
```
@Entity("common-user")
case class User(_id: String, name: String, setting: UserSetting)
case class UserSetting(gender: String, age: Int)
```
### Class -> Bson
我们经常使用下面代码将一个 Model 类实例插入 mongodb ，
```
mongo.insert[User].insert(User("0", "joymufeng", UserSetting("male", 32)))
```
在调用底层驱动的插入操作之前，需要先将 `User` 转换成 `Bson` 。这个转换过程是可逆的，当从 mongodb 读取数据时，可以成功地将 `Bson` 转换回 `User`。

### Json -> Bson
当使用 Json DSL 构建一个 `JsObject` 对象时，所有的数值类型(例如 Byte, Short, Int, Long, Float 和 Double)均会被转换成 `JsNumber` 类型(内部使用`BigDecimal`存储数据)，数值的具体类型在这个转换过程中丢失了。
在调用底层驱动程序前，`Json` 将会被转换为 `Bson`，`JsNumber` 将会被转换为 `BsonDecimal128`。当从数据库读取写入的数据时，我们没办法恢复已经丢失的数值类型信息。
例如我们通常会执行如下更新操作，
```
mongo.update[User](obj("_id" -> "0"), obj("$set" -> UserSetting("male", 18)))
// Or
mongo.update[User](obj("_id" -> "0"), obj("$set" -> obj("setting" -> obj("gender" -> "male", "age" -> 18))))
```
不管是 `UserSetting("male", 32)`， 还是 `obj("gender" -> "male", "age" -> 18)` 最终都会被转换为 `obj("gender" -> JsString("male"), "age" -> JsNumber(BigDecimal(18))`。
所以，在更新操作执行完成后， `user.setting.age` 字段在数据库中的类型为 `NumberDecimal`, 当执行读取操作时便会发生类型转换错误,
```
mongo.findById[User]("0")
// [BsonInvalidOperationException: Invalid numeric type, found: DECIMAL128]
```
当试图将 `BigDecimal` 转换为 `Int` 时出错了. 因为在这个转换过程中会导致数值精度丢失。
为了解决这个问题, 我们在转换 `JsNumber` 时尽量将其转换为较窄的数值类型，以保证其可以被安全地转换回来。
例如 `obj("age" -> JsNumber(18.0))` 会被转换为 `BsonDocument("age", BsonInt32(18))`。



