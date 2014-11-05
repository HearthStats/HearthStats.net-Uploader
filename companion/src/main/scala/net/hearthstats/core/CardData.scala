package net.hearthstats.core

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

@JsonIgnoreProperties(ignoreUnknown = true)
case class CardData(
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

object CardData {
  val mapper = new ObjectMapper with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  lazy val json = mapper.readValue[Map[String, List[CardData]]](getClass.getResourceAsStream("AllSets.json"))

  lazy val allCards = json.values.flatten

  lazy val collectible = allCards.filter(_.collectible == true)
  
  lazy val heroPowers = allCards.filter(_.`type` == "Hero Power") 
}