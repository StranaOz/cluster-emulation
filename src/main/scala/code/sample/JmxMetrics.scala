package code.sample

import com.codahale.metrics.{JmxReporter, MetricRegistry}

trait JmxMetrics {
  val registry = new MetricRegistry()

  private val reporter = JmxReporter.forRegistry(registry).build()
  reporter.start()
}
