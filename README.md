# Welcome!
For the chinese introduction, refer to [README-CN.md](https://github.com/playcommunity/play-mongo/blob/master/README-CN.md).

# What's play-mongo ?
play-mongo is a mongodb module for [Play Framework](https://www.playframework.com/), aims to introduce a concise way to play with mongodb while developing with Play Framework. 
It's designed based on [Official Mongodb Scala Driver](https://github.com/mongodb/mongo-scala-driver) and bring more pragmatic features, such as:

- A concise way for playing with mongodb
- Auto generate the codecs of case class models
- Auto conversion between JsValue and BsonValue
- More convenient to deal with GridFS
- Methods for converting change stream to akka stream.
- Support relationship query

# Getting Started
Add the following dependency to your `build.sbt`,
```
libraryDependencies += "cn.playscala" % "play-mongo_2.12" % "0.1.0"
```
And append your mongodb connection config to `conf/application.conf`,
```
mongodb.uri = "mongodb://user:password@host:port/dbName?authMode=scram-sha1"
```
Then config where to find your models, the following code should be executed once before application start, 
```
Mongo.setModelsPackage("models")
```
you can put it in the play's default `Module` class which is in the root package,
```
class Module extends AbstractModule {
  override def configure() = {
    Mongo.setModelsPackage("models")
    bind(classOf[InitializeService]).asEagerSingleton
  }
}
```
After all, you can inject `Mongo` instance into where you want. 
```
@Singleton
class Application @Inject()(cc: ControllerComponents, mongo: Mongo) extends AbstractController(cc) {}
```
# Model and Collection
A model class represent a document of the mongodb's collection (a collection is just like the table in relation database), which is a case class annotated with `@Entity` annotation,
```
@Entity("common-user")
case class User(_id: String, name: String, password: String, addTime: Instant)
```
The parameter value of `@Entity` specify the collection name in mongodb, if not specified, default to the class name. 
As a convention, `_id` is used as the identity field of model classes, which is the same as the primary key in the related collection of mongodb.

There are two ways to access the mongodb's collection, the first way is using the model class,
```
mongo.find[User]().list().map{ users => ... }
```
The parameter type `User` here not only specify the related mongodb's collection, but also specify the result type. That means the query will be sent to `common-user` collection, 
and the result type of query is `User`. Notice that, the result type in this way can't be changed.
The second way is using `mongo.getCollection` method,
```
mongo.collection("common-user").find[User]().list().map{ users => }
```
Here, the parameter type `User` in `find` method only specify the result type of query, we can change this parameter type to change the result type,
```
mongo.collection("common-user").find[JsObject]().list().map{ jsObjList => }
mongo.collection("common-user").find[CommonUser](Json.obj("userType" -> "common")).list().map{ commonUsers => }
```
You can also use model class to specify the related mongodb's collection,
```
mongo.collection[User].find[User]().list().map{ user => }
```
The first parameter type `User` specifies the related mongodb's collection, and the second parameter type `User` specifies the result type. You can change the second parameter type 'User' to change the result type.

# Common Operations
> The following codes are supposed to have `import play.api.libs.json.Json._` in current scope. So you can write `obj()` instead of `Json.obj()`.
## Create
```
// Insert Model
mongo.insert[User](User("0", "joymufeng", "123456", Instant.now))

// Insert Json
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
mongo.gridFSBucket.uploadFromFile("kf.jpg", "image/jpg", new File("./kf.jpg")).map{ fileId =>
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
`toSource` method will transform Change Stream to Akka Source, and then things become more funny. For example, we can achieve the following features with several line of codes,
- Buffer the elements of Change Stream for bulk processes, and pass to next step if either of the two conditions is established
  - up to 10 elements
  - exceed to 1000 ms
- Traffic shaping with 1 passed per second  
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
For each article, the related author will be fetched on condition `article.authorId == author._id`.

## Class, Json and Bson
You should be more careful while working with json, because `JsNumber` is used to represent all numeric values in json. On the other side, Bson has more concrete numeric types. 
That means the conversion between Json and Bson is irreversible due to the asymmetry numeric types. Next step, we will analyze some common scenes. 
The following is the model class which will be used.
```
case class User(_id: String, name: String, setting: UserSetting)
case class UserSetting(gender: String, age: Int)
```
### Class -> Bson
We usually insert a model class instance into mongodb like this,
```
mongo.insert[User].insert(User("0", "joymufeng", UserSetting("male", 32)))
```
Before invoking the underlying driver，`User` will be converted to `Bson`, and this conversion is reversible. While read from mongodb, the `Bson` document could be converted to `User` successfully.

### Json -> Bson
While we construct a `JsObject` using Json DSL, all numeric values(such as Byte, Short, Int, Long, Float and Double) will be converted to `JsNumber`(with `BigDecimal` inside), the concrete types of numeric values lost in this conversion.
Before invoking the underlying driver，`Json` will be converted to `Bson`, and `JsNumber` will be converted to `BsonDecimal128`. While read from mongodb, we can't recover the concrete types of the original numeric values. 
For example, we usually writes the following update operations,
```
mongo.updateById[User]("0", obj("$set" -> UserSetting("male", 18)))
// Or
mongo.updateById[User]("0", obj("$set" -> obj("setting" -> obj("gender" -> "male", "age" -> 18))))
```
Both `UserSetting("male", 32)` and `obj("gender" -> "male", "age" -> 18)` will be converted to `obj("gender" -> JsString("male"), "age" -> JsNumber(BigDecimal(18))`.
So after these update operations, the type of `user.setting.age` field in mongodb will be `NumberDecimal`, while read it back, an error occurs,
```
mongo.findById[User]("0")
// [BsonInvalidOperationException: Invalid numeric type, found: DECIMAL128]
```
While try to convert `BigDecimal` to `Int`, an exception is thrown. Because the precision value will be lost in this conversion.
To solve this problem, we will convert JsNumber to a narrow type as much as possible, so it could be safely read back. 
For example, `obj("age" -> JsNumber(18.0))` will be converted to `BsonDocument("age", BsonInt32(18))`.


