package akka.persistence.couchbase

import akka.actor.ActorSystem
import com.couchbase.client.java.env.CouchbaseEnvironment
import com.couchbase.client.java.view.Stale
import com.couchbase.client.java.{Bucket, Cluster, CouchbaseCluster}
import com.typesafe.config.Config

trait CouchbasePluginConfig {
  def nodes: java.util.List[String]

  def bucketName: String

  def bucketPassword: Option[String]
}

abstract class DefaultCouchbasePluginConfig(config: Config) extends CouchbasePluginConfig {

  private val bucketConfig = config.getConfig("bucket")

  override val nodes = bucketConfig.getStringList("nodes")
  override val bucketName = bucketConfig.getString("bucket")
  override val bucketPassword = Some(bucketConfig.getString("password")).filter(_.nonEmpty)

  private[couchbase] def createCluster(environment: CouchbaseEnvironment): Cluster = {
    CouchbaseCluster.create(environment, nodes)
  }

  private[couchbase] def openBucket(cluster: Cluster): Bucket = {
    cluster.openBucket(bucketName, bucketPassword.orNull)
  }
}

trait CouchbaseJournalConfig extends CouchbasePluginConfig {

  def stale: Stale

  def replayDispatcherId: String

  def maxMessageBatchSize: Int

}

class DefaultCouchbaseJournalConfig(config: Config)
  extends DefaultCouchbasePluginConfig(config)
  with CouchbaseJournalConfig {

  override val stale = config.getString("stale") match {
    case "false" => Stale.FALSE
    case "ok" => Stale.TRUE
    case "update_after" => Stale.UPDATE_AFTER
  }

  override val replayDispatcherId = config.getString("replay-dispatcher")

  override val maxMessageBatchSize = config.getInt("max-message-batch-size")
}

object CouchbaseJournalConfig {
  def apply(system: ActorSystem) = {
    new DefaultCouchbaseJournalConfig(system.settings.config.getConfig("couchbase-journal"))
  }
}

trait CouchbaseSnapshotStoreConfig extends CouchbasePluginConfig

class DefaultCouchbaseSnapshotStoreConfig(config: Config)
  extends DefaultCouchbasePluginConfig(config)
  with CouchbaseSnapshotStoreConfig

object CouchbaseSnapshotStoreConfig {
  def apply(system: ActorSystem) = {
    new DefaultCouchbaseSnapshotStoreConfig(system.settings.config.getConfig("couchbase-snapshot-store"))
  }
}