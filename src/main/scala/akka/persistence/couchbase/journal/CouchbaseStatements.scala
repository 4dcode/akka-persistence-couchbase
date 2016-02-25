package akka.persistence.couchbase.journal

import akka.actor.ActorLogging
import com.couchbase.client.java.document.JsonDocument
import com.couchbase.client.java.document.json.{JsonObject, JsonArray}
import com.couchbase.client.java.view._
import play.api.libs.json.Json

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.control.NonFatal

trait CouchbaseStatements {
  self: CouchbaseJournal with ActorLogging =>

  def bySequenceNr(persistenceId: String, from: Long, to: Long) = {
    ViewQuery
      .from("journal", "by_sequenceNr")
      .stale(Stale.FALSE)
      .startKey(JsonArray.from(persistenceId, from.asInstanceOf[AnyRef]))
      .endKey(JsonArray.from(persistenceId, to.asInstanceOf[AnyRef]))
  }

  /**
   * Adds all messages in a single atomically updated batch.
   */
  def executeBatch(messages: Seq[JournalMessage]): Future[Unit] = {
    val batch = JournalMessageBatch.create(messages)
    val keyFuture = nextKey(JournalMessageBatch.Name)

    keyFuture.map { key =>
      try {
        val jsonObject = JsonObject.fromJson(Json.toJson(batch).toString())
        val jsonDocument = JsonDocument.create(key, jsonObject)
        bucket.insert(jsonDocument)
        log.debug("Wrote batch: {}", key)
      } catch {
        case NonFatal(e) => log.error(e, "Writing batch: {}", key)
      }
    }
  }

  /**
   * Generates a new key with the given base name.
   *
   * Couchbase guarantees the key is unique within the cluster.
   */
  def nextKey(name: String): Future[String] = {
    val counterKey = s"counter::$name"

    val counter = bucket.counter(counterKey, 1L, 0L).content()
    Future.successful(s"$name-$counter")
  }

  /**
   * Initializes all design documents.
   */
  def initDesignDocs(): Unit = {
    val journalDesignDocumentJson = Json.obj(
      "views" -> Json.obj(
        "by_sequenceNr" -> Json.obj(
          "map" ->
            """
              |function (doc, meta) {
              |  if (doc.dataType === 'journal-messages') {
              |    var messages = doc.messages;
              |    for (var i = 0, l = messages.length; i < l; i++) {
              |      var message = messages[i];
              |      emit([message.persistenceId, message.sequenceNr], message);
              |    }
              |  }
              |}
            """.stripMargin
        ),
        "by_revision" -> Json.obj(
          "map" ->
            """
              |function (doc, meta) {
              |  if (doc.dataType === 'journal-messages') {
              |    var messages = doc.messages;
              |    for (var i = 0, l = messages.length; i < l; i++) {
              |      var message = messages[i];
              |      emit([parseInt(meta.id.substring(17)), message.persistenceId, message.sequenceNr], message);
              |    }
              |  }
              |}
            """.stripMargin
        )
      )
    )

    try {
      val journalDesignDocumentJsonObject = JsonObject.fromJson(journalDesignDocumentJson.toString())
      val journalDesignDocument = DesignDocument.from("journal", journalDesignDocumentJsonObject)
      bucket.bucketManager.upsertDesignDocument(journalDesignDocument)
    } catch {
      case NonFatal(e) => log.error(e, "Syncing journal design docs")
    }
  }
}
