package net.hearthstats.core

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

object CardParserJsonMain extends App {
  val mapper = new ObjectMapper with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  val json = mapper.readValue[Map[String, List[CardJson]]](getClass.getResourceAsStream("AllSets.json"))
  json.values.flatten
    .filter {
      case c => c.playerClass == Some("Rogue") && Option(c.mechanics).getOrElse(Nil).contains("Deathrattle") 
    }.
    foreach(println)
}

@JsonIgnoreProperties(ignoreUnknown = true)
case class CardJson(
  id: String,
  `type`: String,
  cost: Int,
  attack: Int,
  health: Int,
  collectible: Boolean,
  text: String,
  playerClass: Option[String],
  flavor: String,
  rarity: String,
  name: String,
  mechanics: List[String])

