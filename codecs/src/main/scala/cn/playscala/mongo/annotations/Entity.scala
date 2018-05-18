package cn.playscala.mongo.annotations

import scala.annotation.StaticAnnotation

/**
 * Annotation to indicate entity class
 *
 * @param collectionName the key for the stored property
 */
case class Entity(collectionName: String) extends StaticAnnotation
