package tasks

import play.api.inject.SimpleModule
import play.api.inject._
import models.opensky.Scheduler

class TasksModule extends SimpleModule(bind[Scheduler].toSelf.eagerly())