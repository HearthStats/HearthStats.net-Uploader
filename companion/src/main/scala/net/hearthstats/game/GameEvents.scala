package net.hearthstats.game

import java.awt.image.BufferedImage
import net.hearthstats.core.Card
import net.hearthstats.game.CardEventType._
import net.hearthstats.core.HeroClass
import net.hearthstats.core.GameMode
import net.hearthstats.core.CardData
import net.hearthstats.core.MatchOutcome

sealed trait GameEvent
sealed trait HeroEvent extends GameEvent

case object StartupEvent extends GameEvent

case class GameModeDetected(mode: GameMode) extends GameEvent

case class FirstPlayer(name: String, id: Int) extends GameEvent
case class PlayerName(name: String, id: Int) extends GameEvent

case object TurnPassedEvent extends GameEvent
case class HeroPowerEvent(cardCode: String, hero: Int) extends GameEvent with NamedCard {
  override def toString =
    if (isValid) s"hero $hero uses: $cardName"
    else ""

  def isValid = CardData.heroPowers.exists(_.id == cardCode)
}
case class HeroPowerDeclared(cardCode: String, hero: Int) extends GameEvent with NamedCard {
  override def toString =
    s"hero $hero has power : $cardName"
}

case class CardEvent(cardCode: String, cardId: Int, eventType: CardEventType, player: Int) extends GameEvent with NamedCard {
  override def toString =
    s"player$player : $eventType $cardName"
}

trait NamedCard {
  def cardCode: String

  lazy val cardName =
    if (cardCode == "") "Unknown card"
    else CardData.byId(cardCode).name
}

object CardEvents {
  def CardAddedToDeck(card: String, cardId: Int, player: Int) = CardEvent(card, cardId, ADDED_TO_DECK, player: Int)
  def CardPlayed(card: String, cardId: Int, player: Int) = CardEvent(card, cardId, PLAYED, player: Int)
  def CardReturned(card: String, cardId: Int, player: Int) = CardEvent(card, cardId, RETURNED, player: Int)
  def CardDrawn(card: String, cardId: Int, player: Int) = CardEvent(card, cardId, DRAWN, player: Int)
  def CardReplaced(card: String, cardId: Int, player: Int) = CardEvent(card, cardId, REPLACED, player: Int)
  def CardDiscarded(card: String, cardId: Int, player: Int) = CardEvent(card, cardId, DISCARDED, player: Int)
  def CardPutInPlay(card: String, cardId: Int, player: Int) = CardEvent(card, cardId, PUT_IN_PLAY, player: Int)
  def CardReceived(card: String, cardId: Int, player: Int) = CardEvent(card, cardId, RECEIVED, player: Int)
  def CardDestroyed(card: String, cardId: Int, player: Int) = CardEvent(card, cardId, DESTROYED, player: Int)
}

case class HeroDestroyedEvent(opponent: Boolean) extends HeroEvent
case class HeroChosen(hero: String, heroClass: HeroClass, opponent: Boolean, player: Int) extends HeroEvent

case class MatchStart(heroChosen: HeroChosen) extends GameEvent
case class GameOver(outcome: MatchOutcome) extends GameEvent
case class LegendRank(rank: Int) extends GameEvent

case class ScreenEvent(screen: HsScreen, image: BufferedImage) extends GameEvent {
  override def toString = s"ScreenEvent($screen)"
}

sealed trait HsScreen
case object TitleScreen extends HsScreen
case object MainScreen extends HsScreen
case object QuestsScreen extends HsScreen
case object ArenaLobby extends HsScreen
case object ArenaRunEnd extends HsScreen
case object FriendlyLobby extends HsScreen
case object PlayLobby extends HsScreen
case object PracticeLobby extends HsScreen
case object FindingOpponent extends HsScreen
case object MatchStartScreen extends HsScreen
case object StartingHandScreen extends HsScreen
case object OngoingGameScreen extends HsScreen
case object GameResultScreen extends HsScreen
case object CollectionScreen extends HsScreen
case object CollectionDeckScreen extends HsScreen

object GameEvents {
  import net.hearthstats.game.Screen._
  import net.hearthstats.game.ScreenGroup._

  implicit def screenToObject(s: Screen): HsScreen = s match {
    case TITLE => TitleScreen
    case MAIN => MainScreen
    case MAIN_TODAYSQUESTS => QuestsScreen
    case ARENA_LOBBY => ArenaLobby
    case ARENA_END => ArenaRunEnd
    case VERSUS_LOBBY => FriendlyLobby
    case PLAY_LOBBY => PlayLobby
    case PRACTICE_LOBBY => PracticeLobby
    case FINDING_OPPONENT => FindingOpponent
    case MATCH_VS => MatchStartScreen
    case MATCH_STARTINGHAND => StartingHandScreen
    case s if s.group == MATCH_PLAYING => OngoingGameScreen
    case s if s.group == MATCH_END => GameResultScreen
    case COLLECTION_DECK => CollectionDeckScreen
    case COLLECTION => CollectionScreen
    case COLLECTION_ZOOM => CollectionScreen
  }
}