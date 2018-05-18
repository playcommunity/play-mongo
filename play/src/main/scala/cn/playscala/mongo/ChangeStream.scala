package cn.playscala.mongo

import akka.stream.scaladsl.Source
import cn.playscala.mongo.reactivestream.Implicits.ObservableToPublisher
import com.mongodb.async.client.{ChangeStreamIterable, Observable, Observables}
import com.mongodb.client.model.changestream.{ChangeStreamDocument, FullDocument}
import org.reactivestreams.Publisher

case class ChangeStream[TResult](wrapped: ChangeStreamIterable[TResult]) {

  def fullDocument: ChangeStream[TResult] = ChangeStream(wrapped.fullDocument(FullDocument.UPDATE_LOOKUP))

  def toObservable: Observable[ChangeStreamDocument[TResult]] = Observables.observe(wrapped)

  def toPublisher: Publisher[ChangeStreamDocument[TResult]] = ObservableToPublisher(toObservable)

  def toSource: Source[ChangeStreamDocument[TResult], _] = Source.fromPublisher(toPublisher)

}
