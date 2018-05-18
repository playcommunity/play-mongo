package cn.playscala.mongo.reactivestream

/*
 * Copyright 2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.concurrent.atomic.AtomicBoolean
import com.mongodb.async.client.{Observable, Observer, Subscription}
import scala.language.implicitConversions
import org.{reactivestreams => rxStreams}

object Implicits {

  implicit def observableToPublisher[T](observable: Observable[T]): rxStreams.Publisher[T] = ObservableToPublisher(observable)

  case class ObservableToPublisher[T](observable: Observable[T]) extends rxStreams.Publisher[T] {
    def subscribe(subscriber: rxStreams.Subscriber[_ >: T]): Unit = {
      observable.subscribe(
        new Observer[T]() {
          override def onSubscribe(subscription: Subscription): Unit = {
            subscriber.onSubscribe(new rxStreams.Subscription() {
              private final val cancelled: AtomicBoolean = new AtomicBoolean

              def request(n: Long) {
                if (!subscription.isUnsubscribed && n < 1) {
                  subscriber.onError(new IllegalArgumentException(
                    """3.9 While the Subscription is not cancelled,
                      |Subscription.request(long n) MUST throw a java.lang.IllegalArgumentException if the
                      |argument is <= 0.""".stripMargin
                  ))
                } else {
                  subscription.request(n)
                }
              }

              def cancel() {
                if (!cancelled.getAndSet(true)) subscription.unsubscribe()
              }
            })
          }

          def onNext(result: T): Unit = subscriber.onNext(result)

          def onError(e: Throwable): Unit = subscriber.onError(e)

          def onComplete(): Unit = subscriber.onComplete()
        }
      )
    }
  }

}
