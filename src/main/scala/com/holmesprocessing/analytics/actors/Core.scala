package com.holmesprocessing.analytics.actors

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import scala.concurrent.duration._

import com.typesafe.config.Config

object Core {
	def props(cfg: Config, analyticEngineManager: ActorRef): Props = { Props(new Core(cfg, analyticEngineManager)) }
}

class Core(cfg: Config, analyticEngineManager: ActorRef) extends Actor with ActorLogging {
	override def preStart(): Unit = log.info("Core started")
	override def postStop(): Unit = log.info("Core stopped")
	override def receive = Actor.emptyBehavior

	// create the scheduler
	val scheduler = context.actorOf(Scheduler.props(analyticEngineManager, cfg.getString("system.servicesPath")))

	// create the WebServer
	val webserver = context.actorOf(WebServer.props(cfg.getConfig("webserver"), scheduler))

	// create the amqp consumer
	val amqpConsumer = context.actorOf(RabbitConsumer.props(cfg.getConfig("amqp"), scheduler))

	// setup periodic tasks
	implicit val executionContext = context.dispatcher
	
	context.system.scheduler.schedule(5 seconds, 5 seconds, scheduler, SchedulerProtocol.Refresh())
}
